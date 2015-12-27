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

package com.github.notizklotz.derbunddownloader.common.internal;

import android.annotation.TargetApi;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.github.notizklotz.derbunddownloader.common.AlarmScheduler;
import com.github.notizklotz.derbunddownloader.common.ApiLevelChecker;
import com.github.notizklotz.derbunddownloader.common.PendingIntentFactory;

import org.androidannotations.annotations.Bean;
import org.androidannotations.annotations.EBean;
import org.androidannotations.annotations.SystemService;
import org.joda.time.DateTime;

@EBean
public class AlarmSchedulerImpl implements AlarmScheduler {

    @Bean(PendingIntentFactoryImpl.class)
    PendingIntentFactory pendingIntentFactory;

    @SystemService
    AlarmManager alarmManager;

    @Bean(ApiLevelChecker.class)
    ApiLevelChecker apiLevelChecker;

    @Override
    @TargetApi(Build.VERSION_CODES.M)
    public void schedule(@NonNull Class<? extends BroadcastReceiver> broadcastReceiver, @Nullable DateTime trigger) {
        //Update enforces reusing of an existing PendingIntent instance so AlarmManager.cancel(pi)
        //actually cancels the alarm. FLAG_CANCEL_CURRENT cancels the PendingIndent but then there's no
        //way to cancel the alarm programmatically. However, the Intent won't be executed anyway because
        //the fired alarm can't executed the cancelled PendingIntent.
        final PendingIntent pendingIntent = pendingIntentFactory.createPendingIntent(broadcastReceiver);
        alarmManager.cancel(pendingIntent);

        if (trigger != null) {
            // Make sure wakeup trigger is exact across all API versions.
            if (apiLevelChecker.isApiLevelAvailable(Build.VERSION_CODES.M)) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, trigger.getMillis(), pendingIntent);
            } else if (apiLevelChecker.isApiLevelAvailable(Build.VERSION_CODES.KITKAT)) {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, trigger.getMillis(), pendingIntent);
            } else {
                alarmManager.set(AlarmManager.RTC_WAKEUP, trigger.getMillis(), pendingIntent);
            }
        }
    }


}
