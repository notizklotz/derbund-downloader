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

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.DownloadManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.StrictMode;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.SimpleCursorAdapter;
import android.widget.Toast;

import com.github.notizklotz.derbunddownloader.BuildConfig;
import com.github.notizklotz.derbunddownloader.R;
import com.github.notizklotz.derbunddownloader.download.AutomaticIssueDownloadAlarmManager_;
import com.github.notizklotz.derbunddownloader.settings.Settings;
import com.github.notizklotz.derbunddownloader.settings.SettingsActivity_;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.OptionsItem;
import org.androidannotations.annotations.OptionsMenu;
import org.androidannotations.annotations.ViewById;
import org.springframework.util.StringUtils;

import java.io.File;

@SuppressLint("Registered")
@EActivity(R.layout.activity_main)
@OptionsMenu(R.menu.main)
public class MainActivity extends Activity {

    private static final String TAG_DOWNLOAD_ISSUE_DATE_PICKER = "downloadIssueDatePicker";
    private static final String MEDIA_TYPE_PDF = "application/pdf";
    private static final String KEY_ALARM_MIGRATED = "KEY_ALARM_MIGRATED";

    @SuppressWarnings("WeakerAccess")
    @ViewById(R.id.gridview)
    GridView gridView;

    @ViewById(R.id.empty_grid_view)
    View emptyGridView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (BuildConfig.DEBUG) {
            StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder().detectAll().penaltyLog().build());
            StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder().detectAll().penaltyLog().build());
        }

        PreferenceManager.setDefaultValues(getApplicationContext(), R.xml.preferences, false);

        if (!PreferenceManager.getDefaultSharedPreferences(this).contains(KEY_ALARM_MIGRATED)) {
            Log.i(getClass().getSimpleName(), "Migrating alarms");
            AutomaticIssueDownloadAlarmManager_.getInstance_(this).updateAlarm();
            PreferenceManager.getDefaultSharedPreferences(this).edit().putBoolean(KEY_ALARM_MIGRATED, true).apply();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();

        String username = Settings.getUsername(getApplicationContext());
        String password = Settings.getPassword(getApplicationContext());

        if (!(StringUtils.hasText(username) && StringUtils.hasText(password))) {
            SettingsActivity_.intent(this).start();
            Toast.makeText(this, getString(R.string.please_login), Toast.LENGTH_LONG).show();
        }
    }

    @AfterViews
    void setupIssuesGrid() {
        gridView.setEmptyView(emptyGridView);
        gridView.setOnItemClickListener(new IssuesGridOnItemClickListener());
        gridView.setMultiChoiceModeListener(new IssuesGridMultiChoiceModeListener(this, gridView));

        SimpleCursorAdapter issueListAdapter = new SimpleCursorAdapter(this,
                R.layout.issue, null,
                new String[]{DownloadManager.COLUMN_DESCRIPTION, DownloadManager.COLUMN_STATUS},
                new int[]{R.id.dateTextView, R.id.stateTextView}, 0) {


            @Override
            public View getView(final int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);

                View deleteButton = view.findViewById(R.id.issueDeleteButton);
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
            Toast.makeText(this, R.string.no_pdf_reader, Toast.LENGTH_LONG).show();
        }
    }

    @OptionsItem(R.id.action_download)
    void menuItemDownloadSelected() {
        new ManuallyDownloadIssueDatePickerFragment().show(getFragmentManager(), TAG_DOWNLOAD_ISSUE_DATE_PICKER);
    }

    @OptionsItem(R.id.action_settings)
    void menuItemSettingsSelected() {
        SettingsActivity_.intent(this).start();
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
