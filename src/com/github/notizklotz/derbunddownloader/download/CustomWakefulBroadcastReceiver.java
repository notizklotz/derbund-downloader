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


import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.PowerManager;
import android.util.Log;
import android.util.SparseArray;


/**
 * Copied from {@link android.support.v4.content.WakefulBroadcastReceiver} and extended to support more lock-types.
 */
public abstract class CustomWakefulBroadcastReceiver extends BroadcastReceiver {
    private static final String EXTRA_WAKE_LOCK_ID = "android.support.content.wakelockid";

    private static final SparseArray<PowerManager.WakeLock> mActiveWakeLocks
            = new SparseArray<PowerManager.WakeLock>();
    private static int mNextId = 1;

    /**
     * @see android.support.v4.content.WakefulBroadcastReceiver#startWakefulService(android.content.Context, android.content.Intent)
     */
    public static ComponentName startWakefulService(Context context, Intent intent, int wakeLockLevelAndFlags, long wakeLockTimeout) {
        synchronized (mActiveWakeLocks) {
            int id = mNextId;
            mNextId++;
            if (mNextId <= 0) {
                mNextId = 1;
            }

            intent.putExtra(EXTRA_WAKE_LOCK_ID, id);
            ComponentName comp = context.startService(intent);
            if (comp == null) {
                return null;
            }

            PowerManager pm = (PowerManager)context.getSystemService(Context.POWER_SERVICE);
            PowerManager.WakeLock wl = pm.newWakeLock(wakeLockLevelAndFlags,
                    "wake:" + comp.flattenToShortString());
            wl.setReferenceCounted(false);
            wl.acquire(wakeLockTimeout);
            mActiveWakeLocks.put(id, wl);
            return comp;
        }
    }

    /**
     * @see android.support.v4.content.WakefulBroadcastReceiver#completeWakefulIntent(android.content.Intent)
     */
    public static boolean completeWakefulIntent(Intent intent) {
        final int id = intent.getIntExtra(EXTRA_WAKE_LOCK_ID, 0);
        if (id == 0) {
            return false;
        }
        synchronized (mActiveWakeLocks) {
            PowerManager.WakeLock wl = mActiveWakeLocks.get(id);
            if (wl != null) {
                wl.release();
                mActiveWakeLocks.remove(id);
                return true;
            }
            // We return true whether or not we actually found the wake lock
            // the return code is defined to indicate whether the Intent contained
            // an identifier for a wake lock that it was supposed to match.
            // We just log a warning here if there is no wake lock found, which could
            // happen for example if this function is called twice on the same
            // intent or the process is killed and restarted before processing the intent.
            Log.w("WakefulBroadcastReceiver", "No active wake lock id #" + id);
            return true;
        }
    }
}

