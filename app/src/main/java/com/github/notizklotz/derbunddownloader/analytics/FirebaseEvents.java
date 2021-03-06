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

package com.github.notizklotz.derbunddownloader.analytics;

public class FirebaseEvents {

    private FirebaseEvents() {
    }

    public static final String KEY_JOB_API = "job_api";
    public static final String KEY_DOWNLOAD_TRIGGER = "download_trigger";

    public static final String DOWNLOAD_ISSUE_COMPLETED = "download_issue_completed";

    public static final String USER_ERROR = "user_error";

    public static final String RETRY_SUCCEEDED = "retry_succeeded";

    public static final String CONNECTION_ERROR = "connection_error";
}
