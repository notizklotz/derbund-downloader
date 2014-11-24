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
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.preference.PreferenceManager;

import com.github.notizklotz.derbunddownloader.common.DateHandlingUtils;
import com.github.notizklotz.derbunddownloader.settings.Settings;
import com.github.notizklotz.derbunddownloader.settings.TimePickerPreference;

import org.androidannotations.annotations.EBean;
import org.androidannotations.annotations.RootContext;
import org.androidannotations.annotations.SystemService;

import java.util.Calendar;

@EBean
public class AutomaticIssueDownloadAlarmManager {

    @SystemService
    AlarmManager alarmManager;

    @RootContext
    Context context;

    public void updateAlarm() {
        final Context applicationContext = context.getApplicationContext();
        final PendingIntent pendingIntent = PendingIntent.getBroadcast(
                applicationContext, 0,
                new Intent(applicationContext, AutomaticIssueDownloadAlarmReceiver.class),
                PendingIntent.FLAG_CANCEL_CURRENT);
        final SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        if (sharedPreferences.getBoolean(Settings.KEY_AUTO_DOWNLOAD_ENABLED, false)) {
            String auto_download_time = sharedPreferences.getString(Settings.KEY_AUTO_DOWNLOAD_TIME, null);
            if (auto_download_time != null) {
                Integer[] time = TimePickerPreference.toHourMinuteIntegers(auto_download_time);
                Calendar alarmTrigger = Calendar.getInstance();
                alarmTrigger.clear();

                Calendar now = Calendar.getInstance();
                //noinspection MagicConstant
                alarmTrigger.set(now.get(Calendar.YEAR), now.get(Calendar.MONTH), now.get(Calendar.DAY_OF_MONTH), time[0], time[1]);

                //Make sure trigger is in the future
                if (now.after(alarmTrigger)) {
                    alarmTrigger.roll(Calendar.DAY_OF_MONTH, true);
                }

                //Do not schedule Sundays as the newspaper is not issued on Sundays
                if (!(alarmTrigger.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY)) {
                    alarmTrigger.roll(Calendar.DAY_OF_MONTH, true);
                }

                // Make sure wakeup trigger is exact.
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    alarmManager.setExact(AlarmManager.RTC_WAKEUP, alarmTrigger.getTimeInMillis(), pendingIntent);
                } else {
                    alarmManager.set(AlarmManager.RTC_WAKEUP, alarmTrigger.getTimeInMillis(), pendingIntent);
                }

                sharedPreferences.edit().putString(Settings.KEY_NEXT_WAKEUP, DateHandlingUtils.toFullStringDefaultTimezone(alarmTrigger.getTimeInMillis())).apply();
            }
        } else {
            alarmManager.cancel(pendingIntent);
            sharedPreferences.edit().remove(Settings.KEY_NEXT_WAKEUP).apply();
        }
    }
}
