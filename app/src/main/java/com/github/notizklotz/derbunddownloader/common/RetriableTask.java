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

import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;

/**
 * Retries something a couple of times. See http://fahdshariff.blogspot.ch/2009/08/retrying-operations-in-java.html
 *
 * @param <T> return value type
 */
public class RetriableTask<T> implements Callable<T> {

    private static final int DEFAULT_NUMBER_OF_RETRIES = 5;
    private static final long DEFAULT_WAIT_TIME = 1000;
    private final Callable<T> task;
    private final int numberOfRetries;
    private final long timeToWait;

    private int numberOfTriesLeft;

    public RetriableTask(Callable<T> task) {
        this.numberOfRetries = DEFAULT_NUMBER_OF_RETRIES;
        numberOfTriesLeft = DEFAULT_NUMBER_OF_RETRIES;
        this.timeToWait = DEFAULT_WAIT_TIME;
        this.task = task;
    }

    public T call() throws RetryException {
        while (true) {
            try {
                return task.call();
            } catch (InterruptedException | CancellationException e) {
                throw new RetryException("task was aborted", e);
            } catch (Exception e) {
                numberOfTriesLeft--;
                if (numberOfTriesLeft == 0) {
                    throw new RetryException(numberOfRetries +
                            " attempts to retry failed at " + timeToWait +
                            "ms interval", e);
                }
                try {
                    Thread.sleep(timeToWait);
                } catch (InterruptedException e1) {
                    throw new RetryException("task was aborted", e1);
                }
            }
        }
    }

    public boolean hasRetried() {
        return numberOfRetries != numberOfTriesLeft;
    }

    public static class RetryException extends RuntimeException {
        RetryException(String s, Exception e) {
            super(s, e);
        }
    }
}