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

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.app.Activity;
import android.text.util.Linkify;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.DatePicker;
import android.widget.ListView;
import android.widget.TextView;

import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

public class MainActivity extends Activity {

    private ArrayAdapter<String> listViewAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ListView listView = (ListView) findViewById(R.id.listView);
        listViewAdapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_list_item_1) {

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                if(view != null) {
                    ((TextView)view).setAutoLinkMask(Linkify.WEB_URLS);
                }
                return view;
            }
        };
        listView.setAdapter(listViewAdapter);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    public void downloadPaper(@SuppressWarnings("UnusedParameters") View view) {
        DatePicker datePicker = (DatePicker) findViewById(R.id.datePicker);

        int day = datePicker.getDayOfMonth();
        int month = datePicker.getMonth() + 1;
        String dayString = String.format("%02d", day);
        String monthString = String.format("%02d", month);

        ConnectivityManager connMgr = (ConnectivityManager)
                getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        if (networkInfo != null && networkInfo.isConnected()) {
            new DownloadPaperTask().execute(dayString, monthString, "2013");
        } else {
            Log.d(getClass().toString(), "No network connection");
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

    private class DownloadPaperTask extends AsyncTask<String, Void, File> {
        @Override
        protected File doInBackground(String... dayMonthYear) {
            try {
                String url = "http://epaper.derbund.ch/pdf/" + dayMonthYear[2] + "_3_BVBU-001-" + dayMonthYear[0] + dayMonthYear[1] + ".pdf";

                return download(url, dayMonthYear[2] + dayMonthYear[1] + dayMonthYear[0] + ".pdf");
            } catch (IOException e) {
                Log.e(getClass().toString(), "Error downloading issue", e);
                return null;
            }
        }

        @Override
        protected void onPostExecute(File result) {
            if(result != null) {
                listViewAdapter.add(result.toString());
            }
        }
    }

}
