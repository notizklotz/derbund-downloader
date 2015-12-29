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

import android.util.Log;

import com.github.notizklotz.derbunddownloader.common.AlarmScheduler;
import com.github.notizklotz.derbunddownloader.common.DateHandlingUtils;
import com.github.notizklotz.derbunddownloader.common.internal.AlarmSchedulerImpl;
import com.github.notizklotz.derbunddownloader.settings.Settings;
import com.github.notizklotz.derbunddownloader.settings.SettingsImpl;
import com.github.notizklotz.derbunddownloader.settings.TimePickerPreference;

import org.androidannotations.annotations.Bean;
import org.androidannotations.annotations.EBean;
import org.joda.time.DateTime;
import org.joda.time.DateTimeConstants;
import org.springframework.util.StringUtils;

@EBean
public class AutomaticDownloadScheduler {

    @Bean(SettingsImpl.class)
    Settings settings;

    @Bean(AlarmSchedulerImpl.class)
    AlarmScheduler alarmScheduler;

    public void updateAlarm() {
        Log.d(AutomaticDownloadScheduler.class.getSimpleName(), "Updating automatic download alarm");
        DateTime trigger = null;

        boolean autoDownloadEnabled = settings.isAutoDownloadEnabled();
        String autoDownloadTime = settings.getAutoDownloadTime();
        if (autoDownloadEnabled && StringUtils.hasText(autoDownloadTime)) {
            final Integer[] hourMinute = TimePickerPreference.toHourMinuteIntegers(autoDownloadTime);
            trigger = calculateNextAlarm(DateTime.now(), hourMinute[0], hourMinute[1]);
        }

        alarmScheduler.scheduleExact(AutomaticDownloadBroadcastReceiver_.class, trigger);

        settings.updateNextWakeup(trigger != null ? DateHandlingUtils.toFullStringUserTimezone(trigger) : null);
    }

    protected DateTime calculateNextAlarm(DateTime now, int hourOfDay, int minute) {
        DateTime nextAlarm = now.withTime(hourOfDay, minute, 0, 0);

        //Make sure trigger is in the future (incl. grace time)
        if (nextAlarm.isBefore(now.plusSeconds(10))) {
            nextAlarm = nextAlarm.plusDays(1);
        }
        //Do not schedule on Sundays in Switzerland as the newspaper is not issued on Sundays
        if ((nextAlarm.withZone(DateHandlingUtils.TIMEZONE_SWITZERLAND).getDayOfWeek() == DateTimeConstants.SUNDAY)) {
            nextAlarm = nextAlarm.plusDays(1);
        }
        return nextAlarm;
    }

}
