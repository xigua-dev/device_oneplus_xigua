/**
 * Copyright (C) 2020 The LineageOS Project
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.lineageos.settings.freezer;

import android.annotation.Nullable;
import android.app.ActivityManager;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.preference.PreferenceFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.ConcatAdapter;

import com.google.android.material.chip.Chip;
import com.google.android.material.divider.MaterialDividerItemDecoration;
import com.android.settingslib.applications.ApplicationsState;

import org.lineageos.settings.R;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class FreezerSettingsFragment extends PreferenceFragment implements ApplicationsState.Callbacks {

    private static final String TAG = "Freezer";

    private static final int HOLDER_TYPE_APP = 0x0;
    private static final int HOLDER_TYPE_INFO = 0x1;

    private UserPackagesAdapter mUserPackagesAdapter;
    private PackageManager mPm;
    private ActivityManager mAm;
    private ApplicationsState mApplicationsState;
    private ApplicationsState.Session mSession;
    private AppFilter mAppFilter;
    private List<String> mUserApps = new ArrayList<>();
    private RecyclerView mAppsRecyclerView;
    private FreezerUtils mFreezerUtils;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mFreezerUtils = new FreezerUtils(getActivity());
        mFreezerUtils.initializeStopSet(getActivity());

        mApplicationsState = ApplicationsState.getInstance(getActivity().getApplication());
        mSession = mApplicationsState.newSession(this);
        mSession.onResume();

        mAm = getContext().getSystemService(ActivityManager.class);
        mPm = getActivity().getPackageManager();
        mAppFilter = new AppFilter(mPm);

        mUserPackagesAdapter = new UserPackagesAdapter();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.freezer_layout, container, false);
    }

    @Override
    public void onViewCreated(final View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mAppsRecyclerView = view.findViewById(R.id.thermal_rv_view);
        mAppsRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        mAppsRecyclerView.setAdapter(mUserPackagesAdapter);
    }


    @Override
    public void onResume() {
        super.onResume();
        getActivity().setTitle(getResources().getString(R.string.freezer_title));
        rebuild();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        mSession.onPause();
        mSession.onDestroy();
    }

    @Override
    public void onPackageListChanged() {
        mAppFilter.updateLauncherInfoList();
        rebuild();
    }

    @Override
    public void onRebuildComplete(ArrayList<ApplicationsState.AppEntry> entries) {
        if (entries != null) {
            handleAppEntries(entries);
            mUserPackagesAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public void onLoadEntriesCompleted() {
        rebuild();
    }

    @Override
    public void onAllSizesComputed() {
    }

    @Override
    public void onLauncherInfoChanged() {
    }

    @Override
    public void onPackageIconChanged() {
    }

    @Override
    public void onPackageSizeChanged(String packageName) {
    }

    @Override
    public void onRunningStateChanged(boolean running) {
    }

    private void handleAppEntries(List<ApplicationsState.AppEntry> entries) {
        List<ApplicationsState.AppEntry> userEntries = entries.stream().filter(appEntry -> mUserApps.stream().anyMatch(p -> p.equals(appEntry.info.packageName))).sorted((e1, e2) -> {
            int a = mFreezerUtils.getStopStateForPackage(e1.info.packageName) ? 0 : 1;
            int b = mFreezerUtils.getStopStateForPackage(e2.info.packageName) ? 0 : 1;
            int c = mFreezerUtils.getFreezeStateForUid(e1.info.uid) ? 0 : 1;
            int d = mFreezerUtils.getFreezeStateForUid(e2.info.uid) ? 0 : 1;
            return a + c - b - d;
        }).collect(Collectors.toList());
        mUserPackagesAdapter.setEntries(userEntries);
    }

    private void rebuild() {
        mSession.rebuild(mAppFilter, ApplicationsState.ALPHA_COMPARATOR);
    }

    private class InfoViewHolder extends RecyclerView.ViewHolder {
        private TextView info;

        private InfoViewHolder(View view) {
            super(view);
            this.info = view.findViewById(R.id.info);

            view.setTag(this);
        }
    }

    private class AppViewHolder extends RecyclerView.ViewHolder {
        private TextView title;
        private TextView status;
        private ImageView icon;
        private View rootView;
        private Chip op1;
        private Chip op2;

        private AppViewHolder(View view) {
            super(view);
            this.title = view.findViewById(R.id.app_name);
            this.status = view.findViewById(R.id.app_status);
            this.icon = view.findViewById(R.id.app_icon);
            this.op1 = view.findViewById(R.id.chip_op1);
            this.op2 = view.findViewById(R.id.chip_op2);
            this.rootView = view;

            view.setTag(this);
        }
    }

    private class UserPackagesAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

        private final View.OnClickListener stopListener = pref -> {
            final ApplicationsState.AppEntry entry = (ApplicationsState.AppEntry) pref.getTag();
            mFreezerUtils.writeStopPackage(entry.info.packageName, ((Chip) pref).isChecked());
            notifyDataSetChanged();
        };
        private final View.OnClickListener freezeListener = pref -> {
            final ApplicationsState.AppEntry entry = (ApplicationsState.AppEntry) pref.getTag();
            mFreezerUtils.writeFreezeUid(mAm, entry.info.uid, ((Chip) pref).isChecked());
            notifyDataSetChanged();
        };

        private List<ApplicationsState.AppEntry> mEntries = new ArrayList<>();

        @Override
        public int getItemCount() {
            return mEntries.size() + 1;
        }

        @Override
        public int getItemViewType(int position) {
            return position == mEntries.size() ? HOLDER_TYPE_INFO : HOLDER_TYPE_APP;
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            if (viewType == HOLDER_TYPE_INFO) {
                InfoViewHolder holder = new InfoViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.freezer_info_item, parent, false));
                return holder;
            }
            AppViewHolder holder = new AppViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.freezer_list_item, parent, false));
            return holder;
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {

            if (holder instanceof InfoViewHolder) {
                ((InfoViewHolder) holder).info.setText(R.string.freezer_info);
                return;
            }

            ApplicationsState.AppEntry entry = mEntries.get(position);

            if (entry == null) {
                return;
            }

            AppViewHolder appViewHolder = (AppViewHolder) holder;

            appViewHolder.op1.setVisibility(View.VISIBLE);
            appViewHolder.op1.setText(R.string.freezer_stop_op1);
            appViewHolder.op1.setOnClickListener(stopListener);
            appViewHolder.op2.setVisibility(View.VISIBLE);
            appViewHolder.op2.setText(R.string.freezer_stop_op2);
            appViewHolder.op2.setOnClickListener(freezeListener);

            appViewHolder.title.setText(entry.label);
            mApplicationsState.ensureIcon(entry);
            appViewHolder.icon.setImageDrawable(entry.icon);

            boolean stopState = mFreezerUtils.getStopStateForPackage(entry.info.packageName);
            appViewHolder.op1.setTag(entry);
            appViewHolder.op1.setChecked(stopState);
            boolean freezeState = mFreezerUtils.getFreezeStateForUid(entry.info.uid);
            //appViewHolder.status.setText(freezeState ? R.string.freezer_stop_enabled : R.string.freezer_stop_disabled);
            appViewHolder.op2.setTag(entry);
            appViewHolder.op2.setChecked(freezeState);
        }

        private void setEntries(List<ApplicationsState.AppEntry> entries) {
            mEntries = entries;
            notifyDataSetChanged();
        }
    }

    private class AppFilter implements ApplicationsState.AppFilter {

        private final PackageManager mPackageManager;
        private final List<String> mPackageRequired = new ArrayList<String>();

        private AppFilter(PackageManager packageManager) {
            this.mPackageManager = packageManager;

            updateLauncherInfoList();
        }

        public void updateLauncherInfoList() {
            List<PackageInfo> packages = mPackageManager.getInstalledPackages(0);

            synchronized (mPackageRequired) {
                mPackageRequired.clear();
                mUserApps.clear();

                packages.forEach(p -> {
                    boolean c = mFreezerUtils.sDisableSet.stream().anyMatch(cn -> cn.getPackageName().equals(p.packageName));

                    if ((p.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) == 0) {
                        mPackageRequired.add(p.packageName);
                        mUserApps.add(p.packageName);
                    } else if (c) mPackageRequired.add(p.packageName);
                });
            }
        }

        @Override
        public void init() {
        }

        @Override
        public boolean filterApp(ApplicationsState.AppEntry entry) {
            boolean show = !mUserPackagesAdapter.mEntries.contains(entry.info.packageName);
            if (show) {
                synchronized (mPackageRequired) {
                    show = mPackageRequired.contains(entry.info.packageName);
                }
            }
            return show;
        }
    }
}
