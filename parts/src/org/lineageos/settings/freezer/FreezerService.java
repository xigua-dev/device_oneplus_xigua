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
import android.app.ActivityTaskManager;
import android.app.ActivityTaskManager.RootTaskInfo;
import android.app.IActivityTaskManager;
import android.app.TaskStackListener;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.media.session.MediaSessionManager;
import android.os.IBinder;
import android.os.Handler;
import android.os.Message;
import android.os.RemoteException;
import android.util.Log;

public class FreezerService extends Service {

    private static final String TAG = "FreezerService";
    private static final boolean DEBUG = false;

    private Handler mHandler;
    private FreezerUtils mFreezerUtils;

    private ActivityManager mActivityManager;
    private MediaSessionManager mMediaSessionManager;

    private String mPreviousApp;
    private PackageManager mPm;

    private static final long FORCE_STOP_DEBOUNCE_DELAY = 300000L;
    private static final long FREEZE_DELAY = 10000L;
    private BroadcastReceiver mIntentReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            mFreezerUtils.mCurrentPower = intent.getAction();
            mHandler.removeMessages(FreezeHandler.DO_FORCE_STOP);
            if (intent.getAction().equals(Intent.ACTION_SCREEN_OFF)) {
                Log.i(TAG, "Enqueue doForceStop in " + FORCE_STOP_DEBOUNCE_DELAY + "ms.");
                mHandler.sendEmptyMessageDelayed(FreezeHandler.DO_FORCE_STOP, FORCE_STOP_DEBOUNCE_DELAY);
            }
        }
    };

    @Override
    public void onCreate() {
        if (DEBUG) Log.d(TAG, "Creating service");
        mHandler = new FreezeHandler();
        mActivityManager = getSystemService(ActivityManager.class);
        mMediaSessionManager = getSystemService(MediaSessionManager.class);
        mPm = getSystemService(PackageManager.class);
        mFreezerUtils = new FreezerUtils(this);
        registerReceiver();

        mActivityManager.radicalFreezingList(mFreezerUtils.getRadicalFreezingUids());
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (DEBUG) Log.d(TAG, "Starting service");
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void registerReceiver() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        filter.addAction(Intent.ACTION_SCREEN_ON);
        this.registerReceiver(mIntentReceiver, filter);
    }

    class FreezeHandler extends Handler {
        private static final int DO_FORCE_STOP = 0xD1E;

        @Override
        public void handleMessage(@androidx.annotation.NonNull Message msg) {
            super.handleMessage(msg);

            switch (msg.what) {
                case DO_FORCE_STOP:
                    mFreezerUtils.doForceStop(mActivityManager, mMediaSessionManager);
                    break;
            }
        }
    }
}
