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
import android.preference.PreferenceManager;
import android.util.Log;

import com.github.notizklotz.derbunddownloader.BuildConfig;
import com.github.notizklotz.derbunddownloader.R;
import com.github.notizklotz.derbunddownloader.common.DateHandlingUtils;
import com.github.notizklotz.derbunddownloader.issuesgrid.DownloadedIssuesActivity_;
import com.github.notizklotz.derbunddownloader.settings.Settings;

import org.androidannotations.annotations.EIntentService;
import org.androidannotations.annotations.ServiceAction;
import org.androidannotations.annotations.SystemService;
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
    @SuppressWarnings("WeakerAccess")
    @SystemService
    ConnectivityManager connectivityManager;
    @SystemService
    WifiManager wifiManager;
    @SuppressWarnings("WeakerAccess")
    @SystemService
    DownloadManager downloadManager;
    private WifiManager.WifiLock myWifiLock;
    private Intent intent;
    private DownloadCompletedBroadcastReceiver receiver;

    public IssueDownloadService() {
        super(WORKER_THREAD_NAME);
    }

    @ServiceAction
    public void downloadIssue(int day, int month, int year) {
        Log.i(LOG_TAG, "Handling download intent");
        try {
            boolean connected;
            final boolean wifiOnly = Settings.isWifiOnly(getApplicationContext());
            if (wifiOnly) {
                connected = waitForWifiConnection();
                if (!connected) {
                    notifyUser(getText(R.string.download_wifi_connection_failed), getText(R.string.download_wifi_connection_failed_text), R.drawable.ic_stat_error);
                }
            } else {
                NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
                connected = activeNetworkInfo != null && activeNetworkInfo.isConnected();
                if (!connected) {
                    notifyUser(getText(R.string.download_connection_failed), getText(R.string.download_connection_failed_text), R.drawable.ic_stat_error);
                }
            }

            if (connected) {
                if (!checkUserAccount()) {
                    notifyUser(getText(R.string.download_login_failed), getText(R.string.download_login_failed_text), R.drawable.ic_stat_error);
                } else {
                    final CountDownLatch downloadDoneSignal = new CountDownLatch(1);
                    receiver = new DownloadCompletedBroadcastReceiver(downloadDoneSignal);
                    registerReceiver(receiver, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));

                    try {
                        String title = startDownload(this, day, month, year, wifiOnly);
                        downloadDoneSignal.await();
                        notifyUser(title, getString(R.string.download_completed), R.drawable.ic_stat_av_download);
                    } catch (InterruptedException e) {
                        Log.wtf(LOG_TAG, "Interrupted while waiting for the downloadDoneSignal");
                    }
                }
            }
        } catch (Exception e) {
            notifyUser(getText(R.string.download_service_error), getText(R.string.download_service_error_text) + " " + e.getMessage(), R.drawable.ic_stat_error);
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
                notifyUser(getText(R.string.download_connection_failed), getText(R.string.download_connection_failed_no_wifi_text), R.drawable.ic_stat_error);
            } else {
                do {
                    NetworkInfo networkInfo = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
                    assert networkInfo != null;
                    connected = networkInfo.isConnected();

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

    private void notifyUser(CharSequence contentTitle, CharSequence contentText, int icon) {
        Notification.Builder mBuilder =
                new Notification.Builder(getApplicationContext())
                        .setSmallIcon(icon)
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

    private String startDownload(Context context, int day, int month, int year, boolean wifiOnly) {
        String url = "http://epaper.derbund.ch/getFile.php?ausgabe=" + DateHandlingUtils.toDDMMYYYYString(day, month, year);
        String title = "Der Bund ePaper " + DateHandlingUtils.toDD_MM_YYYYString(day, month, year);
        String filename = title + ".pdf";

        if (BuildConfig.DEBUG) {
            File extFilesDir = getExternalFilesDir(null);
            File file = new File(extFilesDir, filename);
            Log.d(LOG_TAG, "Filename: " + file.toString());
            Log.d(LOG_TAG, "Can write? " + (extFilesDir != null && extFilesDir.canWrite()));
        }

        DownloadManager.Request pdfDownloadRequest = new DownloadManager.Request(Uri.parse(url))
                .setTitle(title)
                .setDescription(DateHandlingUtils.toDD_MM_YYYYString(day, month, year))
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
                .setDestinationInExternalFilesDir(context, null, filename);

        if (wifiOnly) {
            pdfDownloadRequest.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI);
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
            final String username = sharedPref.getString(Settings.KEY_USERNAME, "");
            final String password = sharedPref.getString(Settings.KEY_PASSWORD, "");

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
