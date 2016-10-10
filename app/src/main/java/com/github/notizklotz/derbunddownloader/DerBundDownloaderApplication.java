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

import com.github.notizklotz.derbunddownloader.analytics.AnalyticsComponent;
import com.github.notizklotz.derbunddownloader.analytics.AnalyticsModule;
import com.github.notizklotz.derbunddownloader.analytics.DaggerAnalyticsComponent;
import com.github.notizklotz.derbunddownloader.download.DaggerDownloadComponent;
import com.github.notizklotz.derbunddownloader.download.DownloadComponent;
import com.github.notizklotz.derbunddownloader.download.DownloadModule;
import com.github.notizklotz.derbunddownloader.issuesgrid.DaggerDownloadedIssuesComponent;
import com.github.notizklotz.derbunddownloader.issuesgrid.DownloadedIssuesComponent;
import com.github.notizklotz.derbunddownloader.settings.DaggerSettingsComponent;
import com.github.notizklotz.derbunddownloader.settings.SettingsComponent;

import net.danlew.android.joda.JodaTimeAndroid;


public class DerBundDownloaderApplication extends Application {

    private AnalyticsComponent analyticsComponent;

    private DownloadComponent downloadComponent;

    private SettingsComponent settingsComponent;

    private DownloadedIssuesComponent downloadedIssuesComponent;

    @Override
    public void onCreate() {
        super.onCreate();
        JodaTimeAndroid.init(this);

        AppModule appModule = new AppModule(this);
        AnalyticsModule analyticsModule = new AnalyticsModule();
        DownloadModule downloadModule = new DownloadModule();

        analyticsComponent = DaggerAnalyticsComponent.builder().appModule(appModule).analyticsModule(analyticsModule).build();
        downloadComponent = DaggerDownloadComponent.builder().appModule(appModule).analyticsModule(analyticsModule).downloadModule(downloadModule).build();
        settingsComponent = DaggerSettingsComponent.builder().appModule(appModule).build();
        downloadedIssuesComponent = DaggerDownloadedIssuesComponent.builder().appModule(appModule).analyticsModule(analyticsModule).downloadModule(downloadModule).build();

        downloadComponent.automaticDownloadScheduler().update();
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

    public DownloadedIssuesComponent getDownloadedIssuesComponent() {
        return downloadedIssuesComponent;
    }
}
