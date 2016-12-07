package com.yunos.alicontacts.sim;

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
 * The Activity for canceling sim card import/export.
 */
public class CancelActivity extends Activity {
    private final static String LOG_TAG = "SimImportExportCancel";

    /* package */ final static String JOB_ID = "job_id";
    /* package */ final static String SLOT = "slot";
    /* package */ final static String TYPE = "type";

    private static final int DIALOG_CANCEL_CONFIRMATION = 1;
    private static final int DIALOG_CANCEL_JOB_NOT_FOUND = 2;

    private class ConfirmDialogListener
            implements DialogInterface.OnClickListener, DialogInterface.OnCancelListener {
        @Override
        public void onClick(DialogInterface dialog, int which) {
            if (AlertDialog.BUTTON_POSITIVE == which) {
                SimContactListFragment.cancelJob(mJobId);
                cancel();
            }
            finish();
        }

        @Override
        public void onCancel(DialogInterface dialog) {
            finish();
        }

        private void cancel() {
            NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            nm.cancel(SimNotificationListener.SIM_NOTIFICATION_TAG, mJobId);
        }

    }

    private class JobNotFoundDialogListener
            implements DialogInterface.OnClickListener, DialogInterface.OnCancelListener {
        @Override
        public void onClick(DialogInterface dialog, int which) {
            cancelAndFinish();
        }

        @Override
        public void onCancel(DialogInterface dialog) {
            cancelAndFinish();
        }

        private void cancelAndFinish() {
            NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            nm.cancel(SimNotificationListener.SIM_NOTIFICATION_TAG, mJobId);
            finish();
        }
    }

    private int mJobId;
    private int mSlot;
    private int mType;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            final Uri uri = getIntent().getData();
            mJobId = Integer.parseInt(uri.getQueryParameter(JOB_ID));
            mSlot = Integer.parseInt(uri.getQueryParameter(SLOT));
            mType = Integer.parseInt(uri.getQueryParameter(TYPE));
        } catch (NumberFormatException nfe) {
            // maybe attack.
            Log.w(LOG_TAG, "onCreate: Got NumberFormatException.", nfe);
            finish();
            return;
        }

        if ((mJobId < 0)
                || ((mType != SimContactListFragment.ACTION_MODE_IMPORT_CONTACTS)
                        && (mType != SimContactListFragment.ACTION_MODE_EXPORT_CONTACTS))) {
            Log.w(LOG_TAG, "onCreate: invalid job id "+mJobId+"; or invalid type "+mType+". quit.");
            finish();
            return;
        }
        if (SimContactListFragment.isJobRunning(mJobId)) {
            showDropDownDialog(DIALOG_CANCEL_CONFIRMATION);
        } else {
            showDropDownDialog(DIALOG_CANCEL_JOB_NOT_FOUND);
        }
    }

    protected void showDropDownDialog(int id) {
        final String simName = SimUtil.getSimCardDisplayName(this, mSlot);
        switch (id) {
        case DIALOG_CANCEL_CONFIRMATION: {
            final String message;
            if (mType == SimContactListFragment.ACTION_MODE_IMPORT_CONTACTS) {
                message = getString(R.string.sim_import_cancel_confirm, simName);
            } else {
                message = getString(R.string.sim_export_cancel_confirm, simName);
            }
            final ConfirmDialogListener listener = new ConfirmDialogListener();
            final AlertDialog builder = new AlertDialog.Builder(this)
                    .setMessage(message)
                    .setPositiveButton(android.R.string.ok, listener)
                    .setOnCancelListener(listener)
                    .setNegativeButton(android.R.string.cancel, listener).create();
            builder.show(/*this.getWindow().getDecorView().getRootView()*/);
        }
            break;
        case DIALOG_CANCEL_JOB_NOT_FOUND: {
            final JobNotFoundDialogListener listener = new JobNotFoundDialogListener();
            final AlertDialog builder = new AlertDialog.Builder(this)
                    .setTitle(R.string.sim_import_or_export_failed)
                    .setMessage(getString(R.string.fail_reason_job_not_found))
                    .setOnCancelListener(listener)
                    .setPositiveButton(android.R.string.ok, listener).create();
            builder.show(/*this.getWindow().getDecorView().getRootView()*/);
        }
            break;
        default:
            Log.w(LOG_TAG, "Unknown dialog id: " + id);
            break;
        }
    }
}
