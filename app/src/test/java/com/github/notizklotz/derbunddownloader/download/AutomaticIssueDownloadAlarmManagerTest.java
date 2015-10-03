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

import android.content.Context;
import android.test.suitebuilder.annotation.SmallTest;

import com.github.notizklotz.derbunddownloader.settings.SettingsService;

import org.joda.time.DateTime;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import static org.junit.Assert.assertEquals;

@SmallTest
public class AutomaticIssueDownloadAlarmManagerTest {

    private AutomaticIssueDownloadAlarmManager automaticIssueDownloadAlarmManager;

    @Before
    public void setUp() throws Exception {
        automaticIssueDownloadAlarmManager = AutomaticIssueDownloadAlarmManager_.getInstance_(Mockito.mock(Context.class));
        automaticIssueDownloadAlarmManager.alarmScheduler = Mockito.mock(AlarmScheduler.class);
        automaticIssueDownloadAlarmManager.settingsService = Mockito.mock(SettingsService.class);
    }

    @Test
    public void updateAlarmAutoDownloadDisabled() throws Exception {
        //Prepare
        Mockito.when(automaticIssueDownloadAlarmManager.settingsService.isAutoDownloadEnabled()).thenReturn(false);

        //Execute
        automaticIssueDownloadAlarmManager.updateAlarm();

        //Test
        Mockito.verify(automaticIssueDownloadAlarmManager.alarmScheduler).schedule(AutomaticIssueDownloadAlarmReceiver.class, null);
    }

    @Test
    public void updateAlarmAutoDownloadEnabledButNoTimeGiven() throws Exception {
        //Prepare
        Mockito.when(automaticIssueDownloadAlarmManager.settingsService.isAutoDownloadEnabled()).thenReturn(true);

        //Execute
        automaticIssueDownloadAlarmManager.updateAlarm();

        //Test
        Mockito.verify(automaticIssueDownloadAlarmManager.alarmScheduler).schedule(AutomaticIssueDownloadAlarmReceiver.class, null);
    }

    @Test
    public void updateAlarmAutoDownloadEnabled() throws Exception {
        //Prepare
        Mockito.when(automaticIssueDownloadAlarmManager.settingsService.isAutoDownloadEnabled()).thenReturn(true);
        Mockito.when(automaticIssueDownloadAlarmManager.settingsService.getAutoDownloadTime()).thenReturn("10:11");

        //Execute
        automaticIssueDownloadAlarmManager.updateAlarm();

        //Test
        ArgumentCaptor<DateTime> argument = ArgumentCaptor.forClass(DateTime.class);
        Mockito.verify(automaticIssueDownloadAlarmManager.alarmScheduler).schedule(Mockito.eq(AutomaticIssueDownloadAlarmReceiver.class), argument.capture());
        Assert.assertNotNull(argument.getValue());
        assertEquals(10, argument.getValue().getHourOfDay());
        assertEquals(11, argument.getValue().getMinuteOfHour());
    }

    @Test
    public void calculateAlarmSameDay() {
        //Prepare
        DateTime now = new DateTime(2015, 9, 1, 9, 0);

        //Execute
        DateTime nextAlarm = automaticIssueDownloadAlarmManager.calculateNextAlarm(now, 9, 5);

        //Test
        assertEquals(now.plusMinutes(5), nextAlarm);
    }
}