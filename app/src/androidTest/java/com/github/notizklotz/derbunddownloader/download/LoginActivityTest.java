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

import android.support.annotation.NonNull;
import android.support.design.widget.TextInputLayout;
import android.support.test.InstrumentationRegistry;
import android.support.test.espresso.matcher.BoundedMatcher;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.view.View;

import com.github.notizklotz.derbunddownloader.DerBundDownloaderApplication;
import com.github.notizklotz.derbunddownloader.R;
import com.github.notizklotz.derbunddownloader.ui.LoginActivity;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.typeText;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.hasFocus;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static org.hamcrest.Matchers.is;

@RunWith(AndroidJUnit4.class)
public class LoginActivityTest {

    @Rule
    public ActivityTestRule<LoginActivity> mActivityRule = new ActivityTestRule<LoginActivity>(LoginActivity.class){
        @Override
        protected void beforeActivityLaunched() {
            DerBundDownloaderApplication applicationContext = (DerBundDownloaderApplication) InstrumentationRegistry.getTargetContext().getApplicationContext();
            applicationContext.getSettingsComponent().settings().setUsernamePasswort(null, null);
        }
    };

    private DerBundDownloaderApplication app;

    @Before
    public void clearPreferences() throws Exception {
        app = (DerBundDownloaderApplication) InstrumentationRegistry.getTargetContext().getApplicationContext();
        app.getSettingsComponent().settings().setUsernamePasswort(null, null);
    }

    @Test
    public void emptyLogin() throws Exception {
        onView(withId(R.id.email_sign_in_button))
                .perform(click());

        onView(withId(R.id.emailInputLayout))
                .check(matches(isDisplayed()))
                .check(matches(hasErrorText(app.getString(R.string.error_field_required))))
                .check(matches(hasFocus()));

        onView(withId(R.id.passwordInputLayout))
                .check(matches(isDisplayed()))
                .check(matches(hasErrorText(app.getString(R.string.error_field_required))));
    }

    @Test
    public void emptyEmail() throws Exception {
        onView(withId(R.id.email)).perform(typeText("test"));

        onView(withId(R.id.email_sign_in_button))
                .perform(click())
                .check(matches(isDisplayed()));

        onView(withId(R.id.passwordInputLayout))
                .check(matches(isDisplayed()))
                .check(matches(hasErrorText(app.getString(R.string.error_field_required))))
                .check(matches(hasFocus()));
    }

    @NonNull
    private BoundedMatcher<View, TextInputLayout> hasErrorText(final String text) {
        final Matcher<String> stringMatcher = is(text);
        return new BoundedMatcher<View, TextInputLayout>(TextInputLayout.class) {

                @Override
                public void describeTo(Description description) {

                    description.appendText("with error: ");
                    stringMatcher.describeTo(description);
                }

                @Override
                protected boolean matchesSafely(TextInputLayout view) {
                    return stringMatcher.matches(view.getError() != null ? view.getError().toString() : null);
                }
            };
    }

}
