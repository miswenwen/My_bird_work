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
import android.app.Notification;
import android.app.NotificationManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.PowerManager;
import android.provider.OpenableColumns;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.RelativeSizeSpan;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.common.io.Closeables;
import com.yunos.alicontacts.ContactsListActivity;
import com.yunos.alicontacts.R;
import com.yunos.alicontacts.aliutil.alivcard.VCardEntryCounter;
import com.yunos.alicontacts.aliutil.alivcard.VCardParser;
import com.yunos.alicontacts.aliutil.alivcard.VCardParser_V21;
import com.yunos.alicontacts.aliutil.alivcard.VCardParser_V30;
import com.yunos.alicontacts.aliutil.alivcard.VCardSourceDetector;
import com.yunos.alicontacts.aliutil.alivcard.exception.VCardException;
import com.yunos.alicontacts.aliutil.alivcard.exception.VCardNestedException;
import com.yunos.alicontacts.aliutil.alivcard.exception.VCardVersionException;
import com.yunos.alicontacts.model.AccountTypeManager;
import com.yunos.alicontacts.model.account.AccountWithDataSet;
import com.yunos.alicontacts.util.AccountSelectionUtil;
import com.yunos.common.UsageReporter;

import hwdroid.dialog.AlertDialog;
import hwdroid.dialog.Dialog;
import hwdroid.dialog.DialogInterface;
import hwdroid.dialog.DialogInterface.OnKeyListener;
import hwdroid.dialog.ProgressDialog;
import hwdroid.widget.ItemAdapter;
import hwdroid.widget.item.Item.Type;
import hwdroid.widget.item.Text2Item;
import hwdroid.widget.itemview.Text2ItemView;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Vector;

/**
 * The class letting users to import vCard. This includes the UI part for letting them select
 * an Account and posssibly a file if there's no Uri is given from its caller Activity.
 *
 * Note that this Activity assumes that the instance is a "one-shot Activity", which will be
 * finished (with the method {@link Activity#finish()}) after the import and never reuse
 * any Dialog in the instance. So this code is careless about the management around managed
 * dialogs stuffs (like how onCreateDialog() is used).
 */
public class ImportVCardActivity  extends ContactsListActivity {
    private static final String LOG_TAG = "VCardImport";

    private static final int SELECT_ACCOUNT = 0;

    /* package */ static final String VCARD_URI_ARRAY = "vcard_uri";
    /* package */ static final String ESTIMATED_VCARD_TYPE_ARRAY = "estimated_vcard_type";
    /* package */ static final String ESTIMATED_CHARSET_ARRAY = "estimated_charset";
    /* package */ static final String VCARD_VERSION_ARRAY = "vcard_version";
    /* package */ static final String ENTRY_COUNT_ARRAY = "entry_count";

    /* package */ final static int VCARD_VERSION_AUTO_DETECT = 0;
    /* package */ final static int VCARD_VERSION_V21 = 1;
    /* package */ final static int VCARD_VERSION_V30 = 2;

    /**
     * Notification id used when error happened before sending an import request to VCardServer.
     */
    private static final int FAILURE_NOTIFICATION_ID = 1;

    final static String CACHED_URIS = "cached_uris";

    private AccountSelectionUtil.AccountSelectedListener mAccountSelectionListener;

    private AccountWithDataSet mAccount;

    private ProgressDialog mProgressDialogForScanVCard;
    private ProgressDialog mProgressDialogForCachingVCard;

    private List<VCardFile> mAllVCardFileList;
    private VCardScanThread mVCardScanThread;
    private TextView mImportBtn;

    private VCardCacheThread mVCardCacheThread;
    /* package */ VCardImportExportListener mListener;
    private String mErrorMessage;

    private Handler mHandler = new Handler();

    private static final String QRCODE_URI = "isQRCodeUri";

    private static class VCardFile {
        private final String mName;
        private final String mCanonicalPath;
        private final long mLastModified;

        public VCardFile(String name, String canonicalPath, long lastModified) {
            mName = name;
            mCanonicalPath = canonicalPath;
            mLastModified = lastModified;
        }

        public String getName() {
            return mName;
        }

        public String getCanonicalPath() {
            return mCanonicalPath;
        }

        public long getLastModified() {
            return mLastModified;
        }
    }

    // Runs on the UI thread.
    private class DialogDisplayer implements Runnable {
        private final int mResId;
        public DialogDisplayer(int resId) {
            mResId = resId;
        }
        public DialogDisplayer(String errorMessage) {
            mResId = R.id.dialog_error_with_message;
            mErrorMessage = errorMessage;
        }
        @Override
        public void run() {
            if (!isFinishing()) {
                showDropdownDialog(mResId);
            }
        }
    }

    // Runs on the UI thread.
    private class UpdateListView implements Runnable {

        @Override
        public void run() {
            // TODO Auto-generated method stub
            createListItem();
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
            ImportVCardActivity.this.finish();
        }
    }

    private CancelListener mCancelListener = new CancelListener();

    private OnKeyListener mOnKeyListener = new OnKeyListener() {
        @Override
        public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
            if (keyCode == KeyEvent.KEYCODE_BACK) {
                dialog.dismiss();
                ImportVCardActivity.this.finish();
            }
            return false;
        }
    };

    /**
     * Caches given vCard files into a local directory, and sends actual import request to
     * {@link VCardService}.
     *
     * We need to cache given files into local storage. One of reasons is that some data (as Uri)
     * may have special permissions. Callers may allow only this Activity to access that content,
     * not what this Activity launched (like {@link VCardService}).
     */
    private class VCardCacheThread extends Thread implements DialogInterface.OnCancelListener {
        private boolean mCanceled;
        private PowerManager.WakeLock mWakeLock;
        private VCardParser mVCardParser;
        private final Uri[] mSourceUris; // Given from a caller.
        private final byte[] mSource;
        private final String mDisplayName;
        private final boolean mIsQRCodeUri;

        public VCardCacheThread(final Uri[] sourceUris) {
            this(sourceUris, false);
        }

        public VCardCacheThread(final Uri[] sourceUris, boolean isQRCodeUri) {
            mSourceUris = sourceUris;
            mSource = null;
            final Context context = ImportVCardActivity.this;
            final PowerManager powerManager = (PowerManager) context
                    .getSystemService(Context.POWER_SERVICE);
            mWakeLock = powerManager.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK
                    | PowerManager.ON_AFTER_RELEASE, LOG_TAG);
            mDisplayName = null;

            mIsQRCodeUri = isQRCodeUri;
        }

        @Override
        protected void finalize() {
            if (mWakeLock != null && mWakeLock.isHeld()) {
                Log.w(LOG_TAG, "WakeLock is being held.");
                mWakeLock.release();
            }
        }

        @Override
        public void run() {
            Log.i(LOG_TAG, "vCard cache thread starts running.");
            mWakeLock.acquire();
            try {
                if (mCanceled) {
                    Log.i(LOG_TAG, "vCard cache operation is canceled.");
                    return;
                }
                final Context context = ImportVCardActivity.this;
                // Uris given from caller applications may not be opened twice:
                // consider when
                // it is not from local storage (e.g. "file:///...") but from
                // some special
                // provider (e.g. "content://...").
                // Thus we have to once copy the content of Uri into local
                // storage, and read
                // it after it.
                //
                // We may be able to read content of each vCard file during
                // copying them
                // to local storage, but currently vCard code does not allow us
                // to do so.
                int cache_index = 0;
                ArrayList<ImportRequest> requests = new ArrayList<ImportRequest>();
                if (mSource != null) {
                    try {
                        requests.add(constructImportRequest(mSource, null, mDisplayName));
                    } catch (VCardException e) {
                        Log.e(LOG_TAG, "Maybe the file is in wrong format", e);
                        showFailureNotification(R.string.fail_reason_not_supported);
                        return;
                    }
                } else {
                    final ContentResolver resolver = ImportVCardActivity.this.getContentResolver();

                    for (Uri sourceUri : mSourceUris) {
                        String filename = null;
                        // Note: caches are removed by VCardService.
                        Log.v(LOG_TAG, "handle sourceUri = " + sourceUri.toString());
                        while (true) {
                            filename = VCardIntentService.CACHE_FILE_PREFIX + cache_index + ".vcf";
                            final File file = context.getFileStreamPath(filename);
                            if (!file.exists()) {
                                break;
                            } else {
                                if (cache_index == Integer.MAX_VALUE) {
                                    throw new RuntimeException("Exceeded cache limit");
                                }
                                cache_index++;
                            }
                        }
                        final Uri localDataUri = copyTo(sourceUri, filename);
                        if (mCanceled) {
                            Log.i(LOG_TAG, "vCard cache operation is canceled.");
                            break;
                        }
                        if (localDataUri == null) {
                            Log.w(LOG_TAG, "destUri is null");
                            break;
                        }

                        String displayName = null;
                        Cursor cursor = null;
                        // Try to get a display name from the given Uri. If it
                        // fails, we just
                        // pick up the last part of the Uri.
                        try {
                            cursor = resolver.query(sourceUri, new String[] {
                                OpenableColumns.DISPLAY_NAME
                            }, null, null, null);
                            if (cursor != null && cursor.getCount() > 0 && cursor.moveToFirst()) {
                                if (cursor.getCount() > 1) {
                                    Log.w(LOG_TAG, "Unexpected multiple rows: " + cursor.getCount());
                                }
                                int index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                                if (index >= 0) {
                                    displayName = cursor.getString(index);
                                }
                            }
                        } finally {
                            if (cursor != null) {
                                cursor.close();
                            }
                        }
                        if (TextUtils.isEmpty(displayName)) {
                            displayName = sourceUri.getLastPathSegment();
                        }

                        final ImportRequest request;
                        try {
                            request = constructImportRequest(null, localDataUri, displayName);
                        } catch (VCardException e) {
                            Log.e(LOG_TAG, "Maybe the file is in wrong format", e);
                            showFailureNotification(R.string.fail_reason_not_supported);
                            return;
                        } catch (IOException e) {
                            Log.e(LOG_TAG, "Unexpected IOException", e);
                            showFailureNotification(R.string.fail_reason_io_error);
                            return;
                        }
                        if (mCanceled) {
                            Log.i(LOG_TAG, "vCard cache operation is canceled.");
                            return;
                        }
                        requests.add(request);

//                        localUris.add(localDataUri);
//                        names.add(displayName);
                    }
                }
                // send import intent to VCardIntentService
                Intent intent = new Intent(ImportVCardActivity.this, VCardIntentService.class);
                intent.setAction(VCardIntentService.ACTION_IMPORT_VCARD);
//                intent.putExtra(VCardIntentService.EXTRAS_IMPORT_BYTES_DATA, mSource);
//                intent.putExtra(VCardIntentService.EXTRAS_IMPORT_URIS, localUris);
//                intent.putExtra(VCardIntentService.EXTRAS_IMPORT_ACCOUNT, mAccount);
//                if (names != null) {
//                    // each source uri will has own display name
//                    intent.putExtra(VCardIntentService.EXTRAS_IMPORT_DISPALY_NAMES_LIST, names);
//                } else {
//                    // for bytes data source, using mDisplayName
//                    // but now it does not have value, just reserve it for
//                    // future.
//                    intent.putExtra(VCardIntentService.EXTRAS_IMPORT_DISPALY_NAME, mDisplayName);
//                }
//                intent.putExtra(VCardIntentService.EXTRAS_IMPORT_IS_QRCODE_URI, mIsQRCodeUri);
                intent.putExtra(VCardIntentService.EXTRAS_IMPORT_REQUESTS_LIST, requests);
                startService(intent);
            } catch (OutOfMemoryError e) {
                Log.e(LOG_TAG, "OutOfMemoryError occured during caching vCard");
                runOnUiThread(new DialogDisplayer(
                        getString(R.string.fail_reason_low_memory_during_import)));
            } catch (IOException e) {
                Log.e(LOG_TAG, "IOException during caching vCard", e);
                runOnUiThread(new DialogDisplayer(getString(R.string.fail_reason_io_error)));
            } finally {
                Log.i(LOG_TAG, "Finished caching vCard.");
                mWakeLock.release();
                closeProgressDialogForCachingVCard();
                finish();
            }
        }

        /**
         * Copy the content of sourceUri to the destination.
         */
        private Uri copyTo(final Uri sourceUri, String filename) throws IOException {
            Log.v(LOG_TAG, String.format("Copy a Uri to app local storage (%s -> %s)", sourceUri,
                    filename));
            final ContentResolver resolver = getContentResolver();
            ReadableByteChannel inputChannel = null;
            WritableByteChannel outputChannel = null;
            ByteBuffer buffer = null;
            Uri destUri = null;
            try {
                inputChannel = Channels.newChannel(resolver.openInputStream(sourceUri));
                destUri = Uri.parse(getFileStreamPath(filename).toURI().toString());
                outputChannel = openFileOutput(filename, Context.MODE_PRIVATE).getChannel();
                buffer = ByteBuffer.allocateDirect(8192);
                while (inputChannel.read(buffer) != -1) {
                    if (mCanceled) {
                        Log.d(LOG_TAG, "Canceled during caching " + sourceUri);
                        return null;
                    }
                    buffer.flip();
                    outputChannel.write(buffer);
                    buffer.compact();
                }
                buffer.flip();
                while (buffer.hasRemaining()) {
                    outputChannel.write(buffer);
                }
            } finally {
                if (inputChannel != null) {
                    try {
                        inputChannel.close();
                    } catch (IOException e) {
                        Log.w(LOG_TAG, "Failed to close inputChannel.");
                    }
                }
                if (outputChannel != null) {
                    try {
                        outputChannel.close();
                    } catch (IOException e) {
                        Log.w(LOG_TAG, "Failed to close outputChannel");
                    }
                }

                if (buffer != null) {
                    buffer.clear();
                    buffer = null;
                }
            }
            return destUri;
        }

        /**
         * Reads localDataUri (possibly multiple times) and constructs {@link ImportRequest} from
         * its content.
         *
         * @arg localDataUri Uri actually used for the import. Should be stored in
         * app local storage, as we cannot guarantee other types of Uris can be read
         * multiple times. This variable populates {@link ImportRequest#uri}.
         * @arg displayName Used for displaying information to the user. This variable populates
         * {@link ImportRequest#displayName}.
         */
        private ImportRequest constructImportRequest(final byte[] data,
                final Uri localDataUri, final String displayName)
                throws IOException, VCardException {
            final ContentResolver resolver = ImportVCardActivity.this.getContentResolver();
            VCardEntryCounter counter = null;
            VCardSourceDetector detector = null;
            int vcardVersion = VCARD_VERSION_V21;
            try {
                boolean shouldUseV30 = false;
                InputStream is;
                if (data != null) {
                    is = new ByteArrayInputStream(data);
                } else {
                    is = resolver.openInputStream(localDataUri);
                }
                mVCardParser = new VCardParser_V21();
                try {
                    counter = new VCardEntryCounter();
                    detector = new VCardSourceDetector();
                    mVCardParser.addInterpreter(counter);
                    mVCardParser.addInterpreter(detector);
                    mVCardParser.parse(is);
                } catch (VCardVersionException e1) {
//                    try {
//                        is.close();
//                    } catch (IOException e) {
//                    }
                    Closeables.closeQuietly(is);

                    shouldUseV30 = true;
                    if (data != null) {
                        is = new ByteArrayInputStream(data);
                    } else {
                        is = resolver.openInputStream(localDataUri);
                    }
                    mVCardParser = new VCardParser_V30();
                    try {
                        counter = new VCardEntryCounter();
                        detector = new VCardSourceDetector();
                        mVCardParser.addInterpreter(counter);
                        mVCardParser.addInterpreter(detector);
                        mVCardParser.parse(is);
                    } catch (VCardVersionException e2) {
                        throw new VCardException("vCard with unspported version.");
                    }
                } finally {
//                    if (is != null) {
//                        try {
//                            is.close();
//                        } catch (IOException e) {
//                        }
//                    }
                    Closeables.closeQuietly(is);
                }

                vcardVersion = shouldUseV30 ? VCARD_VERSION_V30 : VCARD_VERSION_V21;
            } catch (VCardNestedException e) {
                Log.w(LOG_TAG, "Nested Exception is found (it may be false-positive).");
                // Go through without throwing the Exception, as we may be able to detect the
                // version before it
            }

            if (mIsQRCodeUri) {
                return new ImportRequest(mAccount,
                        data, localDataUri, displayName,
                        detector.getEstimatedType(),
                        detector.getEstimatedCharset(),
                        vcardVersion, counter.getCount(), true);
            } else {
                return new ImportRequest(mAccount,
                        data, localDataUri, displayName,
                        detector.getEstimatedType(),
                        detector.getEstimatedCharset(),
                        vcardVersion, counter.getCount());
            }

        }

        public void cancel() {
            mCanceled = true;
            if (mVCardParser != null) {
                mVCardParser.cancel();
            }
        }

        @Override
        public void onCancel(DialogInterface dialog) {
            Log.i(LOG_TAG, "Cancel request has come. Abort caching vCard.");
            cancel();
        }
    }

    private class ImportTypeSelectedListener implements
            DialogInterface.OnClickListener {
        public static final int IMPORT_ONE = 0;
        public static final int IMPORT_MULTIPLE = 1;
        public static final int IMPORT_ALL = 2;
        public static final int IMPORT_TYPE_SIZE = 3;

        private int mCurrentIndex;

        @Override
        public void onClick(DialogInterface dialog, int which) {
            if (which == DialogInterface.BUTTON_POSITIVE) {
                switch (mCurrentIndex) {
                case IMPORT_ALL:
                    importVCardFromSDCard(mAllVCardFileList);
                    break;
                case IMPORT_MULTIPLE:
                    showDropdownDialog(R.id.dialog_select_multiple_vcard);
                    break;
                default:
                    showDropdownDialog(R.id.dialog_select_one_vcard);
                    break;
                }
            } else if (which == DialogInterface.BUTTON_NEGATIVE) {
                finish();
            } else {
                mCurrentIndex = which;
            }
        }
    }

    private class VCardSelectedListener implements
            DialogInterface.OnClickListener, DialogInterface.OnMultiChoiceClickListener {
        private int mCurrentIndex;
        private Set<Integer> mSelectedIndexSet;

        public VCardSelectedListener(boolean multipleSelect) {
            mCurrentIndex = 0;
            if (multipleSelect) {
                mSelectedIndexSet = new HashSet<Integer>();
            }
        }

        @Override
        public void onClick(DialogInterface dialog, int which) {
            if (which == DialogInterface.BUTTON_POSITIVE) {
                if (mSelectedIndexSet != null) {
                    List<VCardFile> selectedVCardFileList = new ArrayList<VCardFile>();
                    final int size = mAllVCardFileList.size();
                    // We'd like to sort the files by its index, so we do not use Set iterator.
                    for (int i = 0; i < size; i++) {
                        if (mSelectedIndexSet.contains(i)) {
                            selectedVCardFileList.add(mAllVCardFileList.get(i));
                        }
                    }
                    importVCardFromSDCard(selectedVCardFileList);
                } else {
                    importVCardFromSDCard(mAllVCardFileList.get(mCurrentIndex));
                }
            } else if (which == DialogInterface.BUTTON_NEGATIVE) {
                finish();
            } else {
                // Some file is selected.
                mCurrentIndex = which;
                if (mSelectedIndexSet != null) {
                    if (mSelectedIndexSet.contains(which)) {
                        mSelectedIndexSet.remove(which);
                    } else {
                        mSelectedIndexSet.add(which);
                    }
                }
            }
        }

        @Override
        public void onClick(DialogInterface dialog, int which, boolean isChecked) {
            if (mSelectedIndexSet == null || (mSelectedIndexSet.contains(which) == isChecked)) {
                Log.e(LOG_TAG, String.format("Inconsist state in index %d (%s)", which,
                        mAllVCardFileList.get(which).getCanonicalPath()));
            } else {
                onClick(dialog, which);
            }
        }
    }

    /**
     * Thread scanning VCard from SDCard. After scanning, the dialog which lets a user select
     * a vCard file is shown. After the choice, VCardReadThread starts running.
     */
    private class VCardScanThread
        extends Thread
        implements DialogInterface.OnCancelListener,
                   DialogInterface.OnClickListener {
        private boolean mCanceled;
        private boolean mGotIOException;
        private File[] mRootDirectories;

        // To avoid recursive link.
        private Set<String> mCheckedPaths;
        private PowerManager.WakeLock mWakeLock;

        private class CanceledException extends Exception {
        }

        public VCardScanThread(File[] directories) {
            mCanceled = false;
            mGotIOException = false;
            mRootDirectories = directories;
            mCheckedPaths = new HashSet<String>();
            PowerManager powerManager = (PowerManager)ImportVCardActivity.this.getSystemService(
                    Context.POWER_SERVICE);
            mWakeLock = powerManager.newWakeLock(
                    PowerManager.SCREEN_DIM_WAKE_LOCK |
                    PowerManager.ON_AFTER_RELEASE, LOG_TAG);
        }

        @Override
        public void run() {
            mAllVCardFileList = new Vector<VCardFile>();
            try {
                mWakeLock.acquire();
                for (File dir : mRootDirectories) {
                    getVCardFileRecursively(dir);
                }
            } catch (CanceledException e) {
                mCanceled = true;
            } catch (IOException e) {
                mGotIOException = true;
            } finally {
                mWakeLock.release();
            }

            if (mCanceled) {
                mAllVCardFileList = null;
            }

            mProgressDialogForScanVCard.dismiss();
            mProgressDialogForScanVCard = null;

            if (mGotIOException) {
                runOnUiThread(new DialogDisplayer(R.id.dialog_io_exception));
            } else if (mCanceled) {
                finish();
            } else {
                int size = mAllVCardFileList.size();
                //final Context context = ImportVCardActivity.this;
                if (size == 0) {
                    runOnUiThread(new DialogDisplayer(R.id.dialog_vcard_not_found));
                } else {
                    startVCardSelectAndImport();
                }
            }
        }

        private void getVCardFileRecursively(File directory)
                throws CanceledException, IOException {
            if (mCanceled) {
                throw new CanceledException();
            }

            // e.g. secured directory may return null toward listFiles().
            final File[] files = directory.listFiles();
            if (files == null) {
                Log.w(LOG_TAG, "listFiles() returned null (directory: " + directory + ")");
                return;
            }
            for (File file : directory.listFiles()) {
                if (mCanceled) {
                    throw new CanceledException();
                }
                String canonicalPath = file.getCanonicalPath();
                if (mCheckedPaths.contains(canonicalPath)) {
                    continue;
                }

                mCheckedPaths.add(canonicalPath);

                if (file.isDirectory()) {
                    getVCardFileRecursively(file);
                } else if (canonicalPath.toLowerCase().endsWith(".vcf") &&
                        file.canRead()){
                    String fileName = file.getName();
                    VCardFile vcardFile = new VCardFile(
                            fileName, canonicalPath, file.lastModified());
                    mAllVCardFileList.add(vcardFile);
                }
            }
        }

        @Override
        public void onCancel(DialogInterface dialog) {
            mCanceled = true;
        }

        @Override
        public void onClick(DialogInterface dialog, int which) {
            if (which == DialogInterface.BUTTON_NEGATIVE) {
                mCanceled = true;
            }
        }
    }

    private void startVCardSelectAndImport() {
        /*int size = mAllVCardFileList.size();
        if (getResources().getBoolean(R.bool.config_import_all_vcard_from_sdcard_automatically) ||
                size == 1) {
            importVCardFromSDCard(mAllVCardFileList);
        } else if (getResources().getBoolean(R.bool.config_allow_users_select_all_vcard_import)) {
            runOnUiThread(new DialogDisplayer(R.id.dialog_select_import_type));
        } else {
            runOnUiThread(new DialogDisplayer(R.id.dialog_select_one_vcard));
        }*/
        runOnUiThread(new UpdateListView());
    }

    private void importVCardFromSDCard(final List<VCardFile> selectedVCardFileList) {
        final int size = selectedVCardFileList.size();
        String[] uriStrings = new String[size];
        int i = 0;
        for (VCardFile vcardFile : selectedVCardFileList) {
            uriStrings[i] = "file://" + vcardFile.getCanonicalPath();
            i++;
        }
        importVCard(uriStrings);
    }

    private void importVCardFromSDCard(final VCardFile vcardFile) {
        importVCard(new Uri[] {Uri.parse("file://" + vcardFile.getCanonicalPath())});
    }

    private void importVCard(final Uri uri) {
        importVCard(new Uri[] {uri});
    }

    private void importVCard(final String[] uriStrings) {
        final int length = uriStrings.length;
        final Uri[] uris = new Uri[length];
        for (int i = 0; i < length; i++) {
            uris[i] = Uri.parse(uriStrings[i]);
        }
        importVCard(uris);
    }

    private void importVCard(final Uri[] uris) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (!isFinishing()) {
                    mVCardCacheThread = new VCardCacheThread(uris);
                    //mListener = new NotificationImportExportListener(ImportVCardActivity.this);
                    showDropdownDialog(R.id.dialog_cache_vcard);
                    //mVCardCacheThread.start();
                }
            }
        });
    }

    private Dialog getSelectImportTypeDialog() {
        final DialogInterface.OnClickListener listener = new ImportTypeSelectedListener();
        final AlertDialog.Builder builder = new AlertDialog.Builder(ImportVCardActivity.this)
                .setTitle(R.string.select_vcard_title)
                .setPositiveButton(android.R.string.ok, listener)
                .setOnCancelListener(mCancelListener)
                .setNegativeButton(android.R.string.cancel, mCancelListener);

        final String[] items = new String[ImportTypeSelectedListener.IMPORT_TYPE_SIZE];
        items[ImportTypeSelectedListener.IMPORT_ONE] =
                getString(R.string.import_one_vcard_string);
        items[ImportTypeSelectedListener.IMPORT_MULTIPLE] =
                getString(R.string.import_multiple_vcard_string);
        items[ImportTypeSelectedListener.IMPORT_ALL] =
                getString(R.string.import_all_vcard_string);
        builder.setSingleChoiceItems(items, ImportTypeSelectedListener.IMPORT_ONE, listener);
        return builder.create();
    }

    private Dialog getVCardFileSelectDialog(boolean multipleSelect) {
        final int size = mAllVCardFileList.size();
        final VCardSelectedListener listener = new VCardSelectedListener(multipleSelect);
        final AlertDialog.Builder builder =
                new AlertDialog.Builder(ImportVCardActivity.this)
                        .setTitle(R.string.select_vcard_title)
                        .setPositiveButton(android.R.string.ok, listener)
                        .setOnCancelListener(mCancelListener)
                        .setNegativeButton(android.R.string.cancel, mCancelListener);

        CharSequence[] items = new CharSequence[size];
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        for (int i = 0; i < size; i++) {
            VCardFile vcardFile = mAllVCardFileList.get(i);
            SpannableStringBuilder stringBuilder = new SpannableStringBuilder();
            stringBuilder.append(vcardFile.getName());
            stringBuilder.append('\n');
            int indexToBeSpanned = stringBuilder.length();
            // Smaller date text looks better, since each file name becomes easier to read.
            // The value set to RelativeSizeSpan is arbitrary. You can change it to any other
            // value (but the value bigger than 1.0f would not make nice appearance :)
            stringBuilder.append(
                        "(" + dateFormat.format(new Date(vcardFile.getLastModified())) + ")");
            stringBuilder.setSpan(
                    new RelativeSizeSpan(0.7f), indexToBeSpanned, stringBuilder.length(),
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            items[i] = stringBuilder;
        }
        if (multipleSelect) {
            builder.setMultiChoiceItems(items, (boolean[])null, listener);
        } else {
            builder.setSingleChoiceItems(items, 0, listener);
        }
        return builder.create();
    }

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);

        String accountName = null;
        String accountType = null;
        String dataSet = null;
        final Intent intent = getIntent();
        if (intent != null) {
            accountName = intent.getStringExtra(SelectAccountActivity.ACCOUNT_NAME);
            accountType = intent.getStringExtra(SelectAccountActivity.ACCOUNT_TYPE);
            dataSet = intent.getStringExtra(SelectAccountActivity.DATA_SET);
        } else {
            Log.e(LOG_TAG, "intent does not exist");
        }

        if (!TextUtils.isEmpty(accountName) && !TextUtils.isEmpty(accountType)) {
            mAccount = new AccountWithDataSet(accountName, accountType, dataSet);
        } else {
            final AccountTypeManager accountTypes = AccountTypeManager.getInstance(this);
            final List<AccountWithDataSet> accountList = accountTypes.getAccounts(true);
            if (accountList.isEmpty()) {
                mAccount = null;
            } else if (accountList.size() == 1) {
                mAccount = accountList.get(0);
            } else {
                startActivityForResult(new Intent(this, SelectAccountActivity.class),
                        SELECT_ACCOUNT);
                return;
            }
        }

        if (intent != null) {
            final boolean isQRCodeUri = intent.getBooleanExtra(QRCODE_URI, false);
            final Uri uri = intent.getData();
            if (isQRCodeUri && uri != null) {
                importQRCodeVCard(uri);
                finish();
                return;
            }
        }

        startImport();
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onResume() {
        super.onResume();
        UsageReporter.onResume(this, null);
    }

    @Override
    protected void onPause() {
        // TODO Auto-generated method stub
        super.onPause();
        UsageReporter.onPause(this, null);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if (requestCode == SELECT_ACCOUNT) {
            if (resultCode == RESULT_OK) {
                mAccount = new AccountWithDataSet(
                        intent.getStringExtra(SelectAccountActivity.ACCOUNT_NAME),
                        intent.getStringExtra(SelectAccountActivity.ACCOUNT_TYPE),
                        intent.getStringExtra(SelectAccountActivity.DATA_SET));
                startImport();
            } else {
                if (resultCode != RESULT_CANCELED) {
                    Log.w(LOG_TAG, "Result code was not OK nor CANCELED: " + resultCode);
                }
                finish();
            }
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        closeProgressDialogForCachingVCard();
    }

    /**
     * There are 2 places to call this method at the moment adding the method.
     * The 2 callers are in different threads,
     * and if there is a chance to let them call in concurrent,
     * then one thread might call the null-check,
     * and another thread to call all the method,
     * and the first thread call the remainder.
     * Thus a null pointer will be raised.
     * So we have to put synchronized keyword here.
     */
    private synchronized void closeProgressDialogForCachingVCard() {
        if (mProgressDialogForCachingVCard != null) {
            mProgressDialogForCachingVCard.dismiss();
            mProgressDialogForCachingVCard = null;
        }
    }

    private void startImport() {
        Intent intent = getIntent();
        // Handle inbound files
        Uri uri = intent.getData();
        if (uri != null) {
            Log.i(LOG_TAG, "Starting vCard import using Uri " + uri);
            importVCard(uri);
        } else {
            Log.i(LOG_TAG, "Start vCard without Uri. The user will select vCard manually.");
            doScanExternalStorageAndImportVCard();
        }
    }

    protected void showDropdownDialog(int resId) {
        switch (resId) {
            case R.string.import_from_sdcard: {
                if (mAccountSelectionListener == null) {
                    throw new NullPointerException(
                            "mAccountSelectionListener must not be null.");
                }
                Dialog dialog = AccountSelectionUtil.getSelectAccountDialog(ImportVCardActivity.this, resId,
                        mAccountSelectionListener, mCancelListener);
                dialog.show();
                break;
            }
            case R.id.dialog_searching_vcard: {
                if (mProgressDialogForScanVCard == null) {
                    String message = getString(R.string.searching_vcard_message);
                    mProgressDialogForScanVCard =
                        ProgressDialog.show(ImportVCardActivity.this, null, message, true, false);
                    mProgressDialogForScanVCard.setOnKeyListener(mOnKeyListener);
                    mProgressDialogForScanVCard.setOnCancelListener(mVCardScanThread);
                    mVCardScanThread.start();
                }
                break;
            }
            case R.id.dialog_sdcard_not_found: {
                AlertDialog dialog = new AlertDialog.Builder(ImportVCardActivity.this)
                        .setMessage(R.string.no_sdcard_message)
                        .setPositiveButton(android.R.string.ok,
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        ImportVCardActivity.this.finish();
                                    }
                                }).create();
                dialog.setOnKeyListener(mOnKeyListener);
                dialog.show();
                break;
            }
            case R.id.dialog_vcard_not_found: {
                final String message = getString(R.string.import_failure_no_vcard_file);
                AlertDialog dialog = new AlertDialog.Builder(ImportVCardActivity.this)
                        .setMessage(message)
                        .setOnCancelListener(mCancelListener)
                        .setPositiveButton(android.R.string.ok, mCancelListener).create();
                dialog.setOnKeyListener(mOnKeyListener);
                dialog.show();
                break;
            }
            case R.id.dialog_select_import_type: {
                Dialog dialog = getSelectImportTypeDialog();
                dialog.show();
                break;
            }
            case R.id.dialog_select_multiple_vcard: {
                Dialog dialog = getVCardFileSelectDialog(true);
                dialog.show();
                break;
            }
            case R.id.dialog_select_one_vcard: {
                Dialog dialog = getVCardFileSelectDialog(false);
                dialog.show();
                break;
            }
            case R.id.dialog_cache_vcard: {
                if (mProgressDialogForCachingVCard == null) {
                    final String title = getString(R.string.caching_vcard_title);
                    final String message = getString(R.string.caching_vcard_message);
                    mProgressDialogForCachingVCard = new ProgressDialog(ImportVCardActivity.this);
                    mProgressDialogForCachingVCard.setTitle(title);
                    mProgressDialogForCachingVCard.setMessage(message);
                    mProgressDialogForCachingVCard.setProgressStyle(ProgressDialog.STYLE_SPINNER);
                    mProgressDialogForCachingVCard.setOnCancelListener(mVCardCacheThread);
                    startVCardService();
                }
                mProgressDialogForCachingVCard.show(/*this.getWindow().getDecorView().getRootView()*/);
                break;
            }
            case R.id.dialog_io_exception: {
                String message = (getString(R.string.scanning_sdcard_failed_message,
                        getString(R.string.fail_reason_io_error)));
                AlertDialog.Builder builder = new AlertDialog.Builder(ImportVCardActivity.this)
                    //.setIconAttribute(android.R.attr.alertDialogIcon)
                    .setMessage(message)
                    .setOnCancelListener(mCancelListener)
                    .setPositiveButton(android.R.string.ok, mCancelListener);
                AlertDialog dialog = builder.create();
                dialog.show();
                break;
            }
            case R.id.dialog_error_with_message: {
                String message = mErrorMessage;
                if (TextUtils.isEmpty(message)) {
                    Log.e(LOG_TAG, "Error message is null while it must not.");
                    message = getString(R.string.fail_reason_unknown);
                }
                final AlertDialog.Builder builder = new AlertDialog.Builder(ImportVCardActivity.this)
                    .setTitle(getString(R.string.reading_vcard_failed_title))
                    //.setIconAttribute(android.R.attr.alertDialogIcon)
                    .setMessage(message)
                    .setOnCancelListener(mCancelListener)
                    .setPositiveButton(android.R.string.ok, mCancelListener);
                AlertDialog dialog = builder.create();
                dialog.show();
                break;
            }
            default:
                break;
        }
    }

    /* package */ void startVCardService() {
        mVCardCacheThread.start();
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        if (mProgressDialogForCachingVCard != null) {
            Log.i(LOG_TAG, "Cache thread is still running. Show progress dialog again.");
            showDropdownDialog(R.id.dialog_cache_vcard);
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        // This Activity should finish itself on orientation change, and give the main screen back
        // to the caller Activity.
        finish();
    }

    /**
     * Scans vCard in external storage (typically SDCard) and tries to import it.
     * - When there's no SDCard available, an error dialog is shown.
     * - When multiple vCard files are available, asks a user to select one.
     */
    private void doScanExternalStorageAndImportVCard() {
        ArrayList<File> dirs = new ArrayList<File>();
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)
                || Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
            File dir = Environment.getExternalStorageDirectory();
            if (dir.exists() && dir.isDirectory() && dir.canRead()) {
                dirs.add(dir);
            }
        }

        state = Environment.getExternalStorage2State();
        if (Environment.MEDIA_MOUNTED.equals(state)
                || Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
            File dir = Environment.getExternalStorage2Directory();
            if (dir.exists() && dir.isDirectory() && dir.canRead()) {
                dirs.add(dir);
            }
        }

        if (dirs.isEmpty()) {
            showDropdownDialog(R.id.dialog_sdcard_not_found);
            setListAdapter(null);
        } else {
            File[] searchRoots = new File[dirs.size()];
            dirs.toArray(searchRoots);
            mVCardScanThread = new VCardScanThread(searchRoots);
            showDropdownDialog(R.id.dialog_searching_vcard);
        }
    }

    /* package */ void showFailureNotification(int reasonId) {
        final NotificationManager notificationManager =
                (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
        final Notification notification =
                NotificationImportExportListener.constructImportFailureNotification(
                        ImportVCardActivity.this,
                        getString(reasonId));
        notificationManager.notify(NotificationImportExportListener.FAILURE_NOTIFICATION_TAG,
                FAILURE_NOTIFICATION_ID, notification);
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(ImportVCardActivity.this,
                        getString(R.string.vcard_import_failed), Toast.LENGTH_LONG).show();
            }
        });
    }


    private void createListItem() {
        int size = mAllVCardFileList.size();

        showBackKey(true);

        ItemAdapter adapter = new ItemAdapter(this);
        adapter.setTypeMode(Type.CHECK_MODE);

        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        for (int i = 0; i < size; i++) {
            VCardFile vcardFile = mAllVCardFileList.get(i);
            SpannableStringBuilder stringBuilder = new SpannableStringBuilder();
            stringBuilder.append(vcardFile.getName());
            stringBuilder.append('\n');
            int indexToBeSpanned = stringBuilder.length();
            // Smaller date text looks better, since each file name becomes easier to read.
            // The value set to RelativeSizeSpan is arbitrary. You can change it to any other
            // value (but the value bigger than 1.0f would not make nice appearance :)

            SpannableStringBuilder dateBuilder = new SpannableStringBuilder();
            dateBuilder.append(dateFormat.format(new Date(vcardFile.getLastModified())));
            stringBuilder.setSpan(
                    new RelativeSizeSpan(0.7f), indexToBeSpanned, stringBuilder.length(),
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

            adapter.add(createTextItem(stringBuilder.toString(), dateBuilder.toString()));
        }

        setListAdapter(adapter);

        if(adapter != null && adapter.getCount() > 0) {
            showAllCheckBox(new OnAllCheckedListener() {

                @Override
                public void onAllChecked(boolean checked) {
                    // TODO Auto-generated method stub

                    setAllCheckBoxChecked(checked);
                    for(int i = 0; i< getListAdapter().getCount(); i++) {
                        Text2Item textItem = (Text2Item) ((ItemAdapter) getListAdapter()).getItem(i);
                        textItem.setChecked(checked);
                    }

                    updateTitle(checked ? getListAdapter().getCount() : 0);

                    ((ItemAdapter) getListAdapter()).notifyDataSetChanged();
                    mImportBtn.setEnabled(((ItemAdapter) getListAdapter()).getSelectedCounts() > 0);
//                    setFooterBarButtonEnable(BUT2_ID, ((ItemAdapter) getListAdapter()).getSelectedCounts() > 0);
                }


            });
            setAllCheckBoxEnabled(true);

            View v = LayoutInflater.from(this).inflate(R.layout.import_export_footer_item, getFooterBarImpl());
            mImportBtn = (TextView) v.findViewById(R.id.footer_text_id);
            mImportBtn.setText(R.string.xxsim_importSimEntry);
            mImportBtn.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View v) {
                    importVcard();
                }
            });
            mImportBtn.setEnabled(adapter.getSelectedCounts() > 0);
            getFooterBarImpl().setVisibility(View.VISIBLE);
//            setFooterBarButton(getString(android.R.string.cancel), getString(R.string.xxsim_importSimEntry));
            updateTitle(adapter.getSelectedCounts());
            //mFooterBarButton.setItemTextColor(BUT2_ID, getResources().getColorStateList(R.drawable.footer_delete_selector));
        } else {
            hideAllCheckBox();
        }
    }

    private Text2Item createTextItem(String name, String date) {
        final Text2Item textItem = new Text2Item(name, date);
        return textItem;
    }

    private void updateTitle(int selectedCount) {
        setTitle2(getResources().getString(R.string.multiselect_title,
                selectedCount));
    }

    @Override
    protected void onListItemClick(ListView list, View v, int position, long id) {
        Log.d(LOG_TAG, "onListItemClick() textItem.mCheckBox:"+" postion:"+position);

        if(list != null) {
            Text2Item textItem = (Text2Item) list.getAdapter().getItem(position);
            textItem.setChecked(!textItem.isChecked());

            Text2ItemView itemView = (Text2ItemView) v;
            itemView.setObject(textItem);

            updateTitle(((ItemAdapter)list.getAdapter()).getSelectedCounts());

            setAllCheckBoxChecked(((ItemAdapter)list.getAdapter()).getSelectedCounts() == list.getAdapter().getCount());

            mImportBtn.setEnabled(((ItemAdapter)list.getAdapter()).getSelectedCounts() > 0);
            //setFooterBarButtonEnable(BUT2_ID, ((ItemAdapter)list.getAdapter()).getSelectedCounts() > 0);
        }
    }

    private void importVcard() {
        List<VCardFile> selectedVCardFileList = new ArrayList<VCardFile>();
        final int size = getListView().getAdapter().getCount();
        // We'd like to sort the files by its index, so we do not use Set
        // iterator.
        for (int i = 0; i < size; i++) {
            Text2Item textItem = (Text2Item) getListView().getAdapter().getItem(i);
            if (textItem.isChecked()) {
                selectedVCardFileList.add(mAllVCardFileList.get(i));
            }
        }
        importVCardFromSDCard(selectedVCardFileList);
        UsageReporter.onClick(this, null, UsageReporter.ContactsSettingsPage.SETTING_START_IMPORT
                + UsageReporter.ContactsSettingsPage.SDCARD);
    }

    @Override
    public boolean onHandleFooterBarItemClick(int id) {
        if(id == BUT2_ID) {
            importVcard();
        } else {
            finish();
        }
        return true;
    }

    private void importQRCodeVCard(final Uri uri) {
        Log.d(LOG_TAG, "importQRCodeVCard uri:" + uri);
        Uri[] uris = new Uri[] {uri};
        mVCardCacheThread = new VCardCacheThread(uris, true);
        mListener = null;
        startVCardService();
    }

}
