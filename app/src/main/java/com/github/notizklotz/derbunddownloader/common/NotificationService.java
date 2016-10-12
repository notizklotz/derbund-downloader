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

import android.app.Application;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.app.TaskStackBuilder;
import android.support.v7.app.NotificationCompat;

import com.github.notizklotz.derbunddownloader.R;
import com.github.notizklotz.derbunddownloader.issuesgrid.DownloadedIssuesActivity;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class NotificationService {

    private final Context context;

    @Inject
    public NotificationService(Application context) {
        this.context = context;
    }

    public void notifyUser(CharSequence contentTitle, CharSequence contentText, boolean error) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context);
        builder
                .setSmallIcon(R.drawable.ic_stat_newspaper)
                .setContentTitle(contentTitle)
                .setContentText(contentText)
                .setTicker(contentTitle)
                .setAutoCancel(true);
        builder.setVisibility(NotificationCompat.VISIBILITY_PUBLIC);
        if (error) {
            builder.setCategory(NotificationCompat.CATEGORY_ERROR);
        }

        //http://developer.android.com/guide/topics/ui/notifiers/notifications.html
        // The stack builder object will contain an artificial back stack for thestarted Activity.
        // This ensures that navigating backward from the Activity leads out of your application to the Home screen.
        builder.setContentIntent(TaskStackBuilder.create(context).
                addParentStack(DownloadedIssuesActivity.class).
                addNextIntent(new Intent(context, DownloadedIssuesActivity.class)).
                getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT));

        NotificationManagerCompat.from(context).notify(1, builder.build());
    }
}
