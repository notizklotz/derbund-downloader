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

import com.github.notizklotz.derbunddownloader.common.DateHandlingUtils;

import org.androidannotations.annotations.EBean;
import org.androidannotations.annotations.RootContext;

import java.util.Date;

@EBean(scope = EBean.Scope.Singleton)
public class Settings implements SettingsService {

    @RootContext
    Context context;

    @Override
    public String getUsername() {
        return getDefaultSharedPreferences().getString(Settings.KEY_USERNAME, null);
    }


    @Override
    public String getPassword() {
        return getDefaultSharedPreferences().getString(Settings.KEY_PASSWORD, null);
    }

    @Override
    public boolean isWifiOnly() {
        return getDefaultSharedPreferences().getBoolean(SettingsService.KEY_WIFI_ONLY_ENABLED, true);
    }

    @Override
    public boolean isAutoDownloadEnabled() {
        return getDefaultSharedPreferences().getBoolean(Settings.KEY_AUTO_DOWNLOAD_ENABLED, false);
    }

    @Override
    public String getAutoDownloadTime() {
        return getDefaultSharedPreferences().getString(Settings.KEY_AUTO_DOWNLOAD_TIME, null);
    }

    @Override
    public String getNextWakeup() {
        return getDefaultSharedPreferences().getString(Settings.KEY_NEXT_WAKEUP, null);
    }

    @Override
    public void updateNextWakeup(Date nextWakeup) {
        if (nextWakeup == null) {
            getDefaultSharedPreferences().edit().remove(Settings.KEY_NEXT_WAKEUP).apply();
        } else {
            getDefaultSharedPreferences().edit().putString(Settings.KEY_NEXT_WAKEUP, DateHandlingUtils.toFullStringUserTimezone(nextWakeup)).apply();
        }
    }

    private SharedPreferences getDefaultSharedPreferences() {
        return PreferenceManager.getDefaultSharedPreferences(context);
    }
}
