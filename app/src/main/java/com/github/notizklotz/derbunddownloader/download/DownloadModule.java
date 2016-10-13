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

import android.app.Application;
import android.app.DownloadManager;
import android.content.Context;
import android.net.wifi.WifiManager;

import com.evernote.android.job.Job;
import com.evernote.android.job.JobCreator;
import com.evernote.android.job.JobManager;
import com.github.notizklotz.derbunddownloader.BuildConfig;
import com.github.notizklotz.derbunddownloader.common.NotificationService;
import com.github.notizklotz.derbunddownloader.common.WifiCommandExecutor;
import com.github.notizklotz.derbunddownloader.settings.Settings;
import com.google.firebase.analytics.FirebaseAnalytics;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

@Module
public class DownloadModule {

    @Provides
    @Singleton
    JobManager jobManager(Application application) {
        JobManager jobManager = JobManager.create(application);
        if (BuildConfig.DEBUG) {
            jobManager.getConfig().setVerbose(true);
        }

        return jobManager;
    }

    @Provides
    @Singleton
    JobCreator jobCreator(final WifiCommandExecutor wifiCommandExecutor,
                                         final Settings settings,
                                         final FirebaseAnalytics analyticsTracker,
                                         final NotificationService notificationService,
                                         final IssueDownloader issueDownloader,
                                         final AutomaticDownloadScheduler automaticDownloadScheduler) {
        return new JobCreator() {
            @Override
            public Job create(String tag) {
                if (AutomaticIssueDownloadJob.TAG_PERIODIC.equals(tag) || AutomaticIssueDownloadJob.TAG_FALLBACK.equals(tag)) {
                    return new AutomaticIssueDownloadJob(
                            wifiCommandExecutor, settings, analyticsTracker, notificationService,
                            issueDownloader, automaticDownloadScheduler);
                }
                return null;
            }
        };

    }

    @Provides
    @Singleton
    WifiManager wifiManager(Application application) {
        return ((WifiManager) application.getSystemService(Context.WIFI_SERVICE));
    }

    @Provides
    @Singleton
    DownloadManager downloadManager(Application application) {
        return ((DownloadManager) application.getSystemService(Context.DOWNLOAD_SERVICE));
    }

}