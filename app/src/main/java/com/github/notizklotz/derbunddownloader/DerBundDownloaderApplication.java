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

package com.github.notizklotz.derbunddownloader;

import android.app.Application;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.StrictMode;
import android.preference.PreferenceManager;

import com.github.notizklotz.derbunddownloader.analytics.AnalyticsComponent;
import com.github.notizklotz.derbunddownloader.analytics.AnalyticsModule;
import com.github.notizklotz.derbunddownloader.analytics.DaggerAnalyticsComponent;
import com.github.notizklotz.derbunddownloader.analytics.FirebaseEvents;
import com.github.notizklotz.derbunddownloader.download.DaggerDownloadComponent;
import com.github.notizklotz.derbunddownloader.download.DownloadComponent;
import com.github.notizklotz.derbunddownloader.download.DownloadModule;
import com.github.notizklotz.derbunddownloader.settings.DaggerSettingsComponent;
import com.github.notizklotz.derbunddownloader.settings.SettingsComponent;
import com.github.notizklotz.derbunddownloader.ui.DaggerUiComponent;
import com.github.notizklotz.derbunddownloader.ui.UiComponent;

import net.danlew.android.joda.JodaTimeAndroid;


public class DerBundDownloaderApplication extends Application {

    private static final String KEY_LAST_APP_VERSION = "last_app_version";

    private AnalyticsComponent analyticsComponent;

    private DownloadComponent downloadComponent;

    private SettingsComponent settingsComponent;

    private UiComponent uiComponent;

    @Override
    public void onCreate() {
        super.onCreate();

        //noinspection PointlessBooleanExpression
        if (BuildConfig.DEBUG && false) {
            StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder().detectAll().penaltyLog().build());
            StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder().detectAll().penaltyLog().build());
        }

        JodaTimeAndroid.init(this);

        AppModule appModule = new AppModule(this);
        AnalyticsModule analyticsModule = new AnalyticsModule();
        DownloadModule downloadModule = new DownloadModule();

        analyticsComponent = DaggerAnalyticsComponent.builder().appModule(appModule).analyticsModule(analyticsModule).build();
        downloadComponent = DaggerDownloadComponent.builder().appModule(appModule).analyticsModule(analyticsModule).downloadModule(downloadModule).build();
        settingsComponent = DaggerSettingsComponent.builder().appModule(appModule).build();
        uiComponent = DaggerUiComponent.builder().appModule(appModule).analyticsModule(analyticsModule).downloadModule(downloadModule).build();

        downloadComponent.jobManager().addJobCreator(downloadComponent.jobCreator());

        migrate();

        analyticsComponent.firebaseAnalytics().setUserProperty(FirebaseEvents.KEY_JOB_API, downloadComponent.jobManager().getApi().toString());
    }

    private void migrate() {
        SharedPreferences defaultSharedPreferences = PreferenceManager.getDefaultSharedPreferences(DerBundDownloaderApplication.this);
        int lastAppVersion = defaultSharedPreferences.getInt(KEY_LAST_APP_VERSION, 0);

        if (lastAppVersion < 46) {
            downloadComponent.automaticDownloadScheduler().update();
        }

        if (lastAppVersion < 49) {
            downloadComponent.thumnailRegistry().migrate();
        }
        try {
            int currentAppVersion = getPackageManager().getPackageInfo(getPackageName(), 0).versionCode;
            if (currentAppVersion != lastAppVersion) {
                defaultSharedPreferences.edit().putInt(KEY_LAST_APP_VERSION, currentAppVersion).apply();
            }
        } catch (PackageManager.NameNotFoundException e) {
            // Do nothing
        }
    }

    public AnalyticsComponent getAnalyticsComponent() {
        return analyticsComponent;
    }

    public DownloadComponent getDownloadComponent() {
        return downloadComponent;
    }

    public SettingsComponent getSettingsComponent() {
        return settingsComponent;
    }

    public UiComponent getUiComponent() {
        return uiComponent;
    }
}
