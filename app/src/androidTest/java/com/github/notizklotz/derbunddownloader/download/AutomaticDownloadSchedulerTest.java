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

import com.github.notizklotz.derbunddownloader.common.DateHandlingUtils;

import org.joda.time.DateTime;
import org.junit.Test;

import static org.junit.Assert.assertEquals;


public class AutomaticDownloadSchedulerTest {

    @Test
    public void calculateNextAlarmTime() throws Exception {
        DateTime dateTime = new AutomaticDownloadScheduler(null, null)
                .calculateNextAlarmTime(new DateTime(2016, 10, 13, 12, 12, DateHandlingUtils.TIMEZONE_SWITZERLAND).toInstant(), 60);
        assertEquals(new DateTime(2016, 10, 14, 5, 1, DateHandlingUtils.TIMEZONE_SWITZERLAND), dateTime);
    }

}