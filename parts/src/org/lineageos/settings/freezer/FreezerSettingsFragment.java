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
import android.content.ComponentName;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
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
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class FreezerSettingsFragment extends PreferenceFragment implements ApplicationsState.Callbacks {

    private static final String TAG = "Freezer";

    private ConcatAdapter mRVAdapter;
    private UserPackagesAdapter mUserPackagesAdapter;
    private ExtraComponentsAdapter mExtraAdapter;
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
        mExtraAdapter = new ExtraComponentsAdapter();

        mRVAdapter = new ConcatAdapter(new ConcatAdapter.Config.Builder().setIsolateViewTypes(false).build(), mUserPackagesAdapter, mExtraAdapter);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.freezer_layout, container, false);
    }

    @Override
    public void onViewCreated(final View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mAppsRecyclerView = view.findViewById(R.id.thermal_rv_view);
        // MaterialDividerItemDecoration divider = new MaterialDividerItemDecoration(getContext(), LinearLayoutManager.VERTICAL);
        // divider.setDividerInsetStartResource(getContext(), R.dimen.inset_divider);
        // divider.setDividerInsetEndResource(getContext(), R.dimen.inset_divider);
        // divider.setLastItemDecorated(false);
        // mAppsRecyclerView.addItemDecoration(divider);
        mAppsRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        mAppsRecyclerView.setAdapter(mRVAdapter);
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

        List<ApplicationsState.AppEntry> extraEntries = entries.stream().filter(appEntry -> mFreezerUtils.sDisableSet.stream().anyMatch(p -> p.getPackageName().equals(appEntry.info.packageName))).collect(Collectors.toList());
        mExtraAdapter.setEntries(extraEntries);
    }

    private void rebuild() {
        mSession.rebuild(mAppFilter, ApplicationsState.ALPHA_COMPARATOR);
    }

    private class CategoryViewHolder extends RecyclerView.ViewHolder {
        private TextView title;

        private CategoryViewHolder(View view) {
            super(view);
            this.title = view.findViewById(R.id.category_name);

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

        private View.OnClickListener stopListener = pref -> {
            final ApplicationsState.AppEntry entry = (ApplicationsState.AppEntry) pref.getTag();
            mFreezerUtils.writeStopPackage(entry.info.packageName, ((Chip) pref).isChecked());
            notifyDataSetChanged();
        };
        private View.OnClickListener freezeListener = pref -> {
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
            return position;
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            if (viewType == 0) {
                CategoryViewHolder holder = new CategoryViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.freezer_category_item, parent, false));
                return holder;
            }
            AppViewHolder holder = new AppViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.freezer_list_item, parent, false));
            return holder;
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {

            if (holder instanceof CategoryViewHolder) {
                ((CategoryViewHolder) holder).title.setText(R.string.freezer_category_stop);
                return;
            }

            ApplicationsState.AppEntry entry = mEntries.get(position - 1);

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
            appViewHolder.rootView.setOnClickListener(v -> appViewHolder.op1.performClick());
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

    private class ExtraComponentsAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> implements View.OnClickListener {

        private List<ApplicationsState.AppEntry> mEntries = new ArrayList<>();
        private List<ComponentName> mSortedCN = new ArrayList<>(mFreezerUtils.sDisableSet);

        @Override
        public int getItemCount() {
            return mSortedCN.size() + 1;
        }

        @Override
        public int getItemViewType(int position) {
            return position;
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            if (viewType == 0) {
                CategoryViewHolder holder = new CategoryViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.freezer_category_item, parent, false));
                return holder;
            }
            AppViewHolder holder = new AppViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.freezer_list_item, parent, false));
            return holder;
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            if (holder instanceof CategoryViewHolder) {
                ((CategoryViewHolder) holder).title.setText(R.string.freezer_category_freeze);
                return;
            }

            ComponentName comp = mSortedCN.get(position - 1);

            if (comp == null) {
                return;
            }

            AppViewHolder appViewHolder = (AppViewHolder) holder;

            appViewHolder.op1.setVisibility(View.VISIBLE);
            appViewHolder.op1.setText(R.string.freezer_disable_op1);
            appViewHolder.op1.setOnClickListener(this);
            appViewHolder.op2.setVisibility(View.GONE);

            appViewHolder.title.setText(comp.flattenToShortString());
            appViewHolder.rootView.setOnClickListener(v -> appViewHolder.op1.performClick());

            Optional<ApplicationsState.AppEntry> entry = mEntries.stream().filter(p -> p.info.packageName.equals(comp.getPackageName())).findAny();
            if (entry.isPresent()) {
                ApplicationsState.AppEntry appEntry = entry.get();
                mApplicationsState.ensureIcon(appEntry);
                appViewHolder.icon.setImageDrawable(appEntry.icon);
            }

            try {
                boolean disabled = mPm.getComponentEnabledSetting(comp) == PackageManager.COMPONENT_ENABLED_STATE_DISABLED;
                appViewHolder.status.setText(disabled ? R.string.freezer_disable_true : R.string.freezer_disable_false);
                appViewHolder.op1.setTag(comp);
                appViewHolder.op1.setChecked(disabled);
            } catch (Exception e) {
                appViewHolder.title.setEnabled(false);
                appViewHolder.status.setEnabled(false);
                appViewHolder.icon.setEnabled(false);
                appViewHolder.rootView.setEnabled(false);
                appViewHolder.op1.setEnabled(false);
                Log.e(TAG, e.getMessage());
            }
        }

        @Override
        public void onClick(View pref) {
            final ComponentName component = (ComponentName) pref.getTag();
            mPm.setComponentEnabledSetting(component, ((Chip) pref).isChecked() ? PackageManager.COMPONENT_ENABLED_STATE_DISABLED : PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.SYNCHRONOUS);
            notifyDataSetChanged();
        }

        private void setEntries(List<ApplicationsState.AppEntry> entries) {
            mEntries = entries;
            mSortedCN = mFreezerUtils.sDisableSet.stream().sorted((e1, e2) -> {
                try {
                    int a = mPm.getComponentEnabledSetting(e1) == PackageManager.COMPONENT_ENABLED_STATE_DISABLED ? 0 : 1;
                    int b = mPm.getComponentEnabledSetting(e2) == PackageManager.COMPONENT_ENABLED_STATE_DISABLED ? 0 : 1;
                    return a - b;
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return 0;
            }).collect(Collectors.toList());
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
