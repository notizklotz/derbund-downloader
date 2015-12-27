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
import android.content.Intent;
import android.support.v4.content.WakefulBroadcastReceiver;
import android.util.Log;

import com.github.notizklotz.derbunddownloader.common.DateHandlingUtils;
import com.github.notizklotz.derbunddownloader.settings.Settings;
import com.github.notizklotz.derbunddownloader.settings.SettingsImpl;

import org.androidannotations.annotations.Bean;
import org.androidannotations.annotations.EReceiver;
import org.joda.time.DateTime;
import org.joda.time.Instant;
import org.joda.time.format.ISODateTimeFormat;

/**
 * Triggered by an alarm to automatically download the issue of today.
 */
@EReceiver
public class AutomaticIssueDownloadAlarmReceiver extends WakefulBroadcastReceiver {

    @Bean(SettingsImpl.class)
    Settings settings;

    @Bean
    AutomaticIssueDownloadAlarmManager automaticIssueDownloadAlarmManager;

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.i("AutomaticIssueDownload", "Starting service @ " + ISODateTimeFormat.dateTime().print(Instant.now()));

        settings.setLastWakeup(DateHandlingUtils.toFullStringUserTimezone(DateTime.now()));
        automaticIssueDownloadAlarmManager.updateAlarm();
        callDownloadService(context);
    }

    private void callDownloadService(Context context) {
        DateTime now = DateTime.now(DateHandlingUtils.TIMEZONE_SWITZERLAND);
        Intent intent = IssueDownloadService_.intent(context).downloadIssue(now.getDayOfMonth(), now.getMonthOfYear(), now.getYear()).get();
        startWakefulService(context, intent);
    }

}
