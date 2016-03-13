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
import android.app.DownloadManager;
import android.app.IntentService;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.app.TaskStackBuilder;
import android.support.v7.app.NotificationCompat;
import android.util.Log;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.Target;
import com.github.notizklotz.derbunddownloader.BuildConfig;
import com.github.notizklotz.derbunddownloader.R;
import com.github.notizklotz.derbunddownloader.analytics.AnalyticsCategory;
import com.github.notizklotz.derbunddownloader.analytics.AnalyticsTracker;
import com.github.notizklotz.derbunddownloader.common.ThumbnailRegistry;
import com.github.notizklotz.derbunddownloader.issuesgrid.DownloadedIssuesActivity_;
import com.github.notizklotz.derbunddownloader.settings.Settings;
import com.github.notizklotz.derbunddownloader.settings.SettingsImpl;
import com.google.android.gms.analytics.HitBuilders;

import org.androidannotations.annotations.Bean;
import org.androidannotations.annotations.EIntentService;
import org.androidannotations.annotations.ServiceAction;
import org.androidannotations.annotations.SystemService;
import org.joda.time.LocalDate;

import java.io.File;
import java.util.concurrent.CountDownLatch;

import static com.github.notizklotz.derbunddownloader.analytics.AnalyticsTracker.createEventBuilder;

@SuppressLint("Registered")
@EIntentService
public class IssueDownloadService extends IntentService {

    private static final String LOG_TAG = IssueDownloadService.class.getSimpleName();
    private static final int WIFI_RECHECK_WAIT_MILLIS = 5 * 1000;
    private static final int WIFI_CHECK_MAX_MILLIS = 30 * 1000;
    private static final String WORKER_THREAD_NAME = "IssueDownloadService";
    private static final String ISSUE_TITLE_TEMPLATE = "Der Bund ePaper %02d.%02d.%04d";
    private static final String ISSUE_FILENAME_TEMPLATE = "Der Bund ePaper %02d.%02d.%04d.pdf";
    private static final String ISSUE_DESCRIPTION_TEMPLATE = "%02d.%02d.%04d";

    @SuppressWarnings("WeakerAccess")
    @SystemService
    ConnectivityManager connectivityManager;

    @SystemService
    WifiManager wifiManager;

    @SystemService
    DownloadManager downloadManager;

    @Bean
    EpaperApiClient epaperApiClient;

    @Bean(SettingsImpl.class)
    Settings settings;

    @Bean
    ThumbnailRegistry thumbnailRegistry;

    @Bean
    AnalyticsTracker analyticsTracker;

    private WifiManager.WifiLock myWifiLock;
    private Intent intent;
    private DownloadCompletedBroadcastReceiver receiver;

    public IssueDownloadService() {
        super(WORKER_THREAD_NAME);
    }

    private static String expandTemplateWithDate(String template, LocalDate localDate) {
        return String.format(template, localDate.getDayOfMonth(), localDate.getMonthOfYear(), localDate.getYear());
    }

    @ServiceAction
    public void downloadIssue(int day, int month, int year) {
        Log.i(LOG_TAG, "Handling download intent");
        try {
            boolean connected;
            final boolean wifiOnly = settings.isWifiOnly();
            if (wifiOnly) {
                connected = waitForWifiConnection();
                if (!connected) {
                    analyticsTracker.sendWithCustomDimensions(createEventBuilder(AnalyticsCategory.Error).setAction("Wifi connection failed").setNonInteraction(true));
                    notifyUser(getText(R.string.download_wifi_connection_failed), getText(R.string.download_wifi_connection_failed_text), true);
                }
            } else {
                NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
                connected = activeNetworkInfo != null && activeNetworkInfo.isConnected();
                if (!connected) {
                    String connectionState = "No active network";
                    if(activeNetworkInfo != null) {
                        connectionState = activeNetworkInfo.getState().name();
                    }
                    analyticsTracker.sendWithCustomDimensions(createEventBuilder(AnalyticsCategory.Error).setAction("No connection on download").setLabel(connectionState).setNonInteraction(true));
                    notifyUser(getText(R.string.download_connection_failed), getText(R.string.download_connection_failed_text), true);
                }
            }

            if (connected) {
                final LocalDate issueDate = new LocalDate(year, month, day);
                final SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);

                try {
                    Uri pdfDownloadUrl = epaperApiClient.getPdfDownloadUrl(sharedPref.getString(Settings.KEY_USERNAME, ""), sharedPref.getString(Settings.KEY_PASSWORD, ""), issueDate);
                    Uri thumbnailUrl = epaperApiClient.getPdfThumbnailUrl(issueDate);

                    File thumbnailFile = Glide.with(getApplicationContext())
                            .load(thumbnailUrl)
                            .downloadOnly(Target.SIZE_ORIGINAL, Target.SIZE_ORIGINAL)
                            .get();

                    String stableKey = expandTemplateWithDate(ISSUE_DESCRIPTION_TEMPLATE, issueDate);
                    thumbnailRegistry.registerUri(stableKey, Uri.fromFile(thumbnailFile));

                    final CountDownLatch downloadDoneSignal = new CountDownLatch(1);
                    receiver = new DownloadCompletedBroadcastReceiver(downloadDoneSignal);
                    registerReceiver(receiver, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));

                    try {
                        long millisBeforeDownload = SystemClock.elapsedRealtime();

                        String title = enqueueDownloadRequest(pdfDownloadUrl, issueDate, wifiOnly);
                        downloadDoneSignal.await();

                        long elapsedTime = (SystemClock.elapsedRealtime() - millisBeforeDownload);
                        analyticsTracker.send(new HitBuilders.TimingBuilder().setCategory(AnalyticsCategory.Download.name()).setVariable("completion").setValue(elapsedTime));

                        notifyUser(title, getString(R.string.download_completed), false);
                    } catch (InterruptedException e) {
                        analyticsTracker.send(new HitBuilders.ExceptionBuilder().setDescription("Interrupted while waiting for the downloadDoneSignal").setFatal(false));
                        Log.wtf(LOG_TAG, "Interrupted while waiting for the downloadDoneSignal");
                    }
                } catch (EpaperApiInvalidCredentialsException e) {
                    analyticsTracker.sendWithCustomDimensions(createEventBuilder(AnalyticsCategory.Error).setAction("Invalid credentials").setNonInteraction(true));
                    notifyUser(getText(R.string.download_login_failed), getText(R.string.download_login_failed_text), true);
                }
            }
        } catch (Exception e) {
            Log.e(LOG_TAG, e.getMessage());
            analyticsTracker.sendDefaultException(this, e);
            notifyUser(getText(R.string.download_service_error), getText(R.string.download_service_error_text), e.getMessage(), true);
        } finally {
            cleanup();
        }
    }

    private boolean waitForWifiConnection() {
        boolean connected = false;
        if (wifiManager != null) {
            //WIFI_MODE_FULL was not enough on Xperia Tablet Z Android 4.2 to reconnect to the AP if Wifi was enabled but connection
            //was lost
            myWifiLock = wifiManager.createWifiLock(WifiManager.WIFI_MODE_FULL_HIGH_PERF, "IssueDownloadWifilock");
            myWifiLock.setReferenceCounted(false);
            myWifiLock.acquire();

            //Wait for Wifi coming up
            long firstCheckMillis = System.currentTimeMillis();
            if (!wifiManager.isWifiEnabled()) {
                analyticsTracker.sendWithCustomDimensions(createEventBuilder(AnalyticsCategory.Error).setAction("Wifi disabled").setNonInteraction(true));
                notifyUser(getText(R.string.download_connection_failed), getText(R.string.download_connection_failed_no_wifi_text), true);
            } else {
                do {
                    final NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
                    connected = networkInfo != null && networkInfo.getType() == ConnectivityManager.TYPE_WIFI && networkInfo.isConnected();
                    if (!connected) {
                        Log.d(LOG_TAG, "Wifi connection is not yet ready. Wait and recheck");

                        if (System.currentTimeMillis() - firstCheckMillis > WIFI_CHECK_MAX_MILLIS) {
                            break;
                        }

                        try {
                            Thread.sleep(WIFI_RECHECK_WAIT_MILLIS);
                        } catch (InterruptedException e) {
                            Log.wtf(LOG_TAG, "Interrupted while waiting for Wifi connection", e);
                            analyticsTracker.send(new HitBuilders.ExceptionBuilder().setDescription("Interrupted while waiting for Wifi connection").setFatal(false));
                        }
                    }
                } while (!connected);
            }
        }
        return connected;
    }

    private void cleanup() {
        if (myWifiLock != null) {
            if (myWifiLock.isHeld()) {
                myWifiLock.release();
            }
            myWifiLock = null;
        }

        if (receiver != null) {
            unregisterReceiver(receiver);
            receiver = null;
        }

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

    private void notifyUser(CharSequence contentTitle, CharSequence contentText, boolean error) {
        notifyUser(contentTitle, contentText, null, error);
    }

    private void notifyUser(CharSequence contentTitle, CharSequence contentText, CharSequence errorDetails, boolean error) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext());
        builder
                .setSmallIcon(R.drawable.ic_stat_newspaper)
                .setContentTitle(contentTitle)
                .setContentText(contentText)
                .setTicker(contentTitle)
                .setAutoCancel(true);
        builder.setVisibility(NotificationCompat.VISIBILITY_PUBLIC);
        if (error) {
            builder.setCategory(NotificationCompat.CATEGORY_ERROR);
        }

        if (errorDetails != null && errorDetails.length() > 0) {
            NotificationCompat.BigTextStyle style =
                    new NotificationCompat.BigTextStyle();
            style.bigText(errorDetails);
            builder.setStyle(style);
        }

        //http://developer.android.com/guide/topics/ui/notifiers/notifications.html
        // The stack builder object will contain an artificial back stack for thestarted Activity.
        // This ensures that navigating backward from the Activity leads out of your application to the Home screen.
        builder.setContentIntent(TaskStackBuilder.create(getApplicationContext()).
                addParentStack(DownloadedIssuesActivity_.class).
                addNextIntent(new Intent(getApplicationContext(), DownloadedIssuesActivity_.class)).
                getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT));

        NotificationManagerCompat.from(this).notify(1, builder.build());
    }

    private String enqueueDownloadRequest(Uri issueUrl, LocalDate issueDate, boolean wifiOnly) {
        final String title = expandTemplateWithDate(ISSUE_TITLE_TEMPLATE, issueDate);
        final String filename = expandTemplateWithDate(ISSUE_FILENAME_TEMPLATE, issueDate);
        if (BuildConfig.DEBUG) {
            File extFilesDir = getExternalFilesDir(null);
            File file = new File(extFilesDir, filename);
            Log.d(LOG_TAG, "Filename: " + file.toString());
            Log.d(LOG_TAG, "Can write? " + (extFilesDir != null && extFilesDir.canWrite()));
        }

        DownloadManager.Request pdfDownloadRequest = new DownloadManager.Request(issueUrl)
                .setTitle(title)
                .setDescription(expandTemplateWithDate(ISSUE_DESCRIPTION_TEMPLATE, issueDate))
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
                .setDestinationInExternalFilesDir(this, null, filename);

        if (wifiOnly) {
            pdfDownloadRequest.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                pdfDownloadRequest.setAllowedOverMetered(false);
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
}
