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
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Intent;
import android.os.Bundle;
import android.widget.CalendarView;
import android.widget.DatePicker;
import android.widget.Toast;
import com.github.notizklotz.derbunddownloader.R;
import com.github.notizklotz.derbunddownloader.download.IssueDownloadService;

import java.util.Calendar;

class ManuallyDownloadIssueDatePickerFragment extends DialogFragment implements DatePickerDialog.OnDateSetListener {

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final Calendar c = Calendar.getInstance();
        Activity activity = getActivity();
        if (activity == null) {
            throw new IllegalStateException("Activity is null");
        }

        DatePickerDialog datePickerDialog = new DatePickerDialog(activity, this,
                c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH));

        DatePicker datePicker = datePickerDialog.getDatePicker();
        if (datePicker != null) {
            CalendarView calendarView = datePicker.getCalendarView();
            if (calendarView != null) {
                calendarView.setFirstDayOfWeek(Calendar.MONDAY);
            }
            datePicker.setMaxDate(System.currentTimeMillis());
        }

        return datePickerDialog;
    }

    @Override
    public void onDateSet(DatePicker view, final int year, final int monthOfYear, final int dayOfMonth) {
        final Activity activity = getActivity();
        if (activity == null) {
            throw new IllegalStateException("Activity is null");
        }

        Calendar selectedDate = Calendar.getInstance();
        //noinspection MagicConstant
        selectedDate.set(year, monthOfYear, dayOfMonth);
        if (selectedDate.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY) {
            Toast.makeText(activity, activity.getString(R.string.error_no_issue_on_sundays), Toast.LENGTH_SHORT).show();
        } else {
            Intent downloadIntent = new Intent(getActivity(), IssueDownloadService.class);
            downloadIntent.putExtra(IssueDownloadService.EXTRA_DAY, dayOfMonth);
            downloadIntent.putExtra(IssueDownloadService.EXTRA_MONTH, monthOfYear + 1);
            downloadIntent.putExtra(IssueDownloadService.EXTRA_YEAR, year);
            getActivity().startService(downloadIntent);
        }
    }
}
