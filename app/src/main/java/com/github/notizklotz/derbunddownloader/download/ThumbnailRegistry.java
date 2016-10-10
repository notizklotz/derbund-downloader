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

package com.github.notizklotz.derbunddownloader.download;

import android.app.Application;
import android.content.Context;
import android.support.annotation.NonNull;
import android.util.Log;

import org.joda.time.LocalDate;

import java.io.File;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class ThumbnailRegistry {

    private static final String TAG = "ThumbnailRegistry";

    private final Context context;

    @Inject
    public ThumbnailRegistry(Application context) {
        this.context = context;
    }

    @NonNull
    public File getThumbnailFile(@NonNull LocalDate issueDate) {
        return new File(getCacheDir(), issueDate.toString() + ".jpg");
    }

    public void clear(@NonNull LocalDate issueDate) {
        try {
            File thumbnailFile = getThumbnailFile(issueDate);
            //noinspection ResultOfMethodCallIgnored
            thumbnailFile.delete();
        } catch (Exception e) {
            Log.e(TAG, "clear: failed", e);
        }
    }

    public void clearAll() {
        try {
            File cacheDir = getCacheDir();
            if (cacheDir.exists()) {
                for (File file : cacheDir.listFiles()) {
                    //noinspection ResultOfMethodCallIgnored
                    file.delete();
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "clearAll: failed", e);
        }
    }

    @NonNull
    private File getCacheDir() {
        return new File(context.getCacheDir(), "thumbs");
    }
}
