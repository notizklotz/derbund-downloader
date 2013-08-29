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
import android.app.AlarmManager;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.DownloadManager;
import android.app.LoaderManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.DatePicker;
import android.widget.GridView;
import android.widget.SimpleCursorAdapter;

import java.util.Calendar;

public class MainActivity extends Activity implements
        LoaderManager.LoaderCallbacks<Cursor> {

    private static final Uri DOWNLOADS_URI = Uri.parse("content://downloads/my_downloads");
    private SimpleCursorAdapter issueListAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        GridView gridView = (GridView) findViewById(R.id.gridview);
        gridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Cursor selectedIssue = (Cursor) parent.getItemAtPosition(position);
                if (selectedIssue != null) {

                    Intent intent = new Intent(DownloadManager.ACTION_VIEW_DOWNLOADS);
                    startActivity(intent);
                }
            }
        });

        issueListAdapter = new SimpleCursorAdapter(this,
                R.layout.issue_item_in_grid, null,
                new String[]{DownloadManager.COLUMN_TITLE},
                new int[]{R.id.issueTextView}, 0);
        gridView.setAdapter(issueListAdapter);

        getLoaderManager().initLoader(1, null, this);

//        setRecurringAlarm(getApplicationContext());
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @SuppressWarnings("UnusedDeclaration")
    private void setRecurringAlarm(Context context) {
        Calendar updateTime = Calendar.getInstance();
        updateTime.set(Calendar.HOUR_OF_DAY, 23);
        updateTime.set(Calendar.MINUTE, 4);
        Intent downloader = new Intent(context, DownloadAlarmReceiver.class);
        PendingIntent recurringDownload = PendingIntent.getBroadcast(context, 0, downloader, PendingIntent.FLAG_CANCEL_CURRENT);
        AlarmManager alarms = (AlarmManager) this.getSystemService(Context.ALARM_SERVICE);
        //alarms.setInexactRepeating(AlarmManager.RTC_WAKEUP, updateTime.getTimeInMillis(), AlarmManager.INTERVAL_DAY, recurringDownload);
        alarms.set(AlarmManager.RTC_WAKEUP, updateTime.getTimeInMillis(), recurringDownload);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_download:
                new DatePickerFragment().show(getFragmentManager(), "downloadIssueDatePicker");
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private static class DatePickerFragment extends DialogFragment implements DatePickerDialog.OnDateSetListener {

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final Calendar c = Calendar.getInstance();
            Activity activity = getActivity();
            if (activity == null) {
                throw new IllegalStateException("Activity is null");
            }

            return new DatePickerDialog(activity, this, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH));
        }

        @Override
        public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
            Activity activity = getActivity();
            if (activity == null) {
                throw new IllegalStateException("Activity is null");
            }

            activity.startService(IssueDownloadService.createDownloadIntent(activity, dayOfMonth, monthOfYear + 1, year));
        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return new CursorLoader(this, DOWNLOADS_URI, null, null, null, null) {

            @Override
            public Cursor loadInBackground() {
                Cursor cursor = super.loadInBackground();
                cursor.setNotificationUri(getContentResolver(), DOWNLOADS_URI);
                return cursor;
            }
        };
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        issueListAdapter.changeCursor(data);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        issueListAdapter.changeCursor(null);
    }
}
