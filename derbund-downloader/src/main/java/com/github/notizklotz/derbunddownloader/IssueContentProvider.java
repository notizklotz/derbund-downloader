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

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.provider.BaseColumns;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class IssueContentProvider extends ContentProvider {

    public static final String COLUMN_DAY = "day";
    public static final String COLUMN_MONTH = "month";
    public static final String COLUMN_YEAR = "year";
    public static final String COLUMN_FILEPATH = "filepath";

    public static final String[] COLUMN_NAMES = new String[]{BaseColumns._ID, COLUMN_DAY, COLUMN_MONTH, COLUMN_YEAR, COLUMN_FILEPATH};

    public static final Uri ISSUES_URI = Uri.parse("content://com.github.notizklotz.derbunddownloader.issues");

    public static ContentValues createContentValues(int day, int month, int year, String filepath) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(COLUMN_DAY, day);
        contentValues.put(COLUMN_MONTH, month);
        contentValues.put(COLUMN_YEAR, year);
        contentValues.put(COLUMN_FILEPATH, filepath);
        return contentValues;
    }

    public static File getIssuesDirectory(Context context) {
        return new File(context.getCacheDir(), "issues");
    }

    @Override
    public boolean onCreate() {
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        MatrixCursor matrixCursor = new MatrixCursor(COLUMN_NAMES);

        Context context = getContext();
        if (context == null) {
            throw new IllegalStateException("Context not available");
        }
        File issuesDir = getIssuesDirectory(context);
        File[] files = issuesDir.listFiles();
        if (files != null) {
            List<File> sortedFiles = Arrays.asList(files);
            Collections.sort(sortedFiles, Collections.reverseOrder());

            for (File issueFile : sortedFiles) {
                int year = Integer.parseInt(issueFile.getName().substring(0, 4));
                int month = Integer.parseInt(issueFile.getName().substring(4, 6));
                int day = Integer.parseInt(issueFile.getName().substring(6, 8));
                matrixCursor.newRow().add(createId(year, month, day)).add(day).add(month).add(year).add(issueFile.getPath());
            }
        }

        return matrixCursor;
    }

    private String createId(int year, int month, int day) {
        return String.valueOf(year) + month + day;
    }

    @Override
    public String getType(Uri uri) {
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        Integer year = values.getAsInteger(COLUMN_YEAR);
        Integer month = values.getAsInteger(COLUMN_MONTH);
        Integer day = values.getAsInteger(COLUMN_DAY);

        if (year == null || month == null || day == null) {
            throw new IllegalArgumentException("All date colums must be given");
        }

        String id = createId(year, month, year);
        Uri uri1 = ContentUris.withAppendedId(ISSUES_URI, Integer.parseInt(id));
        Context context = getContext();
        if (context == null) {
            throw new IllegalStateException("Context is not initialized");
        }

        context.getContentResolver().notifyChange(uri1, null);
        return uri1;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        return 0;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        return 0;
    }
}
