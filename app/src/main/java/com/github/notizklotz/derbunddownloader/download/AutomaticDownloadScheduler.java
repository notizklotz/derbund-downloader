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

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.util.Log;

import com.github.notizklotz.derbunddownloader.settings.Settings;
import com.github.notizklotz.derbunddownloader.settings.SettingsImpl;

import org.androidannotations.annotations.Bean;
import org.androidannotations.annotations.EBean;
import org.androidannotations.annotations.RootContext;
import org.androidannotations.annotations.SystemService;

import java.util.concurrent.TimeUnit;

@EBean
public class AutomaticDownloadScheduler {

    private static final String TAG = "AutoDownloadScheduler";

    @Bean(SettingsImpl.class)
    Settings settings;

    @SystemService
    AlarmManager alarmManager;

    @RootContext
    Context context;

    public void updateAlarm() {
        boolean autoDownloadEnabled = settings.isAutoDownloadEnabled();
        if (autoDownloadEnabled) {
            scheduleHalfHourly(AutomaticDownloadBroadcastReceiver_.class);
        } else {
            cancel(AutomaticDownloadBroadcastReceiver_.class);
        }
    }

    private void scheduleHalfHourly(@NonNull Class<? extends BroadcastReceiver> broadcastReceiver) {
        Log.d(TAG, "Scheduling auto download alarm");

        final PendingIntent pendingIntent = createPendingIntent(broadcastReceiver);
        alarmManager.cancel(pendingIntent);
        alarmManager.setInexactRepeating(
                AlarmManager.ELAPSED_REALTIME_WAKEUP,
                SystemClock.elapsedRealtime() + TimeUnit.MINUTES.toMillis(1),
                AlarmManager.INTERVAL_HALF_HOUR,
                pendingIntent);
    }

    private void cancel(@NonNull Class<? extends BroadcastReceiver> broadcastReceiver) {
        Log.d(TAG, "Canceling auto download alarm");

        alarmManager.cancel(createPendingIntent(broadcastReceiver));
    }

    private PendingIntent createPendingIntent(@NonNull Class<? extends BroadcastReceiver> broadcastReceiver) {
        return PendingIntent.getBroadcast(
                context,
                0,
                new Intent(context, broadcastReceiver),
                PendingIntent.FLAG_UPDATE_CURRENT);
    }

}
