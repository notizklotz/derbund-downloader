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

import com.github.notizklotz.derbunddownloader.common.DateHandlingUtils;
import com.github.notizklotz.derbunddownloader.settings.Settings;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.StandardExceptionParser;
import com.google.android.gms.analytics.Tracker;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class AnalyticsTracker {

    private final Settings settings;

    private final ConnectivityManager connectivityManager;

    private final Tracker tracker;

    @Inject
    public AnalyticsTracker(Tracker tracker, Settings settings, ConnectivityManager connectivityManager) {
        this.tracker = tracker;
        this.settings = settings;
        this.connectivityManager = connectivityManager;
    }

    public static HitBuilders.EventBuilder createEventBuilder(AnalyticsCategory category) {
        return new HitBuilders.EventBuilder().setCategory(category.name());
    }

    public void send(HitBuilders.EventBuilder eventBuilder) {
        tracker.send(eventBuilder.build());
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
        tracker.send(exceptionBuilder.build());
    }

    public void sendDefaultException(Context context, Exception e) {
        send(new HitBuilders.ExceptionBuilder().setDescription(new StandardExceptionParser(context, null).getDescription(Thread.currentThread().getName(), e)).setFatal(true));
    }

    public void send(HitBuilders.TimingBuilder timingBuilder) {
        tracker.send(timingBuilder.build());
    }

    public void sendScreenView(String screen, HitBuilders.ScreenViewBuilder builder) {
        tracker.setScreenName(screen);
        tracker.send(builder.build());
    }
}
