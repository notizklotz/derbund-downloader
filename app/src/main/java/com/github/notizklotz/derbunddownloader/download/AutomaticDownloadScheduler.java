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

import com.evernote.android.job.JobManager;
import com.evernote.android.job.JobRequest;
import com.github.notizklotz.derbunddownloader.BuildConfig;
import com.github.notizklotz.derbunddownloader.common.DateHandlingUtils;
import com.github.notizklotz.derbunddownloader.settings.Settings;
import com.github.notizklotz.derbunddownloader.settings.SettingsImpl;

import org.androidannotations.annotations.Bean;
import org.androidannotations.annotations.EBean;
import org.androidannotations.annotations.RootContext;
import org.joda.time.DateTime;
import org.joda.time.DateTimeConstants;
import org.joda.time.Duration;
import org.joda.time.LocalTime;

@EBean
public class AutomaticDownloadScheduler {

    @RootContext
    Context context;

    @Bean(SettingsImpl.class)
    Settings settings;

    public void update() {
        if (settings.isAutoDownloadEnabled()) {
            if (JobManager.instance().getAllJobRequestsForTag(AutomaticIssueDownloadJob.TAG).isEmpty()) {
                schedule();
            }
        } else {
            cancel();
        }
    }

    public void scheduleNextJobRequest() {
        schedule();
    }

    private void schedule() {
        LocalTime alarmTime = new LocalTime(5, 0);
        if (BuildConfig.DEBUG) {
            alarmTime = new LocalTime(10, 15);
        }

        DateTime nextAlarmSwissTime = new DateTime(DateHandlingUtils.TIMEZONE_SWITZERLAND).withTime(alarmTime);
        if (nextAlarmSwissTime.isBeforeNow()) {
            nextAlarmSwissTime = nextAlarmSwissTime.plusDays(1);
        }
        if (!BuildConfig.DEBUG) {
            if (nextAlarmSwissTime.getDayOfWeek() == DateTimeConstants.SUNDAY) {
                nextAlarmSwissTime = nextAlarmSwissTime.plusDays(1);
            }
        }

        DateTime nowDeviceTime = DateTime.now();
        Duration windowStart = new Duration(nowDeviceTime, nextAlarmSwissTime);
        Duration windowEnd =  new Duration(nowDeviceTime, nextAlarmSwissTime.plusMinutes(5));

        new JobRequest.Builder(AutomaticIssueDownloadJob.TAG)
                .setExecutionWindow(windowStart.getMillis(), windowEnd.getMillis())
                .setPersisted(true)
                .build()
                .schedule();
    }

    private void cancel() {
        JobManager.instance().cancelAllForTag(AutomaticIssueDownloadJob.TAG);
    }

}
