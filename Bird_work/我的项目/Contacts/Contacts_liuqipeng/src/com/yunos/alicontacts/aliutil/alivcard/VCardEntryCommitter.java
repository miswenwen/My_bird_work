/*
 * Copyright (C) 2009 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.yunos.alicontacts.aliutil.alivcard;

import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.ContentResolver;
import android.content.OperationApplicationException;
import android.net.Uri;
import android.os.RemoteException;
import android.provider.ContactsContract;
import android.util.Log;

import java.util.ArrayList;

/**
 * <P>
 * {@link VCardEntryHandler} implementation which commits the entry to ContentResolver.
 * </P>
 * <P>
 * Note:<BR />
 * Each vCard may contain big photo images encoded by BASE64,
 * If we store all vCard entries in memory, OutOfMemoryError may be thrown.
 * Thus, this class push each VCard entry into ContentResolver immediately.
 * </P>
 */
public class VCardEntryCommitter implements VCardEntryHandler {
    public static String LOG_TAG = VCardConstants.LOG_TAG;

    /**
     * The max operation count for applyBatch is 500.
     * We need a value that is less than 500 and cost proper time for applyBatch.
     * On L9, a typical value is about 100 op/s,
     * to make a response, e.g. send notification, in a reasonable time (2s),
     * we use 200 here.
     * We have a risk of on VCard entry with more than 300 data items.
     * But this issue also exists in old code. In the old (android) code,
     * the average count of data items is considered to be no more than 25.
     * So this is more safe than count the VCard entries.
     */
    public static final int MAX_DB_OPERATION_COUNT = 200;

    /**
     * As we have made a big batch operations in MAX_DB_OPERATION_COUNT.
     * This will cause a long time (about 2s, or even more if phone is busy) db lock.
     * If other app can not get db access between 2 batch operations,
     * then it will be too long to wait.
     * So we let the db have chance to serve other apps every batch.
     */
    public static final long TIME_INTERVAL_FOR_YIELD = 1000;
    public static final long YIELD_TIME_IN_MILLIS = 50;

    private final ContentResolver mContentResolver;
    private long mTimeToCommit;
    private long mLastYieldTime = 0;
    private ArrayList<ContentProviderOperation> mOperationList;
    private final ArrayList<Uri> mCreatedUris = new ArrayList<Uri>();

    public VCardEntryCommitter(ContentResolver resolver) {
        mContentResolver = resolver;
    }

    @Override
    public void onStart() {
    }

    @Override
    public void onEnd() {
        if (mOperationList != null) {
            mCreatedUris.add(pushIntoContentResolver(mOperationList));
        }

        if (VCardConfig.showPerformanceLog()) {
            Log.d(LOG_TAG, String.format("time to commit entries: %d ms", mTimeToCommit));
        }
    }

    @Override
    public void onEntryCreated(final VCardEntry vcardEntry) {
        final long start = System.currentTimeMillis();
        mOperationList = vcardEntry.constructInsertOperations(mContentResolver, mOperationList);
        if (mOperationList.size() > MAX_DB_OPERATION_COUNT) {
            mCreatedUris.add(pushIntoContentResolver(mOperationList));
            mOperationList.clear();
        }
        mTimeToCommit += System.currentTimeMillis() - start;
        yieldIfNecessary();
    }

    private void yieldIfNecessary() {
        if ((mTimeToCommit - mLastYieldTime) >= TIME_INTERVAL_FOR_YIELD) {
            try {
                Thread.sleep(YIELD_TIME_IN_MILLIS);
            } catch (InterruptedException ie) {
                Log.w(LOG_TAG, "yieldIfNecessary: interrupted.", ie);
            }
            mLastYieldTime = mTimeToCommit;
        }
    }

    private Uri pushIntoContentResolver(ArrayList<ContentProviderOperation> operationList) {
        if(operationList==null || operationList.size()==0)
            return null;
        try {
            final ContentProviderResult[] results = mContentResolver.applyBatch(
                    ContactsContract.AUTHORITY, operationList);

            // the first result is always the raw_contact. return it's uri so
            // that it can be found later. do null checking for badly behaving
            // ContentResolvers
            return ((results == null || results.length == 0 || results[0] == null)
                            ? null : results[0].uri);
        } catch (RemoteException e) {
            Log.e(LOG_TAG, String.format("%s: %s", e.toString(), e.getMessage()));
            return null;
        } catch (OperationApplicationException e) {
            Log.e(LOG_TAG, String.format("%s: %s", e.toString(), e.getMessage()));
            return null;
        }
    }

    /**
     * Returns the list of created Uris. This list should not be modified by the caller as it is
     * not a clone.
     */
   public ArrayList<Uri> getCreatedUris() {
        return mCreatedUris;
    }
}