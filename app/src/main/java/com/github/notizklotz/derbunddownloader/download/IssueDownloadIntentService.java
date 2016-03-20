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

import android.app.IntentService;
import android.content.Intent;

import com.github.notizklotz.derbunddownloader.R;
import com.github.notizklotz.derbunddownloader.analytics.AnalyticsCategory;
import com.github.notizklotz.derbunddownloader.analytics.AnalyticsTracker;
import com.github.notizklotz.derbunddownloader.common.NotificationService;
import com.github.notizklotz.derbunddownloader.common.WifiCommandExecutor;
import com.github.notizklotz.derbunddownloader.settings.Settings;
import com.github.notizklotz.derbunddownloader.settings.SettingsImpl;

import org.androidannotations.annotations.Bean;
import org.androidannotations.annotations.EIntentService;
import org.androidannotations.annotations.ServiceAction;
import org.joda.time.LocalDate;

import java.io.IOException;

import static com.github.notizklotz.derbunddownloader.analytics.AnalyticsTracker.createEventBuilder;

@EIntentService
public class IssueDownloadIntentService extends IntentService {

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

    public IssueDownloadIntentService() {
        super("IssueDownloadIntentService");
    }

    @ServiceAction
    public void downloadIssue(int day, int month, int year) {
        final LocalDate issueDate = new LocalDate(year, month, day);
        try {
            issueDownloader.download(issueDate);
        } catch (IOException e) {
            notificationService.notifyUser(this.getText(R.string.download_connection_failed), this.getText(R.string.download_connection_failed_text), true);
        } catch (EpaperApiInexistingIssueRequestedException e) {
            analyticsTracker.sendDefaultException(this, e);
            notificationService.notifyUser(this.getText(R.string.download_service_error), this.getText(R.string.download_service_error_text), e.getMessage(), true);
        } catch (EpaperApiInvalidResponseException e) {
            analyticsTracker.sendDefaultException(this, e);
            notificationService.notifyUser(this.getText(R.string.download_service_error), this.getText(R.string.download_service_error_text), e.getMessage(), true);
        } catch (EpaperApiInvalidCredentialsException e) {
            analyticsTracker.sendWithCustomDimensions(createEventBuilder(AnalyticsCategory.Error).setAction("Invalid credentials").setNonInteraction(true));
            notificationService.notifyUser(this.getText(R.string.download_login_failed), this.getText(R.string.download_login_failed_text), true);
        }
    }

    @Override
    protected void onHandleIntent(Intent intent) {

    }
}
