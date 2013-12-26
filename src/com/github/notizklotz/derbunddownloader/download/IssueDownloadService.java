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
import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.*;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.util.Log;
import com.github.notizklotz.derbunddownloader.R;
import com.github.notizklotz.derbunddownloader.common.DateFormatterUtils;
import com.github.notizklotz.derbunddownloader.settings.Settings;
import org.springframework.util.Assert;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.concurrent.CountDownLatch;

public class IssueDownloadService extends IntentService {

    private static final boolean ENABLE_WIFI_CHECK = true;
    private static final boolean ENABLE_USER_CHECK = true;

    public static final String EXTRA_DAY = "day";
    public static final String EXTRA_MONTH = "month";
    public static final String EXTRA_YEAR = "year";

    public IssueDownloadService() {
        super("IssueDownloadService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (!(intent.hasExtra(EXTRA_DAY) && intent.hasExtra(EXTRA_MONTH) && intent.hasExtra(EXTRA_YEAR))) {
            throw new IllegalArgumentException("Intent is missing extras");
        }

        WifiManager.WifiLock myWifiLock;
        WifiManager wm;
        boolean previousWifiState;
        if(ENABLE_WIFI_CHECK) {
            //Enable Wifi and lock it
            wm = (WifiManager) getSystemService(Context.WIFI_SERVICE);
            previousWifiState = wm.isWifiEnabled();
            wm.setWifiEnabled(true);
            myWifiLock = wm.createWifiLock(WifiManager.WIFI_MODE_FULL, "IssueDownloadWifilock");
            myWifiLock.acquire();
        }

        if (!checkUserAccount()) {
            Notification.Builder mBuilder =
                    new Notification.Builder(this)
                            .setSmallIcon(R.drawable.issue)
                            .setContentTitle(getString(R.string.download_login_failed))
                            .setContentText(getString(R.string.download_login_failed_text));
            NotificationManager mNotifyMgr = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            //noinspection deprecation
            mNotifyMgr.notify(1, mBuilder.getNotification());
        } else {
            //Download
            final CountDownLatch downloadDoneSignal = new CountDownLatch(1);
            final DownloadCompletedBroadcastReceiver receiver = new DownloadCompletedBroadcastReceiver(downloadDoneSignal);
            registerReceiver(receiver, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
            startDownload(this, intent.getIntExtra(EXTRA_DAY, 0), intent.getIntExtra(EXTRA_MONTH, 0), intent.getIntExtra(EXTRA_YEAR, 0));

            try {
                downloadDoneSignal.await();
            } catch (InterruptedException e) {
                Log.w(getClass().getName(), "Interrupted while waiting for the downloadDoneSignal");
            }
            unregisterReceiver(receiver);
        }

        if(ENABLE_WIFI_CHECK) {
            //Stop Wifi if it was before and release Wifi lock
            myWifiLock.release();
            wm.setWifiEnabled(previousWifiState);
        }

        AutomaticIssueDownloadAlarmReceiver.completeWakefulIntent(intent);
    }

    private void startDownload(Context context, int day, int month, int year) {
        DownloadManager downloadManager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);

        String url = "http://epaper.derbund.ch/getFile.php?ausgabe=" + DateFormatterUtils.toDDMMYYYYString(day, month, year);
        String title = "Der Bund ePaper " + DateFormatterUtils.toDD_MM_YYYYString(day, month, year);
        DownloadManager.Request pdfDownloadRequest = new DownloadManager.Request(Uri.parse(url))
                .setTitle(title)
                .setDescription(DateFormatterUtils.toDD_MM_YYYYString(day, month, year))
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
                .setDestinationInExternalFilesDir(context, Environment.DIRECTORY_DOWNLOADS, title + ".pdf");
        if(ENABLE_WIFI_CHECK) {
            pdfDownloadRequest.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI);
        }
        downloadManager.enqueue(pdfDownloadRequest);

        Log.d(IssueDownloadService.class.getName(), "Download enqueued");
    }

    private boolean checkUserAccount() {
        //noinspection PointlessBooleanExpression,ConstantConditions
        if(!ENABLE_USER_CHECK) {
            return true;
        }
        Log.d(getClass().getName(), "Checking user account validty");

        ConnectivityManager connMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        try {

            if(ENABLE_WIFI_CHECK) {
                boolean connected;
                do {
                    NetworkInfo networkInfo = connMgr.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
                    if (networkInfo == null) {
                        throw new IllegalStateException("No Wifi device found");
                    }
                    connected = networkInfo.isConnected();

                    if (!connected) {
                        Log.d(getClass().getName(), "Wifi connection is not yet ready. Wait 3 seconds and recheck");
                        Thread.sleep(3000);
                    }
                } while (!connected);
            }

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
            Log.d(getClass().getName(), "Login successful? " + loginSuccessful);
            return loginSuccessful;
        } catch (InterruptedException e) {
            Log.e(getClass().getName(), "Error while waiting for Wifi connection", e);
            return false;
        } catch (RestClientException e) {
            Log.e(getClass().getName(), "Error while trying to login", e);
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
