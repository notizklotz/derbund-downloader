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

class DateFormatterUtils {

    private static final String FORMAT_HH_MM = "%02d:%02d";
    private static final String FORMAT_DDMMYYYY = "%02d%02d%04d";
    private static final String FORMAT_DD_MM_YYYY = "%02d.%02d.%04d";

    private DateFormatterUtils() {
    }

    public static String toDDMMYYYYString(int day, int month, int year) {
        return String.format(FORMAT_DDMMYYYY, day, month, year);
    }

    public static String toDD_MM_YYYYString(int day, int month, int year) {
        return String.format(FORMAT_DD_MM_YYYY, day, month, year);
    }

    public static String toHH_MM(int hours, int minutes) {
        return String.format(FORMAT_HH_MM, hours, minutes);
    }
}
