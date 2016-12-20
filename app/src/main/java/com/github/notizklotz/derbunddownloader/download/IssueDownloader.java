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
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.util.Log;

import com.github.notizklotz.derbunddownloader.R;
import com.github.notizklotz.derbunddownloader.analytics.FirebaseEvents;
import com.github.notizklotz.derbunddownloader.analytics.FirebaseParams;
import com.github.notizklotz.derbunddownloader.common.DateHandlingUtils;
import com.github.notizklotz.derbunddownloader.common.NotificationService;
import com.github.notizklotz.derbunddownloader.settings.Settings;
import com.google.firebase.analytics.FirebaseAnalytics;

import org.joda.time.LocalDate;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class IssueDownloader {
    private static final String LOG_TAG = IssueDownloader.class.getSimpleName();

    private final Context context;

    private final ConnectivityManager connectivityManager;

    private final DownloadManager downloadManager;

    private final EpaperApiClient epaperApiClient;

    private final FirebaseAnalytics firebaseAnalytics;

    private final NotificationService notificationService;

    private final Settings settings;

    @Inject
    public IssueDownloader(Application context, ConnectivityManager connectivityManager,
                           DownloadManager downloadManager, EpaperApiClient epaperApiClient,
                           FirebaseAnalytics firebaseAnalytics, NotificationService notificationService,
                           Settings settings) {
        this.context = context;
        this.connectivityManager = connectivityManager;
        this.downloadManager = downloadManager;
        this.epaperApiClient = epaperApiClient;
        this.firebaseAnalytics = firebaseAnalytics;
        this.notificationService = notificationService;
        this.settings = settings;
    }

    public void download(LocalDate issueDate, String trigger) throws IOException, EpaperApiInexistingIssueRequestedException, EpaperApiInvalidResponseException, EpaperApiInvalidCredentialsException {
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
        if (networkInfo == null || !networkInfo.isConnected()) {
            throw new IOException("Not connected");
        }

        final SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
        Uri pdfDownloadUrl = epaperApiClient.getPdfDownloadUrl(sharedPref.getString(Settings.KEY_USERNAME, ""), sharedPref.getString(Settings.KEY_PASSWORD, ""), issueDate);

        try {
            epaperApiClient.savePdfThumbnail(issueDate);
        } catch (Exception e) {
            Log.e(LOG_TAG, "Could not save thumbnail", e);
        }

        final CountDownLatch downloadDoneSignal = new CountDownLatch(1);
        final DownloadCompletedBroadcastReceiver receiver = new DownloadCompletedBroadcastReceiver(downloadDoneSignal);
        context.registerReceiver(receiver, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));

        try {
            long millisBeforeDownload = SystemClock.elapsedRealtime();

            String title = enqueueDownloadRequest(pdfDownloadUrl, issueDate, settings.isWifiOnly());
            downloadDoneSignal.await();

            Bundle bundle = new Bundle();
            bundle.putString(FirebaseAnalytics.Param.ITEM_ID, issueDate.toString());
            bundle.putString(FirebaseParams.DOWNLOAD_TRIGGER, trigger);
            bundle.putLong(FirebaseParams.DOWNLOAD_TIME_SECONDS, TimeUnit.MILLISECONDS.toSeconds(SystemClock.elapsedRealtime() - millisBeforeDownload));

            firebaseAnalytics.logEvent(FirebaseEvents.DOWNLOAD_ISSUE_COMPLETED, bundle);

            notificationService.notifyUser(title, context.getString(R.string.download_completed), false);
        } catch (InterruptedException e) {
            Log.wtf(LOG_TAG, "Interrupted while waiting for the downloadDoneSignal");
        } finally {
            context.unregisterReceiver(receiver);
        }
    }

    private String enqueueDownloadRequest(Uri issueUrl, LocalDate issueDate, boolean wifiOnly) {
        final String title = expandTemplateWithDate(context.getString(R.string.issue_title) + " ePaper %02d.%02d.%04d", issueDate);
        final String filename = expandTemplateWithDate(context.getString(R.string.issue_title) + " ePaper %02d.%02d.%04d.pdf", issueDate);

        DownloadManager.Request pdfDownloadRequest = new DownloadManager.Request(issueUrl)
                .setTitle(title)
                .setDescription(DateHandlingUtils.toDateString(issueDate))
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
                .setDestinationInExternalFilesDir(context, null, filename)
                .setAllowedOverMetered(!wifiOnly);

        downloadManager.enqueue(pdfDownloadRequest);

        return title;
    }

    private static class DownloadCompletedBroadcastReceiver extends BroadcastReceiver {

        private final CountDownLatch downloadDoneSignal;

        private DownloadCompletedBroadcastReceiver(@NonNull CountDownLatch downloadDoneSignal) {
            this.downloadDoneSignal = downloadDoneSignal;
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            downloadDoneSignal.countDown();
        }
    }

    private static String expandTemplateWithDate(String template, LocalDate localDate) {
        return String.format(template, localDate.getDayOfMonth(), localDate.getMonthOfYear(), localDate.getYear());
    }

}
