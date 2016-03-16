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
import android.support.annotation.NonNull;

import com.github.notizklotz.derbunddownloader.BuildConfig;
import com.github.notizklotz.derbunddownloader.common.DateHandlingUtils;
import com.github.notizklotz.derbunddownloader.settings.Settings;
import com.github.notizklotz.derbunddownloader.settings.SettingsImpl;

import org.androidannotations.annotations.Bean;
import org.androidannotations.annotations.EBean;
import org.androidannotations.annotations.RootContext;
import org.androidannotations.annotations.SystemService;
import org.joda.time.DateTime;
import org.joda.time.DateTimeConstants;
import org.joda.time.LocalTime;

@EBean
public class AutomaticDownloadScheduler {

    @Bean(SettingsImpl.class)
    Settings settings;

    @SystemService
    AlarmManager alarmManager;

    @RootContext
    Context context;

    public void updateAlarm() {
        boolean autoDownloadEnabled = settings.isAutoDownloadEnabled();
        if (autoDownloadEnabled) {
            schedule();
        } else {
            cancel();
        }
    }

    private void schedule() {
        LocalTime initialAlarmTime = new LocalTime(5, 0);

        if (BuildConfig.DEBUG) {
            initialAlarmTime = new LocalTime(19,52);
        }

        DateTime nextAlarm = new DateTime(DateHandlingUtils.TIMEZONE_SWITZERLAND).withTime(initialAlarmTime);
        if (nextAlarm.isBeforeNow()) {
            nextAlarm = nextAlarm.plusDays(1);
        }
        if (nextAlarm.getDayOfWeek() == DateTimeConstants.SUNDAY) {
            nextAlarm = nextAlarm.plusDays(1);
        }

        final PendingIntent pendingIntent = createPendingIntent(AutomaticDownloadBroadcastReceiver_.class);
        alarmManager.cancel(pendingIntent);
        alarmManager.setInexactRepeating(
                AlarmManager.RTC_WAKEUP,
                nextAlarm.getMillis(),
                AlarmManager.INTERVAL_FIFTEEN_MINUTES,
                pendingIntent);
}


    private void cancel() {
        alarmManager.cancel(createPendingIntent(AutomaticDownloadBroadcastReceiver_.class));
    }

    private PendingIntent createPendingIntent(@NonNull Class<? extends BroadcastReceiver> broadcastReceiver) {
        return PendingIntent.getBroadcast(
                context,
                0,
                new Intent(context, broadcastReceiver),
                PendingIntent.FLAG_UPDATE_CURRENT);
    }

}
