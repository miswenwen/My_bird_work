/*
 * Copyright (C) 2011 The Android Open Source Project
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

package com.yunos.alicontacts.detail;

import android.annotation.Nullable;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.SystemProperties;
import android.provider.ContactsContract.CommonDataKinds.Photo;
import android.provider.ContactsContract.RawContacts;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Toast;

import com.yunos.alicontacts.R;
import com.yunos.alicontacts.editor.PhotoActionPopup;
import com.yunos.alicontacts.model.AccountTypeManager;
import com.yunos.alicontacts.model.RawContactDelta;
import com.yunos.alicontacts.model.RawContactDelta.ValuesDelta;
import com.yunos.alicontacts.model.RawContactDeltaList;
import com.yunos.alicontacts.model.RawContactModifier;
import com.yunos.alicontacts.model.account.AccountType;
import com.yunos.alicontacts.util.ContactPhotoUtils;
import com.yunos.alicontacts.util.ContactPortraitDialogManager;
import com.yunos.alicontacts.util.MemoryUtils;

import hwdroid.dialog.DialogInterface;
import hwdroid.dialog.ProgressDialog;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Handles displaying a photo selection popup for a given photo view and dealing with the results
 * that come back.
 */
public abstract class PhotoSelectionHandler implements OnClickListener {

    private static final String TAG = PhotoSelectionHandler.class.getSimpleName();

    /**
     * Minimal time for displaying copy progress dialog.
     * If the progress dialog is displayed too quickly, the user won't see the content clearly.
     * NOTE: 1) overflow without 'L'. 2) in nano seconds. */
    private static final long MIN_PROGRESS_TIME_FOR_COPY_PHOTO = 1L * 1000 * 1000 * 1000;

    private static final String[] SUPPORTED_FILE_EXTENSIONS = {
        ".jpg", ".jpeg", ".png", ".gif", ".bmp", ".tif", ".tiff"
    };
    /** If user select a photo from file manager, then the scheme of uri will be "file". */
    private static final String PHOTO_URI_SCHEME_FILE = "file";

    private static final int REQUEST_CODE_CAMERA_WITH_DATA = 1001;
    private static final int REQUEST_CODE_PHOTO_PICKED_WITH_DATA = 1002;
    private static final int REQUEST_CROP_PHOTO = 1003;

    protected final Context mContext;
    private final int mPhotoPickSize;
    private final Uri mCroppedPhotoUri;
    private final Uri mTempPhotoUri;
    private Uri mOtherTmpPicUri;
    private final RawContactDeltaList mState;
    private final boolean mIsDirectoryContact;
    private ContactPortraitDialogManager mPortraitManager;
    private PhotoCopyThread mCopyThread = null;
    private ProgressDialog mCopyDialog = null;

    public PhotoSelectionHandler(Context context, View photoView,
            boolean isDirectoryContact, RawContactDeltaList state) {
        mContext = context;
        mTempPhotoUri = ContactPhotoUtils.generateTempImageUri(context);
        mCroppedPhotoUri = ContactPhotoUtils.generateTempCroppedImageUri(mContext);
        mIsDirectoryContact = isDirectoryContact;
        mState = state;
        mPhotoPickSize = getMaxDisplayPhotoSize();
    }

    public void cancel() {
        dismissCopyDialog();
        destroyCopyThread(true);
    }

    private synchronized void destroyCopyThread(boolean setQuitFlag) {
        if (mCopyThread != null) {
            if (setQuitFlag) {
                mCopyThread.quit();
            }
            mCopyThread = null;
        }
    }

    public abstract PhotoActionListener getListener();
    public abstract Activity getAttachedActivity();

    @Override
    public void onClick(View v) {
        final PhotoActionListener listener = getListener();
        if (listener != null) {
            if (getWritableEntityIndex() != -1) {
                /*
                mPopup = PhotoActionPopup.createPopupMenu(
                        mContext, mPhotoView, listener, mPhotoMode);
                mPopup.setOnDismissListener(new OnDismissListener() {
                    @Override
                    public void onDismiss() {
                        listener.onPhotoSelectionDismissed();
                    }
                });
                mPopup.show();
                */
                if (mPortraitManager == null) {
                    // mPortraitManager = null;
                    mPortraitManager = new ContactPortraitDialogManager(mContext,
                            mAliPortraitsListener, mOtherPortraitsListener);
                }
                mPortraitManager.show();
            }
        }
    }

    android.widget.AdapterView.OnItemClickListener mAliPortraitsListener = new android.widget.AdapterView.OnItemClickListener() {

        @Override
        public void onItemClick(android.widget.AdapterView<?> parent, View view,
                int pos, long id) {
            final PhotoActionListener listener = getListener();
            Resources res = mContext.getResources();
            int seletedResId = mPortraitManager.getAliPortraitId(pos);
            Bitmap bitmapPersonality = BitmapFactory.decodeResource(res, seletedResId);
            if (pos == 0) {
                // select the default avatar, mark to remove from db.
                listener.onPhotoSelected(bitmapPersonality, null);
            } else {
                ContactPhotoUtils.savePhotoFromBitmapToUri(mContext, bitmapPersonality, mCroppedPhotoUri, false);
                listener.onPhotoSelected(bitmapPersonality, mCroppedPhotoUri);
            }
            mPortraitManager.dismiss();
        }
    };

    DialogInterface.OnClickListener mOtherPortraitsListener = new DialogInterface.OnClickListener() {

        @Override
        public void onClick(DialogInterface dialog, int which) {
            switch (which) {
            case 0:
                // Take a photo by Camera
                takePhotoChosen();
                break;
            case 1:// Pick a pic from Gallery
                pickFromGalleryChosen();
                break;
            default:
                break;
            }
            mPortraitManager.dismiss();
            //mPortraitManager = null;
        }
    };

    /**
     * Attempts to handle the given activity result.  Returns whether this handler was able to
     * process the result successfully.
     * @param requestCode The request code.
     * @param resultCode The result code.
     * @param data The intent that was returned.
     * @return Whether the handler was able to process the result.
     */
    /*public boolean handlePhotoActivityResult(int requestCode, int resultCode, Intent data) {
        final PhotoActionListener listener = getListener();
        if (resultCode == Activity.RESULT_OK) {
            switch (requestCode) {
                // Photo was chosen (either new or existing from gallery), and cropped.
                case REQUEST_CODE_PHOTO_PICKED_WITH_DATA: {
                    Log.i(TAG, "handlePhotoActivityResult -- REQUEST_CODE_PHOTO_PICKED_WITH_DATA, uri = " + data.getData());
                    final String path = ContactPhotoUtils.pathForCroppedPhoto(
                            mContext, listener.getCurrentPhotoFile());
                    Bitmap bitmap = BitmapFactory.decodeFile(path);
                    Log.i(TAG, "handlePhotoActivityResult -- REQUEST_CODE_PHOTO_PICKED_WITH_DATA , bitmap = " + bitmap);
                    listener.onPhotoSelected(bitmap, ContactPhotoUtils.generateTempPhotoFileName());
                    return true;
                }
                // Photo was successfully taken, now crop it.
                case REQUEST_CODE_CAMERA_WITH_DATA: {
                    Log.i(TAG, "handlePhotoActivityResult -- REQUEST_CODE_CAMERA_WITH_DATA ");
                    doCropPhoto(listener.getCurrentPhotoFile());
                    return true;
                }
            }
        }
        return false;
    }
    */
    /**
     * Attempts to handle the given activity result.  Returns whether this handler was able to
     * process the result successfully.
     * @param requestCode The request code.
     * @param resultCode The result code.
     * @param data The intent that was returned.
     * @return Whether the handler was able to process the result.
     */
    public boolean handlePhotoActivityResult(int requestCode, int resultCode, Intent data) {
        final PhotoActionListener listener = getListener();
        if (resultCode == Activity.RESULT_OK) {
            switch (requestCode) {
                // Cropped photo was returned
                case REQUEST_CROP_PHOTO: {
                    final Uri uri;
                    if (data != null && data.getData() != null) {
                        uri = data.getData();
                    } else {
                        uri = mCroppedPhotoUri;
                    }

                    try {
                         //TODO:move to handle???
                        // delete the original temporary photo if it exists
                        mContext.getContentResolver().delete(mTempPhotoUri, null, null);
                        if(mOtherTmpPicUri != null){
                            mContext.getContentResolver().delete(mOtherTmpPicUri, null, null);
                            mOtherTmpPicUri = null;
                        }
                        listener.onPhotoSelected(uri);
                        return true;
                    } catch (FileNotFoundException e) {
                        return false;
                    }
                }

                // Photo was successfully taken or selected from gallery, now crop it.
                case REQUEST_CODE_PHOTO_PICKED_WITH_DATA:
                case REQUEST_CODE_CAMERA_WITH_DATA:
                    final Uri uri;
                    boolean isWritable = false;
                    if (data != null && data.getData() != null) {
                        uri = data.getData();
                        ///M:fix ALPS01258109:if it is drm image,just return the inputUri to CropImage @{
                        Log.i(TAG, "[handlePhotoActivityResult] uri:" + uri);
                        ///@}
                    } else {
                        uri = listener.getCurrentPhotoUri();
                        isWritable = true;
                    }
                    if (!checkPhotoSelected(uri)) {
                        showToastForSelectPhotoFail(R.string.no_photo_file_selected);
                        return false;
                    }
                    // If we pick photo from file manager, we might get any type file.
                    if (!checkSelectedPhotoSupported(uri)) {
                        showToastForSelectPhotoFail(R.string.unsupported_photo_file);
                        return false;
                    }
                    final Uri toCrop;
                    if (isWritable) {
                        // Since this uri belongs to our file provider, we know that it is writable
                        // by us. This means that we don't have to save it into another temporary
                        // location just to be able to crop it.
                        toCrop = uri;
                        doCropPhoto(toCrop, mCroppedPhotoUri);
                        return true;
                    } else {
                        // YUNOS BEGIN
                        // BugID:5871041
                        // Description: can not load picture except .jpg
                        // author:changjun.bcj
                        // date:2015-04-02
                        toCrop = ContactPhotoUtils.getPicUriWithSurffix(mContext, uri, mTempPhotoUri);
                        mOtherTmpPicUri = toCrop;
                        // YUNOS END
                        startCopyPhoto(uri, toCrop);
                        return true;
                    }
            }
        }
        Log.w(TAG, "[handlePhotoActivityResult]the result: " + resultCode + " is not for photo");
        return false;
    }

    /**
     * Check if photo picker activity really returns a photo file.
     * The photo picker will either put a uri in the result intent,
     * or fill photo data into the file pointed by output uri,
     * which is specified in the start intent of picker activity.
     * @param uri The uri to be checked.
     * @return true if a photo is picked.
     */
    private boolean checkPhotoSelected(Uri uri) {
        Log.d(TAG, "checkPhotoSelected: uri="+uri);
        if (uri == null) {
            return false;
        }
        if (!uri.equals(mTempPhotoUri)) {
            // the photo select activity returns an external uri.
            return true;
        }
        File file = getTempPhotoFile(uri);
        if (file.exists() && file.length() > 0) {
            return true;
        }
        return false;
    }

    private File getTempPhotoFile(Uri photoUri) {
        String fileName = mTempPhotoUri.getLastPathSegment();
        String filePath = ContactPhotoUtils.pathForTempPhoto(mContext, fileName);
        File file = new File(filePath);
        return file;
    }

    private boolean checkSelectedPhotoSupported(Uri uri) {
        Log.d(TAG, "checkSelectedPhotoSupported: uri="+uri);
        if (uri == null) {
            return false;
        }
        // The photo comes from 2 ways: one is media db, the other is file system.
        // If the photo comes from file system, we need to check is it is a supported file.
        // If the photo comes from media db, we assume it is supported by default.
        if (!PHOTO_URI_SCHEME_FILE.equals(uri.getScheme())) {
            return true;
        }
        // The uri does not come from media provider.
        // It shall be a file in the storage,
        // we need to check if the file type is supported.
        String path = uri.getPath();
        if (TextUtils.isEmpty(path)) {
            return false;
        }
        path = path.toLowerCase(Locale.US);
        for (String extension : SUPPORTED_FILE_EXTENSIONS) {
            if (path.endsWith(extension)) {
                return true;
            }
        }
        return false;
    }

    private void showToastForSelectPhotoFail(final int textResId) {
        final Activity activity = getAttachedActivity();
        if (activity == null) {
            return;
        }
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(activity, textResId, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void startCopyPhoto(Uri from, Uri to) {
        if (!showCopyDialog()) {
            return;
        }
        destroyCopyThread(true);
        mCopyThread = new PhotoCopyThread(from, to);
        mCopyThread.start();
    }

    private boolean showCopyDialog() {
        dismissCopyDialog();
        Activity activity = getAttachedActivity();
        if ((activity == null) || activity.isFinishing() || activity.isDestroyed()) {
            return false;
        }
        mCopyDialog = new ProgressDialog(activity);
        mCopyDialog.setCancelable(false);
        mCopyDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        mCopyDialog.setMessage(activity.getString(R.string.copy_photo_progress));
        mCopyDialog.show();
        return true;
    }

    private synchronized void dismissCopyDialog() {
        if (mCopyDialog == null) {
            return;
        }
        mCopyDialog.dismiss();
        mCopyDialog = null;
    }

    /*public void setPortraitsPhoto(Bitmap bitmap, String fileName) {
        final PhotoActionListener listener = getListener();
        listener.onPhotoSelected(bitmap, fileName);
    }*/

    /**
     * Return the index of the first entity in the contact data that belongs to a contact-writable
     * account, or -1 if no such entity exists.
     */
    private int getWritableEntityIndex() {
        // Directory entries are non-writable.
        if (mIsDirectoryContact) return -1;
        return mState.indexOfFirstWritableRawContact(mContext);
    }

    /**
     * Return the raw-contact id of the first entity in the contact data that belongs to a
     * contact-writable account, or -1 if no such entity exists.
     */
    protected long getWritableEntityId() {
        int index = getWritableEntityIndex();
        if (index == -1) return -1;
        return mState.get(index).getValues().getId();
    }

    /**
     * Utility method to retrieve the entity delta for attaching the given bitmap to the contact.
     * This will attach the photo to the first contact-writable account that provided data to the
     * contact.  It is the caller's responsibility to apply the delta.
     * @return An entity delta list that can be applied to associate the bitmap with the contact,
     *     or null if the photo could not be parsed or none of the accounts associated with the
     *     contact are writable.
     */
    public RawContactDeltaList getDeltaForAttachingPhotoToContact() {
        // Find the first writable entity.
        int writableEntityIndex = getWritableEntityIndex();
        if (writableEntityIndex != -1) {
            // We are guaranteed to have contact data if we have a writable entity index.
            final RawContactDelta delta = mState.get(writableEntityIndex);

            // Need to find the right account so that EntityModifier knows which fields to add
            final ContentValues entityValues = delta.getValues().getCompleteValues();
            final String type = entityValues.getAsString(RawContacts.ACCOUNT_TYPE);
            final String dataSet = entityValues.getAsString(RawContacts.DATA_SET);
            final AccountType accountType = AccountTypeManager.getInstance(mContext).getAccountType(
                        type, dataSet);

            final ValuesDelta child = RawContactModifier.ensureKindExists(
                    delta, accountType, Photo.CONTENT_ITEM_TYPE);
            child.setFromTemplate(false);
            child.setSuperPrimary(true);

            return mState;
        }
        return null;
    }

    /** Used by subclasses to delegate to their enclosing Activity or Fragment. */
    //protected abstract void startPhotoActivity(Intent intent, int requestCode, String photo);
    protected abstract void startPhotoActivity(Intent intent, int requestCode, Uri photoUri);

    /**
     * Sends a newly acquired photo to Gallery for cropping
     */
    /*private void doCropPhoto(String fileName) {
        try {
            // Obtain the absolute paths for the newly-taken photo, and the destination
            // for the soon-to-be-cropped photo.
            final String newPath = ContactPhotoUtils.pathForNewCameraPhoto(fileName);
            final String croppedPath = ContactPhotoUtils.pathForCroppedPhoto(mContext, fileName);

            // Add the image to the media store
            MediaScannerConnection.scanFile(
                    mContext,
                    new String[] { newPath },
                    new String[] { null },
                    null);

            // Launch gallery to crop the photo
            final Intent intent = getCropImageIntent(newPath, croppedPath);
            startPhotoActivity(intent, REQUEST_CODE_PHOTO_PICKED_WITH_DATA, fileName);
        } catch (Exception e) {
            Log.e(TAG, "Cannot crop image", e);
            Toast.makeText(mContext, R.string.photoPickerNotFoundText, Toast.LENGTH_LONG).show();
        }
    }*/

    /**
     * Sends a newly acquired photo to Gallery for cropping
     */
    private void doCropPhoto(Uri inputUri, Uri outputUri) {
        try {
            dismissCopyDialog();
            // Launch gallery to crop the photo
            final Intent intent = getCropImageIntent(inputUri, outputUri);
            startPhotoActivity(intent, REQUEST_CROP_PHOTO, inputUri);
        } catch (Exception e) {
            Log.e(TAG, "Cannot crop image", e);
            Toast.makeText(mContext, R.string.photoPickerNotFoundText, Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Should initiate an activity to take a photo using the camera.
     * @param photoFile The file path that will be used to store the photo.  This is generally
     *     what should be returned by
     *     {@link PhotoSelectionHandler.PhotoActionListener#getCurrentPhotoFile()}.
     */
    /*private void startTakePhotoActivity(String photoFile) {
        final Intent intent = getTakePhotoIntent(photoFile);
        startPhotoActivity(intent, REQUEST_CODE_CAMERA_WITH_DATA, photoFile);
    }*/


    private void startTakePhotoActivity(Uri photoUri) {
        deleteTempPhotoOutput(photoUri);
        final Intent intent = getTakePhotoIntent(photoUri);
        startPhotoActivity(intent, REQUEST_CODE_CAMERA_WITH_DATA, photoUri);
    }

    /**
     * Should initiate an activity pick a photo from the gallery.
     * @param photoFile The temporary file that the cropped image is written to before being
     *     stored by the content-provider.
     *     {@link PhotoSelectionHandler#handlePhotoActivityResult(int, int, Intent)}.
     */
    private void startPickFromGalleryActivity(Uri photoUri) {
        deleteTempPhotoOutput(photoUri);
        final Intent intent = getPhotoPickIntent(photoUri);
        startPhotoActivity(intent, REQUEST_CODE_PHOTO_PICKED_WITH_DATA, photoUri);
    }

    /**
     * We have to delete the file pointed by output uri for photo picker before start it.
     * When the picker activity returns, we need to know if the user selected a photo
     * in the picker.
     * But some picker (e.g. AliFileBrowser) will return RESULT_OK without pick any photo file,
     * so we have to check the content of output uri to make sure if the user selected a photo.
     * @param photoUri
     */
    private void deleteTempPhotoOutput(Uri photoUri) {
        if (photoUri == null) {
            return;
        }
        File file = getTempPhotoFile(photoUri);
        if (file.exists()) {
            file.delete();
        }
    }

    // Code from ContactsProvider to give photo size without query db.
    private static int sMaxThumbnailDim = -1;
    private static int sMaxDisplayPhotoDim = -1;

    /** Size of a thumbnail */
    public static final int DEFAULT_THUMBNAIL = 96;

    /**
     * Size of a display photo on memory constrained devices (those are devices with less than
     * {@link #DEFAULT_LARGE_RAM_THRESHOLD} of reported RAM
     */
    public static final int DEFAULT_DISPLAY_PHOTO_MEMORY_CONSTRAINED = 480;

    /**
     * Size of a display photo on devices with enough ram (those are devices with at least
     * {@link #DEFAULT_LARGE_RAM_THRESHOLD} of reported RAM
     */
    public static final int DEFAULT_DISPLAY_PHOTO_LARGE_MEMORY = 720;

    /**
     * If the device has less than this amount of RAM, it is considered RAM constrained for
     * photos
     */
    public static final int LARGE_RAM_THRESHOLD = 640 * 1024 * 1024;

    /** If present, overrides the size given in {@link #DEFAULT_THUMBNAIL} */
    public static final String SYS_PROPERTY_THUMBNAIL_SIZE = "contacts.thumbnail_size";

    /** If present, overrides the size determined for the display photo */
    public static final String SYS_PROPERTY_DISPLAY_PHOTO_SIZE = "contacts.display_photo_size";

    /**
     * Returns the maximum size in pixel of a thumbnail (which has a default that can be overriden
     * using a system-property)
     */
    public static int getMaxThumbnailSize() {
        if (sMaxThumbnailDim == -1) {
            sMaxThumbnailDim = SystemProperties.getInt(
                    SYS_PROPERTY_THUMBNAIL_SIZE, DEFAULT_THUMBNAIL);
        }
        return sMaxThumbnailDim;
    }

    /**
     * Returns the maximum size in pixel of a display photo (which is determined based
     * on available RAM or configured using a system-property)
     */
    public static int getMaxDisplayPhotoSize() {
        if (sMaxDisplayPhotoDim == -1) {
            final boolean isExpensiveDevice = MemoryUtils.getTotalMemorySize() >= LARGE_RAM_THRESHOLD;
            sMaxDisplayPhotoDim = SystemProperties.getInt(SYS_PROPERTY_DISPLAY_PHOTO_SIZE,
                    isExpensiveDevice ?
                              DEFAULT_DISPLAY_PHOTO_LARGE_MEMORY
                            : DEFAULT_DISPLAY_PHOTO_MEMORY_CONSTRAINED);
        }
        return sMaxDisplayPhotoDim;
    }
    // Code from ContactsProvider end.

    /**
     * Constructs an intent for picking a photo from Gallery, cropping it and returning the bitmap.
     */
    /*private Intent getPhotoPickIntent(String photoFile) {
        final String croppedPhotoPath = ContactPhotoUtils.pathForCroppedPhoto(mContext, photoFile);
        final Uri croppedPhotoUri = Uri.fromFile(new File(croppedPhotoPath));
        final Intent intent = new Intent(Intent.ACTION_GET_CONTENT, null);
        intent.setType("image/*");
        ContactPhotoUtils.addGalleryIntentExtras(intent, croppedPhotoUri, mPhotoPickSize);
        return intent;
    }*/

    private Intent getPhotoPickIntent(Uri outputUri) {
        final Intent intent = new Intent(Intent.ACTION_GET_CONTENT, null);
        intent.setType("image/*");
        ContactPhotoUtils.addPhotoPickerExtras(intent, outputUri);
        return intent;
    }

    public void pickFromGalleryChosen() {
        try {
            // Launch picker to choose photo for selected contact
            startPickFromGalleryActivity(mTempPhotoUri);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(
                    mContext, R.string.photoPickerNotFoundText, Toast.LENGTH_LONG).show();
        }
    }


    /**
     * Constructs an intent for image cropping.
     */
//    private Intent getCropImageIntent(String inputPhotoPath, String croppedPhotoPath) {
//        final Uri inputPhotoUri = Uri.fromFile(new File(inputPhotoPath));
//        final Uri croppedPhotoUri = Uri.fromFile(new File(croppedPhotoPath));
//        Intent intent = new Intent("com.android.camera.action.CROP");
//        intent.setDataAndType(inputPhotoUri, "image/*");
//        ContactPhotoUtils.addGalleryIntentExtras(intent, croppedPhotoUri, mPhotoPickSize);
//        return intent;
//    }

    /**
     * Constructs an intent for capturing a photo and storing it in a temporary file.
     */
    /*private static Intent getTakePhotoIntent(String fileName) {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE, null);
        final String newPhotoPath = ContactPhotoUtils.pathForNewCameraPhoto(fileName);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(new File(newPhotoPath)));
        return intent;
    }*/

    private Intent getTakePhotoIntent(Uri outputUri) {
        final Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE, null);
        ContactPhotoUtils.addPhotoPickerExtras(intent, outputUri);
        return intent;
    }

    public void takePhotoChosen() {
        try {
            // Launch camera to take photo for selected contact
            startTakePhotoActivity(mTempPhotoUri);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(
                    mContext, R.string.photoPickerNotFoundText, Toast.LENGTH_LONG).show();
        }
    }


    /**
     * Constructs an intent for image cropping.
     */
    private Intent getCropImageIntent(Uri inputUri, Uri outputUri) {
        Intent intent = new Intent("com.android.camera.action.CROP");
        intent.setDataAndType(inputUri, "image/*");
        ContactPhotoUtils.addPhotoPickerExtras(intent, outputUri);
        ContactPhotoUtils.addCropExtras(intent, mPhotoPickSize);
        return intent;
    }

   /* public abstract class PhotoActionListener implements PhotoActionPopup.Listener {
        @Override
        public void onUseAsPrimaryChosen() {
            // No default implementation.
        }

        @Override
        public void onRemovePictureChosen() {
            // No default implementation.
        }

        @Override
        public void onTakePhotoChosen() {
            takePhotoChosen();
        }

        @Override
        public void onPickFromGalleryChosen() {
            pickFromGalleryChosen();
        }

        *//**
         * Called when the user has completed selection of a photo.
         * @param bitmap The selected and cropped photo.
         *//*
        public abstract void onPhotoSelected(Bitmap bitmap);
        public abstract void onPhotoSelected(Bitmap bitmap, String fileName);

        *//**
         * Gets the current photo file that is being interacted with.  It is the activity or
         * fragment's responsibility to maintain this in saved state, since this handler instance
         * will not survive rotation.
         *//*
        public abstract String getCurrentPhotoFile();

        *//**
         * Called when the photo selection dialog is dismissed.
         *//*
        public abstract void onPhotoSelectionDismissed();
    }*/

    public abstract class PhotoActionListener implements PhotoActionPopup.Listener {
        @Override
        public void onUseAsPrimaryChosen() {
            // No default implementation.
        }

        @Override
        public void onRemovePictureChosen() {
            // No default implementation.
        }

        @Override
        public void onTakePhotoChosen() {
            try {
                // Launch camera to take photo for selected contact
                startTakePhotoActivity(mTempPhotoUri);
            } catch (ActivityNotFoundException e) {
                Toast.makeText(
                        mContext, R.string.photoPickerNotFoundText, Toast.LENGTH_LONG).show();
            }
        }

        @Override
        public void onPickFromGalleryChosen() {
            try {
                // Launch picker to choose photo for selected contact
                startPickFromGalleryActivity(mTempPhotoUri);
            } catch (ActivityNotFoundException e) {
                Toast.makeText(
                        mContext, R.string.photoPickerNotFoundText, Toast.LENGTH_LONG).show();
            }
        }

        /**
         * Called when the user has completed selection of a photo.
         * @throws FileNotFoundException
         */
        public abstract void onPhotoSelected(Uri uri) throws FileNotFoundException;

        /**
         * Called when the user has completed selection of a pre-defined avatar icon.
         * @param bitmap
         * @param uri
         */
        public abstract void onPhotoSelected(Bitmap bitmap, @Nullable Uri uri);

        /**
         * Gets the current photo file that is being interacted with.  It is the activity or
         * fragment's responsibility to maintain this in saved state, since this handler instance
         * will not survive rotation.
         */
        public abstract Uri getCurrentPhotoUri();

        /**
         * Called when the photo selection dialog is dismissed.
         */
        public abstract void onPhotoSelectionDismissed();
    }

    private class PhotoCopyThread extends Thread {
        private final Uri mFrom;
        private final Uri mTo;
        private AtomicBoolean mQuit = new AtomicBoolean(false);

        PhotoCopyThread(Uri from, Uri to) {
            mFrom = from;
            mTo = to;
        }

        @Override
        public void run() {
            if (!copyPhoto()) {
                dismissCopyDialog();
                // There are many ways to get here, e.g.
                // 1. to press home key in copying photo.
                // 2. no storage space left to copy photo.
                // We can NOT tell which happens at this time.
                // so the abort toast can only have a common brief message.
                showAbortToast();
                // TODO:
                // The temp copied photo is in cache dir,
                // and will be never used in this case.
                // It will be deleted in next low storage notification.
                // A good solution is to delete the useless file ASAP.
            }
            // The thread quits by executing all code,
            // and the UI thread will check the quit flag to call crop activity.
            // So do not pass true to destroyCopyThread().
            destroyCopyThread(false);
        }

        public void quit() {
            dismissCopyDialog();
            mQuit.set(true);
            interrupt();
        }

        private boolean copyPhoto() {
            // System.currentTimeMillis() might return wrong time
            // if system time is changed during copy.
            long startTime = System.nanoTime();
            boolean result = false;
            try {
                result = ContactPhotoUtils.savePhotoFromUriToUri(mContext, mFrom, mTo, false);
            } catch (SecurityException e) {
                Log.d(TAG, "Did not have read-access to uri : " + mFrom);
            }
            if (!result) {
                return false;
            }
            long timeCost = System.nanoTime() - startTime;
            long timeToWaitInMs = (MIN_PROGRESS_TIME_FOR_COPY_PHOTO - timeCost) / (1000 * 1000);
            if (timeToWaitInMs > 0) {
                try {
                    Thread.sleep(timeToWaitInMs);
                } catch (InterruptedException ie) {
                    // ignore.
                    Log.i(TAG, "PhotoCopyThread: interrupted", ie);
                    if (mQuit.get()) {
                        return false;
                    }
                }
            }
            final Activity activity = getAttachedActivity();
            if (activity == null) {
                return false;
            }
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    // activity destroy shall call dismissCopyDialog().
                    // quit will also call dismissCopyDialog().
                    // so no need to dismissCopyDialog() here.
                    if (activity.isFinishing() || activity.isDestroyed() || mQuit.get()) {
                        return;
                    }
                    doCropPhoto(mTo, mCroppedPhotoUri);
                }
            });
            return true;
        }

        private void showAbortToast() {
            final Activity activity = getAttachedActivity();
            if (activity == null) {
                return;
            }
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(activity, R.string.copy_photo_abort, Toast.LENGTH_SHORT).show();
                }
            });
        }

    }

}
