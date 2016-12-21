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

import android.app.Application;
import android.app.IntentService;
import android.content.Intent;
import android.os.Bundle;

import com.github.notizklotz.derbunddownloader.DerBundDownloaderApplication;
import com.github.notizklotz.derbunddownloader.R;
import com.github.notizklotz.derbunddownloader.common.NotificationService;
import com.google.firebase.crash.FirebaseCrash;

import org.joda.time.LocalDate;

import java.io.IOException;

import javax.inject.Inject;

public class IssueDownloadIntentService extends IntentService {

    private final static String ACTION_DOWNLOAD_ISSUE = "downloadIssue";
    private final static String DAY_EXTRA = "day";
    private final static String MONTH_EXTRA = "month";
    private final static String YEAR_EXTRA = "year";

    @Inject
    NotificationService notificationService;

    @Inject
    IssueDownloader issueDownloader;

    public IssueDownloadIntentService() {
        super("IssueDownloadIntentService");
    }

    @Override
    public void onCreate() {
        super.onCreate();

        ((DerBundDownloaderApplication)getApplication()).getDownloadComponent().inject(this);
    }

    private void downloadIssue(int day, int month, int year) {
        final LocalDate issueDate = new LocalDate(year, month, day);
        try {
            issueDownloader.download(issueDate, "manual");
        } catch (IOException e) {
            FirebaseCrash.log(e.getMessage());
            FirebaseCrash.report(e);
            notificationService.notifyUser(this.getText(R.string.download_connection_failed), this.getText(R.string.download_connection_failed_text), true);
        } catch (EpaperApiInexistingIssueRequestedException | EpaperApiInvalidResponseException e) {
            FirebaseCrash.log(e.getMessage());
            FirebaseCrash.report(e);
            notificationService.notifyUser(this.getText(R.string.download_service_error), e.getMessage(), true);
        } catch (EpaperApiInvalidCredentialsException e) {
            FirebaseCrash.log(e.getMessage());
            FirebaseCrash.report(e);
            notificationService.notifyUser(this.getText(R.string.download_login_failed), this.getText(R.string.download_login_failed_text), true);
        }
    }

    public static void startDownload(Application application, int day, int month, int year) {
        Intent intent = new Intent(application, IssueDownloadIntentService.class);
        intent.setAction(ACTION_DOWNLOAD_ISSUE);
        intent.putExtra(DAY_EXTRA, day);
        intent.putExtra(MONTH_EXTRA, month);
        intent.putExtra(YEAR_EXTRA, year);

        application.startService(intent);
    }

    @Override
    public void onHandleIntent(Intent intent) {
        String action = intent.getAction();
        if (ACTION_DOWNLOAD_ISSUE.equals(action)) {
            Bundle extras = intent.getExtras();
            if (extras!= null) {
                int dayExtra = extras.getInt(DAY_EXTRA);
                int monthExtra = extras.getInt(MONTH_EXTRA);
                int yearExtra = extras.getInt(YEAR_EXTRA);
                downloadIssue(dayExtra, monthExtra, yearExtra);
            }
        }
    }

}
