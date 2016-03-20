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

import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.util.Log;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.Target;
import com.github.notizklotz.derbunddownloader.R;
import com.github.notizklotz.derbunddownloader.analytics.AnalyticsCategory;
import com.github.notizklotz.derbunddownloader.analytics.AnalyticsTracker;
import com.github.notizklotz.derbunddownloader.common.NotificationService;
import com.github.notizklotz.derbunddownloader.common.ThumbnailRegistry;
import com.github.notizklotz.derbunddownloader.settings.Settings;
import com.github.notizklotz.derbunddownloader.settings.SettingsImpl;
import com.google.android.gms.analytics.HitBuilders;

import org.androidannotations.annotations.Bean;
import org.androidannotations.annotations.EBean;
import org.androidannotations.annotations.RootContext;
import org.androidannotations.annotations.SystemService;
import org.joda.time.LocalDate;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.CountDownLatch;

@EBean
public class IssueDownloader {
    private static final String LOG_TAG = IssueDownloader.class.getSimpleName();
    private static final String ISSUE_TITLE_TEMPLATE = "Der Bund ePaper %02d.%02d.%04d";
    private static final String ISSUE_FILENAME_TEMPLATE = "Der Bund ePaper %02d.%02d.%04d.pdf";
    private static final String ISSUE_DESCRIPTION_TEMPLATE = "%02d.%02d.%04d";

    @RootContext
    Context context;

    @SystemService
    ConnectivityManager connectivityManager;

    @SystemService
    DownloadManager downloadManager;

    @Bean
    EpaperApiClient epaperApiClient;

    @Bean
    ThumbnailRegistry thumbnailRegistry;

    @Bean
    AnalyticsTracker analyticsTracker;

    @Bean
    NotificationService notificationService;

    @Bean(SettingsImpl.class)
    Settings settings;

    public void download(LocalDate issueDate) throws IOException, EpaperApiInexistingIssueRequestedException, EpaperApiInvalidResponseException, EpaperApiInvalidCredentialsException {
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
        if (networkInfo == null || !networkInfo.isConnected()) {
            throw new IOException("Not connected");
        }

        final SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
        Uri pdfDownloadUrl = epaperApiClient.getPdfDownloadUrl(sharedPref.getString(Settings.KEY_USERNAME, ""), sharedPref.getString(Settings.KEY_PASSWORD, ""), issueDate);

        downloadThumbnail(issueDate);

        final CountDownLatch downloadDoneSignal = new CountDownLatch(1);
        final DownloadCompletedBroadcastReceiver receiver = new DownloadCompletedBroadcastReceiver(downloadDoneSignal);
        context.registerReceiver(receiver, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));

        try {
            long millisBeforeDownload = SystemClock.elapsedRealtime();

            String title = enqueueDownloadRequest(pdfDownloadUrl, issueDate, settings.isWifiOnly());
            downloadDoneSignal.await();

            long elapsedTime = (SystemClock.elapsedRealtime() - millisBeforeDownload);
            analyticsTracker.send(new HitBuilders.TimingBuilder().setCategory(AnalyticsCategory.Download.name()).setVariable("completion").setValue(elapsedTime));

            notificationService.notifyUser(title, context.getString(R.string.download_completed), false);
        } catch (InterruptedException e) {
            analyticsTracker.send(new HitBuilders.ExceptionBuilder().setDescription("Interrupted while waiting for the downloadDoneSignal").setFatal(false));
            Log.wtf(LOG_TAG, "Interrupted while waiting for the downloadDoneSignal");
        } finally {
            context.unregisterReceiver(receiver);
        }
    }

    private void downloadThumbnail(LocalDate issueDate) {
        try {
            Uri thumbnailUrl = epaperApiClient.getPdfThumbnailUrl(issueDate);

            File thumbnailFile = Glide.with(context)
                    .load(thumbnailUrl)
                    .downloadOnly(Target.SIZE_ORIGINAL, Target.SIZE_ORIGINAL)
                    .get();

            String stableKey = expandTemplateWithDate(ISSUE_DESCRIPTION_TEMPLATE, issueDate);
            thumbnailRegistry.registerUri(stableKey, Uri.fromFile(thumbnailFile));
        } catch (Exception e) {
            analyticsTracker.sendDefaultException(this.context, e);
        }
    }

    private String enqueueDownloadRequest(Uri issueUrl, LocalDate issueDate, boolean wifiOnly) {
        final String title = expandTemplateWithDate(ISSUE_TITLE_TEMPLATE, issueDate);
        final String filename = expandTemplateWithDate(ISSUE_FILENAME_TEMPLATE, issueDate);

        DownloadManager.Request pdfDownloadRequest = new DownloadManager.Request(issueUrl)
                .setTitle(title)
                .setDescription(expandTemplateWithDate(ISSUE_DESCRIPTION_TEMPLATE, issueDate))
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
                .setDestinationInExternalFilesDir(context, null, filename);

        if (wifiOnly) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                pdfDownloadRequest.setAllowedOverMetered(false);
            } else {
                pdfDownloadRequest.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI);
            }
        }
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
