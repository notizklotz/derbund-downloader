/*
 * Der Bund ePaper Downloader - App to download ePaper issues of the Der Bund newspaper
 * Copyright (C) 2013 Adrian Gygax
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see {http://www.gnu.org/licenses/}.
 */

package com.github.notizklotz.derbunddownloader.ui;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.support.v7.app.AppCompatActivity;

import com.github.notizklotz.derbunddownloader.DerBundDownloaderApplication;
import com.github.notizklotz.derbunddownloader.R;
import com.github.notizklotz.derbunddownloader.download.AutomaticDownloadScheduler;
import com.github.notizklotz.derbunddownloader.settings.Settings;
import com.github.notizklotz.derbunddownloader.settings.SettingsImpl;

import javax.inject.Inject;

public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_settings);

        // Display the fragment as the main content.
        getFragmentManager().beginTransaction()
                .replace(R.id.container, new SettingsFragment())
                .commit();
    }

    public static class SettingsFragment extends PreferenceFragment implements SharedPreferences.OnSharedPreferenceChangeListener {

        @Inject
        Settings settings;

        @Inject
        AutomaticDownloadScheduler automaticDownloadScheduler;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            addPreferencesFromResource(R.xml.preferences);

            ((DerBundDownloaderApplication) getActivity().getApplication()).getUiComponent().inject(this);

            ((PreferenceCategory)getPreferenceScreen().getPreference(0)).getPreference(0).setIntent(new Intent(this.getActivity(), LoginActivity.class));

            updateSummaries(getPreferenceScreen().getSharedPreferences());
        }

        @Override
        public void onResume() {
            super.onResume();
            SharedPreferences sharedPreferences = getPreferenceScreen().getSharedPreferences();
            assert sharedPreferences != null;
            sharedPreferences.registerOnSharedPreferenceChangeListener(this);
        }

        @Override
        public void onPause() {
            super.onPause();
            SharedPreferences sharedPreferences = getPreferenceScreen().getSharedPreferences();
            assert sharedPreferences != null;
            sharedPreferences.unregisterOnSharedPreferenceChangeListener(this);
        }

        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            updateSummaries(sharedPreferences);

            if (SettingsImpl.KEY_AUTO_DOWNLOAD_ENABLED.equals(key)) {
                updateAutomaticDownloadScheduler();
            }
        }

        void updateAutomaticDownloadScheduler() {
            new AsyncTask<Void, Void, Void>() {
                @Override
                protected Void doInBackground(Void... params) {
                    automaticDownloadScheduler.update();
                    return null;
                }
            }.execute();
        }

        private void updateSummaries(SharedPreferences sharedPreferences) {
            updateLastWakeup(sharedPreferences);
        }

        private void updateLastWakeup(SharedPreferences sharedPreferences) {
            getPreferenceScreen().findPreference(SettingsImpl.KEY_LAST_WAKEUP).setSummary(sharedPreferences.getString(SettingsImpl.KEY_LAST_WAKEUP,
                    this.getString(R.string.last_wakeup_never)));
        }

    }
}