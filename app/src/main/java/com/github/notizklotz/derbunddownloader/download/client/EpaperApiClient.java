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

package com.github.notizklotz.derbunddownloader.download.client;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.support.annotation.NonNull;

import com.github.notizklotz.derbunddownloader.R;
import com.github.notizklotz.derbunddownloader.common.DateHandlingUtils;
import com.github.notizklotz.derbunddownloader.common.RetriableTask;
import com.github.notizklotz.derbunddownloader.download.ThumbnailRegistry;

import org.joda.time.LocalDate;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import javax.inject.Inject;
import javax.inject.Singleton;

import okhttp3.Cookie;
import okhttp3.CookieJar;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okio.BufferedSink;
import okio.Okio;

@Singleton
public class EpaperApiClient {

    private static final String ISSUE_DATE__TEMPLATE = "%04d-%02d-%02d";

    private static final MediaType JSON_CONTENTTYPE = MediaType.parse("application/json; charset=utf-8");
    private static final MediaType JSON_ACCEPT = MediaType.parse("application/json");

    private final ThumbnailRegistry thumbnailRegistry;

    private final Context context;

    private final OkHttpClient client;

    private final SharedPreferences cookiejar;

    private final String domain;

    @Inject
    EpaperApiClient(final Application context, ThumbnailRegistry thumbnailRegistry) {
        this.context = context;
        this.thumbnailRegistry = thumbnailRegistry;
        this.domain = context.getString(R.string.epaper_api_domain);

        cookiejar = context.getSharedPreferences("cookiejar", Context.MODE_PRIVATE);
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
                List<Cookie> result = new LinkedList<>();
                for (Map.Entry<String, ?> cookieEntry : cookiejar.getAll().entrySet()) {
                    result.add(new Cookie.Builder().name(cookieEntry.getKey()).value(cookieEntry.getValue().toString()).domain(domain).build());
                }

                return result;
            }
        }).build();
    }

    public Uri getPdfDownloadUrl(@NonNull String username, @NonNull String password, @NonNull LocalDate issueDate) throws InvalidCredentialsException, InexistingIssueRequestedException {

        boolean subscribed;
        try {
            subscribed = isSubscribed(issueDate);
        } catch (InvalidResponseException e) {
            subscribed = false;
        }

        if (subscribed) {
            return requestPdfDownloadUrl(issueDate);
        } else {
            login(username, password);
            return requestPdfDownloadUrl(issueDate);
        }
    }

    public void login(@NonNull String username, @NonNull String password) throws InvalidCredentialsException, InvalidResponseException {
        cookiejar.edit().clear().apply();

        JSONObject bodyJson;
        try {
            bodyJson = new JSONObject().put("user", username).put("password", password).put("stayLoggedIn", true).put("closeActiveSessions", false);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }

        RequestBody body = RequestBody.create(JSON_CONTENTTYPE, bodyJson.toString());
        Request request = new Request.Builder()
                .header("Accept", JSON_ACCEPT.toString())
                .url("https://" + domain + "/index.cfm/authentication/login")
                .post(body)
                .build();

        Response response = new RetriableTask<>(new HttpRequestCallable(client, request)).call();

        if (!response.isSuccessful()) {
            throw new InvalidResponseException("Login response was not successful " + response.code());
        }

        try {
            JSONObject responseBodyJson = new JSONObject(response.body().string());
            if (!responseBodyJson.getBoolean("success")) {
                String errorCode = responseBodyJson.optString("errorCode");
                throw new InvalidCredentialsException("Code: " + errorCode + ", " + responseBodyJson.optString("error"));
            }
        } catch (JSONException | IOException e) {
            throw new InvalidResponseException(e);
        }
    }

    private boolean isSubscribed(@NonNull LocalDate issueDate) throws InvalidResponseException {
        String issueDateString = String.format(DateHandlingUtils.SERVER_LOCALE, ISSUE_DATE__TEMPLATE, issueDate.getYear(), issueDate.getMonthOfYear(), issueDate.getDayOfMonth());

        JSONObject bodyJson;
        try {
            bodyJson = new JSONObject()
                    .put("editions", new JSONArray().put(new JSONObject().put("defId", context.getString(R.string.epaper_api_defid)).put("publicationDate", issueDateString)))
                    .put("articles", new JSONArray().put(42));
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }

        RequestBody body = RequestBody.create(JSON_CONTENTTYPE, bodyJson.toString());
        Request request = new Request.Builder()
                .header("Accept", JSON_ACCEPT.toString())
                .url("https://" + domain + "/index.cfm/epaper/1.0/getArticle")
                .post(body)
                .build();
        Response response = new RetriableTask<>(new HttpRequestCallable(client, request)).call();
        if (!response.isSuccessful()) {
            throw new InvalidResponseException("Subscription check url response was not successful " + response.code());
        }

        try {
            return new JSONObject(response.body().string()).getBoolean("isSubscribed");
        } catch (JSONException | IOException e) {
            throw new InvalidResponseException(e);
        }
    }

    @NonNull
    private Uri requestPdfDownloadUrl(@NonNull LocalDate issueDate) throws InexistingIssueRequestedException {
        String issueDateString = String.format(DateHandlingUtils.SERVER_LOCALE, ISSUE_DATE__TEMPLATE, issueDate.getYear(), issueDate.getMonthOfYear(), issueDate.getDayOfMonth());

        JSONObject bodyJson;
        try {
            bodyJson = new JSONObject()
                    .put("editions", new JSONArray().put(new JSONObject().put("defId", context.getString(R.string.epaper_api_defid)).put("publicationDate", issueDateString)))
                    .put("isAttachment", true)
                    .put("fileName", "Gesamtausgabe_Tages-Anzeiger_" + issueDateString + ".pdf");
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }

        RequestBody body = RequestBody.create(JSON_CONTENTTYPE, bodyJson.toString());
        Request request = new Request.Builder()
                .header("Accept", JSON_ACCEPT.toString())
                .url("https://" + domain + "/index.cfm/epaper/1.0/getEditionDoc")
                .post(body)
                .build();

        Response response = new RetriableTask<>(new HttpRequestCallable(client, request)).call();

        if (!response.isSuccessful()) {
            if (response.code() == 500) {
                throw new InexistingIssueRequestedException(issueDate);
            }

            throw new InvalidResponseException("Request PDF url response was not successful " + response.code());
        }

        String uriString;
        try {
            JSONObject jsonObject = new JSONObject(response.body().string());
            uriString = null;
            JSONArray data = jsonObject.optJSONArray("data");
            if (data != null) {
                JSONObject jsonObject1 = data.optJSONObject(0);
                if (jsonObject1 != null) {
                    uriString = jsonObject1.optString("issuepdf");
                    if (uriString == null) {
                        uriString = jsonObject1.getString("issuefile");
                    }
                }
            }
        } catch (JSONException | IOException e) {
            throw new InvalidResponseException("Could not parse body", e);
        }

        if (uriString == null) {
            throw new InexistingIssueRequestedException(issueDate);
        }

        return Uri.parse(uriString);
    }

    @NonNull
    private Uri getPdfThumbnailUrl(@NonNull LocalDate issueDate) {
        String issueDateString = String.format(DateHandlingUtils.SERVER_LOCALE, ISSUE_DATE__TEMPLATE,
                issueDate.getYear(), issueDate.getMonthOfYear(), issueDate.getDayOfMonth());

        JSONObject bodyJson;
        try {
            bodyJson = new JSONObject()
                    .put("editions", new JSONArray()
                            .put(new JSONObject()
                                    .put("defId", context.getString(R.string.epaper_api_defid))
                                    .put("publicationDate", issueDateString)));
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }

        RequestBody body = RequestBody.create(JSON_CONTENTTYPE, bodyJson.toString());
        final Request request = new Request.Builder()
                .header("Accept", JSON_ACCEPT.toString())
                .url("https://" + domain + "/index.cfm/epaper/1.0/getFirstPage")
                .post(body)
                .build();

        Response response = new RetriableTask<>(new HttpRequestCallable(client, request)).call();

        if (!response.isSuccessful()) {
            throw new InvalidResponseException("Request PDF url response was not successful " + response.code());
        }

        try {
            return Uri.parse(new JSONObject(response.body().string())
                    .getJSONObject("data")
                    .getJSONArray("pages")
                    .getJSONObject(0)
                    .getJSONObject("pageDocUrl")
                    .getJSONObject("PREVIEW")
                    .getString("url"));
        } catch (JSONException | IOException e) {
            throw new InvalidResponseException("Could not parse body", e);
        }
    }

    public void savePdfThumbnail(@NonNull final LocalDate issueDate) {
        final Uri thumbnailUrl = getPdfThumbnailUrl(issueDate);

        final Request request = new Request.Builder()
                .url(thumbnailUrl.toString())
                .get()
                .build();

        final File thumbnailFile = thumbnailRegistry.getThumbnailFile(issueDate);

        //noinspection ResultOfMethodCallIgnored
        thumbnailFile.getParentFile().mkdirs();

        new RetriableTask<>(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                Response response = client.newCall(request).execute();
                if (response.isSuccessful()) {
                    BufferedSink sink = Okio.buffer(Okio.sink(thumbnailFile));
                    ResponseBody body = response.body();
                    sink.writeAll(body.source());
                    sink.close();
                    body.close();
                }

                return null;
            }
        }).call();
    }

    private static class HttpRequestCallable implements Callable<Response> {

        private final OkHttpClient client;
        private final Request request;

        private HttpRequestCallable(@NonNull OkHttpClient client, @NonNull Request request) {
            this.client = client;
            this.request = request;
        }

        @Override
        @NonNull
        public Response call() throws Exception {
            return client.newCall(request).execute();
        }
    }


}