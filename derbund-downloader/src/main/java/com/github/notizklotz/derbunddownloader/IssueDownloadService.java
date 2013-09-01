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

import android.app.DownloadManager;
import android.content.Context;
import android.net.Uri;

public class IssueDownloadService {

    private IssueDownloadService() {

    }

    public static void startDownload(Context context, int day, int month, int year) {
        String dayString = String.format("%02d", day);
        String monthString = String.format("%02d", month);
        String yearString = Integer.toString(year);

        String url = "http://epaper.derbund.ch/getAll.asp?d=" + dayString + monthString + yearString;

        DownloadManager downloadManager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url)).
                setTitle("derbund" + dayString + monthString + yearString).
                setDescription("Der Bund ePaper " + dayString + "." + monthString + "." + yearString).
                setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED).
                setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI);
        downloadManager.enqueue(request);
    }

}
