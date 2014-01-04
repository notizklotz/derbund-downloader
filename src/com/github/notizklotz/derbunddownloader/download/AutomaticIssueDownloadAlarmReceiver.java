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
import android.content.SharedPreferences;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.util.Log;
import com.github.notizklotz.derbunddownloader.settings.Settings;

import java.util.Calendar;

/**
 * Triggered by an alarm to automatically download the issue of today.
 */
public class AutomaticIssueDownloadAlarmReceiver extends CustomWakefulBroadcastReceiver {

    private static final long WAKE_LOCK_TIMEOUT = 2 * 60 * 1000;

    private static final String LOG_TAG = AutomaticIssueDownloadAlarmReceiver.class.getSimpleName();

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(LOG_TAG, "I woke up this morning and got ready to start the service");

        final Calendar c = Calendar.getInstance();
        if (!(c.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY)) {
            int day = c.get(Calendar.DAY_OF_MONTH);
            int month = c.get(Calendar.MONTH) + 1;
            int year = c.get(Calendar.YEAR);

            Intent service = new Intent(context, IssueDownloadService.class);
            service.putExtra(IssueDownloadService.EXTRA_DAY, day);
            service.putExtra(IssueDownloadService.EXTRA_MONTH, month);
            service.putExtra(IssueDownloadService.EXTRA_YEAR, year);

            final SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
            final boolean altWakelockMode = sharedPref.getBoolean(Settings.KEY_ALT_WAKELOCK_MODE, false);
            final int wakelockMode = altWakelockMode ? PowerManager.SCREEN_DIM_WAKE_LOCK : PowerManager.PARTIAL_WAKE_LOCK;

            //noinspection deprecation
            startWakefulService(context, service, wakelockMode, WAKE_LOCK_TIMEOUT);
        }
    }
}
