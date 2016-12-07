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

import android.app.IntentService;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.util.LongSparseArray;

import com.yunos.alicontacts.dialpad.smartsearch.NameConvertWorker;
import com.yunos.alicontacts.sim.SimContactUtils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * The class responsible for handling vCard import/export requests.
 *
 * This IntentService handles each request
 * one by one, and notifies users when needed.
 */

public class VCardIntentService extends IntentService {
    public VCardIntentService() {
        super(LOG_TAG);
    }

    private final static String LOG_TAG = "VCardIntentService";

    /* package */ final static boolean DEBUG = false;

    /**
     * Specifies the type of operation. Used when constructing a notification, canceling
     * some operation, etc.
     */
    /* package */ static final int TYPE_IMPORT = 1;
    /* package */ static final int TYPE_EXPORT = 2;

    /* package */ static final String CACHE_FILE_PREFIX = "import_tmp_";

    /* package */ final static int VCARD_VERSION_AUTO_DETECT = 0;
    /* package */ final static int VCARD_VERSION_V21 = 1;
    /* package */ final static int VCARD_VERSION_V30 = 2;

    /**
     * Define the intent actions and extras key for import/export intent
     */
    final static String ACTION_IMPORT_VCARD = "action_import_varcd";
    final static String ACTION_EXPORT_VCARD = "action_export_varcd";

    final static String EXTRAS_EXPORT_REQUEST = "extras_export_request";
    final static String EXTRAS_EXPORT_DATA = "extras_export_data";
    final static String EXTRAS_IMPORT_REQUESTS_LIST= "extras_import_requests_list";

    // Should be single thread, as we don't want to simultaneously handle import and export
    // requests.
    // private final ExecutorService mExecutorService = Executors.newSingleThreadExecutor();

    // this service maybe create many times and this values will keep increasing.
    private static int sCurrentJobId = (int) (System.nanoTime() & 0x0FFFFFFF);

    // Stores current running processor as static member. When one processor starts,
    // this member will be set and cleared when processor complete.
    private static ProcessorBase sRunningProcessor;

    /* ** vCard exporter params ** */
    // File names currently reserved by some export job.
    private final Set<String> mReservedDestination = new HashSet<String>();
    /* ** end of vCard exporter params ** */

    private boolean mCanceled;
    private VCardImportExportListener mListener;
    private  String mSelectedContactIds = "";

   @Override
    public void onCreate() {
        super.onCreate();
        if (DEBUG) Log.d(LOG_TAG, "vCard IntentService is being created.");
        mListener = new NotificationImportExportListener(this);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        String action = intent.getAction();

        // handle export/import intent and each request in request list of intent one by one,
        // so only one notification will appear in status bar for current request.
        // cancel command call static method handleCancelRequest directly
        if (ACTION_IMPORT_VCARD.equals(action)){
            NameConvertWorker.pause();
            handleImportIntent(intent);
            NameConvertWorker.resume();
        } else if (ACTION_EXPORT_VCARD.equals(action)) {

            handleExportIntent(intent);

        }
    }

    @Override
    public void onDestroy() {
        if (DEBUG) Log.d(LOG_TAG, "VCardService is being destroyed.");
        cancelAllRequestsAndShutdown();
        clearCache();
        super.onDestroy();
    }

    //
    private void handleImportIntent(Intent intent) {
        Log.v(LOG_TAG,"handleImportIntent() into.");
        if (mCanceled) {
            Log.i(LOG_TAG, "vCard import operation is canceled.");
            return;
        }

        final Bundle bundle = intent.getExtras();
        if (bundle == null) {
            Log.w(LOG_TAG, "Empty import intent bundle. Ignore it.");
            return;
        }

        ArrayList<ImportRequest> requests = bundle
                .getParcelableArrayList(EXTRAS_IMPORT_REQUESTS_LIST);
        if (!requests.isEmpty()) {
            handleImportRequest(requests);
        } else {
            Log.w(LOG_TAG, "Empty import requests. Ignore it.");
        }
    }

    private synchronized void handleImportRequest(List<ImportRequest> requests) {
        if (DEBUG) {
            final ArrayList<String> uris = new ArrayList<String>();
            final ArrayList<String> displayNames = new ArrayList<String>();
            for (ImportRequest request : requests) {
                uris.add(request.uri.toString());
                displayNames.add(request.displayName);
            }
            Log.d(LOG_TAG,
                    String.format("received multiple import request (uri: %s, displayName: %s)",
                            uris.toString(), displayNames.toString()));
        }
        final int size = requests.size();
        for (int i = 0; i < size; i++) {
            if (mCanceled) {
                Log.i(LOG_TAG, "vCard import operation is canceled.");
                return;
            }
            ImportRequest request = requests.get(i);
            ImportProcessor processor = new ImportProcessor(this, mListener, request, sCurrentJobId);
            sRunningProcessor = processor;

            if (mListener != null) {
                mListener.onImportProcessed(request, sCurrentJobId, i);
            }

            sCurrentJobId++;
            processor.run();

            if (!processor.isDone()) {
                mListener.onImportFailed(request);
            }
        }
    }

    private void handleExportIntent(Intent intent) {
        final Bundle extras = intent.getExtras();
        if(extras == null) {
            return;
        }

        final ExportRequest request = (ExportRequest) extras.getParcelable(EXTRAS_EXPORT_REQUEST);
        if(request == null) {
            Log.w(LOG_TAG, "handleExportIntent() : export requestion is null !!!");
            return;
        }
        /*YunOS BEGIN PB*/
        //##module:Contacts##author:xingnuan.cxn@alibaba-inc.com
        //##BugID:(8466294) ##date:2016-7-22 09:00
        //##description:suppot export some contacts to vcard
        mSelectedContactIds = intent.getStringExtra("id");
        handleExportRequest(mSelectedContactIds,request, mListener);
    }

    private synchronized void handleExportRequest(String contactIds,ExportRequest request,
            VCardImportExportListener listener) {
        ExportProcessor processor = new ExportProcessor(this, contactIds,request, sCurrentJobId);
        sRunningProcessor = processor;

        final String path = request.destUri.getEncodedPath();
        if (DEBUG)
            Log.d(LOG_TAG, "Reserve the path " + path);

        if (!mReservedDestination.add(path)) {
            Log.w(LOG_TAG,
                    String.format("The path %s is already reserved. Reject export request", path));
            if (listener != null) {
                listener.onExportFailed(request);
            }
            return;
        }

        if (listener != null) {
            listener.onExportProcessed(request, sCurrentJobId);
        }

        sCurrentJobId++;
        processor.run();

        if (!processor.isDone()) {
            // import failure in processor
            if (listener != null) {
                listener.onExportFailed(request);
            }
        }
    }
    /*YUNOS END PB*/

    static synchronized boolean isProcessorRunning(int jobId) {
        final ProcessorBase processor = sRunningProcessor;
        return (processor != null && processor.getJobID() == jobId);
    }

    static synchronized void handleCancelRequest(CancelRequest request,
            VCardImportExportListener listener) {
        final int jobId = request.jobId;
        if (DEBUG) Log.d(LOG_TAG, String.format("Received cancel request. (id: %d)", jobId));

        final ProcessorBase processor = sRunningProcessor;

        if (processor != null && processor.getJobID() == jobId) {
            processor.cancel(true);
            final int type = processor.getType();
            if (listener != null) {
                listener.onCancelRequest(request, type);
            }
        } else {
            Log.w(LOG_TAG, String.format("Tried to remove unknown job (id: %d)", jobId));
        }
    }

//    public synchronized void handleRequestAvailableExportDestination(final Messenger messenger) {
//        if (DEBUG) Log.d(LOG_TAG, "Received available export destination request.");
//        final String path = getAppropriateDestination(mTargetDirectory);
//        final Message message;
//        if (path != null) {
//            message = Message.obtain(null,
//                    VCardIntentService.MSG_SET_AVAILABLE_EXPORT_DESTINATION, 0, 0, path);
//        } else {
//            message = Message.obtain(null,
//                    VCardIntentService.MSG_SET_AVAILABLE_EXPORT_DESTINATION,
//                    R.id.dialog_fail_to_export_with_reason, 0, mErrorReason);
//        }
//        try {
//            messenger.send(message);
//        } catch (RemoteException e) {
//            Log.w(LOG_TAG, "Failed to send reply for available export destination request.", e);
//        }
//    }

    /* package */ synchronized void handleFinishImportNotification(
            int jobId, boolean successful) {
        if (DEBUG) {
            Log.d(LOG_TAG, String.format("Received vCard import finish notification (id: %d). "
                    + "Result: %b", jobId, (successful ? "success" : "failure")));
        }
        sRunningProcessor = null;
    }

    /* package */ synchronized void handleFinishExportNotification(
            int jobId, boolean successful) {
        if (DEBUG) {
            Log.d(LOG_TAG, String.format("Received vCard export finish notification (id: %d). "
                    + "Result: %b", jobId, (successful ? "success" : "failure")));
        }
        final ProcessorBase job = sRunningProcessor;
        sRunningProcessor = null;
        if (job == null) {
            Log.w(LOG_TAG, String.format("Tried to remove unknown job (id: %d)", jobId));
        } else if (!(job instanceof ExportProcessor)) {
            Log.w(LOG_TAG,
                    String.format("Removed job (id: %s) isn't ExportProcessor", jobId));
        } else {
            final String path = ((ExportProcessor)job).getRequest().destUri.getEncodedPath();
            if (DEBUG) Log.d(LOG_TAG, "Remove reserved path " + path);
            mReservedDestination.remove(path);
        }
    }

    /**
     * Cancels current running processor and set mCanceled flag to true, which
     * means this Service becomes no longer ready for import/export requests.
     *
     * Mainly called from onDestroy().
     */
    private synchronized void cancelAllRequestsAndShutdown() {
        if(sRunningProcessor != null) {
            sRunningProcessor.cancel(true);
            sRunningProcessor = null;
        }
        mCanceled = true; // all pending intents will be ignore
    }

    /**
     * Removes import caches stored locally.
     */
    private void clearCache() {
        for (final String fileName : fileList()) {
            if (fileName.startsWith(CACHE_FILE_PREFIX)) {
                // We don't want to keep all the caches so we remove cache files old enough.
                Log.i(LOG_TAG, "Remove a temporary file: " + fileName);
                deleteFile(fileName);
            }
        }
    }

}
