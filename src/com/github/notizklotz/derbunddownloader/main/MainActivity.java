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

package com.github.notizklotz.derbunddownloader.main;

import android.app.Activity;
import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.StrictMode;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import com.github.notizklotz.derbunddownloader.BuildConfig;
import com.github.notizklotz.derbunddownloader.R;
import com.github.notizklotz.derbunddownloader.settings.Settings;
import com.github.notizklotz.derbunddownloader.settings.SettingsActivity;
import org.springframework.util.StringUtils;

import java.io.File;

@SuppressWarnings("WeakerAccess")
public class MainActivity extends Activity {

    private static final String TAG_DOWNLOAD_ISSUE_DATE_PICKER = "downloadIssueDatePicker";
    private static final String MEDIA_TYPE_PDF = "application/pdf";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (BuildConfig.DEBUG) {
            StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder().detectAll().penaltyLog().build());
            StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder().detectAll().penaltyLog().build());
        }

        setContentView(R.layout.activity_main);

        PreferenceManager.setDefaultValues(getNullSafeApplicationContext(), R.xml.preferences, false);

        setupIssuesGrid();
    }

    @Override
    protected void onStart() {
        super.onStart();

        String username = Settings.getUsername(getApplicationContext());
        String password = Settings.getPassword(getApplicationContext());
        if(!(StringUtils.hasText(username) && StringUtils.hasText(password))) {
            startActivity(new Intent(getApplicationContext(), SettingsActivity.class));
            Toast.makeText(getNullSafeApplicationContext(), getString(R.string.please_login), Toast.LENGTH_LONG).show();
        }
    }

    private void setupIssuesGrid() {
        final GridView gridView = (GridView) findViewById(R.id.gridview);
        gridView.setEmptyView(findViewById(R.id.empty_grid_view));
        gridView.setOnItemClickListener(new IssuesGridOnItemClickListener());
        gridView.setMultiChoiceModeListener(new IssuesGridMultiChoiceModeListener(this, gridView));

        SimpleCursorAdapter issueListAdapter = new SimpleCursorAdapter(this,
                R.layout.issue, null,
                new String[]{DownloadManager.COLUMN_DESCRIPTION, DownloadManager.COLUMN_STATUS},
                new int[]{R.id.dateTextView, R.id.stateTextView}, 0) {


            @Override
            public View getView(final int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);

                ImageButton deleteButton = (ImageButton) view.findViewById(R.id.issueDeleteButton);
                deleteButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        gridView.setItemChecked(position, true);
                    }
                });

                return view;
            }
        };
        issueListAdapter.setViewBinder(new IssuesGridViewBinder(this));

        gridView.setAdapter(issueListAdapter);

        getLoaderManager().initLoader(1, null, new IssuesGridLoaderCallbacks(this, issueListAdapter));
    }

    private void openPDF(String uri) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(Uri.fromFile(new File(uri)), MEDIA_TYPE_PDF);
        intent.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);

        PackageManager packageManager = getPackageManager();
        assert packageManager != null;

        if (intent.resolveActivity(packageManager) != null) {
            startActivity(intent);
        } else {
            Toast.makeText(getNullSafeApplicationContext(), R.string.no_pdf_reader, Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_download:
                new ManuallyDownloadIssueDatePickerFragment().show(getFragmentManager(), TAG_DOWNLOAD_ISSUE_DATE_PICKER);
                return true;
            case R.id.action_settings:
                startActivity(new Intent(getApplicationContext(), SettingsActivity.class));
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private Context getNullSafeApplicationContext() {
        Context applicationContext = getApplicationContext();
        if(applicationContext != null) {
            return applicationContext;
        } else {
            throw new IllegalStateException("Application context was null");
        }
    }

    private class IssuesGridOnItemClickListener implements AdapterView.OnItemClickListener {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            Cursor selectedIssue = (Cursor) parent.getItemAtPosition(position);
            if (selectedIssue != null) {
                boolean completed = selectedIssue.getInt(selectedIssue.getColumnIndex(DownloadManager.COLUMN_STATUS)) == DownloadManager.STATUS_SUCCESSFUL;
                if (completed) {
                    String uri = selectedIssue.getString(selectedIssue.getColumnIndex(DownloadManager.COLUMN_LOCAL_FILENAME));
                    openPDF(uri);
                }
            }
        }
    }

}
