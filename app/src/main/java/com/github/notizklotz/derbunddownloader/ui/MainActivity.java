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

package com.github.notizklotz.derbunddownloader.ui;

import android.app.Dialog;
import android.app.DialogFragment;
import android.app.DownloadManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.content.FileProvider;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

import com.github.notizklotz.derbunddownloader.DerBundDownloaderApplication;
import com.github.notizklotz.derbunddownloader.R;
import com.github.notizklotz.derbunddownloader.analytics.FirebaseEvents;
import com.github.notizklotz.derbunddownloader.common.DateHandlingUtils;
import com.github.notizklotz.derbunddownloader.download.EpaperApiInexistingIssueRequestedException;
import com.github.notizklotz.derbunddownloader.download.EpaperApiInvalidCredentialsException;
import com.github.notizklotz.derbunddownloader.download.IssueDownloader;
import com.github.notizklotz.derbunddownloader.download.ThumbnailRegistry;
import com.github.notizklotz.derbunddownloader.settings.Settings;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.crash.FirebaseCrash;
import com.squareup.picasso.Picasso;

import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTimeConstants;
import org.joda.time.LocalDate;

import java.io.File;
import java.io.IOException;

import javax.inject.Inject;

public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener, ManuallyDownloadIssueDatePickerFragment.DateSelectionListener {

    private static final String TAG_DOWNLOAD_ISSUE_DATE_PICKER = "downloadIssueDatePicker";
    private static final String MEDIA_TYPE_PDF = "application/pdf";
    private static final String KEY_LAST_WHATS_NEW_VERSION = "last_whats_new";

    private GridView gridView;

    private View emptyGridView;

    @Inject
    DownloadManager downloadManager;

    @Inject
    Settings settings;

    @Inject
    ThumbnailRegistry thumbnailRegistry;

    @Inject
    IssueDownloader issueDownloader;

    @Inject
    FirebaseAnalytics firebaseAnalytics;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ((DerBundDownloaderApplication)getApplication()).getUiComponent().inject(this);

        super.onCreate(savedInstanceState);

        if (StringUtils.isBlank(settings.getUsername()) || StringUtils.isBlank(settings.getPassword())) {
            Intent intent = new Intent(this, LoginActivity.class);
            startActivity(intent);
            finish();
            return;
        }

        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                menuItemDownloadSelected();
            }
        });

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        TextView navHeaderMail = (TextView) navigationView.getHeaderView(0).findViewById(R.id.nav_header_email);
        navHeaderMail.setText(settings.getUsername());
        navHeaderMail.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, LoginActivity.class);
                startActivity(intent);
            }
        });

        NotificationManagerCompat.from(this).cancelAll();

        this.gridView = ((GridView) findViewById(R.id.gridview));
        this.emptyGridView = findViewById(R.id.empty_grid_view);

        setupIssuesGrid();

        showWhatsNewDialogIfApplicable();
    }

    private void showWhatsNewDialogIfApplicable() {
        try {
            SharedPreferences defaultSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
            int lastWhatsNewVersion = defaultSharedPreferences.getInt(KEY_LAST_WHATS_NEW_VERSION, 0);
            int currentAppVersion = getPackageManager().getPackageInfo(getPackageName(), 0).versionCode;

            if (lastWhatsNewVersion < 43) {
                defaultSharedPreferences.edit().putInt(KEY_LAST_WHATS_NEW_VERSION, currentAppVersion).apply();

                if (!"com.github.notizklotz.derbunddownloader.tagesanzeiger".equals(getPackageName())) {
                    AlertDialog.Builder dialog = new AlertDialog.Builder(this);
                    dialog.setMessage(R.string.tagi_teaser)
                            .setPositiveButton(R.string.tryit, new DialogInterface.OnClickListener() {

                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    Intent intent = new Intent(Intent.ACTION_VIEW);
                                    intent.setData(Uri.parse("market://details?id=com.github.notizklotz.derbunddownloader.tagesanzeiger"));
                                    startActivity(intent);
                                }
                            })
                            .setNegativeButton(R.string.ignoreit, new DialogInterface.OnClickListener() {

                                @Override
                                public void onClick(DialogInterface dialog, int which) {

                                }
                            })
                            .create().show();
                }
            }
        } catch (Exception e) {
            FirebaseCrash.report(e);
        }
    }

    private void setupIssuesGrid() {
        gridView.setEmptyView(emptyGridView);
        gridView.setOnItemClickListener(new MainActivity.IssuesGridOnItemClickListener());

        final SimpleCursorAdapter issueListAdapter = new SimpleCursorAdapter(this,
                R.layout.include_issue, null,
                new String[]{DownloadManager.COLUMN_DESCRIPTION, DownloadManager.COLUMN_STATUS},
                new int[]{R.id.dateTextView, R.id.stateTextView}, 0) {

            @Override
            public View getView(final int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                View deleteButton = view.findViewById(R.id.issueDeleteButton);
                deleteButton.setOnClickListener(new MainActivity.IssueDeleteButtonOnClickListener(position));

                // Load the thumbnail image
                ImageView image = (ImageView) view.findViewById(R.id.issueImageView);
                String description = getDescriptionFromCursor(getCursor());

                LocalDate issueDate = DateHandlingUtils.fromDateString(description);
                File originalThumbnailUri = thumbnailRegistry.getThumbnailFile(issueDate);

                Picasso.with(MainActivity.this)
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

    private void deleteIssue(final long id) {
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

    private static String getDescriptionFromCursor(Cursor cursor) {
        return cursor.getString(cursor.getColumnIndex(DownloadManager.COLUMN_DESCRIPTION));
    }

    private void showDeleteAllIssuesDialog() {
        new DeleteAllDialogFragment().show(getFragmentManager(), "issueDelete");
    }

    private void deleteAllIssues() {
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

    private void menuItemDownloadSelected() {
        new ManuallyDownloadIssueDatePickerFragment().show(getFragmentManager(), TAG_DOWNLOAD_ISSUE_DATE_PICKER);
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

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_deleteAll) {
            showDeleteAllIssuesDialog();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    public static class ConfirmIssueDeleteDialogFragment extends DialogFragment {

        private static final String ARG_ISSUE_ID = "issueID";

        static ConfirmIssueDeleteDialogFragment createDialogFragment(long issueID) {
            Bundle bundle = new Bundle();
            bundle.putLong(ARG_ISSUE_ID, issueID);
            ConfirmIssueDeleteDialogFragment confirmIssueDeleteDialogFragment = new ConfirmIssueDeleteDialogFragment();
            confirmIssueDeleteDialogFragment.setArguments(bundle);
            return confirmIssueDeleteDialogFragment;
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final long issueId = getArguments().getLong("issueID");

            return new AlertDialog.Builder(getActivity()).setMessage("Heruntergeladene Ausgabe entfernen?")
                    .setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            ((MainActivity) getActivity()).deleteIssue(issueId);
                        }
                    })
                    .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            // User cancelled the dialog
                        }
                    }).create();
        }
    }

    public static class DeleteAllDialogFragment extends DialogFragment {
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            return new AlertDialog.Builder(getActivity()).setMessage("Alle heruntergeladenen Ausgaben entfernen?")
                    .setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            ((MainActivity) getActivity()).deleteAllIssues();
                        }
                    })
                    .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            // User cancelled the dialog
                        }
                    }).create();
        }
    }

    @Override
    public void onDateSet(LocalDate selectedDate) {
        if (selectedDate.getDayOfWeek() == DateTimeConstants.SUNDAY) {
            Bundle bundle = new Bundle();
            bundle.putString("cause", "Sunday issue download attempted");
            firebaseAnalytics.logEvent(FirebaseEvents.USER_ERROR, bundle);

            Snackbar.make(gridView, R.string.error_no_issue_on_sundays, Snackbar.LENGTH_LONG).show();
        } else {
            new ManualIssueDownloadAsyncTask().execute(selectedDate);
        }
    }

    private class ManualIssueDownloadAsyncTask extends AsyncTask<LocalDate, Void, Integer> {

        @Override
        protected Integer doInBackground(LocalDate... issueDate) {
            try {
                issueDownloader.download(issueDate[0], "manual", false);
            } catch (IOException e) {
                return R.string.download_connection_failed_text;
            } catch (EpaperApiInexistingIssueRequestedException e) {
                return R.string.error_issue_not_available;
            } catch (EpaperApiInvalidCredentialsException e) {
                return R.string.download_login_failed_text;
            } catch (Exception e) {
                return R.string.download_service_error;
            }
            return null;
        }

        @Override
        protected void onPostExecute(Integer integer) {
            if (integer != null) {
                Snackbar.make(gridView, integer, Snackbar.LENGTH_LONG).show();
            }
        }
    }
}
