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

import android.support.annotation.NonNull;

import com.evernote.android.job.Job;
import com.github.notizklotz.derbunddownloader.R;
import com.github.notizklotz.derbunddownloader.analytics.AnalyticsCategory;
import com.github.notizklotz.derbunddownloader.analytics.AnalyticsTracker;
import com.github.notizklotz.derbunddownloader.common.DateHandlingUtils;
import com.github.notizklotz.derbunddownloader.common.NotificationService;
import com.github.notizklotz.derbunddownloader.common.WifiCommandExecutor;
import com.github.notizklotz.derbunddownloader.common.WifiConnectionFailedException;
import com.github.notizklotz.derbunddownloader.common.WifiNotEnabledException;
import com.github.notizklotz.derbunddownloader.settings.Settings;
import com.github.notizklotz.derbunddownloader.settings.SettingsImpl;
import com.google.android.gms.analytics.HitBuilders;

import org.androidannotations.annotations.Bean;
import org.androidannotations.annotations.EBean;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;

import java.io.IOException;
import java.util.concurrent.Callable;

import static com.github.notizklotz.derbunddownloader.analytics.AnalyticsTracker.createEventBuilder;

@EBean
public class AutomaticIssueDownloadJob extends Job {

    public static final String TAG = "AutomaticIssueDownloadJob";

    @Bean
    WifiCommandExecutor wifiCommandExecutor;

    @Bean(SettingsImpl.class)
    Settings settings;

    @Bean
    AnalyticsTracker analyticsTracker;

    @Bean
    NotificationService notificationService;

    @Bean
    IssueDownloader issueDownloader;

    @Bean
    AutomaticDownloadScheduler automaticDownloadScheduler;

    @NonNull
    @Override
    protected Result onRunJob(Params params) {
        settings.setLastWakeup(DateTime.now().toString());

        DateTime nowInSwitzerland = DateTime.now(DateHandlingUtils.TIMEZONE_SWITZERLAND);
        final LocalDate issueDate = new LocalDate(nowInSwitzerland.getYear(), nowInSwitzerland.getMonthOfYear(), nowInSwitzerland.getDayOfMonth());

        final boolean wifiOnly = settings.isWifiOnly();
        try {
            if (wifiOnly) {
                wifiCommandExecutor.execute(new Callable() {
                    @Override
                    public Object call() throws Exception {
                        issueDownloader.download(issueDate);
                        return null;
                    }
                });
            } else {
                issueDownloader.download(issueDate);
            }
            analyticsTracker.sendWithCustomDimensions(AnalyticsTracker.createEventBuilder(AnalyticsCategory.Download).setAction("auto").setLabel(issueDate.toString()).setValue(1));

            automaticDownloadScheduler.scheduleNextJobRequest();

            return Result.SUCCESS;
        }
        catch (WifiConnectionFailedException e) {
            analyticsTracker.sendWithCustomDimensions(createEventBuilder(AnalyticsCategory.Error).setAction("Wifi connection failed").setNonInteraction(true));
            notificationService.notifyUser(getContext().getText(R.string.download_wifi_connection_failed), getContext().getText(R.string.download_wifi_connection_failed_text), true);
            return Result.RESCHEDULE;
        } catch (WifiNotEnabledException e) {
            analyticsTracker.sendWithCustomDimensions(createEventBuilder(AnalyticsCategory.Error).setAction("Wifi disabled").setNonInteraction(true));
            notificationService.notifyUser(getContext().getText(R.string.download_connection_failed), getContext().getText(R.string.download_connection_failed_no_wifi_text), true);
            return Result.FAILURE;
        } catch (EpaperApiInvalidCredentialsException e) {
            analyticsTracker.sendWithCustomDimensions(createEventBuilder(AnalyticsCategory.Error).setAction("Invalid credentials").setNonInteraction(true));
            notificationService.notifyUser(getContext().getText(R.string.download_login_failed), getContext().getText(R.string.download_login_failed_text), true);
            return Result.FAILURE;
        } catch (EpaperApiInexistingIssueRequestedException e) {
            analyticsTracker.send(new HitBuilders.ExceptionBuilder().setFatal(false).setDescription("Inexisting issue " + e.getIssueDate().toString()));
            return Result.RESCHEDULE;
        } catch (IOException e) {
            analyticsTracker.sendWithCustomDimensions(createEventBuilder(AnalyticsCategory.Error).setAction("No connection on download").setNonInteraction(true));
            notificationService.notifyUser(getContext().getText(R.string.download_connection_failed), getContext().getText(R.string.download_connection_failed_text), true);
            return Result.RESCHEDULE;
        } catch (Exception e) {
            analyticsTracker.sendDefaultException(getContext(), e);
            notificationService.notifyUser(getContext().getText(R.string.download_service_error), getContext().getText(R.string.download_service_error_text), e.getMessage(), true);
            return Result.FAILURE;
        }
    }
}
