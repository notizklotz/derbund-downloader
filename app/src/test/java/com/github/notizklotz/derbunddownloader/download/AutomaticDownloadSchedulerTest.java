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

import android.test.suitebuilder.annotation.SmallTest;

import com.github.notizklotz.derbunddownloader.common.AlarmScheduler;
import com.github.notizklotz.derbunddownloader.common.DateHandlingUtils;
import com.github.notizklotz.derbunddownloader.settings.Settings;

import org.joda.time.DateTime;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import static org.junit.Assert.assertEquals;

@SmallTest
public class AutomaticDownloadSchedulerTest {

    private AutomaticDownloadScheduler automaticDownloadScheduler;

    @Before
    public void setUp() throws Exception {
        automaticDownloadScheduler = new AutomaticDownloadScheduler();
        automaticDownloadScheduler.alarmScheduler = Mockito.mock(AlarmScheduler.class);
        automaticDownloadScheduler.settings = Mockito.mock(Settings.class);
    }

    @Test
    public void updateAlarmAutoDownloadDisabled() throws Exception {
        //Prepare
        Mockito.when(automaticDownloadScheduler.settings.isAutoDownloadEnabled()).thenReturn(false);

        //Execute
        automaticDownloadScheduler.updateAlarm();

        //Test
        Mockito.verify(automaticDownloadScheduler.alarmScheduler).schedule(AutomaticDownloadBroadcastReceiver.class, null);
    }

    @Test
    public void updateAlarmAutoDownloadEnabledButNoTimeGiven() throws Exception {
        //Prepare
        Mockito.when(automaticDownloadScheduler.settings.isAutoDownloadEnabled()).thenReturn(true);

        //Execute
        automaticDownloadScheduler.updateAlarm();

        //Test
        Mockito.verify(automaticDownloadScheduler.alarmScheduler).schedule(AutomaticDownloadBroadcastReceiver.class, null);
    }

    @Test
    public void updateAlarmAutoDownloadEnabled() throws Exception {
        //Prepare
        Mockito.when(automaticDownloadScheduler.settings.isAutoDownloadEnabled()).thenReturn(true);
        Mockito.when(automaticDownloadScheduler.settings.getAutoDownloadTime()).thenReturn("10:11");

        //Execute
        automaticDownloadScheduler.updateAlarm();

        //Test
        ArgumentCaptor<DateTime> argument = ArgumentCaptor.forClass(DateTime.class);
        Mockito.verify(automaticDownloadScheduler.alarmScheduler).schedule(Mockito.eq(AutomaticDownloadBroadcastReceiver.class), argument.capture());
        Assert.assertNotNull(argument.getValue());
        assertEquals(10, argument.getValue().getHourOfDay());
        assertEquals(11, argument.getValue().getMinuteOfHour());
    }

    @Test
    public void calculateAlarmSameDay() {
        //Prepare
        DateTime now = new DateTime(2015, 9, 1, 9, 30, 15, 15, DateHandlingUtils.TIMEZONE_SWITZERLAND);

        //Execute
        DateTime nextAlarm = automaticDownloadScheduler.calculateNextAlarm(now, 9, 35);

        //Test
        assertEquals(new DateTime(2015, 9, 1, 9, 35, DateHandlingUtils.TIMEZONE_SWITZERLAND), nextAlarm);
    }

    @Test
    public void calculateAlarmNextDay() {
        //Prepare
        DateTime now = new DateTime(2015, 9, 1, 9, 30, 15, 15, DateHandlingUtils.TIMEZONE_SWITZERLAND);

        //Execute
        DateTime nextAlarm = automaticDownloadScheduler.calculateNextAlarm(now, 9, 20);

        //Test
        assertEquals(new DateTime(2015, 9, 2, 9, 20, DateHandlingUtils.TIMEZONE_SWITZERLAND), nextAlarm);
    }

    @Test
    public void calculateAlarmMonthRollover() {
        //Prepare
        DateTime now = new DateTime(2015, 9, 30, 9, 30, 15, 15, DateHandlingUtils.TIMEZONE_SWITZERLAND);

        //Execute
        DateTime nextAlarm = automaticDownloadScheduler.calculateNextAlarm(now, 9, 20);

        //Test
        assertEquals(new DateTime(2015, 10, 1, 9, 20, DateHandlingUtils.TIMEZONE_SWITZERLAND), nextAlarm);
    }

    @Test
    public void calculateAlarmSkipSunday() {
        //Prepare
        DateTime now = new DateTime(2015, 12, 5, 18, 30, 15, 15, DateHandlingUtils.TIMEZONE_SWITZERLAND);

        //Execute
        DateTime nextAlarm = automaticDownloadScheduler.calculateNextAlarm(now, 9, 20);

        //Test
        assertEquals(new DateTime(2015, 12, 7, 9, 20, DateHandlingUtils.TIMEZONE_SWITZERLAND), nextAlarm);
    }
}