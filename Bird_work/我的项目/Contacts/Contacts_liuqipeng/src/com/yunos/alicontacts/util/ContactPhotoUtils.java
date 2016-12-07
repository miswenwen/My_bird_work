/*
 * Copyright (C) 2012 The Android Open Source Project
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

package com.yunos.alicontacts.util;

import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;

import com.google.common.io.Closeables;

import yunos.support.v4.content.FileProvider;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Utilities related to loading/saving contact photos.
 */
public class ContactPhotoUtils {
    private static final String TAG = "ContactPhotoUtils";

    private static final String PHOTO_DATE_FORMAT = "'IMG'_yyyyMMdd_HHmmss";
    private static final String JPG_SURFFIX = ".jpg";

    public static final String FILE_PROVIDER_AUTHORITY = "com.yunos.alicontacts.files";

    /**
     * Generate a new, unique file to be used as an out-of-band communication
     * channel, since hi-res Bitmaps are too big to serialize into a Bundle.
     * This file will be passed to other activities (such as the
     * gallery/camera/cropper/etc.), and read by us once they are finished
     * writing it.
     */
    public static File generateTempPhotoFile(Context context) {
        return new File(pathForCroppedPhoto(context,
                generateTempPhotoFileName()));
    }

    public static String pathForCroppedPhoto(Context context, String fileName) {
        final File dir = new File(context.getExternalCacheDir() + "/tmp");
        dir.mkdirs();
        final File f = new File(dir, fileName);
        return f.getAbsolutePath();
    }

    public static Uri generateTempImageUri(Context context) {
        return FileProvider
                .getUriForFile(context, FILE_PROVIDER_AUTHORITY, new File(
                        pathForTempPhoto(context, generateTempPhotoFileName())));
    }

    public static Uri generateTempCroppedImageUri(Context context) {
        return FileProvider.getUriForFile(
                context,
                FILE_PROVIDER_AUTHORITY,
                new File(pathForTempPhoto(context,
                        generateTempCroppedPhotoFileName())));
    }

    public static Uri generateUriForPath(Context context, String absPath) {
        return FileProvider.getUriForFile(context, FILE_PROVIDER_AUTHORITY,
                new File(absPath));
    }

    public static String pathForTempPhoto(Context context, String fileName) {
        final File dir = context.getCacheDir();
        dir.mkdirs();
        final File f = new File(dir, fileName);
        return f.getAbsolutePath();
    }

    private static String generateTempCroppedPhotoFileName() {
        final Date date = new Date(System.currentTimeMillis());
        SimpleDateFormat dateFormat = new SimpleDateFormat(PHOTO_DATE_FORMAT,
                Locale.US);
        return "ContactPhoto-" + dateFormat.format(date) + "-cropped" + JPG_SURFFIX;
    }

    public static Uri getPicUriWithSurffix(Context context, Uri originUri, Uri tmpUri) {
        if (originUri == null) {
            return originUri;
        }
        String surffix = "";
        Uri outUri = tmpUri;
        if (originUri.getScheme().equals("file")) {
            surffix = getExtensionName(originUri.getPath());
        } else {
            Cursor cursor = null;
            try {
                String[] proj = {
                    MediaStore.Images.Media.DATA
                };
                cursor = context.getContentResolver().query(originUri, proj, null, null, null);
                cursor.moveToFirst();
                String path = cursor.getString(0);
                surffix = getExtensionName(path);
            } catch (Exception e) {
                Log.e(TAG, "query file name error!", e);
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }
        }
        if (!JPG_SURFFIX.equals(surffix)) {
            String path = tmpUri.getPath() + surffix;
            outUri = tmpUri.buildUpon().path(path).build();
        }
        return outUri;
    }

    public static String getExtensionName(String filename) {
        if (!TextUtils.isEmpty(filename)) {
            int dot = filename.lastIndexOf('.');
            if (dot != -1) {
                return filename.substring(dot);
            }
        }
        return filename;
    }

    public static String generateTempPhotoFileName() {
        Date date = new Date(System.currentTimeMillis());
        SimpleDateFormat dateFormat = new SimpleDateFormat(PHOTO_DATE_FORMAT);
        return "ContactPhoto-" + dateFormat.format(date) + JPG_SURFFIX;
    }

    /**
     * Given a uri pointing to a bitmap, reads it into a bitmap and returns it.
     *
     * @throws FileNotFoundException
     */
    public static Bitmap getBitmapFromUri(Context context, Uri uri)
            throws FileNotFoundException {
        final InputStream imageStream = context.getContentResolver()
                .openInputStream(uri);
        try {
            return BitmapFactory.decodeStream(imageStream);
        } finally {
            Closeables.closeQuietly(imageStream);
        }
    }

    /**
     * Creates a byte[] containing the PNG-compressed bitmap, or null if
     * something goes wrong.
     */
    public static byte[] compressBitmap(Bitmap bitmap) {
        final int size = bitmap.getWidth() * bitmap.getHeight() * 4;
        final ByteArrayOutputStream out = new ByteArrayOutputStream(size);
        try {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
            out.flush();
            out.close();
            return out.toByteArray();
        } catch (IOException e) {
            Log.w(TAG, "Unable to serialize photo: " + e.toString());
            return null;
        }
    }

    /**
     * Adds common extras to gallery intents.
     *
     * @param intent The intent to add extras to.
     * @param croppedPhotoUri The uri of the file to save the image to.
     * @param photoSize The size of the photo to scale to.
     */
    public static void addGalleryIntentExtras(Intent intent,
            Uri croppedPhotoUri, int photoSize) {
        intent.putExtra("crop", "true");
        intent.putExtra("scale", true);
        intent.putExtra("scaleUpIfNeeded", true);
        intent.putExtra("aspectX", 1);
        intent.putExtra("aspectY", 1);
        intent.putExtra("outputX", photoSize);
        intent.putExtra("outputY", photoSize);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, croppedPhotoUri);
    }

    public static void addCropExtras(Intent intent, int photoSize) {
        intent.putExtra("crop", "true");
        intent.putExtra("scale", true);
        intent.putExtra("scaleUpIfNeeded", true);
        intent.putExtra("aspectX", 1);
        intent.putExtra("aspectY", 1);
        intent.putExtra("outputX", photoSize);
        intent.putExtra("outputY", photoSize);
    }

    /**
     * Adds common extras to gallery intents.
     *
     * @param intent The intent to add extras to.
     * @param photoUri The uri of the file to save the image to.
     */
    public static void addPhotoPickerExtras(Intent intent, Uri photoUri) {
        intent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
        intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                | Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.setClipData(ClipData
                .newRawUri(MediaStore.EXTRA_OUTPUT, photoUri));
    }

    /**
     * Given an input photo stored in a uri, save it to a destination uri
     */
    public static boolean savePhotoFromUriToUri(Context context, Uri inputUri,
            Uri outputUri, boolean deleteAfterSave) {
        FileOutputStream outputStream = null;
        InputStream inputStream = null;
        try {
            outputStream = context.getContentResolver()
                    .openAssetFileDescriptor(outputUri, "rw")
                    .createOutputStream();
            inputStream = context.getContentResolver()
                    .openInputStream(inputUri);
            if (outputStream != null && inputStream != null) {
                final byte[] buffer = new byte[16 * 1024];
                int length;
                while ((length = inputStream.read(buffer)) > 0) {
                    // do NOT call Thread.interrupted(),
                    // it will clear the interrupted flag.
                    if (Thread.currentThread().isInterrupted()) {
                        return false;
                    }
                    outputStream.write(buffer, 0, length);
                }
            }
        } catch (IOException e) {
            Log.e(TAG, "Failed to write photo: " + inputUri.toString()
                    + " because: " + e, e);
            return false;
        } finally {
            Closeables.closeQuietly(inputStream);
            Closeables.closeQuietly(outputStream);

            if (deleteAfterSave) {
                context.getContentResolver().delete(inputUri, null, null);
            }
        }
        return true;
    }

    /**
     * Given an input photo stored in a Bitmap, save it to a destination uri
     */
    public static boolean savePhotoFromBitmapToUri(Context context,
            Bitmap bitmap, Uri outputUri, boolean deleteAfterSave) {
        FileOutputStream outputStream = null;
        InputStream inputStream = null;
        try {
            outputStream = context.getContentResolver()
                    .openAssetFileDescriptor(outputUri, "rw")
                    .createOutputStream();
            inputStream = Bitmap2InputStream(bitmap);
            if (outputStream != null && inputStream != null) {
                final byte[] buffer = new byte[16 * 1024];
                int length;
                int totalLength = 0;
                while ((length = inputStream.read(buffer)) > 0) {
                    outputStream.write(buffer, 0, length);
                    totalLength += length;
                }
            }
            // Log.v(TAG, "Wrote " + totalLength + " bytes for photo " +
            // inputUri.toString());
        } catch (IOException e) {
            Log.e(TAG, "Failed to write photo: " + bitmap.toString()
                    + " because: " + e);
            return false;
        } finally {
            Closeables.closeQuietly(inputStream);
            Closeables.closeQuietly(outputStream);

            if (deleteAfterSave) {
                // context.getContentResolver().delete(inputUri, null, null);
                bitmap.recycle();
            }
        }
        return true;
    }

    // get InputStream from Bitmap
    public static InputStream Bitmap2InputStream(Bitmap bm) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bm.compress(Bitmap.CompressFormat.PNG, 100, baos);
        InputStream is = new ByteArrayInputStream(baos.toByteArray());
        return is;
    }
}
