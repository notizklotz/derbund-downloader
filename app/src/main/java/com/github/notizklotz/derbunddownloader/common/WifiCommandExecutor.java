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

package com.github.notizklotz.derbunddownloader.common;

import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.support.annotation.NonNull;
import android.util.Log;

import org.androidannotations.annotations.AfterInject;
import org.androidannotations.annotations.EBean;
import org.androidannotations.annotations.SystemService;

import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

@EBean
public class WifiCommandExecutor {
    private static final String LOG_TAG = "WifiCommandExecutor";
    private static final long WIFI_RECHECK_WAIT_MILLIS = TimeUnit.SECONDS.toMillis(5);
    private static final long WIFI_CHECK_MAX_MILLIS = TimeUnit.MINUTES.toMillis(2);

    @SystemService
    WifiManager wifiManager;

    @SystemService
    ConnectivityManager connectivityManager;

    private WifiManager.WifiLock myWifiLock;

    @AfterInject
    public void init() {
        myWifiLock = wifiManager.createWifiLock(WifiManager.WIFI_MODE_FULL_HIGH_PERF, "ePaperDownloader");
        myWifiLock.setReferenceCounted(true);
    }

    /**
     * Makes sure Wifi connection is available and stays until callable is completed. This method is thread safe.
     *
     * @throws WifiNotEnabledException if Wifi is not enabled.
     * @throws WifiConnectionFailedException if Wifi connection could not be obtained within {@link #WIFI_CHECK_MAX_MILLIS}.
     */
    @NonNull
    public <T> T execute(@NonNull Callable<T> callable) throws Exception {
        if (!wifiManager.isWifiEnabled()) {
            throw new WifiNotEnabledException();
        }

        myWifiLock.acquire();
        try {
            wifiManager.reconnect();
            boolean connected = waitForWifiConnection();
            if (connected) {
                return callable.call();
            } else {
                throw new WifiConnectionFailedException();
            }
        } finally {
            myWifiLock.release();
        }
    }

    private boolean waitForWifiConnection() throws WifiNotEnabledException {

        boolean connected;

        //Wait for Wifi coming up
        long firstCheckMillis = System.currentTimeMillis();

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
        return connected;
    }

}
