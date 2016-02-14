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
import android.support.annotation.NonNull;

import com.github.notizklotz.derbunddownloader.common.DateHandlingUtils;

import org.androidannotations.annotations.EBean;
import org.joda.time.LocalDate;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@EBean
public class EpaperApiClient {

    private static final String ISSUE_DATE__TEMPLATE = "%04d-%02d-%02d";

    public Uri getPdfDownloadUrl(@NonNull String username, @NonNull String password, @NonNull LocalDate issueDate) throws EpaperApiInvalidResponseException, EpaperApiInvalidCredentialsException {
        String cookiesHeader = login(username, password);
        Uri uri = requestPdfDownloadUrl(issueDate, cookiesHeader);
        logout(cookiesHeader);
        return uri;
    }

    @NonNull
    private String login(@NonNull String username, @NonNull String password) throws EpaperApiInvalidCredentialsException, EpaperApiInvalidResponseException {
        HttpHeaders loginRequestHeaders = new HttpHeaders();
        loginRequestHeaders.add("Accept", "application/json");
        loginRequestHeaders.add("Content-Type", "application/json;charset=UTF-8");
        HttpEntity<String> stringHttpEntity = new HttpEntity<String>("{\"user\":\"" + username + "\",\"password\":\"" + password + "\",\"stayLoggedIn\":false,\"closeActiveSessions\":true}", loginRequestHeaders);
        ResponseEntity<String> response = new RestTemplate(true).exchange("http://epaper.derbund.ch/index.cfm/authentication/login", HttpMethod.POST, stringHttpEntity, String.class);

        try {
            JSONObject body = new JSONObject(response.getBody());
            if (body.getBoolean("success")) {
                return extractCookies(response.getHeaders());
            } else {
                if ("INVALID_CREDENTIALS".equals(body.getString("errorCode"))) {
                    throw new EpaperApiInvalidCredentialsException();
                } else {
                    throw new EpaperApiInvalidResponseException(body.getString("error"));
                }
            }
        } catch (JSONException e) {
            throw new EpaperApiInvalidResponseException(e);
        }
    }

    private void logout(@NonNull String cookiesHeader) throws EpaperApiInvalidResponseException {
        HttpHeaders requestHeaders = new HttpHeaders();
        requestHeaders.add("Cookie", cookiesHeader);
        requestHeaders.add("Accept", "application/json");
        HttpEntity<Void> requestEntity = new HttpEntity<Void>(requestHeaders);
        ResponseEntity<String> responseEntity = new RestTemplate(true).exchange("http://epaper.derbund.ch/index.cfm/authentication/logout?", HttpMethod.GET, requestEntity, String.class);

        try {
            JSONObject body = new JSONObject(responseEntity.getBody());
            if (!body.getBoolean("success")) {
                throw new EpaperApiInvalidResponseException(body.getString("error"));
            }
        } catch (JSONException e) {
            throw new EpaperApiInvalidResponseException(e);
        }
    }

    @NonNull
    private Uri requestPdfDownloadUrl(@NonNull LocalDate issueDate, @NonNull String cookiesHeader) throws EpaperApiInvalidResponseException {
        HttpHeaders requestHeaders = new HttpHeaders();
        requestHeaders.add("Cookie", cookiesHeader);
        requestHeaders.add("Accept", "application/json");
        requestHeaders.add("Content-Type", "application/json");
        String issueDateString = String.format(DateHandlingUtils.SERVER_LOCALE, ISSUE_DATE__TEMPLATE, issueDate.getYear(), issueDate.getMonthOfYear(), issueDate.getDayOfMonth());
        HttpEntity<String> request = new HttpEntity<String>("{\"editions\":[{\"defId\":\"46\",\"publicationDate\":\"" + issueDateString + "\"}],\"isAttachment\":true,\"fileName\":\"Gesamtausgabe_Der_Bund_" + issueDateString + ".pdf\"}", requestHeaders);

        ResponseEntity<String> doc = new RestTemplate(true).exchange("http://epaper.derbund.ch/index.cfm/epaper/1.0/getEditionDoc", HttpMethod.POST, request, String.class);

        try {
            return Uri.parse(new JSONObject(doc.getBody()).getJSONArray("data").getJSONObject(0).getString("issuepdf"));
        } catch (JSONException e) {
            throw new EpaperApiInvalidResponseException(e);
        }
    }

    @NonNull
    public Uri getPdfThumbnailUrl(@NonNull LocalDate issueDate) throws EpaperApiInvalidResponseException {
        HttpHeaders requestHeaders = new HttpHeaders();
        requestHeaders.add("Accept", "application/json");
        requestHeaders.add("Content-Type", "application/json");
        String issueDateString = String.format(DateHandlingUtils.SERVER_LOCALE, ISSUE_DATE__TEMPLATE, issueDate.getYear(), issueDate.getMonthOfYear(), issueDate.getDayOfMonth());
        HttpEntity<String> request = new HttpEntity<String>("{\"editions\":[{\"defId\":\"46\",\"publicationDate\":\"" + issueDateString + "\"}]}", requestHeaders);

        ResponseEntity<String> doc = new RestTemplate(true).exchange("http://epaper.derbund.ch/index.cfm/epaper/1.0/getFirstPage", HttpMethod.POST, request, String.class);
        try {
            return Uri.parse(new JSONObject(doc.getBody()).getJSONObject("data").getJSONArray("pages").getJSONObject(0).getJSONObject("pageDocUrl").getJSONObject("PREVIEW").getString("url"));
        } catch (JSONException e) {
            throw new EpaperApiInvalidResponseException(e);
        }
    }

    @NonNull
    private String extractCookies(@NonNull HttpHeaders headers) {
        StringBuilder sb = new StringBuilder();
        List<String> cookiesList = headers.get("Set-Cookie");
        if (cookiesList != null) {
            for (int i = 0; i < cookiesList.size(); i++) {
                sb.append(cookiesList.get(i).split(";")[0]);
                if (i < cookiesList.size() - 1) {
                    sb.append("; ");
                }
            }
        }
        return sb.toString();
    }

}
