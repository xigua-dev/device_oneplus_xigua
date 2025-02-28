/*
 * Copyright (C) 2020 The LineageOS Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.lineageos.settings.freezer;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.media.session.MediaController;
import android.media.session.MediaSessionManager;
import android.media.session.PlaybackState;
import android.os.UserHandle;
import android.util.Log;
import android.content.ComponentName;
import android.util.Slog;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import androidx.preference.PreferenceManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.lineageos.settings.R;

public final class FreezerUtils {
  private static final String TAG = "FreezerUtils";

  private static final String KEY_STOP_SET = "freezer_stop_set";
  private static final String KEY_FREEZE_SET = "freezer_freeze_set";
  private static List<Integer> sFreezeList = null;
  private static Set<String> sStopSet = null;
  public static Set<ComponentName> sDisableSet = null;
  String mCurrentPower = null;

  private SharedPreferences mSharedPrefs;

  FreezerUtils(Context context) {
    mSharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);

    if (sStopSet == null) {
      Set<String> stopSet = mSharedPrefs.getStringSet(KEY_STOP_SET, null);
      if (stopSet == null) sStopSet = new HashSet<>();
      else sStopSet = new HashSet<>(stopSet);
    }
    
    if (sFreezeList == null) {
      Set<String> freezeSet = mSharedPrefs.getStringSet(KEY_FREEZE_SET, null);
      if (freezeSet == null) sFreezeList = new ArrayList<>();
      else sFreezeList = new ArrayList<>(freezeSet.stream().map(Integer::valueOf).toList());
    }

    if (sDisableSet == null) {
      sDisableSet = Arrays.stream(context.getResources().getStringArray(R.array.freezer_disable_set))
      .map(ComponentName::unflattenFromString)
      .collect(Collectors.toSet());
    }
  }


  void initializeStopSet(Context context) {
    PackageManager pm = context.getPackageManager();

    Set<String> stopSet = mSharedPrefs.getStringSet(KEY_STOP_SET, null);
    if (stopSet == null) {
      Arrays.stream(context.getResources().getStringArray(R.array.freezer_stop_set))
        .forEach( pkg -> checkPackage(pm, pkg, p -> writeStopPackage(p, true)));
    }
  }

  private void checkPackage(PackageManager pm, String pkg, Consumer<String> positive) {
    try {
      if (pm.getPackageInfo(pkg, PackageManager.PackageInfoFlags.of(0)) != null){
        positive.accept(pkg);
      }
    } catch (PackageManager.NameNotFoundException e) { e.printStackTrace(); }
  }

  public static void startService(Context context) {
    context.startServiceAsUser(new Intent(context, FreezerService.class),
      UserHandle.CURRENT);
  }

  public void writeStopPackage(String packageName, boolean enabled) {
    if (enabled) sStopSet.add(packageName); else sStopSet.remove(packageName);
    mSharedPrefs.edit().putStringSet(KEY_STOP_SET, sStopSet).commit();
  }
  public void writeFreezeUid(ActivityManager am, Integer uid, boolean enabled) {
    if (enabled) sFreezeList.add(uid); else sFreezeList.remove(uid);
    if (am != null) am.radicalFreezingList(getRadicalFreezingUids());
    Set<String> uids = sFreezeList.stream().map(String::valueOf).collect(Collectors.toSet());
    Slog.i(TAG, "radicalFreezingList: " + String.join(",", uids));
    mSharedPrefs.edit().putStringSet(KEY_FREEZE_SET, uids).commit();
  }

  public boolean getStopStateForPackage(String packageName) {
    return sStopSet != null && sStopSet.contains(packageName);
  }
  public boolean getFreezeStateForUid(int uid) {
    return sFreezeList != null && sFreezeList.contains(uid);
  }

  public int[] getRadicalFreezingUids() {
    return sFreezeList.stream().mapToInt(Integer::intValue).toArray();
  }

  public void doForceStop(ActivityManager am, MediaSessionManager msm) {
    Log.i(TAG,"doForceStop:" + mCurrentPower);
    List<MediaController> sessions = msm.getActiveSessions(null);
    for (String pkg : sStopSet) {
      boolean toBeStopped = true;

      for (MediaController controller : sessions) {
        int state = controller.getPlaybackState().getState();
        if (pkg.equals(controller.getPackageName())
        && state != PlaybackState.STATE_PAUSED && state != PlaybackState.STATE_STOPPED) {
          toBeStopped = false;

          MediaController.Callback callback = new MediaController.Callback() {
            @Override
            public void onPlaybackStateChanged(PlaybackState state) {
              super.onPlaybackStateChanged(state);

              if (mCurrentPower.equals(Intent.ACTION_SCREEN_ON)){
                controller.unregisterCallback(this);
                Log.i(TAG, "screen is on, ignore stopping: " + pkg);
                return;
              }

              int playState = state.getState();
              if (playState == PlaybackState.STATE_PAUSED || playState == PlaybackState.STATE_STOPPED) {
                controller.unregisterCallback(this);
                am.forceStopPackage(pkg);
                Log.i(TAG, pkg + " was stopped. PlaybackState: " + playState);
              }
            }
          };
          controller.registerCallback(callback);
          Log.i(TAG, pkg + " is in an active media session.");
          break;
        }
      }

      if (toBeStopped) {
        am.forceStopPackage(pkg);
        Log.i(TAG, pkg + " was stopped.");
      }
    }
  }
}
