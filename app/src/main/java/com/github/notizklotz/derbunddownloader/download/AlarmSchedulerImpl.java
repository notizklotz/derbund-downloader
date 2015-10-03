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
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.androidannotations.annotations.EBean;
import org.androidannotations.annotations.RootContext;
import org.androidannotations.annotations.SystemService;
import org.joda.time.DateTime;

@EBean
public class AlarmSchedulerImpl implements AlarmScheduler {

    @RootContext
    Context context;

    @SystemService
    AlarmManager alarmManager;

    @Override
    public void schedule(@NonNull Class<? extends BroadcastReceiver> broadcastReceiver, @Nullable DateTime trigger) {
        //Update enforces reusing of an existing PendingIntent instance so AlarmManager.cancel(pi)
        //actually cancels the alarm. FLAG_CANCEL_CURRENT cancels the PendingIndent but then there's no
        //way to cancel the alarm programmatically. However, the Intent won't be executed anyway because
        //the fired alarm can't executed the cancelled PendingIntent.
        final PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                0,
                new Intent(context, broadcastReceiver),
                PendingIntent.FLAG_UPDATE_CURRENT);
        alarmManager.cancel(pendingIntent);

        if (trigger != null) {
            // Make sure wakeup trigger is exact across all API versions.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, trigger.getMillis(), pendingIntent);
            } else {
                alarmManager.set(AlarmManager.RTC_WAKEUP, trigger.getMillis(), pendingIntent);
            }
        }
    }
}
