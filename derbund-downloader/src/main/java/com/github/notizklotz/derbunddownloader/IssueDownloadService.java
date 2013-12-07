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

package com.github.notizklotz.derbunddownloader;

import android.app.DownloadManager;
import android.app.IntentService;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Environment;
import android.util.Log;

import org.springframework.util.Assert;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.concurrent.CountDownLatch;

public class IssueDownloadService extends IntentService {

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

        //Enable Wifi and lock it
        final WifiManager wm = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        final boolean previousWifiState = wm.isWifiEnabled();
        wm.setWifiEnabled(true);
        final WifiManager.WifiLock myWifiLock = wm.createWifiLock(WifiManager.WIFI_MODE_FULL, "IssueDownloadWifilock");
        myWifiLock.acquire();

        //Download
        final CountDownLatch downloadDoneSignal = new CountDownLatch(1);
        final DownloadCompletedBroadcastReceiver receiver = new DownloadCompletedBroadcastReceiver(downloadDoneSignal);
        registerReceiver(receiver, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
        startDownload(this, intent.getIntExtra(EXTRA_DAY, 0), intent.getIntExtra(EXTRA_MONTH, 0), intent.getIntExtra(EXTRA_YEAR, 0));

        try {
            downloadDoneSignal.await();
        } catch (InterruptedException e) {
            Log.w(getClass().getName(), "Interrupted while waiting for the downloadDoneSignal");
        } finally {
            unregisterReceiver(receiver);

            //Stop Wifi if it was before and release Wifi lock
            myWifiLock.release();
            wm.setWifiEnabled(previousWifiState);

            AutomaticIssueDownloadAlarmReceiver.completeWakefulIntent(intent);
        }
    }

    private void startDownload(Context context, int day, int month, int year) {
        if (!checkUserAccount()) {
            //TODO: Login failed
        }

        String url = "http://epaper.derbund.ch/getFile.php?ausgabe=" + DateFormatterUtils.toDDMMYYYYString(day, month, year);
        DownloadManager downloadManager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
        String title = "Der Bund ePaper " + DateFormatterUtils.toDD_MM_YYYYString(day, month, year);
        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url))
                .setTitle(title)
                .setDescription(DateFormatterUtils.toDD_MM_YYYYString(day, month, year))
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
                .setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI)
                .setDestinationInExternalFilesDir(context, Environment.DIRECTORY_DOWNLOADS, title + ".pdf");
        downloadManager.enqueue(request);

        Log.d(IssueDownloadService.class.getName(), "Download enqueued");
    }

    private boolean checkUserAccount() {
        Log.d(IssueDownloadService.class.getName(), "Trying to checkUserAccount");

        RestTemplate restTemplate = new RestTemplate(true);
        LinkedMultiValueMap<String, String> form = new LinkedMultiValueMap<String, String>();
        form.add("user", "asdf");
        form.add("password", "asdf");
        form.add("dologin", "1");
        form.add("t", "");

        ConnectivityManager connMgr = (ConnectivityManager)
                getSystemService(Context.CONNECTIVITY_SERVICE);
        while (!connMgr.getNetworkInfo(ConnectivityManager.TYPE_WIFI).isConnected()) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        String response = restTemplate.postForObject("http://epaper.derbund.ch", form, String.class);
        boolean loginSuccessful = response.contains("flashcontent");
        Log.d(MainActivity.class.getName(), "Login successful: " + loginSuccessful);
        return loginSuccessful;
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
