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

import android.content.AsyncTaskLoader;
import android.content.Context;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

class IssuesLoader extends AsyncTaskLoader<List<Issue>> {

    private final File issuesDirectory;

    private List<Issue> cachedIssues;

    public IssuesLoader(Context context, File issuesDirectory) {
        super(context);
        this.issuesDirectory = issuesDirectory;
    }

    @Override
    public List<Issue> loadInBackground() {
        List<Issue> issues = new ArrayList<Issue>();

        File[] files = issuesDirectory.listFiles();
        if (files == null) {
            throw new IllegalStateException("Given path is not a directory " + issuesDirectory);
        }
        for (File issueFile : files) {
            int year = Integer.parseInt(issueFile.getName().substring(0, 4));
            int month = Integer.parseInt(issueFile.getName().substring(4, 6));
            int day = Integer.parseInt(issueFile.getName().substring(6, 8));
            issues.add(new Issue(day, month, year, issueFile));
        }
        Collections.sort(issues, Collections.reverseOrder());
        return issues;
    }

    @Override
    public void deliverResult(List<Issue> issues) {
        this.cachedIssues = issues;

        if (isStarted()) {
            // If the Loader is currently started, we can immediately
            // deliver its results.
            super.deliverResult(issues);
        }
    }

    /**
     * Handles a request to start the Loader.
     */
    @Override
    protected void onStartLoading() {
        if (cachedIssues != null) {
            // If we currently have a result available, deliver it
            // immediately.
            deliverResult(cachedIssues);
        }

        if (takeContentChanged() || cachedIssues == null) {
            // If the data has changed since the last time it was loaded
            // or is not currently available, start a load.
            forceLoad();
        }
    }

    /**
     * Handles a request to stop the Loader.
     */
    @Override
    protected void onStopLoading() {
        // Attempt to cancel the current load task if possible.
        cancelLoad();
    }

    /**
     * Handles a request to completely reset the Loader.
     */
    @Override
    protected void onReset() {
        super.onReset();

        // Ensure the loader is stopped
        onStopLoading();

        // At this point we can release the resources associated with 'apps'
        // if needed.
        if (cachedIssues != null) {
            cachedIssues = null;
        }
    }

}
