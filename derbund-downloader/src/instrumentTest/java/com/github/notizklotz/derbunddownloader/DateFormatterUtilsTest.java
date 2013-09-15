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

import junit.framework.TestCase;

public class DateFormatterUtilsTest extends TestCase {

    public void testToHH_MM() {
        String result = DateFormatterUtils.toHH_MM(6, 2);
        assertEquals("06:02", result);
    }

    public void testToDDMMYYYYString() {
        String result = DateFormatterUtils.toDDMMYYYYString(5, 6, 2000);
        assertEquals("05062000", result);
    }

    public void testToDD_MM_YYYYString() {
        String result = DateFormatterUtils.toDD_MM_YYYYString(5, 6, 2000);
        assertEquals("05.06.2000", result);
    }
}
