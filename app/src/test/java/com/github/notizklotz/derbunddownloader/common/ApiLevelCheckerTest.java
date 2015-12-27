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

import android.os.Build;

import org.junit.Test;

import static org.junit.Assert.*;

public class ApiLevelCheckerTest {

    @Test
    public void expectedApiLevelIsEqualToAvailableLevel() throws Exception {
        assertTrue(new ApiLevelChecker().isApiLevelAvailable(Build.VERSION.SDK_INT));
    }

    @Test
    public void expectedApiLevelIsHigherThanAvailableLevel() throws Exception {
        assertFalse(new ApiLevelChecker().isApiLevelAvailable(Build.VERSION.SDK_INT + 1));
    }

    @Test
    public void expectedApiLevelIsLowerThanAvailableLevel() throws Exception {
        assertTrue(new ApiLevelChecker().isApiLevelAvailable(Build.VERSION.SDK_INT - 1));
    }
}