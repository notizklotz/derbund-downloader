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

package com.github.notizklotz.derbunddownloader.download;

import android.app.IntentService;
import android.content.Intent;
import android.util.Log;

import com.github.notizklotz.derbunddownloader.analytics.AnalyticsTracker;
import com.github.notizklotz.derbunddownloader.common.NotificationService;
import com.github.notizklotz.derbunddownloader.common.WifiCommandExecutor;
import com.github.notizklotz.derbunddownloader.settings.Settings;
import com.github.notizklotz.derbunddownloader.settings.SettingsImpl;

import org.androidannotations.annotations.Bean;
import org.androidannotations.annotations.EIntentService;
import org.androidannotations.annotations.ServiceAction;
import org.joda.time.LocalDate;

import java.io.IOException;

@EIntentService
public class IssueDownloadIntentService extends IntentService {

    private static final String TAG = "IssueDownloadService";

    @Bean
    WifiCommandExecutor wifiCommandExecutor;

    @Bean(SettingsImpl.class)
    Settings settings;

    @Bean
    AnalyticsTracker analyticsTracker;

    @Bean
    NotificationService notificationService;

    @Bean
    IssueDownloader issueDownloader;

    public IssueDownloadIntentService() {
        super("IssueDownloadIntentService");
    }

    @ServiceAction
    public void downloadIssue(int day, int month, int year) {
        final LocalDate issueDate = new LocalDate(year, month, day);
        try {
            issueDownloader.download(issueDate);
        } catch (IOException e) {
            Log.e(TAG, "downloadIssue: connection failed", e);
        }
    }

    @Override
    protected void onHandleIntent(Intent intent) {

    }
}
