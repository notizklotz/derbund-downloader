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
import android.app.Notification;
import android.app.NotificationManager;
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
import android.preference.PreferenceManager;
import android.util.Log;

import com.github.notizklotz.derbunddownloader.BuildConfig;
import com.github.notizklotz.derbunddownloader.R;
import com.github.notizklotz.derbunddownloader.issuesgrid.DownloadedIssuesActivity_;
import com.github.notizklotz.derbunddownloader.settings.Settings;
import com.github.notizklotz.derbunddownloader.settings.SettingsImpl;
import com.squareup.picasso.Picasso;

import org.androidannotations.annotations.Bean;
import org.androidannotations.annotations.EIntentService;
import org.androidannotations.annotations.ServiceAction;
import org.androidannotations.annotations.SystemService;
import org.joda.time.LocalDate;
import org.springframework.util.Assert;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.util.concurrent.CountDownLatch;

@SuppressLint("Registered")
@EIntentService
public class IssueDownloadService extends IntentService {

    private static final String LOG_TAG = IssueDownloadService.class.getSimpleName();
    private static final int WIFI_RECHECK_WAIT_MILLIS = 5 * 1000;
    private static final int WIFI_CHECK_MAX_MILLIS = 30 * 1000;
    private static final String WORKER_THREAD_NAME = "IssueDownloadService";
    private static final String ISSUE_PDF_URL_TEMPLATE = "http://epaper.derbund.ch/getFile.php?ausgabe=%02d%02d%04d";
    private static final String ISSUE_THUMBNAIL_URL_TEMPLATE = "http://epaper.derbund.ch/jpg/%04d-BVBU-001-%02d%02d.pdf.jpg";
    private static final String ISSUE_TITLE_TEMPLATE = "Der Bund ePaper %02d.%02d.%04d";
    private static final String ISSUE_FILENAME_TEMPLATE = "Der Bund ePaper %02d.%02d.%04d.pdf";
    private static final String ISSUE_DESCRIPTION_TEMPLATE = "%02d.%02d.%04d";

    @SystemService
    ConnectivityManager connectivityManager;

    @SystemService
    WifiManager wifiManager;

    @SystemService
    DownloadManager downloadManager;

    @Bean(SettingsImpl.class)
    Settings settings;

    private WifiManager.WifiLock myWifiLock;
    private Intent intent;
    private DownloadCompletedBroadcastReceiver receiver;

    public IssueDownloadService() {
        super(WORKER_THREAD_NAME);
    }

    private static String expandTemplateWithDate(String template, LocalDate localDate) {
        return String.format(template, localDate.getDayOfMonth(), localDate.getMonthOfYear(), localDate.getYear());
    }

    public static Uri getThumbnailUriForPDFUri(Uri pdfUri) {
        String ddmmyyyy = pdfUri.getQueryParameter("ausgabe");

        int day = Integer.parseInt(ddmmyyyy.substring(0, 2));
        int month = Integer.parseInt(ddmmyyyy.substring(2, 4));
        int year = Integer.parseInt(ddmmyyyy.substring(4, 8));

        return Uri.parse(String.format(ISSUE_THUMBNAIL_URL_TEMPLATE, year, day, month));
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
                    notifyUser(getText(R.string.download_wifi_connection_failed), getText(R.string.download_wifi_connection_failed_text));
                }
            } else {
                NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
                connected = activeNetworkInfo != null && activeNetworkInfo.isConnected();
                if (!connected) {
                    notifyUser(getText(R.string.download_connection_failed), getText(R.string.download_connection_failed_text));
                }
            }

            if (connected) {
                if (!checkUserAccount()) {
                    notifyUser(getText(R.string.download_login_failed), getText(R.string.download_login_failed_text));
                } else {
                    final LocalDate issueDate = new LocalDate(year, month, day);
                    fetchThumbnail(issueDate);

                    final CountDownLatch downloadDoneSignal = new CountDownLatch(1);
                    receiver = new DownloadCompletedBroadcastReceiver(downloadDoneSignal);
                    registerReceiver(receiver, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));

                    try {
                        String title = startDownload(issueDate, wifiOnly);
                        downloadDoneSignal.await();
                        notifyUser(title, getString(R.string.download_completed));
                    } catch (InterruptedException e) {
                        Log.wtf(LOG_TAG, "Interrupted while waiting for the downloadDoneSignal");
                    }
                }
            }
        } catch (Exception e) {
            notifyUser(getText(R.string.download_service_error), getText(R.string.download_service_error_text) + " " + e.getMessage());
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
                notifyUser(getText(R.string.download_connection_failed), getText(R.string.download_connection_failed_no_wifi_text));
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
            AutomaticIssueDownloadAlarmReceiver.completeWakefulIntent(intent);
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

    private void notifyUser(CharSequence contentTitle, CharSequence contentText) {
        Notification.Builder mBuilder =
                new Notification.Builder(getApplicationContext())
                        .setSmallIcon(R.drawable.ic_stat_newspaper)
                        .setContentTitle(contentTitle)
                        .setContentText(contentText)
                        .setTicker(contentTitle)
                        .setAutoCancel(true);

        //http://developer.android.com/guide/topics/ui/notifiers/notifications.html
        // The stack builder object will contain an artificial back stack for thestarted Activity.
        // This ensures that navigating backward from the Activity leads out of your application to the Home screen.
        mBuilder.setContentIntent(android.support.v4.app.TaskStackBuilder.create(getApplicationContext()).
                addParentStack(DownloadedIssuesActivity_.class).
                addNextIntent(new Intent(getApplicationContext(), DownloadedIssuesActivity_.class)).
                getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT));

        NotificationManager mNotifyMgr = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        //noinspection deprecation
        mNotifyMgr.notify(1, mBuilder.getNotification());
    }

    private void fetchThumbnail(LocalDate issueDate) {
        Uri uri = Uri.parse(String.format(ISSUE_THUMBNAIL_URL_TEMPLATE, issueDate.getYear(), issueDate.getDayOfMonth(), issueDate.getMonthOfYear()));
        Picasso.with(this).load(uri).fetch();
    }

    private String startDownload(LocalDate issueDate, boolean wifiOnly) {
        final String title = expandTemplateWithDate(ISSUE_TITLE_TEMPLATE, issueDate);
        final String filename = expandTemplateWithDate(ISSUE_FILENAME_TEMPLATE, issueDate);
        if (BuildConfig.DEBUG) {
            File extFilesDir = getExternalFilesDir(null);
            File file = new File(extFilesDir, filename);
            Log.d(LOG_TAG, "Filename: " + file.toString());
            Log.d(LOG_TAG, "Can write? " + (extFilesDir != null && extFilesDir.canWrite()));
        }

        Uri issueUrl = Uri.parse(expandTemplateWithDate(ISSUE_PDF_URL_TEMPLATE, issueDate));
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

    private boolean checkUserAccount() {
        if (BuildConfig.DEBUG) {
            Log.d(LOG_TAG, "Checking user account validity");
        }

        try {
            final SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
            final String username = sharedPref.getString(SettingsImpl.KEY_USERNAME, "");
            final String password = sharedPref.getString(SettingsImpl.KEY_PASSWORD, "");

            RestTemplate restTemplate = new RestTemplate(true);
            LinkedMultiValueMap<String, String> form = new LinkedMultiValueMap<String, String>();
            form.add("user", username);
            form.add("password", password);
            form.add("dologin", "1");
            form.add("t", "");

            String response = restTemplate.postForObject("http://epaper.derbund.ch", form, String.class);
            boolean loginSuccessful = response.contains("flashcontent");
            Log.d(LOG_TAG, "Login successful? " + loginSuccessful);
            return loginSuccessful;
        } catch (RestClientException e) {
            Log.e(LOG_TAG, "Error while trying to login", e);
            return false;
        }
    }

    private static class DownloadCompletedBroadcastReceiver extends BroadcastReceiver {

        private final CountDownLatch downloadDoneSignal;

        private DownloadCompletedBroadcastReceiver(CountDownLatch downloadDoneSignal) {
            Assert.notNull(downloadDoneSignal);
            this.downloadDoneSignal = downloadDoneSignal;
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            downloadDoneSignal.countDown();
        }
    }
}
