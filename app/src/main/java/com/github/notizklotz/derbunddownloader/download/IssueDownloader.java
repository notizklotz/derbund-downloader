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
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;

import com.evernote.android.job.JobManager;
import com.github.notizklotz.derbunddownloader.R;
import com.github.notizklotz.derbunddownloader.analytics.FirebaseEvents;
import com.github.notizklotz.derbunddownloader.common.DateHandlingUtils;
import com.github.notizklotz.derbunddownloader.common.NotificationService;
import com.github.notizklotz.derbunddownloader.common.RetriableTask;
import com.github.notizklotz.derbunddownloader.download.client.EpaperApiClient;
import com.github.notizklotz.derbunddownloader.download.client.InexistingIssueRequestedException;
import com.github.notizklotz.derbunddownloader.download.client.InvalidCredentialsException;
import com.github.notizklotz.derbunddownloader.download.client.IssueAlreadyDownloadedException;
import com.github.notizklotz.derbunddownloader.settings.Settings;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.crash.FirebaseCrash;

import org.joda.time.LocalDate;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;

import javax.inject.Inject;
import javax.inject.Singleton;

@SuppressWarnings("WeakerAccess")
@Singleton
public class IssueDownloader {

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

    public static LocalDate getIssueDateFromDownloadManagerCursor(Cursor cursor) {
        String dateString = cursor.getString(cursor.getColumnIndex(DownloadManager.COLUMN_DESCRIPTION));
        return DateHandlingUtils.fromDateString(dateString);
    }

    public void download(LocalDate issueDate, DownloadTrigger trigger, boolean waitForCompletion)
            throws IOException, InexistingIssueRequestedException, InvalidCredentialsException, IssueAlreadyDownloadedException {

        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
        if (networkInfo == null || !networkInfo.isConnected()) {
            throw new IOException("Not connected");
        }
        Cursor cursor = downloadManager.query(new DownloadManager.Query());
        try {
            while (cursor.moveToNext()) {
                if (issueDate.equals(getIssueDateFromDownloadManagerCursor(cursor))) {
                    throw new IssueAlreadyDownloadedException(issueDate);
                }
            }
        } finally {
            cursor.close();
        }

        final SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);

        Uri pdfDownloadUrl;
        try {
            pdfDownloadUrl = epaperApiClient.getPdfDownloadUrl(sharedPref.getString(Settings.KEY_USERNAME, ""), sharedPref.getString(Settings.KEY_PASSWORD, ""), issueDate);
        } catch (InvalidCredentialsException | InexistingIssueRequestedException e) {
            logException(FirebaseEvents.USER_ERROR, e);
            throw e;
        } catch (RetriableTask.RetryException e) {
            Exception cause = (Exception) e.getCause();
            if (cause instanceof IOException) {
                logException(FirebaseEvents.CONNECTION_ERROR, e);
                throw (IOException) cause;
            }
            throw e;
        }

        try {
            epaperApiClient.savePdfThumbnail(issueDate);
        } catch (Exception e) {
            FirebaseCrash.report(e);
        }

        final CountDownLatch downloadDoneSignal = new CountDownLatch(1);
        final DownloadCompletedBroadcastReceiver receiver = new DownloadCompletedBroadcastReceiver(downloadDoneSignal);
        context.registerReceiver(receiver, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));

        try {
            String title = enqueueDownloadRequest(pdfDownloadUrl, issueDate, settings.isWifiOnly());

            if (waitForCompletion) {
                downloadDoneSignal.await();
                notificationService.notifyUser(title, context.getString(R.string.download_completed), false);
            }

            Bundle bundle = new Bundle();
            bundle.putString(FirebaseAnalytics.Param.ITEM_ID, issueDate.toString());
            bundle.putString(FirebaseEvents.KEY_DOWNLOAD_TRIGGER, trigger.name());
            if (trigger == DownloadTrigger.AUTO) {
                bundle.putString(FirebaseEvents.KEY_JOB_API, JobManager.instance().getApi().name());
            }
            firebaseAnalytics.logEvent(FirebaseEvents.DOWNLOAD_ISSUE_COMPLETED, bundle);
        } catch (InterruptedException e) {
            FirebaseCrash.report(e);
        } finally {
            context.unregisterReceiver(receiver);
        }
    }

    private void logException(String event, Exception e) {
        Bundle bundle = new Bundle();
        String message = e.getMessage();
        if (message.length() > 100) {
            message = message.substring(0, 100);
        }
        bundle.putString("cause", message);
        firebaseAnalytics.logEvent(event, bundle);
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

    public enum DownloadTrigger {

        AUTO, MANUAL

    }

}
