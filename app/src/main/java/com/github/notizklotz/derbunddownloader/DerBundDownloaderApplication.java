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

import net.danlew.android.joda.JodaTimeAndroid;

public class DerBundDownloaderApplication extends Application {

    private static DerBundDownloaderApplication INSTANCE_;

    public static DerBundDownloaderApplication getInstance() {
        return INSTANCE_;
    }

    @Override
    public void onCreate() {
        INSTANCE_ = this;
        super.onCreate();
        JodaTimeAndroid.init(this);
    }
}
