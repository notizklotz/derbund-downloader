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
import android.os.PowerManager;
import android.support.v4.content.WakefulBroadcastReceiver;
import android.util.Log;

import com.github.notizklotz.derbunddownloader.analytics.AnalyticsCategory;
import com.github.notizklotz.derbunddownloader.analytics.AnalyticsTracker;
import com.github.notizklotz.derbunddownloader.common.DateHandlingUtils;
import com.github.notizklotz.derbunddownloader.settings.Settings;
import com.github.notizklotz.derbunddownloader.settings.SettingsImpl;

import org.androidannotations.annotations.Bean;
import org.androidannotations.annotations.EReceiver;
import org.androidannotations.annotations.SystemService;
import org.joda.time.DateTime;
import org.joda.time.DateTimeConstants;
import org.joda.time.LocalDate;

/**
 * Triggered by an alarm to automatically download the issue of today.
 */
@EReceiver
public class AutomaticDownloadBroadcastReceiver extends WakefulBroadcastReceiver {

    private static final String TAG = "AutomaticDownloadBR";

    @Bean(SettingsImpl.class)
    Settings settings;

    @Bean
    AutomaticDownloadScheduler automaticDownloadScheduler;

    @Bean
    AnalyticsTracker analyticsTracker;

    @SystemService
    PowerManager powerManager;

    @Bean
    AutomaticallyDownloadedIssuesRegistry automaticallyDownloadedIssuesRegistry;

    @Override
    public void onReceive(Context context, Intent intent) {
        settings.setLastWakeup(DateHandlingUtils.toFullStringUserTimezone(DateTime.now()));
        callDownloadService(context);
    }

    private void callDownloadService(Context context) {
        DateTime now = DateTime.now(DateHandlingUtils.TIMEZONE_SWITZERLAND);

        int year = now.getYear();
        int monthOfYear = now.getMonthOfYear();
        int dayOfMonth = now.getDayOfMonth();

        LocalDate issueDate = new LocalDate(year, monthOfYear, dayOfMonth);
        if (!automaticallyDownloadedIssuesRegistry.isRegisteredAsDownloaded(issueDate)) {

            //Do not schedule on Sundays in Switzerland as the newspaper is not issued on Sundays
            if ((now.getDayOfWeek() != DateTimeConstants.SUNDAY)) {
                analyticsTracker.sendWithCustomDimensions(AnalyticsTracker.createEventBuilder(AnalyticsCategory.Download)
                        .setAction("auto").setLabel(issueDate.toString()).setValue(1));

                Intent intent = IssueDownloadService_.intent(context).downloadIssue(dayOfMonth, monthOfYear, year).get();
                startWakefulService(context, intent);
            } else {
                Log.d(TAG, "callDownloadService: Skipping download. It's Sunday.");
            }
        } else {
            Log.d(TAG, "callDownloadService: Skipping download. Issue is registered as already downloaded");
        }
    }

}
