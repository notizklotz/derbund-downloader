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

package com.github.notizklotz.derbunddownloader;

import android.app.Application;
import android.os.Build;

import com.evernote.android.job.Job;
import com.evernote.android.job.JobCreator;
import com.evernote.android.job.JobManager;
import com.evernote.android.job.util.JobApi;
import com.github.notizklotz.derbunddownloader.download.AutomaticDownloadScheduler;
import com.github.notizklotz.derbunddownloader.download.AutomaticIssueDownloadJob;
import com.github.notizklotz.derbunddownloader.download.AutomaticIssueDownloadJob_;

import net.danlew.android.joda.JodaTimeAndroid;

import org.androidannotations.annotations.Bean;
import org.androidannotations.annotations.EApplication;

@EApplication
public class DerBundDownloaderApplication extends Application {

    @Bean
    AutomaticDownloadScheduler automaticDownloadScheduler;

    @Override
    public void onCreate() {
        super.onCreate();
        JodaTimeAndroid.init(this);
        JobManager jobManager = JobManager.create(this);

        //Doze makes JobApi.V_21 suboptimal for automatic issue downloading as the issue is downloaded too late
        //JobApi.V_14 with android.app.AlarmManager.setExactAndAllowWhileIdle() also doesn't solve the problem as network is not available then
        //Thus: Prefer GCM over V_21 API as this is excluded from Doze restrictions
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (JobApi.GCM.isSupported(getApplicationContext())) {
               jobManager.forceApi(JobApi.GCM);
            } else {
                jobManager.forceApi(JobApi.V_21);
            }
        }

        if (BuildConfig.DEBUG) {
            jobManager.setVerbose(true);
        }
        jobManager.addJobCreator(new JobCreator() {
            @Override
            public Job create(String tag) {
                if (AutomaticIssueDownloadJob.TAG.equals(tag)) {
                    return AutomaticIssueDownloadJob_.getInstance_(DerBundDownloaderApplication.this);
                }
                return null;
            }
        });
        automaticDownloadScheduler.update();
    }
}
