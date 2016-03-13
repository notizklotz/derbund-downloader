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

package com.github.notizklotz.derbunddownloader.common;

import android.content.Context;
import android.net.Uri;
import android.support.annotation.NonNull;

import com.bumptech.glide.Glide;

import org.androidannotations.annotations.EBean;
import org.androidannotations.annotations.RootContext;

@EBean
public class ThumbnailRegistry {

    @RootContext
    Context context;

    public void registerUri(@NonNull String description, @NonNull Uri thumbnailUri) {
        context.getSharedPreferences("thumbnailregistry", Context.MODE_PRIVATE).edit().putString(description, thumbnailUri.toString()).commit();
    }

    @NonNull
    public String getUri(@NonNull String description) {
        return context.getSharedPreferences("thumbnailregistry", Context.MODE_PRIVATE).getString(description, "");
    }
    
    public void clear(@NonNull String description) {
        context.getSharedPreferences("thumbnailregistry", Context.MODE_PRIVATE).edit().remove(description).commit();
    }

    public void clearAll() {
        context.getSharedPreferences("thumbnailregistry", Context.MODE_PRIVATE).edit().clear().commit();
        Glide.get(context).clearDiskCache();
    }
}
