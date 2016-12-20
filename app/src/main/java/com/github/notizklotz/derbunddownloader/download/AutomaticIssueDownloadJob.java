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

import android.net.wifi.WifiManager;
import android.support.annotation.NonNull;

import com.evernote.android.job.Job;
import com.github.notizklotz.derbunddownloader.R;
import com.github.notizklotz.derbunddownloader.common.DateHandlingUtils;
import com.github.notizklotz.derbunddownloader.common.NotificationService;
import com.github.notizklotz.derbunddownloader.settings.Settings;
import com.google.firebase.crash.FirebaseCrash;

import org.joda.time.DateTime;
import org.joda.time.LocalDate;

class AutomaticIssueDownloadJob extends Job {

    static final String TAG_PERIODIC = "AutomaticIssueDownloadJob";

    static final String TAG_FALLBACK = "AutomaticIssueDownloadJobFallback";

    private final Settings settings;

    private final NotificationService notificationService;

    private final IssueDownloader issueDownloader;

    private final AutomaticDownloadScheduler automaticDownloadScheduler;

    private final WifiManager wifiManager;

    AutomaticIssueDownloadJob(WifiManager wifiManager,
                              Settings settings,
                              NotificationService notificationService,
                              IssueDownloader issueDownloader,
                              AutomaticDownloadScheduler automaticDownloadScheduler) {
        this.wifiManager = wifiManager;
        this.settings = settings;
        this.notificationService = notificationService;
        this.issueDownloader = issueDownloader;
        this.automaticDownloadScheduler = automaticDownloadScheduler;
    }

    @NonNull
    @Override
    protected Result onRunJob(Params params) {
        settings.setLastWakeup(DateTime.now().toString());

        DateTime nowInSwitzerland = DateTime.now(DateHandlingUtils.TIMEZONE_SWITZERLAND);
        final LocalDate issueDate = new LocalDate(nowInSwitzerland.getYear(), nowInSwitzerland.getMonthOfYear(), nowInSwitzerland.getDayOfMonth());



        boolean retry = false;
        try {

            if (isRequirementNetworkTypeMet()) {
                issueDownloader.download(issueDate, "auto");
                return Result.SUCCESS;
            } else {
                if (settings.isWifiOnly() && !wifiManager.isWifiEnabled()) {
                    notificationService.notifyUser(getContext().getText(R.string.download_connection_failed), getContext().getText(R.string.download_connection_failed_no_wifi_text), true);
                } else {
                    retry = true;
                }
            }
        } catch (EpaperApiInvalidCredentialsException e) {
            FirebaseCrash.log("Invalid credentials");
            FirebaseCrash.report(e);
            notificationService.notifyUser(getContext().getText(R.string.download_login_failed), getContext().getText(R.string.download_login_failed_text), true);
        } catch (EpaperApiInexistingIssueRequestedException e) {
            FirebaseCrash.log("Inexisting issue");
            FirebaseCrash.report(e);
            retry = true;
        } catch (Exception e) {
            FirebaseCrash.report(e);
            notificationService.notifyUser(getContext().getText(R.string.download_service_error), e.getMessage(), true);

            retry = true;
        } finally {
            if (TAG_PERIODIC.equals(params.getTag())) {
                automaticDownloadScheduler.scheduleNextPeriodicJob();
            }
        }

        if (TAG_PERIODIC.equals(params.getTag())) {
            if (retry) {
                automaticDownloadScheduler.scheduleFallbackJob();
            }

            return Result.FAILURE;
        } else if (TAG_FALLBACK.equals(params.getTag())) {
            return retry ? Result.RESCHEDULE : Result.FAILURE;
        }

        return Result.FAILURE;
    }

}
