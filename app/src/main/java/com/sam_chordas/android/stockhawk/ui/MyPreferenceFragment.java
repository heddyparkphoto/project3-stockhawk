package com.sam_chordas.android.stockhawk.ui;

import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;

import com.sam_chordas.android.stockhawk.R;

/**
 * Created by hyeryungpark on 2/22/17.
 */

public class MyPreferenceFragment extends PreferenceFragment
            implements Preference.OnPreferenceChangeListener {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // load the layout
        addPreferencesFromResource(R.xml.pref_main);

        bindPreferenceToValue(findPreference(getString(R.string.pref_historic_key)));
    }

    private void bindPreferenceToValue(Preference preference) {
        preference.setOnPreferenceChangeListener(this);

        // Trigger the listener right away what is saved in the file
        onPreferenceChange(preference,
                PreferenceManager.getDefaultSharedPreferences(
                        preference.getContext()).getString(preference.getKey(), "14")
                );
    }


    @Override
    public boolean onPreferenceChange(Preference preference, Object value) {
        String uiStr = value.toString();

        if (preference instanceof ListPreference){
            ListPreference l = (ListPreference)preference;
            int ix = l.findIndexOfValue(uiStr);
            if (ix >= 0){
                preference.setSummary(l.getEntries()[ix]);
            }
        }

        return true;
    }
}
