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

import com.github.notizklotz.derbunddownloader.common.ApiLevelChecker;
import com.github.notizklotz.derbunddownloader.common.PendingIntentFactory;

import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class AlarmSchedulerImplTest {

    private AlarmSchedulerImpl alarmScheduler;
    private PendingIntent pendingIntent;

    @Before
    public void setup() {
        alarmScheduler = new AlarmSchedulerImpl();
        alarmScheduler.alarmManager = mock(AlarmManager.class);
        alarmScheduler.pendingIntentFactory = mock(PendingIntentFactory.class);
        alarmScheduler.apiLevelChecker = mock(ApiLevelChecker.class);
        pendingIntent = mock(PendingIntent.class);
        when(alarmScheduler.pendingIntentFactory.createPendingIntent(BroadcastReceiver.class)).thenReturn(pendingIntent);
    }

    @Test
    public void testCancelAlarm() throws Exception {
        //Interaction
        alarmScheduler.schedule(BroadcastReceiver.class, null);

        //Verification
        verify(alarmScheduler.alarmManager).cancel(pendingIntent);
        verifyNoMoreInteractions(alarmScheduler.alarmManager);
    }

    @Test
    public void testScheduleAlarmBelowKitkat() throws Exception {
        //Interaction
        DateTime trigger = new DateTime();
        alarmScheduler.schedule(BroadcastReceiver.class, trigger);

        //Verification
        verify(alarmScheduler.alarmManager).cancel(pendingIntent);
        verify(alarmScheduler.alarmManager).set(AlarmManager.RTC_WAKEUP, trigger.getMillis(), pendingIntent);
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    @Test
    public void testScheduleAlarmKitkat() throws Exception {
        when(alarmScheduler.apiLevelChecker.isApiLevelAvailable(Build.VERSION_CODES.KITKAT)).thenReturn(true);

        //Interaction
        DateTime trigger = new DateTime();
        alarmScheduler.schedule(BroadcastReceiver.class, trigger);

        //Verification
        verify(alarmScheduler.alarmManager).cancel(pendingIntent);
        verify(alarmScheduler.alarmManager).setExact(AlarmManager.RTC_WAKEUP, trigger.getMillis(), pendingIntent);
    }

    @TargetApi(Build.VERSION_CODES.M)
    @Test
    public void testScheduleAlarmMarshmallow() throws Exception {
        when(alarmScheduler.apiLevelChecker.isApiLevelAvailable(Build.VERSION_CODES.M)).thenReturn(true);

        //Interaction
        DateTime trigger = new DateTime();
        alarmScheduler.schedule(BroadcastReceiver.class, trigger);

        //Verification
        verify(alarmScheduler.alarmManager).cancel(pendingIntent);
        verify(alarmScheduler.alarmManager).setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, trigger.getMillis(), pendingIntent);
    }
}