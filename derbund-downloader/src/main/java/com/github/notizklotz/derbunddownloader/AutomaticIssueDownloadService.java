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
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.WifiManager;
import android.os.IBinder;

/**
 * Wrapper service around {@link com.github.notizklotz.derbunddownloader.IssueDownloadService} which
 * is intendet to be used together with {@link AutomaticIssueDownloadAlarmReceiver}. Optimized to hold
 * wakelocks and Wifi locks together with {@link AutomaticIssueDownloadAlarmReceiver}
 */
public class AutomaticIssueDownloadService extends Service {

    public static final String DAY = "day";
    public static final String MONTH = "month";
    public static final String YEAR = "year";

    private Intent intent;
    private WifiManager.WifiLock myWifiLock;

    private final AutomaticIssueDownloadCompletedReceiver automaticIssueDownloadCompletedReceiver = new AutomaticIssueDownloadCompletedReceiver();

    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        this.intent = intent;

        WifiManager wm = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        myWifiLock = wm.createWifiLock(WifiManager.WIFI_MODE_FULL, "IssueDownloadWifilock");
        myWifiLock.acquire();

        registerReceiver(automaticIssueDownloadCompletedReceiver, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));

        if (!(intent.hasExtra(DAY) && intent.hasExtra(MONTH) && intent.hasExtra(YEAR))) {
            throw new IllegalArgumentException("Intent is missing extras");
        }

        IssueDownloadService.startDownload(this, intent.getIntExtra(DAY, 0), intent.getIntExtra(MONTH, 0), intent.getIntExtra(YEAR, 0));

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        if (myWifiLock != null) {
            myWifiLock.release();
        }

        unregisterReceiver(automaticIssueDownloadCompletedReceiver);

        if (intent != null) {
            AutomaticIssueDownloadAlarmReceiver.completeWakefulIntent(intent);
        }
    }
}
