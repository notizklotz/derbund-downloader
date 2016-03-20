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

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import com.github.notizklotz.derbunddownloader.BuildConfig;
import com.github.notizklotz.derbunddownloader.R;
import com.github.notizklotz.derbunddownloader.common.DateHandlingUtils;
import com.github.notizklotz.derbunddownloader.settings.Settings;
import com.github.notizklotz.derbunddownloader.settings.SettingsImpl;
import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.StandardExceptionParser;
import com.google.android.gms.analytics.Tracker;

import org.androidannotations.annotations.AfterInject;
import org.androidannotations.annotations.Bean;
import org.androidannotations.annotations.EBean;
import org.androidannotations.annotations.RootContext;
import org.androidannotations.annotations.SystemService;

@EBean(scope = EBean.Scope.Singleton)
public class AnalyticsTracker {

    @Bean(SettingsImpl.class)
    Settings settings;

    @SystemService
    ConnectivityManager connectivityManager;

    @RootContext
    Context context;

    private Tracker mTracker;

    @AfterInject
    void initTracker() {
        GoogleAnalytics analytics = GoogleAnalytics.getInstance(context);
        // To enable debug logging use: adb shell setprop log.tag.GAv4 DEBUG
        mTracker = analytics.newTracker(R.xml.app_tracker);
        mTracker.setAnonymizeIp(true);

        if (BuildConfig.DEBUG) {
            analytics.setDryRun(true);
        }
    }

    public static HitBuilders.EventBuilder createEventBuilder(AnalyticsCategory category) {
        return new HitBuilders.EventBuilder().setCategory(category.name());
    }

    public void send(HitBuilders.EventBuilder eventBuilder) {
        mTracker.send(eventBuilder.build());
    }

    public void sendWithCustomDimensions(HitBuilders.EventBuilder eventBuilder) {
        final String networkType;
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        if(activeNetworkInfo != null) {
            networkType = activeNetworkInfo.getTypeName();
        } else {
            networkType = "NONE";
        }

        send(eventBuilder.
                setCustomDimension(1, DateHandlingUtils.getUserTimezone().getID()).
                setCustomDimension(2, String.valueOf(settings.isWifiOnly())).
                setCustomDimension(3, networkType)
        );
    }

    public void send(HitBuilders.ExceptionBuilder exceptionBuilder) {
        mTracker.send(exceptionBuilder.build());
    }

    public void sendDefaultException(Context context, Exception e) {
        send(new HitBuilders.ExceptionBuilder().setDescription(new StandardExceptionParser(context, null).getDescription(Thread.currentThread().getName(), e)).setFatal(true));
    }

    public void send(HitBuilders.TimingBuilder timingBuilder) {
        mTracker.send(timingBuilder.build());
    }

    public void sendScreenView(String screen, HitBuilders.ScreenViewBuilder builder) {
        mTracker.setScreenName(screen);
        mTracker.send(builder.build());
    }
}
