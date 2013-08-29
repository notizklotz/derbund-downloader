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
import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;

public class IssueDownloadService extends IntentService {

    public IssueDownloadService() {
        super(IssueDownloadService.class.getName());
    }

    public static Intent createDownloadIntent(Context context, int day, int month, int year) {
        Intent intent = new Intent(context, IssueDownloadService.class);
        intent.putExtra("day", day);
        intent.putExtra("month", month);
        intent.putExtra("year", year);

        return intent;
    }

    @Override
    protected void onHandleIntent(Intent intent) {

        if (!(intent.hasExtra("day") && intent.hasExtra("month") && intent.hasExtra("year"))) {
            Log.e(getClass().toString(), "extras missing");
        }

        int day = intent.getIntExtra("day", 0);
        int month = intent.getIntExtra("month", 0);
        int year = intent.getIntExtra("year", 0);

        String dayString = String.format("%02d", day);
        String monthString = String.format("%02d", month);
        String yearString = Integer.toString(year);

        String url = "http://epaper.derbund.ch/getAll.asp?d=" + dayString + monthString + yearString;

        DownloadManager downloadManager = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url)).
                setTitle("derbund" + dayString + monthString + yearString).
                setDescription("Der Bund ePaper " + dayString + "." + monthString + "." + yearString).
                setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
        downloadManager.enqueue(request);
    }

}
