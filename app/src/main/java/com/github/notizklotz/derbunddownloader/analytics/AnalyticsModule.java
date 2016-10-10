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

package com.github.notizklotz.derbunddownloader.analytics;

import android.app.Application;

import com.github.notizklotz.derbunddownloader.BuildConfig;
import com.github.notizklotz.derbunddownloader.R;
import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.Tracker;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

@Module
public class AnalyticsModule {

    @Provides
    @Singleton
    GoogleAnalytics googleAnalytics(Application application) {
        GoogleAnalytics analytics = GoogleAnalytics.getInstance(application);
        if (BuildConfig.DEBUG) {
            analytics.setDryRun(true);
        }
        return analytics;
    }

    @Provides
    @Singleton
    Tracker tracker(GoogleAnalytics analytics) {
        // To enable debug logging use: adb shell setprop log.tag.GAv4 DEBUG
        Tracker tracker = analytics.newTracker(R.xml.app_tracker);
        tracker.setAnonymizeIp(true);
        return tracker;
    }

}