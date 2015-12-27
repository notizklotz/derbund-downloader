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

import android.net.Uri;
import android.support.test.runner.AndroidJUnit4;

import org.joda.time.LocalDate;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
@Ignore("Issues real requests against the ePaper API")
public class EpaperApiClientIntegrationTest {

    @Test(expected = EpaperApiInvalidCredentialsException.class)
    public void invalidLogin() throws Exception {
        new EpaperApiClient().getPdfDownloadUrl("asdfawer", "asdfasdfa", new LocalDate(2015, 12, 14));
    }

    /**
     * This test can only be executed manually because it requires valid credentials.
     */
    @SuppressWarnings("ConstantConditions")
    @Test
    public void validLogin() throws Exception {
        String username = null;
        String password = null;

        Assume.assumeNotNull(username, password);

        Uri pdfDownloadUrl = new EpaperApiClient().getPdfDownloadUrl(username, password, new LocalDate(2015, 12, 14));
        Assert.assertNotNull(pdfDownloadUrl);
    }

}