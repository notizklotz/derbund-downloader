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

import android.annotation.SuppressLint;
import android.app.IntentService;
import android.content.Intent;
import android.util.Log;

import com.github.notizklotz.derbunddownloader.R;
import com.github.notizklotz.derbunddownloader.analytics.AnalyticsCategory;
import com.github.notizklotz.derbunddownloader.analytics.AnalyticsTracker;
import com.github.notizklotz.derbunddownloader.common.NotificationService;
import com.github.notizklotz.derbunddownloader.common.WifiCommandExecutor;
import com.github.notizklotz.derbunddownloader.common.WifiConnectionFailedException;
import com.github.notizklotz.derbunddownloader.common.WifiNotEnabledException;
import com.github.notizklotz.derbunddownloader.settings.Settings;
import com.github.notizklotz.derbunddownloader.settings.SettingsImpl;

import org.androidannotations.annotations.Bean;
import org.androidannotations.annotations.EIntentService;
import org.androidannotations.annotations.ServiceAction;
import org.joda.time.LocalDate;

import java.io.IOException;
import java.util.concurrent.Callable;

import static com.github.notizklotz.derbunddownloader.analytics.AnalyticsTracker.createEventBuilder;

@SuppressLint("Registered")
@EIntentService
public class IssueDownloadIntentService extends IntentService {

    private static final String TAG = IssueDownloadIntentService.class.getSimpleName();

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

    private Intent intent;

    public IssueDownloadIntentService() {
        super("IssueDownloadIntentService");
    }

    @ServiceAction
    public void downloadIssue(int day, int month, int year) {
        final LocalDate issueDate = new LocalDate(year, month, day);
        final boolean wifiOnly = settings.isWifiOnly();

        try {
            if (wifiOnly) {
                try {
                    wifiCommandExecutor.execute(new Callable() {
                        @Override
                        public Object call() throws Exception {
                            issueDownloader.download(issueDate, true);
                            return null;
                        }
                    });
                } catch (WifiConnectionFailedException e) {
                    analyticsTracker.sendWithCustomDimensions(createEventBuilder(AnalyticsCategory.Error).setAction("Wifi connection failed").setNonInteraction(true));
                    notificationService.notifyUser(getText(R.string.download_wifi_connection_failed), getText(R.string.download_wifi_connection_failed_text), true);
                } catch (WifiNotEnabledException e) {
                    analyticsTracker.sendWithCustomDimensions(createEventBuilder(AnalyticsCategory.Error).setAction("Wifi disabled").setNonInteraction(true));
                    notificationService.notifyUser(getText(R.string.download_connection_failed), getText(R.string.download_connection_failed_no_wifi_text), true);
                }
            } else {
                issueDownloader.download(issueDate, false);
            }
        }
        catch (IOException e) {
            analyticsTracker.sendWithCustomDimensions(createEventBuilder(AnalyticsCategory.Error).setAction("No connection on download").setNonInteraction(true));
            notificationService.notifyUser(getText(R.string.download_connection_failed), getText(R.string.download_connection_failed_text), true);
        }
        catch (Exception e) {
            Log.e(TAG, "downloadIssue: failed", e);
            analyticsTracker.sendDefaultException(this, e);
        } finally {
            cleanup();
        }
    }

    private void cleanup() {
        if (intent != null) {
            AutomaticDownloadBroadcastReceiver.completeWakefulIntent(intent);
            intent = null;
        }
    }

    @Override
    public void onDestroy() {
        cleanup();
        super.onDestroy();
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        this.intent = intent;
    }

}
