/*
 * Copyright (C) 2010 The Android Open Source Project
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

package com.yunos.alicontacts.vcard;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.RawContactsEntity;
import android.text.TextUtils;
import android.util.Log;
import android.util.LongSparseArray;

import com.yunos.alicontacts.R;
import com.yunos.alicontacts.aliutil.alivcard.VCardComposer;
import com.yunos.alicontacts.aliutil.alivcard.VCardConfig;

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.HashMap;

/**
 * Class for processing one export request from a user. Dropped after exporting
 * requested Uri(s). {@link VCardService} will create another object when there
 * is another export request.
 */
public class ExportProcessor extends ProcessorBase {
    private static final String LOG_TAG = "ExportProcessor";
    private static final boolean DEBUG = VCardIntentService.DEBUG;

    private final VCardIntentService mService;
    private final ContentResolver mResolver;
    private final NotificationManager mNotificationManager;
    private final ExportRequest mExportRequest;
    private final int mJobId;

    private volatile boolean mCanceled;
    private volatile boolean mDone;
    /*YunOS BEGIN PB*/
    //##module:Contacts##author:xingnuan.cxn@alibaba-inc.com
    //##BugID:(8466294) ##date:2016-7-22 09:00
    //##description:suppot export some contacts to vcard
    private  String mSelectedContactIds ;
    /*YUNOS END PB*/

    public ExportProcessor(VCardIntentService service, String contactIds,ExportRequest exportRequest, int jobId) {
        mService = service;
        mResolver = service.getContentResolver();
        mNotificationManager =
                (NotificationManager) mService.getSystemService(Context.NOTIFICATION_SERVICE);
        mExportRequest = exportRequest;
        mJobId = jobId;
        /*YunOS BEGIN PB*/
        //##module:Contacts##author:xingnuan.cxn@alibaba-inc.com
        //##BugID:(8466294) ##date:2016-7-22 09:00
        //##description:suppot export some contacts to vcard
        mSelectedContactIds = contactIds;
        /*YUNOS END PB*/
    }

    @Override
    public final int getType() {
        return VCardIntentService.TYPE_EXPORT;
    }

    @Override
    public void run() {
        // ExecutorService ignores RuntimeException, so we need to show it here.
        try {
            runInternal();

            if (isCancelled()) {
                doCancelNotification();
            }
            scanMedia();
        } catch (OutOfMemoryError e) {
            Log.e(LOG_TAG, "OutOfMemoryError thrown during import", e);
            throw e;
        } catch (RuntimeException e) {
            Log.e(LOG_TAG, "RuntimeException thrown during export", e);
            throw e;
        } finally {
            synchronized (this) {
                mDone = true;
            }
        }
    }

    /**
     * To make the exported vcf file visible via MTP, e.g. a Windows PC,
     * we have to start a scan.
     */
    private void scanMedia() {
        Intent scanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        scanIntent.setData(mExportRequest.destUri);
        Log.i(LOG_TAG, "scanMedia: send scan broadcast for "+mExportRequest.destUri);
        mService.sendBroadcast(scanIntent);
    }

    private void runInternal() {
        if (DEBUG)
            Log.d(LOG_TAG, String.format("vCard export (id: %d) has started.", mJobId));
        final ExportRequest request = mExportRequest;
        VCardComposer composer = null;
        Writer writer = null;
        boolean successful = false;
        try {
            if (isCancelled()) {
                Log.i(LOG_TAG, "Export request is cancelled before handling the request");
                return;
            }
            final Uri uri = request.destUri;
            final OutputStream outputStream;
            try {
                outputStream = mResolver.openOutputStream(uri);
            } catch (FileNotFoundException e) {
                Log.w(LOG_TAG, "FileNotFoundException thrown", e);
                // Need concise title.

                final String errorReason =
                        mService.getString(R.string.fail_reason_could_not_open_file,
                                uri, e.getMessage());
                doFinishNotification(errorReason, null);
                return;
            }

            final String exportType = request.exportType;
            final int vcardType;
            if (TextUtils.isEmpty(exportType)) {
                vcardType = VCardConfig.getVCardTypeFromString(
                        mService.getString(R.string.config_export_vcard_type));
            } else {
                vcardType = VCardConfig.getVCardTypeFromString(exportType);
            }

            // YunOS does not need phone number format now
            // by using FLAG_REFRAIN_PHONE_NUMBER_FORMATTING flag
            composer = new VCardComposer(mService, vcardType | VCardConfig.FLAG_REFRAIN_PHONE_NUMBER_FORMATTING,
                    true);

            // for test
            // int vcardType = (VCardConfig.VCARD_TYPE_V21_GENERIC |
            // VCardConfig.FLAG_USE_QP_TO_PRIMARY_PROPERTIES);
            // composer = new VCardComposer(ExportVCardActivity.this, vcardType,
            // true);

            writer = new BufferedWriter(new OutputStreamWriter(outputStream));
            final Uri contentUriForRawContactsEntity = RawContactsEntity.CONTENT_URI.buildUpon()
                    .appendQueryParameter(RawContactsEntity.FOR_EXPORT_ONLY, "1").build();
            /*YunOS BEGIN PB*/
            //##module:Contacts##author:xingnuan.cxn@alibaba-inc.com
            //##BugID:(8466294) ##date:2016-7-22 09:00
            //##description:suppot export some contacts to vcard
            composer.initIds(mSelectedContactIds);
            /*YUNOS END PB*/

            String selection = composer.preInitExtend();
            if (!composer.init(Contacts.CONTENT_URI, new String[] { Contacts._ID },
                    selection, null, null, contentUriForRawContactsEntity)) {
                final String errorReason = composer.getErrorReason();
                Log.e(LOG_TAG, "initialization of vCard composer failed: " + errorReason);
                final String translatedErrorReason = translateComposerError(errorReason);
                final String title = mService.getString(R.string.fail_reason_could_not_initialize_exporter,
                        translatedErrorReason);
                doFinishNotification(title, null);
                return;
            }
            final int total = composer.getCount();
            if (total == 0) {
                final String title =
                        mService.getString(R.string.fail_reason_no_exportable_contact);
                doFinishNotification(title, null);
                return;
            }
            int current = 1; // 1-origin
            while (!composer.isAfterLast()) {
                if (isCancelled()) {
                    Log.i(LOG_TAG, "Export request is cancelled during composing vCard");
                    return;
                }
                try {
                    writer.write(composer.createOneEntry());
                } catch (IOException e) {
                    final String errorReason = composer.getErrorReason();
                    Log.e(LOG_TAG, "Failed to read a contact: " + errorReason);
                    final String translatedErrorReason =
                            translateComposerError(errorReason);
                    final String title =
                            mService.getString(R.string.fail_reason_error_occurred_during_export,
                                    translatedErrorReason);
                    doFinishNotification(title, null);
                    return;
                }
                // vCard export is quite fast (compared to import), and frequent
                // notifications
                // bother notification bar too much.
                if (current % 100 == 1) {
                    doProgressNotification(uri, total, current);
                }
                current++;
            }
            Log.i(LOG_TAG, "Successfully finished exporting vCard " + request.destUri);
            if (DEBUG) {
                Log.d(LOG_TAG, "Ask MediaScanner to scan the file: " + request.destUri.getPath());
            }
            successful = true;
            final String filename = uri.getLastPathSegment();
            final String title = mService.getString(R.string.exporting_vcard_finished_title,
                    filename);
            doFinishNotification(title, null);
        } finally {
            if (composer != null) {
                composer.terminate();
            }
            if (writer != null) {
                try {
                    writer.close();
                } catch (IOException e) {
                    Log.w(LOG_TAG, "IOException is thrown during close(). Ignored. " + e);
                }
            }
            mService.handleFinishExportNotification(mJobId, successful);
        }
    }

    private String translateComposerError(String errorMessage) {
        final Resources resources = mService.getResources();
        if (VCardComposer.FAILURE_REASON_FAILED_TO_GET_DATABASE_INFO.equals(errorMessage)) {
            return resources.getString(R.string.composer_failed_to_get_database_infomation);
        } else if (VCardComposer.FAILURE_REASON_NO_ENTRY.equals(errorMessage)) {
            return resources.getString(R.string.composer_has_no_exportable_contact);
        } else if (VCardComposer.FAILURE_REASON_NOT_INITIALIZED.equals(errorMessage)) {
            return resources.getString(R.string.composer_not_initialized);
        } else {
            return errorMessage;
        }
    }

    private void doProgressNotification(Uri uri, int totalCount, int currentCount) {
        final String displayName = uri.getLastPathSegment();
        final String description =
                mService.getString(R.string.exporting_contact_list_message, displayName);
        final String tickerText =
                mService.getString(R.string.exporting_contact_list_title);
        final Notification notification =
                NotificationImportExportListener.constructProgressNotification(mService,
                        VCardIntentService.TYPE_EXPORT, description, tickerText, mJobId, displayName,
                        totalCount, currentCount);
        mNotificationManager.notify(NotificationImportExportListener.DEFAULT_NOTIFICATION_TAG,
                mJobId, notification);
    }

    private void doCancelNotification() {
        if (DEBUG)
            Log.d(LOG_TAG, "send cancel notification");
        final String description = mService.getString(R.string.exporting_vcard_canceled_title,
                mExportRequest.destUri.getLastPathSegment());
        final Notification notification =
                NotificationImportExportListener.constructCancelNotification(mService, description);
        mNotificationManager.notify(NotificationImportExportListener.DEFAULT_NOTIFICATION_TAG,
                mJobId, notification);
    }

    private void doFinishNotification(final String title, final String description) {
        if (DEBUG) {
            Log.d(LOG_TAG, "send finish notification: " + title + ", "
                    + description);
        }
        mNotificationManager.cancel(NotificationImportExportListener.DEFAULT_NOTIFICATION_TAG,
                mJobId);
    }

    @Override
    public synchronized boolean cancel(boolean mayInterruptIfRunning) {
        if (DEBUG)
            Log.d(LOG_TAG, "received cancel request");
        if (mDone || mCanceled) {
            return false;
        }
        mCanceled = true;
        return true;
    }

    @Override
    public synchronized boolean isCancelled() {
        return mCanceled;
    }

    @Override
    public synchronized boolean isDone() {
        return mDone;
    }

    public ExportRequest getRequest() {
        return mExportRequest;
    }

    @Override
    public int getJobID() {
        return mJobId;
    }
}
