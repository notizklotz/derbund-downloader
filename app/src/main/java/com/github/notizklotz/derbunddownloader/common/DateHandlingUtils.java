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

import org.joda.time.DateTimeZone;
import org.joda.time.LocalDate;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.util.Locale;

public class DateHandlingUtils {

    public static final DateTimeZone TIMEZONE_SWITZERLAND = DateTimeZone.forID("Europe/Zurich");
    public static final Locale SERVER_LOCALE = new Locale("de", "CH");
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormat.forPattern("dd.MM.yyyy");

    private DateHandlingUtils() {
    }

    public static DateTimeZone getUserTimezone() {
        return DateTimeZone.getDefault();
    }

    public static String toDateString(LocalDate localDate) {
        return localDate != null ? localDate.toString(DATE_TIME_FORMATTER) : "";
    }

    public static LocalDate fromDateString(String dateString) {
        return LocalDate.parse(dateString, DATE_TIME_FORMATTER);
    }

}
