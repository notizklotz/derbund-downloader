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

package com.github.notizklotz.derbunddownloader.settings;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import org.androidannotations.annotations.EBean;
import org.androidannotations.annotations.RootContext;

@EBean(scope = EBean.Scope.Singleton)
public class SettingsImpl implements Settings {

    @RootContext
    Context context;

    @Override
    public String getUsername() {
        return getDefaultSharedPreferences().getString(SettingsImpl.KEY_USERNAME, null);
    }


    @Override
    public String getPassword() {
        return getDefaultSharedPreferences().getString(SettingsImpl.KEY_PASSWORD, null);
    }

    @Override
    public boolean isWifiOnly() {
        return getDefaultSharedPreferences().getBoolean(Settings.KEY_WIFI_ONLY_ENABLED, true);
    }

    @Override
    public boolean isAutoDownloadEnabled() {
        return getDefaultSharedPreferences().getBoolean(SettingsImpl.KEY_AUTO_DOWNLOAD_ENABLED, false);
    }

    @Override
    public void setLastWakeup(String s) {
        getDefaultSharedPreferences().edit().putString(Settings.KEY_LAST_WAKEUP, s).apply();
    }

    private SharedPreferences getDefaultSharedPreferences() {
        return PreferenceManager.getDefaultSharedPreferences(context);
    }
}
