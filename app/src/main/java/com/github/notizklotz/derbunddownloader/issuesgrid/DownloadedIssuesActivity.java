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

package com.github.notizklotz.derbunddownloader.issuesgrid;

import android.app.DownloadManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.StrictMode;
import android.preference.PreferenceManager;
import android.support.design.widget.Snackbar;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.SimpleCursorAdapter;
import android.widget.Toast;

import com.github.notizklotz.derbunddownloader.BuildConfig;
import com.github.notizklotz.derbunddownloader.DerBundDownloaderApplication;
import com.github.notizklotz.derbunddownloader.R;
import com.github.notizklotz.derbunddownloader.analytics.FirebaseEvents;
import com.github.notizklotz.derbunddownloader.common.DateHandlingUtils;
import com.github.notizklotz.derbunddownloader.download.ThumbnailRegistry;
import com.github.notizklotz.derbunddownloader.settings.Settings;
import com.github.notizklotz.derbunddownloader.settings.SettingsActivity;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.squareup.picasso.Picasso;

import org.apache.commons.lang3.StringUtils;
import org.joda.time.LocalDate;

import java.io.File;

import javax.inject.Inject;

public class DownloadedIssuesActivity extends AppCompatActivity {

    private static final String TAG_DOWNLOAD_ISSUE_DATE_PICKER = "downloadIssueDatePicker";
    private static final String MEDIA_TYPE_PDF = "application/pdf";

    private GridView gridView;

    private View emptyGridView;

    @Inject
    DownloadManager downloadManager;

    @Inject
    Settings settings;

    @Inject
    ThumbnailRegistry thumbnailRegistry;

    @Inject
    FirebaseAnalytics firebaseAnalytics;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //noinspection PointlessBooleanExpression
        if (BuildConfig.DEBUG && false) {
            StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder().detectAll().penaltyLog().build());
            StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder().detectAll().penaltyLog().build());
        }

        PreferenceManager.setDefaultValues(getApplicationContext(), R.xml.preferences, false);

        NotificationManagerCompat.from(this).cancelAll();

        setContentView(R.layout.activity_downloaded_issues);

        this.gridView = ((GridView) findViewById(R.id.gridview));
        this.emptyGridView = findViewById(R.id.empty_grid_view);

        ((DerBundDownloaderApplication)getApplication()).getDownloadedIssuesComponent().inject(this);

        setupIssuesGrid();
    }

    @Override
    protected void onStart() {
        super.onStart();

        String username = settings.getUsername();
        String password = settings.getPassword();

        if (StringUtils.isEmpty(username) || StringUtils.isEmpty(password)) {
            startActivity(new Intent(this, SettingsActivity.class));

            Toast.makeText(this, getString(R.string.please_login), Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId_ = item.getItemId();
        if (itemId_ == R.id.action_deleteAll) {
            showDeleteAllIssuesDialog();
            return true;
        }
        if (itemId_ == R.id.action_download) {
            menuItemDownloadSelected();
            return true;
        }
        if (itemId_ == R.id.action_settings) {
            menuItemSettingsSelected();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }



    void setupIssuesGrid() {
        gridView.setEmptyView(emptyGridView);
        gridView.setOnItemClickListener(new IssuesGridOnItemClickListener());

        final SimpleCursorAdapter issueListAdapter = new SimpleCursorAdapter(this,
                R.layout.include_issue, null,
                new String[]{DownloadManager.COLUMN_DESCRIPTION, DownloadManager.COLUMN_STATUS},
                new int[]{R.id.dateTextView, R.id.stateTextView}, 0) {

            @Override
            public View getView(final int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                View deleteButton = view.findViewById(R.id.issueDeleteButton);
                deleteButton.setOnClickListener(new IssueDeleteButtonOnClickListener(position));

                // Load the thumbnail image
                ImageView image = (ImageView) view.findViewById(R.id.issueImageView);
                String description = getDescriptionFromCursor(getCursor());

                LocalDate issueDate = DateHandlingUtils.fromDateString(description);
                File originalThumbnailUri = thumbnailRegistry.getThumbnailFile(issueDate);

                Picasso.with(DownloadedIssuesActivity.this)
                        .load(originalThumbnailUri)
                        .placeholder(R.drawable.issue_placeholder)
                        .fit()
                        .into(image);
                return view;
            }
        };
        issueListAdapter.setViewBinder(new IssuesGridViewBinder(this));
        gridView.setAdapter(issueListAdapter);

        getLoaderManager().initLoader(1, null, new IssuesGridLoaderCallbacks(this, issueListAdapter));
    }

    private void openPDF(Uri uri) {
        NotificationManagerCompat.from(this).cancelAll();

        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);

        Uri dataUri;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            File file = new File(uri.getPath());
            dataUri = FileProvider.getUriForFile(this, getPackageName(), file);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        } else {
            dataUri = uri;
        }
        intent.setDataAndType(dataUri, MEDIA_TYPE_PDF);

        PackageManager packageManager = getPackageManager();
        assert packageManager != null;

        if (intent.resolveActivity(packageManager) != null) {
            startActivity(intent);
        } else {
            Bundle bundle = new Bundle();
            bundle.putString("cause", "No PDF reader installed");
            firebaseAnalytics.logEvent(FirebaseEvents.USER_ERROR, bundle);

            Snackbar.make(gridView, R.string.no_pdf_reader, Snackbar.LENGTH_LONG).show();
        }
    }

    public void deleteIssue(final long id) {
        new AsyncTask<Void, Void, String>() {
            @Override
            protected String doInBackground(Void... params) {
                Cursor cursor = downloadManager.query(new DownloadManager.Query().setFilterById(id));
                String descriptionFromCursor = "";
                //noinspection TryFinallyCanBeTryWithResources
                try {
                    if (cursor.moveToFirst()) {
                        descriptionFromCursor = getDescriptionFromCursor(cursor);
                        thumbnailRegistry.clear(DateHandlingUtils.fromDateString(descriptionFromCursor));
                    }
                } finally {
                    cursor.close();
                }

                downloadManager.remove(id);
                return descriptionFromCursor;
            }

            @Override
            protected void onPostExecute(String aVoid) {
                Snackbar.make(gridView, getText(R.string.issue_deleted) + " " + aVoid, Snackbar.LENGTH_SHORT).show();
            }
        }.execute();
    }

    static String getDescriptionFromCursor(Cursor cursor) {
        return cursor.getString(cursor.getColumnIndex(DownloadManager.COLUMN_DESCRIPTION));
    }

    void showDeleteAllIssuesDialog() {
        ConfirmAllIssuesDeleteDialogFragment.createDialogFragment().show(getFragmentManager(), "issueDelete");
    }

    void deleteAllIssues() {
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                Cursor cursor = downloadManager.query(new DownloadManager.Query());
                //noinspection TryFinallyCanBeTryWithResources
                try {
                    if (cursor.moveToFirst()) {
                        while (!cursor.isAfterLast()) {
                            long itemID = cursor.getLong(cursor.getColumnIndex(DownloadManager.COLUMN_ID));
                            downloadManager.remove(itemID);
                            cursor.moveToNext();
                        }
                    }
                } finally {
                    cursor.close();
                }

                thumbnailRegistry.clearAll();

                return null;
            }
        }.execute();
    }

    void menuItemDownloadSelected() {
        new ManuallyDownloadIssueDatePickerFragment().show(getFragmentManager(), TAG_DOWNLOAD_ISSUE_DATE_PICKER);
    }

    void menuItemSettingsSelected() {
        startActivity(new Intent(this, SettingsActivity.class));
    }

    private class IssuesGridOnItemClickListener implements AdapterView.OnItemClickListener {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            Cursor selectedIssue = (Cursor) parent.getItemAtPosition(position);
            if (selectedIssue != null) {
                boolean completed = selectedIssue.getInt(selectedIssue.getColumnIndex(DownloadManager.COLUMN_STATUS)) == DownloadManager.STATUS_SUCCESSFUL;
                if (completed) {
                    String uri = selectedIssue.getString(selectedIssue.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI));
                    openPDF(Uri.parse(uri));
                }
            }
        }
    }

    private class IssueDeleteButtonOnClickListener implements View.OnClickListener {
        private final int position;

        IssueDeleteButtonOnClickListener(int position) {
            this.position = position;
        }

        @Override
        public void onClick(View v) {
            Cursor item = (Cursor) gridView.getAdapter().getItem(position);
            long itemID = item.getLong(item.getColumnIndex(DownloadManager.COLUMN_ID));
            ConfirmIssueDeleteDialogFragment.createDialogFragment(itemID).show(getFragmentManager(), "issueDelete");
        }
    }

}
