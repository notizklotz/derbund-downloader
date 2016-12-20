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

import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.widget.DatePicker;
import android.widget.Toast;

import com.github.notizklotz.derbunddownloader.DerBundDownloaderApplication;
import com.github.notizklotz.derbunddownloader.R;
import com.github.notizklotz.derbunddownloader.analytics.FirebaseEvents;
import com.github.notizklotz.derbunddownloader.download.IssueDownloadIntentService;
import com.google.firebase.analytics.FirebaseAnalytics;

import org.joda.time.DateTime;
import org.joda.time.DateTimeConstants;
import org.joda.time.LocalDate;

public class ManuallyDownloadIssueDatePickerFragment extends DialogFragment {

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Activity activity = getActivity();
        if (activity == null) {
            throw new IllegalStateException("Activity is null");
        }

        DateTime now = DateTime.now();
        DatePickerDialog datePickerDialog = new DatePickerDialog(activity, null,
                now.getYear(), now.getMonthOfYear() - 1, now.getDayOfMonth());

        final DatePicker datePicker = datePickerDialog.getDatePicker();

        datePicker.setMaxDate(System.currentTimeMillis());

        //Override the OK button instead of using the OnDateSetListener callback due to bug https://code.google.com/p/android/issues/detail?id=34833
        datePickerDialog.setButton(DatePickerDialog.BUTTON_POSITIVE, activity.getText(R.string.action_download), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                onDateSet(datePicker.getYear(), datePicker.getMonth() + 1, datePicker.getDayOfMonth());
            }
        });

        return datePickerDialog;
    }

    private void onDateSet(final int year, final int monthOfYear, final int dayOfMonth) {
        final Activity activity = getActivity();
        assert activity != null;

        LocalDate selectedDate = new LocalDate(year, monthOfYear, dayOfMonth);

        FirebaseAnalytics firebaseAnalytics = ((DerBundDownloaderApplication)getActivity().getApplication()).getAnalyticsComponent().firebaseAnalytics();

        if (selectedDate.getDayOfWeek() == DateTimeConstants.SUNDAY) {
            Bundle bundle = new Bundle();
            bundle.putString("cause", "Sunday issue download attempted");
            firebaseAnalytics.logEvent(FirebaseEvents.USER_ERROR, bundle);

            Toast.makeText(activity, activity.getString(R.string.error_no_issue_on_sundays), Toast.LENGTH_SHORT).show();
        } else {
            IssueDownloadIntentService.startDownload(activity.getApplication(), dayOfMonth, monthOfYear, year);
        }
    }

}