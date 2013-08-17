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

import java.io.File;
import java.util.Calendar;

class Issue implements Comparable<Issue> {

    Issue(int day, int month, int year, File file) {
        this.day = day;
        this.month = month;
        this.year = year;
        this.file = file;
    }

    private final int day;

    private final int month;

    private final int year;

    private final File file;

    public Calendar getDate() {
        Calendar instance = Calendar.getInstance();
        //noinspection MagicConstant
        instance.set(year, month - 1, day);
        return instance;
    }

    @Override
    public String toString() {
        return "Issue{" +
                "day=" + day +
                ", month=" + month +
                ", year=" + year +
                ", file=" + file +
                '}';
    }

    @Override
    public int compareTo(Issue another) {
        return this.getDate().compareTo(another.getDate());
    }
}
