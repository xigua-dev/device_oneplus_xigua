/*
 * Copyright (C) 2024 The LineageOS Project
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

package org.lineageos.settings.dimming;

import android.os.Bundle;
import android.os.SystemProperties;

import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.Preference.OnPreferenceChangeListener;
import androidx.preference.PreferenceFragment;

import org.lineageos.settings.R;


public class DimmingSettingsFragment
        extends PreferenceFragment implements OnPreferenceChangeListener {
    private static final String KEY_DIMMING = "dimming";

    private static final String PROP_1EM_PULSE = "persist.xigua.1em_pulse";
    private static final String VALUE_DIMMING_1EM = "1";
    private static final String VALUE_DIMMING_3EM = "0";


    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        addPreferencesFromResource(R.xml.dimming);

        ListPreference dimmingOps = (ListPreference) findPreference(KEY_DIMMING);
        String setting = SystemProperties.get(PROP_1EM_PULSE, VALUE_DIMMING_3EM);
        dimmingOps.setValue(setting.equals(VALUE_DIMMING_1EM) ? VALUE_DIMMING_1EM : VALUE_DIMMING_3EM);
        dimmingOps.setOnPreferenceChangeListener(this);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        try {
            SystemProperties.set(PROP_1EM_PULSE, (String) newValue);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }
}
