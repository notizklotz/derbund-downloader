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

import android.app.Activity;
import android.app.LoaderManager;
import android.content.Context;
import android.content.Intent;
import android.content.Loader;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.DatePicker;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.text.DateFormat;
import java.util.List;

public class MainActivity extends Activity {

    private ArrayAdapter<Issue> listViewAdapter;
    private File issuesDirectory;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        issuesDirectory = getExternalFilesDir("issues");

        ListView listView = (ListView) findViewById(R.id.listView);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Issue selectedIssue = (Issue) parent.getItemAtPosition(position);
                if (selectedIssue != null) {
                    openPDF(selectedIssue.getFile());
                }
            }
        });
        listViewAdapter = new ArrayAdapter<Issue>(this,
                android.R.layout.simple_list_item_1) {

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                TextView view = (TextView) super.getView(position, convertView, parent);

                if(view != null) {
                    Issue item = getItem(position);
                    String displayText = "Ausgabe " + DateFormat.getDateInstance().format(item.getDate().getTime());
                    view.setText(displayText);
                }

                return view;
            }
        };
        listView.setAdapter(listViewAdapter);

        getLoaderManager().initLoader(666, null, new LoaderManager.LoaderCallbacks<List<Issue>>() {
            @Override
            public Loader<List<Issue>> onCreateLoader(int id, Bundle args) {
                return new IssuesLoader(getApplicationContext(), issuesDirectory);
            }

            @Override
            public void onLoadFinished(Loader<List<Issue>> loader, List<Issue> data) {
                listViewAdapter.addAll(data);
            }

            @Override
            public void onLoaderReset(Loader<List<Issue>> loader) {
                listViewAdapter.clear();
            }
        });
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
            //noinspection ConstantConditions
            Toast.makeText(getApplicationContext(), R.string.download_noconnection, Toast.LENGTH_SHORT).show();
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
            File outputFile = new File(issuesDirectory, filename);
            fileOutputStream = new FileOutputStream(outputFile);
            IOUtils.copy(is, fileOutputStream);
            return outputFile;
        } finally {
            IOUtils.closeQuietly(fileOutputStream);
            IOUtils.closeQuietly(is);
            IOUtils.close(conn);
        }
    }

    @SuppressWarnings("UnusedDeclaration")
    private void openPDF(File pdfFile) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(Uri.fromFile(pdfFile), "application/pdf");
        intent.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
        startActivity(intent);
    }

    private class DownloadPaperTask extends AsyncTask<String, Void, Issue> {
        @Override
        protected Issue doInBackground(String... dayMonthYear) {
            try {
                String year = dayMonthYear[2];
                String day = dayMonthYear[0];
                String month = dayMonthYear[1];
                String url = "http://epaper.derbund.ch/pdf/" + year + "_3_BVBU-001-" + day + month + ".pdf";

                File download = download(url, year + month + day + ".pdf");

                return new Issue(Integer.parseInt(day), Integer.parseInt(month), Integer.parseInt(year), download);
            } catch (IOException e) {
                Log.e(getClass().toString(), "Error downloading issue", e);
                return null;
            }
        }

        @Override
        protected void onPostExecute(Issue result) {
            if(result != null) {
                //noinspection ConstantConditions
                Toast.makeText(getApplicationContext(), R.string.download_success, Toast.LENGTH_SHORT).show();
                listViewAdapter.add(result);
            }
        }
    }


}
