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

import com.evernote.android.job.JobManager;
import com.evernote.android.job.JobRequest;
import com.evernote.android.job.util.JobApi;
import com.github.notizklotz.derbunddownloader.BuildConfig;
import com.github.notizklotz.derbunddownloader.common.DateHandlingUtils;
import com.github.notizklotz.derbunddownloader.settings.Settings;

import org.joda.time.DateTime;
import org.joda.time.DateTimeConstants;
import org.joda.time.Duration;
import org.joda.time.LocalDate;
import org.joda.time.LocalTime;

import java.util.HashSet;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class AutomaticDownloadScheduler {

    private final Settings settings;

    private final JobManager jobManager;

    private static final Set<LocalDate> HOLIDAYS = new HashSet<LocalDate>();

    static {
        HOLIDAYS.add(new LocalDate(2016, 3, 28));
        HOLIDAYS.add(new LocalDate(2016, 5, 5));
        HOLIDAYS.add(new LocalDate(2016, 5, 16));
        HOLIDAYS.add(new LocalDate(2016, 8, 1));
        HOLIDAYS.add(new LocalDate(2016, 12, 26));

        HOLIDAYS.add(new LocalDate(2017, 1, 2));
        HOLIDAYS.add(new LocalDate(2017, 4, 14));
        HOLIDAYS.add(new LocalDate(2017, 4, 17));
        HOLIDAYS.add(new LocalDate(2017, 5, 25));
        HOLIDAYS.add(new LocalDate(2017, 6, 5));
        HOLIDAYS.add(new LocalDate(2017, 8, 1));
        HOLIDAYS.add(new LocalDate(2017, 12, 25));
        HOLIDAYS.add(new LocalDate(2017, 12, 26));
    }

    @Inject
    public AutomaticDownloadScheduler(Settings settings, JobManager jobManager) {
        this.settings = settings;
        this.jobManager = jobManager;
    }

    public void update() {
        if (settings.isAutoDownloadEnabled()) {
            if (jobManager.getAllJobRequestsForTag(AutomaticIssueDownloadJob.TAG).isEmpty()) {
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
            alarmTime = new LocalTime().plusMinutes(2);
        }

        DateTime nextAlarmSwissTime = new DateTime(DateHandlingUtils.TIMEZONE_SWITZERLAND).withTime(alarmTime);
        if (nextAlarmSwissTime.isBeforeNow()) {
            nextAlarmSwissTime = nextAlarmSwissTime.plusDays(1);
        }
        if (!BuildConfig.DEBUG) {
            if (nextAlarmSwissTime.getDayOfWeek() == DateTimeConstants.SUNDAY) {
                nextAlarmSwissTime = nextAlarmSwissTime.plusDays(1);
            }

            while (nextAlarmSwissTime.getDayOfWeek() == DateTimeConstants.SUNDAY || HOLIDAYS.contains(nextAlarmSwissTime.toLocalDate())) {
                nextAlarmSwissTime = nextAlarmSwissTime.plusDays(1);
            }
        }

        DateTime nowDeviceTime = DateTime.now();
        Duration windowStart = new Duration(nowDeviceTime, nextAlarmSwissTime);
        Duration windowEnd =  new Duration(nowDeviceTime, nextAlarmSwissTime.plusMinutes(5));

        JobRequest.Builder builder = new JobRequest.Builder(AutomaticIssueDownloadJob.TAG)
                .setPersisted(true).setRequirementsEnforced(false);
        if (JobApi.V_14.equals(jobManager.getApi())) {
            //Currently, com.evernote.android.job.v14.JobProxy14 doesn't use RTC_WAKEUP for non-exact jobs.
            builder.setExact(windowStart.getMillis());
        } else {
            builder.setExecutionWindow(windowStart.getMillis(), windowEnd.getMillis());

            if (settings.isWifiOnly()) {
                builder.setRequiredNetworkType(JobRequest.NetworkType.UNMETERED);
            } else {
                builder.setRequiredNetworkType(JobRequest.NetworkType.CONNECTED);
            }
        }

        builder.build().schedule();

    }

    private void cancel() {
        jobManager.cancelAllForTag(AutomaticIssueDownloadJob.TAG);
    }

}
