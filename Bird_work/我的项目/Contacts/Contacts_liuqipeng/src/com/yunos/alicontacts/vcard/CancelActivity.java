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

import android.app.Activity;
import android.app.NotificationManager;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

import com.yunos.alicontacts.R;

import hwdroid.dialog.AlertDialog;
import hwdroid.dialog.DialogInterface;

/**
 * The Activity for canceling vCard import/export.
 */
public class CancelActivity extends Activity {
    private final static String LOG_TAG = "VCardCancel";

    /* package */ final static String JOB_ID = "job_id";
    /* package */ final static String DISPLAY_NAME = "display_name";

    private static final int DIALOG_CANCEL_CONFIRMATION = 1;
    private static final int DIALOG_CANCEL_JOB_NOT_FOUND = 2;

    /**
     * Type of the process to be canceled. Only used for choosing appropriate title/message.
     * Must be {@link VCardService#TYPE_IMPORT} or {@link VCardService#TYPE_EXPORT}.
     */
    /* package */ final static String TYPE = "type";

    private class RequestCancelListener implements DialogInterface.OnClickListener {
        @Override
        public void onClick(DialogInterface dialog, int which) {
            final CancelRequest request = new CancelRequest(mJobId, mDisplayName);
            VCardIntentService.handleCancelRequest(request, null);
            finish();
        }
    }

    private class CancelListener
            implements DialogInterface.OnClickListener, DialogInterface.OnCancelListener {
        @Override
        public void onClick(DialogInterface dialog, int which) {
            finish();
        }
        @Override
        public void onCancel(DialogInterface dialog) {
            finish();
        }
    }

    private class DismissListener
            implements DialogInterface.OnClickListener, DialogInterface.OnCancelListener {
        @Override
        public void onClick(DialogInterface dialog, int which) {
            dismiss();
        }
        @Override
        public void onCancel(DialogInterface dialog) {
            dismiss();
        }
        private void dismiss() {
            NotificationManager nm = (NotificationManager) getSystemService(
                    Context.NOTIFICATION_SERVICE);
            nm.cancel(NotificationImportExportListener.DEFAULT_NOTIFICATION_TAG, mJobId);
            finish();
        }
    }

    private final CancelListener mCancelListener = new CancelListener();
    private int mJobId;
    private String mDisplayName;
    private int mType;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final Uri uri = getIntent().getData();
        mJobId = Integer.parseInt(uri.getQueryParameter(JOB_ID));
        mDisplayName = uri.getQueryParameter(DISPLAY_NAME);
        mType = Integer.parseInt(uri.getQueryParameter(TYPE));
        if (VCardIntentService.isProcessorRunning(mJobId)) {
            showDropDownDialog(DIALOG_CANCEL_CONFIRMATION);
        } else {
            showDropDownDialog(DIALOG_CANCEL_JOB_NOT_FOUND);
        }
    }

    protected void showDropDownDialog(int id) {
        switch (id) {
        case DIALOG_CANCEL_CONFIRMATION: {
            final String message;
            if (mType == VCardIntentService.TYPE_IMPORT) {
                message = getString(R.string.cancel_import_confirmation_message, mDisplayName);
            } else {
                message = getString(R.string.cancel_export_confirmation_message, mDisplayName);
            }
            final AlertDialog builder = new AlertDialog.Builder(this)
                    .setMessage(message)
                    .setPositiveButton(android.R.string.ok, new RequestCancelListener())
                    .setOnCancelListener(mCancelListener)
                    .setNegativeButton(android.R.string.cancel, mCancelListener).create();
            builder.show(/*this.getWindow().getDecorView().getRootView()*/);
        }
            break;
        case DIALOG_CANCEL_JOB_NOT_FOUND:
            final AlertDialog builder = new AlertDialog.Builder(this)
                    .setTitle(R.string.cancel_vcard_import_or_export_failed)
                    //.setIconAttribute(android.R.attr.alertDialogIcon)
                    .setMessage(getString(R.string.fail_reason_job_not_found))
                    .setOnCancelListener(mCancelListener)
                    .setPositiveButton(android.R.string.ok, new DismissListener()).create();
            builder.show(/*this.getWindow().getDecorView().getRootView()*/);
            break;
        default:
            Log.w(LOG_TAG, "Unknown dialog id: " + id);
            break;
        }
    }
}
