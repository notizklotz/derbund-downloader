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

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.support.annotation.NonNull;

import com.github.notizklotz.derbunddownloader.analytics.AnalyticsTracker;
import com.github.notizklotz.derbunddownloader.common.DateHandlingUtils;
import com.google.android.gms.analytics.HitBuilders;

import org.androidannotations.annotations.AfterInject;
import org.androidannotations.annotations.Bean;
import org.androidannotations.annotations.EBean;
import org.androidannotations.annotations.RootContext;
import org.joda.time.LocalDate;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import okhttp3.Cookie;
import okhttp3.CookieJar;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

@EBean(scope = EBean.Scope.Singleton)
public class EpaperApiClient {

    private static final String ISSUE_DATE__TEMPLATE = "%04d-%02d-%02d";

    private static final MediaType JSON_CONTENTTYPE = MediaType.parse("application/json; charset=utf-8");
    private static final MediaType JSON_ACCEPT = MediaType.parse("application/json");

    private OkHttpClient client;

    @Bean
    AnalyticsTracker analyticsTracker;

    @RootContext
    Context context;

    @AfterInject
    public void init() {
        final SharedPreferences cookiejar = context.getSharedPreferences("cookiejar", Context.MODE_PRIVATE);
        this.client = new OkHttpClient.Builder().cookieJar(new CookieJar() {
            @Override
            public void saveFromResponse(HttpUrl url, List<Cookie> cookies) {
                SharedPreferences.Editor edit = cookiejar.edit();
                for (Cookie cookie : cookies) {
                    edit.putString(cookie.name(), cookie.value());
                }
                edit.apply();
            }

            @Override
            public List<Cookie> loadForRequest(HttpUrl url) {
                List<Cookie> result = new LinkedList<Cookie>();
                for (Map.Entry<String, ?> cookieEntry : cookiejar.getAll().entrySet()) {
                    result.add(new Cookie.Builder().name(cookieEntry.getKey()).value(cookieEntry.getValue().toString()).domain("epaper.derbund.ch").build());
                }

                return result;
            }
        }).build();
    }

    public Uri getPdfDownloadUrl(@NonNull String username, @NonNull String password, @NonNull LocalDate issueDate)
            throws EpaperApiInvalidResponseException, EpaperApiInvalidCredentialsException, EpaperApiInexistingIssueRequestedException {
        try {
            return requestPdfDownloadUrl(issueDate);
        } catch (EpaperApiInvalidResponseException e) {
            analyticsTracker.send(new HitBuilders.ExceptionBuilder().setFatal(false).setDescription("Retry requestPdfDownloadUrl with login"));
            login(username, password);
            return requestPdfDownloadUrl(issueDate);
        }
    }

    private void login(@NonNull String username, @NonNull String password) throws EpaperApiInvalidCredentialsException, EpaperApiInvalidResponseException {
        try {
            JSONObject bodyJson = new JSONObject().put("user", username).put("password", password).put("stayLoggedIn", true).put("closeActiveSessions", false);
            RequestBody body = RequestBody.create(JSON_CONTENTTYPE, bodyJson.toString());
            Request request = new Request.Builder()
                    .header("Accept", JSON_ACCEPT.toString())
                    .url("http://epaper.derbund.ch/index.cfm/authentication/login")
                    .post(body)
                    .build();
            Response response = client.newCall(request).execute();
            if (!response.isSuccessful()) {
                throw new EpaperApiInvalidResponseException("Login response was not successful " + response.code());
            }

            JSONObject responseBodyJson = new JSONObject(response.body().string());
            if (!responseBodyJson.getBoolean("success")) {
                if ("INVALID_CREDENTIALS".equals(responseBodyJson.getString("errorCode"))) {
                    throw new EpaperApiInvalidCredentialsException();
                } else {
                    throw new EpaperApiInvalidResponseException(responseBodyJson.getString("error"));
                }
            }
        } catch (JSONException e) {
            throw new EpaperApiInvalidResponseException(e);
        } catch (IOException e) {
            throw new EpaperApiInvalidResponseException(e);
        }
    }

    @NonNull
    private Uri requestPdfDownloadUrl(@NonNull LocalDate issueDate) throws EpaperApiInvalidResponseException, EpaperApiInexistingIssueRequestedException {
        String issueDateString = String.format(DateHandlingUtils.SERVER_LOCALE, ISSUE_DATE__TEMPLATE, issueDate.getYear(), issueDate.getMonthOfYear(), issueDate.getDayOfMonth());

        try {
            JSONObject bodyJson = new JSONObject()
                    .put("editions", new JSONArray().put(new JSONObject().put("defId", "46").put("publicationDate", issueDateString)))
                    .put("isAttachment", true)
                    .put("fileName", "Gesamtausgabe_Der_Bund_" + issueDateString + ".pdf");

            RequestBody body = RequestBody.create(JSON_CONTENTTYPE, bodyJson.toString());
            Request request = new Request.Builder()
                    .header("Accept", JSON_ACCEPT.toString())
                    .url("http://epaper.derbund.ch/index.cfm/epaper/1.0/getEditionDoc")
                    .post(body)
                    .build();
            Response response = client.newCall(request).execute();
            if (!response.isSuccessful()) {
                if (response.code() == 500) {
                    throw new EpaperApiInexistingIssueRequestedException(issueDate);
                }

                throw new EpaperApiInvalidResponseException("Request PDF url response was not successful " + response.code());
            }

            return Uri.parse(new JSONObject(response.body().string()).getJSONArray("data").getJSONObject(0).getString("issuepdf"));
        } catch (JSONException e) {
            throw new EpaperApiInvalidResponseException(e);
        } catch (IOException e) {
            throw new EpaperApiInvalidResponseException(e);
        }
    }

    @NonNull
    public Uri getPdfThumbnailUrl(@NonNull LocalDate issueDate) throws EpaperApiInvalidResponseException {
        String issueDateString = String.format(DateHandlingUtils.SERVER_LOCALE, ISSUE_DATE__TEMPLATE, issueDate.getYear(), issueDate.getMonthOfYear(), issueDate.getDayOfMonth());

        try {
            JSONObject bodyJson = new JSONObject()
                    .put("editions", new JSONArray().put(new JSONObject().put("defId", "46").put("publicationDate", issueDateString)));

            RequestBody body = RequestBody.create(JSON_CONTENTTYPE, bodyJson.toString());
            Request request = new Request.Builder()
                    .header("Accept", JSON_ACCEPT.toString())
                    .url("http://epaper.derbund.ch/index.cfm/epaper/1.0/getFirstPage")
                    .post(body)
                    .build();
            Response response = client.newCall(request).execute();
            if (!response.isSuccessful()) {
                throw new EpaperApiInvalidResponseException("Request PDF url response was not successful " + response.code());
            }

            return Uri.parse(new JSONObject(response.body().string()).getJSONObject("data").getJSONArray("pages").getJSONObject(0).getJSONObject("pageDocUrl").getJSONObject("PREVIEW").getString("url"));
        } catch (JSONException e) {
            throw new EpaperApiInvalidResponseException(e);
        } catch (IOException e) {
            throw new EpaperApiInvalidResponseException(e);
        }
    }

}
