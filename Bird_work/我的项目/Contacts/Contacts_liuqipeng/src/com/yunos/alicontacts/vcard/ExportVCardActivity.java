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
package com.yunos.alicontacts.vcard;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;
import android.util.LongSparseArray;

import com.yunos.alicontacts.ContactsUtils;
import com.yunos.alicontacts.R;
import com.yunos.alicontacts.sim.SimContactUtils;
import com.yunos.common.UsageReporter;

import hwdroid.dialog.AlertDialog;
import hwdroid.dialog.DialogInterface;

import java.io.File;
import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

/**
 * Shows a dialog confirming the export and asks actual vCard export to {@link VCardService}
 *
 * This Activity first connects to VCardService and ask an available file name and shows it to
 * a user. After the user's confirmation, it send export request with the file name, assuming the
 * file name is not reserved yet.
 */
public class ExportVCardActivity extends Activity implements
        DialogInterface.OnClickListener, DialogInterface.OnCancelListener {
    private static final String LOG_TAG = "VCardExport";
    private static final boolean DEBUG = VCardIntentService.DEBUG;

    private File mTargetDirectory;
    private String mFileNamePrefix;
    private String mFileNameSuffix;
    private int mFileIndexMinimum;
    private int mFileIndexMaximum;
    private String mFileNameExtension;
    private Set<String> mExtensionsToConsider;

    // File names currently reserved by some export job.
    private final Set<String> mReservedDestination = new HashSet<String>();
    /* ** end of vCard exporter params ** */

    // Used temporarily when asking users to confirm the file name
    private String mTargetFileName;

    // String for storing error reason temporarily.
    private String mErrorReason;

    private AlertDialog mCurrentDialog = null;

    private String mStrDirectory;
    private String mSelectedContactIds = "";

    private class ExportConfirmationListener implements DialogInterface.OnClickListener {
        private final Uri mDestinationUri;

        public ExportConfirmationListener(String path) {
            this(Uri.parse("file://" + path));
        }

        public ExportConfirmationListener(Uri uri) {
            mDestinationUri = uri;
        }

        @Override
        public void onClick(DialogInterface dialog, int which) {
            if (which == DialogInterface.BUTTON_POSITIVE) {
                if (DEBUG) {
                    Log.d(LOG_TAG,
                            String.format("Try sending export request (uri: %s)", mDestinationUri));
                }
                UsageReporter.onClick(ExportVCardActivity.this, null,
                        UsageReporter.ContactsSettingsPage.SETTING_START_EXPORT + UsageReporter.ContactsSettingsPage.SDCARD);
                // send export intent to VCardIntentService
                final ExportRequest request = new ExportRequest(mDestinationUri);
                Intent intent = new Intent(ExportVCardActivity.this, VCardIntentService.class);
                intent.setAction(VCardIntentService.ACTION_EXPORT_VCARD);
                /*YunOS BEGIN PB*/
                //##module:Contacts##author:xingnuan.cxn@alibaba-inc.com
                //##BugID:(8466294) ##date:2016-7-22 09:00
                //##description:suppot export some contacts to vcard
                intent.putExtra("id", mSelectedContactIds);
                /*YUNOS END PB*/
                intent.putExtra(VCardIntentService.EXTRAS_EXPORT_REQUEST, request);
                startService(intent);
            }
            finish();
        }
    }

    private void initExporterParams() {
        mTargetDirectory = new File(mStrDirectory);
        mFileNamePrefix = getString(R.string.config_export_file_prefix);
        mFileNameSuffix = getString(R.string.config_export_file_suffix);
        mFileNameExtension = getString(R.string.config_export_file_extension);

        mExtensionsToConsider = new HashSet<String>();
        mExtensionsToConsider.add(mFileNameExtension);

        final String additionalExtensions =
            getString(R.string.config_export_extensions_to_consider);
        if (!TextUtils.isEmpty(additionalExtensions)) {
            for (String extension : additionalExtensions.split(",")) {
                String trimed = extension.trim();
                if (trimed.length() > 0) {
                    mExtensionsToConsider.add(trimed);
                }
            }
        }

        final Resources resources = getResources();
        mFileIndexMinimum = resources.getInteger(R.integer.config_export_file_min_index);
        mFileIndexMaximum = resources.getInteger(R.integer.config_export_file_max_index);
    }

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        
        
        // Check directory is available.
        if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            Log.w(LOG_TAG, "External storage is in state " + Environment.getExternalStorageState() +
                    ". Cancelling export");
            showDropdownDialog(R.id.dialog_sdcard_not_found);
            return;
        }

        final File targetDirectory = Environment.getExternalStorageDirectory();
        if (!(targetDirectory.exists() &&
                targetDirectory.isDirectory() &&
                targetDirectory.canRead()) &&
                !targetDirectory.mkdirs()) {
            showDropdownDialog(R.id.dialog_sdcard_not_found);
            return;
        }
        /*YunOS BEGIN PB*/
        //##module:Contacts##author:xingnuan.cxn@alibaba-inc.com
        //##BugID:(8466294) ##date:2016-7-22 09:00
        //##description:suppot export some contacts to vcard
        Intent intent = getIntent();
        mSelectedContactIds =intent.getStringExtra("id");
        /*YUNOS END PB*/

        String strDirectory = ContactsUtils.getExternalStorageDirectory(ExportVCardActivity.this);
        if (strDirectory == null) {
            mStrDirectory = ContactsUtils.getInternalStorageDirectory(ExportVCardActivity.this);
            startVCardService();
        } else {
            final String[] mItems = {getString(R.string.storage_enternal), getString(R.string.storage_internal)};
            AlertDialog.Builder builder = new AlertDialog.Builder(ExportVCardActivity.this);
            builder.setTitle(getString(R.string.select_option));
            builder.setItems(mItems, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    String importDirectory;
                    if (which == 0) {
                        mStrDirectory = ContactsUtils.getExternalStorageDirectory(ExportVCardActivity.this);
                    } else {
                        mStrDirectory = ContactsUtils.getInternalStorageDirectory(ExportVCardActivity.this);
                    }
                    startVCardService();
                }
            });
            builder.setOnCancelListener(new DialogInterface.OnCancelListener() {
                @Override
                public void onCancel(DialogInterface dialog) {
                    finish();
                }
            });
            mCurrentDialog = builder.create();
            mCurrentDialog.show();
        }
    }

    private void startVCardService() {
        initExporterParams();
        // get vcf destination path
        mTargetFileName = getAppropriateDestination(mTargetDirectory);
        if (TextUtils.isEmpty(mTargetFileName)) {
            Log.w(LOG_TAG, "doesn't get valid path for export vcard");
            showDropdownDialog(R.id.dialog_fail_to_export_with_reason);
        } else {
            if (DEBUG) {
                Log.d(LOG_TAG,
                        String.format("Target file name is set (%s). " +
                                "Show confirmation dialog", mTargetFileName));
            }
            showDropdownDialog(R.id.dialog_export_confirmation);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        UsageReporter.onResume(this, null);

    }

    @Override
    protected void onPause() {
        super.onPause();
        UsageReporter.onPause(this, null);
    }

    protected void showDropdownDialog(int id) {
        switch (id) {
            case R.id.dialog_export_confirmation: {
                int lastSeperatorIndex = mTargetFileName.lastIndexOf("/");
                String message = mTargetFileName;
                if (lastSeperatorIndex > 0) {
                    message = getString(R.string.export_storage)
                            + mTargetFileName.substring(lastSeperatorIndex);
                }

                mCurrentDialog = new AlertDialog.Builder(this)
                        .setTitle(R.string.confirm_export_title)
                        .setMessage(getString(R.string.confirm_export_message, message))
                        .setPositiveButton(android.R.string.ok,
                                new ExportConfirmationListener(mTargetFileName))
                        .setNegativeButton(android.R.string.cancel, this)
                        .setOnCancelListener(this)
                        .create();
                mCurrentDialog.show();
                break;
            }
            case R.id.dialog_fail_to_export_with_reason: {
                mCurrentDialog = new AlertDialog.Builder(this)
                        .setTitle(R.string.exporting_contact_failed_title)
                        .setMessage(getString(R.string.exporting_contact_failed_message,
                                mErrorReason != null ? mErrorReason :
                                        getString(R.string.fail_reason_unknown)))
                        .setPositiveButton(android.R.string.ok, this)
                        .setOnCancelListener(this)
                        .create();
                mCurrentDialog.show();
                break;
            }
            case R.id.dialog_sdcard_not_found: {
                mCurrentDialog = new AlertDialog.Builder(this)
                        .setMessage(R.string.no_sdcard_message)
                        .setCancelable(false)
                        .setPositiveButton(android.R.string.ok,
                                new DialogInterface.OnClickListener() {

                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        finish();
                                    }
                                }).create();
                mCurrentDialog.show();
                break;
            }
            default:
                break;
        }
    }
/*
    @Override
    protected void onPrepareDialog(int id, Dialog dialog, Bundle args) {
        if (id == R.id.dialog_fail_to_export_with_reason) {
            ((AlertDialog)dialog).setMessage(mErrorReason);
        } else if (id == R.id.dialog_export_confirmation) {
            ((AlertDialog)dialog).setMessage(
                    getString(R.string.confirm_export_message, mTargetFileName));
        } else {
            super.onPrepareDialog(id, dialog, args);
        }
    }
*/
    @Override
    protected void onStop() {
        super.onStop();

        if (!isFinishing()) {
            if (mCurrentDialog != null) {
                mCurrentDialog.dismiss();
            }
            finish();
        }
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        if (DEBUG) Log.d(LOG_TAG, "ExportVCardActivity#onClick() is called");
        finish();
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        if (DEBUG) Log.d(LOG_TAG, "ExportVCardActivity#onCancel() is called");
        finish();
    }

//    private synchronized void unbindAndFinish() {
//        if (mConnected) {
//            unbindService(this);
//            mConnected = false;
//        }
//        finish();
//    }

    /**
     * Returns an appropriate file name for vCard export. Returns null when impossible.
     *
     * @return destination path for a vCard file to be exported. null on error and mErrorReason
     * is correctly set.
     */
    private String getAppropriateDestination(final File destDirectory) {
        /*
         * Here, file names have 5 parts: directory, prefix, index, suffix, and extension.
         * e.g. "/mnt/sdcard/prfx00001sfx.vcf" -> "/mnt/sdcard", "prfx", "00001", "sfx", and ".vcf"
         *      (In default, prefix and suffix is empty, so usually the destination would be
         *       /mnt/sdcard/00001.vcf.)
         *
         * This method increments "index" part from 1 to maximum, and checks whether any file name
         * following naming rule is available. If there's no file named /mnt/sdcard/00001.vcf, the
         * name will be returned to a caller. If there are 00001.vcf 00002.vcf, 00003.vcf is
         * returned.
         *
         * There may not be any appropriate file name. If there are 99999 vCard files in the
         * storage, for example, there's no appropriate name, so this method returns
         * null.
         */

        // Count the number of digits of mFileIndexMaximum
        // e.g. When mFileIndexMaximum is 99999, fileIndexDigit becomes 5, as we will count the
        int fileIndexDigit = 0;
        {
            // Calling Math.Log10() is costly.
            int tmp;
            for (fileIndexDigit = 0, tmp = mFileIndexMaximum; tmp > 0;
                fileIndexDigit++, tmp /= 10) {
            }
        }

        // %s05d%s (e.g. "p00001s")
        final String bodyFormat = getString(R.string.people) + "%s%0" + fileIndexDigit + "d%s";

        for (int i = mFileIndexMinimum; i <= mFileIndexMaximum; i++) {
            boolean numberIsAvailable = true;
            final String body = String.format(bodyFormat, mFileNamePrefix, i, mFileNameSuffix);
            // Make sure that none of the extensions of mExtensionsToConsider matches. If this
            // number is free, we'll go ahead with mFileNameExtension (which is included in
            // mExtensionsToConsider)
            for (String possibleExtension : mExtensionsToConsider) {
                final File file = new File(destDirectory, body + "." + possibleExtension);
                final String path = file.getAbsolutePath();
                synchronized (this) {
                    // Is this being exported right now? Skip this number
                    if (mReservedDestination.contains(path)) {
                        if (DEBUG) {
                            Log.d(LOG_TAG, String.format("%s is already being exported.", path));
                        }
                        numberIsAvailable = false;
                        break;
                    }
                }
                if (file.exists()) {
                    numberIsAvailable = false;
                    break;
                }
            }
            if (numberIsAvailable) {
                return new File(destDirectory, body + "." + mFileNameExtension).getAbsolutePath();
            }
        }

        Log.w(LOG_TAG, "Reached vCard number limit. Maybe there are too many vCard in the storage");
        mErrorReason = getString(R.string.fail_reason_too_many_vcard);
        return null;
    }
}
