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

import android.app.IntentService;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Parcelable;
import android.util.Log;

import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

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

        String dayString = String.format("%02d", intent.getIntExtra("day", 0));
        String monthString = String.format("%02d", intent.getIntExtra("month", 0));
        String yearString = Integer.toString(intent.getIntExtra("year", 0));

        String url = "http://epaper.derbund.ch/pdf/" + yearString + "_3_BVBU-001-" + dayString + monthString + ".pdf";
        try {

            ConnectivityManager connMgr = (ConnectivityManager)
                    getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
            if (networkInfo != null && networkInfo.isConnected()) {
                download(url, yearString + monthString + dayString + ".pdf");

                Parcelable pendingIntent = intent.getParcelableExtra("pendingIntent");
                if (pendingIntent != null && pendingIntent instanceof PendingIntent) {
                    ((PendingIntent) pendingIntent).send(0);
                }
            } else {
                Log.d(getClass().toString(), "No network connection");
            }
        } catch (IOException e) {
            Log.e(getClass().toString(), "Download failed", e);
        } catch (PendingIntent.CanceledException e) {
            Log.e(getClass().toString(), "Download notification failed", e);
        }
    }

    private File download(String urlString, String filename) throws IOException {
        InputStream is = null;
        FileOutputStream fileOutputStream = null;
        URL url = new URL(urlString);
        URLConnection conn = url.openConnection();
        try {
            conn.connect();
            is = conn.getInputStream();
            File outputFile = new File(getExternalFilesDir("issues"), filename);
            fileOutputStream = new FileOutputStream(outputFile);
            IOUtils.copy(is, fileOutputStream);
            return outputFile;
        } finally {
            IOUtils.closeQuietly(fileOutputStream);
            IOUtils.closeQuietly(is);
            IOUtils.close(conn);
        }
    }
}
