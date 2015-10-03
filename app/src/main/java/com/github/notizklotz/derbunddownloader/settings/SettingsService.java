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

package com.github.notizklotz.derbunddownloader.settings;

import java.util.Date;

public interface SettingsService {
    String KEY_AUTO_DOWNLOAD_ENABLED = "auto_download_enabled";
    String KEY_AUTO_DOWNLOAD_TIME = "auto_download_time";
    String KEY_USERNAME = "username";
    String KEY_PASSWORD = "password";
    String KEY_LAST_WAKEUP = "last_wakeup";
    String KEY_NEXT_WAKEUP = "next_wakeup";
    String KEY_WIFI_ONLY_ENABLED = "wifi_only";

    String getUsername();

    String getPassword();

    boolean isWifiOnly();

    boolean isAutoDownloadEnabled();

    String getAutoDownloadTime();

    String getNextWakeup();

    void updateNextWakeup(Date nextWakeup);
}
