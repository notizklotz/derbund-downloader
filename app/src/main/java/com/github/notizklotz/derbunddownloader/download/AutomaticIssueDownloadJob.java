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

import android.os.Bundle;
import android.support.annotation.NonNull;

import com.evernote.android.job.Job;
import com.github.notizklotz.derbunddownloader.R;
import com.github.notizklotz.derbunddownloader.analytics.FirebaseEvents;
import com.github.notizklotz.derbunddownloader.analytics.FirebaseParams;
import com.github.notizklotz.derbunddownloader.common.DateHandlingUtils;
import com.github.notizklotz.derbunddownloader.common.NotificationService;
import com.github.notizklotz.derbunddownloader.common.WifiCommandExecutor;
import com.github.notizklotz.derbunddownloader.common.WifiConnectionFailedException;
import com.github.notizklotz.derbunddownloader.common.WifiNotEnabledException;
import com.github.notizklotz.derbunddownloader.settings.Settings;
import com.google.firebase.analytics.FirebaseAnalytics;

import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;

import java.io.IOException;
import java.util.concurrent.Callable;

class AutomaticIssueDownloadJob extends Job {

    static final String TAG_PERIODIC = "AutomaticIssueDownloadJob";

    static final String TAG_FALLBACK = "AutomaticIssueDownloadJobFallback";

    private final WifiCommandExecutor wifiCommandExecutor;

    private final Settings settings;

    private final FirebaseAnalytics firebaseAnalytics;

    private final NotificationService notificationService;

    private final IssueDownloader issueDownloader;

    private final AutomaticDownloadScheduler automaticDownloadScheduler;

    AutomaticIssueDownloadJob(WifiCommandExecutor wifiCommandExecutor,
                              Settings settings,
                              FirebaseAnalytics firebaseAnalytics,
                              NotificationService notificationService,
                              IssueDownloader issueDownloader,
                              AutomaticDownloadScheduler automaticDownloadScheduler) {
        this.wifiCommandExecutor = wifiCommandExecutor;
        this.settings = settings;
        this.firebaseAnalytics = firebaseAnalytics;
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

        final boolean wifiOnly = settings.isWifiOnly();

        boolean retry = false;
        try {

            if (wifiOnly) {
                wifiCommandExecutor.execute(new Callable() {
                    @Override
                    public Object call() throws Exception {
                        issueDownloader.download(issueDate, "auto");
                        return null;
                    }
                });
            } else {
                issueDownloader.download(issueDate, "auto");
            }

            return Result.SUCCESS;
        }
        catch (WifiConnectionFailedException e) {
            logErrorEvent(issueDate, "Wifi connection failed");
            notificationService.notifyUser(getContext().getText(R.string.download_wifi_connection_failed), getContext().getText(R.string.download_wifi_connection_failed_text), true);
            retry = true;

        } catch (WifiNotEnabledException e) {
            logErrorEvent(issueDate, "Wifi disabled");
            notificationService.notifyUser(getContext().getText(R.string.download_connection_failed), getContext().getText(R.string.download_connection_failed_no_wifi_text), true);

        } catch (EpaperApiInvalidCredentialsException e) {
            logErrorEvent(issueDate, "Invalid credentials");
            notificationService.notifyUser(getContext().getText(R.string.download_login_failed), getContext().getText(R.string.download_login_failed_text), true);

        } catch (EpaperApiInexistingIssueRequestedException e) {
            logErrorEvent(issueDate, "Inexisting issue");

            retry = true;
        } catch (IOException e) {
            logErrorEvent(issueDate, "Connection failed");
            notificationService.notifyUser(getContext().getText(R.string.download_connection_failed), getContext().getText(R.string.download_connection_failed_text), true);

            retry = true;
        } catch (Exception e) {
            logErrorEvent(issueDate, e.getMessage());
            notificationService.notifyUser(getContext().getText(R.string.download_service_error), e.getMessage(), true);

            retry = true;
        } finally {
            automaticDownloadScheduler.scheduleNextPeriodicJob();
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

    private void logErrorEvent(LocalDate issueDate, String cause) {
        Bundle bundle = new Bundle();
        bundle.putString(FirebaseAnalytics.Param.ITEM_ID, issueDate.toString());
        bundle.putString(FirebaseParams.ERROR_CAUSE,  StringUtils.substring(cause, 0, 36));
        firebaseAnalytics.logEvent(FirebaseEvents.DOWNLOAD_ISSUE_ERROR, bundle);
    }
}
