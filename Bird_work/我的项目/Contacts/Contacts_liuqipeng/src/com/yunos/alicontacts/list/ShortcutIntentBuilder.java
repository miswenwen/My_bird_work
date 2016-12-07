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
package com.yunos.alicontacts.list;

import android.app.ActivityManager;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PorterDuff.Mode;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.CommonDataKinds.Photo;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.Data;
import android.text.TextUtils;
import android.util.Log;

import com.yunos.alicontacts.R;
import com.yunos.alicontacts.util.Constants;

import java.io.IOException;
import java.io.InputStream;

/**
 * Constructs shortcut intents.
 */
public class ShortcutIntentBuilder {

    private static final float MAX_SHORT_CUT_BITMAP_SIZE = 180f; //This is xxhdpi default icon size.
    private static final float PREFER_PHOTO_SIZE_IN_DP = 56.0f;

    private static final String TAG = "ShortcutIntentBuilder";
    public static final String NEED_TOAST_WHEN＿CONTACT_NOT_FOUND = "NeedToast";
    private static final String[] CONTACT_COLUMNS = {
        Contacts.DISPLAY_NAME,
        Contacts.PHOTO_ID,
    };

    private static final int CONTACT_DISPLAY_NAME_COLUMN_INDEX = 0;
    private static final int CONTACT_PHOTO_ID_COLUMN_INDEX = 1;

    private static final String[] PHONE_COLUMNS = {
        Phone.DISPLAY_NAME,
        Phone.PHOTO_ID,
        Phone.NUMBER,
        Phone.TYPE,
        Phone.LABEL
    };

    private static final int PHONE_DISPLAY_NAME_COLUMN_INDEX = 0;
    private static final int PHONE_PHOTO_ID_COLUMN_INDEX = 1;
    private static final int PHONE_NUMBER_COLUMN_INDEX = 2;
    private static final int PHONE_TYPE_COLUMN_INDEX = 3;
    private static final int PHONE_LABEL_COLUMN_INDEX = 4;

    private static final String[] PHOTO_COLUMNS = {
        Photo.PHOTO,
    };

    private static final String PHOTO_SELECTION = Photo._ID + "=?";

    private final OnShortcutIntentCreatedListener mListener;
    private final Context mContext;
    private int mIconSize;
    private final int mIconDensity;
    private boolean mIsAliContacts;
    /**
     * This is a hidden API of the launcher in JellyBean that allows us to disable the animation
     * that it would usually do, because it interferes with our own animation for QuickContact
     */
    public static final String INTENT_EXTRA_IGNORE_LAUNCH_ANIMATION =
            "com.android.launcher.intent.extra.shortcut.INGORE_LAUNCH_ANIMATION";

    /**
     * Listener interface.
     */
    public interface OnShortcutIntentCreatedListener {

        /**
         * Callback for shortcut intent creation.
         *
         * @param uri the original URI for which the shortcut intent has been
         *            created.
         * @param shortcutIntent resulting shortcut intent.
         */
        void onShortcutIntentCreated(Uri uri, Intent shortcutIntent);
    }

    public ShortcutIntentBuilder(Context context, OnShortcutIntentCreatedListener listener) {
        mContext = context;
        mListener = listener;

        final Resources r = context.getResources();
        final ActivityManager am = (ActivityManager) context
                .getSystemService(Context.ACTIVITY_SERVICE);
        mIconSize = r.getDimensionPixelSize(R.dimen.shortcut_icon_size);
        if (mIconSize == 0) {
            mIconSize = am.getLauncherLargeIconSize();
        }
        mIconDensity = am.getLauncherLargeIconDensity();
    }

    public void createContactShortcutIntent(Uri contactUri, boolean aliContacts) {
        mIsAliContacts = aliContacts;
        createContactShortcutIntent(contactUri);
    }

    public void createContactShortcutIntent(Uri contactUri) {
        new ContactLoadingAsyncTaskForShortcut(contactUri).execute();
    }

    public void createPhoneNumberShortcutIntent(Uri dataUri, String shortcutAction) {
        new PhoneNumberLoadingAsyncTask(dataUri, shortcutAction).execute();
    }

    /**
     * An asynchronous task that loads name, photo and other data from the database.
     */
    private abstract class LoadingAsyncTask extends AsyncTask<Void, Void, Void> {
        protected Uri mUri;
        protected String mContentType;
        protected String mDisplayName;
        protected Bitmap mBitmap;
        protected long mPhotoId;

        public LoadingAsyncTask(Uri uri) {
            mUri = uri;
        }

        @Override
        protected Void doInBackground(Void... params) {
            mContentType = mContext.getContentResolver().getType(mUri);
            loadData();
            loadPhoto();
            return null;
        }

        protected abstract void loadData();

        private void loadPhoto() {
            ContentResolver resolver = mContext.getContentResolver();
            // 1. load from high resolution photo.
            mBitmap = loadContactPhotoByStream(resolver);
            // 2. load from thumbnail.
            if (mBitmap == null) {
                mBitmap = loadContactPhotoByBlob(resolver);
            }
            // 3. use default photo.
            if (mBitmap == null) {
                mBitmap = ((BitmapDrawable) mContext.getResources().getDrawableForDensity(
                        R.drawable.aui_ic_contacts_default, mIconDensity)).getBitmap();
            }
        }

        private Bitmap loadContactPhotoByStream(ContentResolver resolver) {
            String idStr = mUri.getLastPathSegment();
            long contactId = -1;
            try {
                contactId = Long.parseLong(idStr);
            } catch (NumberFormatException nfe) {
                // on invalid uri, use default photo
                Log.e(TAG, "LoadingAsyncTask.loadContactPhontoByStream: invalid id from uri ["+mUri+"].");
                return null;
            }
            Uri uri = ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, contactId);
            InputStream input = null;
            Bitmap photo = null;
            try {
                input = ContactsContract.Contacts.openContactPhotoInputStream(resolver, uri, true);
                photo = BitmapFactory.decodeStream(input);
            } finally {
                if (input != null) {
                    try {
                        input.close();
                    } catch (IOException e) {
                        // ignore;
                    }
                }
            }
            return photo;
        }

        private Bitmap loadContactPhotoByBlob(ContentResolver resolver) {
            if (mPhotoId == 0) {
                return null;
            }
            Cursor cursor = null;
            byte[] bitmapData = null;
            try {
                cursor = resolver.query(Data.CONTENT_URI, PHOTO_COLUMNS, PHOTO_SELECTION,
                        new String[] { String.valueOf(mPhotoId) }, null);
                if (cursor != null && cursor.moveToFirst()) {
                    bitmapData = cursor.getBlob(0);
                }
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }
            if (bitmapData != null) {
                return BitmapFactory.decodeByteArray(bitmapData, 0, bitmapData.length, null);
            }
            return null;
        }

    }

    private final class ContactLoadingAsyncTaskForShortcut extends LoadingAsyncTask {
        public ContactLoadingAsyncTaskForShortcut(Uri uri) {
            super(uri);
        }

        @Override
        protected Void doInBackground(Void... params) {
            if (!validateUri()) {
                Log.w(TAG, "ContactLoadingAsyncTaskForShortcut.doInBackground: invalid uri "+mUri+". quit.");
                return null;
            }
            return super.doInBackground(params);
        }

        @Override
        protected void loadData() {
            ContentResolver resolver = mContext.getContentResolver();
            Cursor cursor = resolver.query(mUri, CONTACT_COLUMNS, null, null, null);
            if (cursor != null) {
                try {
                    if (cursor.moveToFirst()) {
                        mDisplayName = cursor.getString(CONTACT_DISPLAY_NAME_COLUMN_INDEX);
                        mPhotoId = cursor.getLong(CONTACT_PHOTO_ID_COLUMN_INDEX);
                    }
                } finally {
                    cursor.close();
                }
            }
        }
        @Override
        protected void onPostExecute(Void result) {
            createContactShortcutIntent(mUri, mContentType, mDisplayName, mBitmap);
        }

        private boolean validateUri() {
            if (mUri == null) {
                return false;
            }
            // sometimes, other app will start ContactDetailActivity and pass a non-lookup uri.
            // In this case, we need to convert the uri to lookup uri.
            // otherwise, we can install more than one shortcut for this contact in homeshell.
            if (!mUri.toString().startsWith(Constants.LOOKUP_URI_PREFIX)) {
                Uri lookupUri = Contacts.getLookupUri(mContext.getContentResolver(), mUri);
                if (lookupUri != null) {
                    mUri = lookupUri;
                }
            }
            return true;
        }
    }

    private final class PhoneNumberLoadingAsyncTask extends LoadingAsyncTask {
        private final String mShortcutAction;
        private String mPhoneNumber;
        private int mPhoneType;
        private String mPhoneLabel;

        public PhoneNumberLoadingAsyncTask(Uri uri, String shortcutAction) {
            super(uri);
            mShortcutAction = shortcutAction;
        }

        @Override
        protected void loadData() {
            ContentResolver resolver = mContext.getContentResolver();
            Cursor cursor = resolver.query(mUri, PHONE_COLUMNS, null, null, null);
            if (cursor != null) {
                try {
                    if (cursor.moveToFirst()) {
                        mDisplayName = cursor.getString(PHONE_DISPLAY_NAME_COLUMN_INDEX);
                        mPhotoId = cursor.getLong(PHONE_PHOTO_ID_COLUMN_INDEX);
                        mPhoneNumber = cursor.getString(PHONE_NUMBER_COLUMN_INDEX);
                        mPhoneType = cursor.getInt(PHONE_TYPE_COLUMN_INDEX);
                        mPhoneLabel = cursor.getString(PHONE_LABEL_COLUMN_INDEX);
                    }
                } finally {
                    cursor.close();
                }
            }
        }

        @Override
        protected void onPostExecute(Void result) {
            createPhoneNumberShortcutIntent(mUri, mDisplayName, mBitmap, mPhoneNumber,
                    mPhoneType, mPhoneLabel, mShortcutAction);
        }
    }

    private void createContactShortcutIntent(Uri contactUri, String contentType, String displayName,
            Bitmap bitmap) {
        Intent shortcutIntent;
        if (mIsAliContacts) {
            shortcutIntent = new Intent(Intent.ACTION_VIEW);
        } else {
            shortcutIntent = new Intent(ContactsContract.QuickContact.ACTION_QUICK_CONTACT);
        }

        // When starting from the launcher, start in a new, cleared task.
        // CLEAR_WHEN_TASK_RESET cannot reset the root of a task, so we
        // clear the whole thing preemptively here since QuickContactActivity will
        // finish itself when launching other detail activities.
        shortcutIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        // Tell the launcher to not do its animation, because we are doing our own
        shortcutIntent.putExtra(INTENT_EXTRA_IGNORE_LAUNCH_ANIMATION, true);

        shortcutIntent.setDataAndType(contactUri, contentType);
        shortcutIntent.putExtra(ContactsContract.QuickContact.EXTRA_MODE,
                ContactsContract.QuickContact.MODE_LARGE);
        shortcutIntent.putExtra(ContactsContract.QuickContact.EXTRA_EXCLUDE_MIMES,
                (String[]) null);
        shortcutIntent.putExtra(NEED_TOAST_WHEN＿CONTACT_NOT_FOUND, true);

        //final Bitmap icon = generateQuickContactIcon(bitmap);

        Intent intent = new Intent();
        // Scale icon if too large, avoid binder transaction fail. Max size 40k.
        intent.putExtra(Intent.EXTRA_SHORTCUT_ICON, scaleBitmapToPreferSize(bitmap));
        intent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent);
        if (TextUtils.isEmpty(displayName)) {
            intent.putExtra(Intent.EXTRA_SHORTCUT_NAME, mContext.getResources().getString(
                    R.string.missing_name));
        } else {
            intent.putExtra(Intent.EXTRA_SHORTCUT_NAME, displayName);
        }

        mListener.onShortcutIntentCreated(contactUri, intent);
    }

    private Bitmap scaleBitmapToPreferSize(Bitmap bitmap) {
        int height = bitmap.getHeight();
        int width = bitmap.getWidth();
        float density = mContext.getResources().getDisplayMetrics().density;
        float preferSize = MAX_SHORT_CUT_BITMAP_SIZE;
        if (density != 0) {
            preferSize = PREFER_PHOTO_SIZE_IN_DP * density;
        }
        int larger = height > width ? height : width;
        Matrix matrix = null;
        if (larger > (int) preferSize) {
            float scale = preferSize / larger;
            matrix = new Matrix();
            matrix.postScale(scale, scale);
        } else if (larger < preferSize) {
            float scale = preferSize / larger;
            matrix = new Matrix();
            matrix.postScale(scale, scale);
        }
        if (matrix != null) {
            bitmap = Bitmap.createBitmap(bitmap, 0, 0, width, height, matrix, true);
        }
        bitmap = circleMaskBitmap(bitmap);
        return bitmap;
    }

    private void createPhoneNumberShortcutIntent(Uri uri, String displayName, Bitmap bitmap,
            String phoneNumber, int phoneType, String phoneLabel, String shortcutAction) {
        Uri phoneUri;
        if (Intent.ACTION_CALL.equals(shortcutAction)) {
            // Make the URI a direct tel: URI so that it will always continue to work
            phoneUri = Uri.fromParts(Constants.SCHEME_TEL, phoneNumber, null);
            bitmap = generatePhoneNumberIcon(bitmap, phoneType, phoneLabel,
                    R.drawable.ic_elder_call);
        } else {
            phoneUri = Uri.fromParts(Constants.SCHEME_SMSTO, phoneNumber, null);
            bitmap = generatePhoneNumberIcon(bitmap, phoneType, phoneLabel,
                    R.drawable.ic_elder_message);
        }

        Intent shortcutIntent = new Intent(shortcutAction, phoneUri);
        shortcutIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

        Intent intent = new Intent();
        intent.putExtra(Intent.EXTRA_SHORTCUT_ICON, bitmap);
        intent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent);
        intent.putExtra(Intent.EXTRA_SHORTCUT_NAME, TextUtils.isEmpty(displayName) ? phoneNumber : displayName);

        mListener.onShortcutIntentCreated(uri, intent);
    }

    /*
    private void drawBorder(Canvas canvas, Rect dst) {
        // Darken the border
        final Paint workPaint = new Paint();
        workPaint.setColor(mBorderColor);
        workPaint.setStyle(Paint.Style.STROKE);
        // The stroke is drawn centered on the rect bounds, and since half will be drawn outside the
        // bounds, we need to double the width for it to appear as intended.
        workPaint.setStrokeWidth(mBorderWidth * 2);
        canvas.drawRect(dst, workPaint);
    }*/

    /**
     * Generates a phone number shortcut icon. Adds an overlay describing the type of the phone
     * number, and if there is a photo also adds the call action icon.
     */
    private Bitmap generatePhoneNumberIcon(Bitmap photo, int phoneType, String phoneLabel,
            int actionResId) {
        final Resources r = mContext.getResources();

        Bitmap phoneIcon = ((BitmapDrawable) r.getDrawableForDensity(actionResId, mIconDensity))
                .getBitmap();

        // Setup the drawing classes
        Bitmap icon = Bitmap.createBitmap(mIconSize, mIconSize, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(icon);

        // Copy in the photo
        Paint photoPaint = new Paint();
        photoPaint.setDither(true);
        photoPaint.setFilterBitmap(true);
        photoPaint.setAntiAlias(true);
        photo = circleMaskBitmap(photo);
        Rect src = new Rect(0, 0, photo.getWidth(), photo.getHeight());
        Rect dst = new Rect(0, 0, mIconSize, mIconSize);
        canvas.drawBitmap(photo, src, dst, photoPaint);

        // Draw the phone action icon as an overlay
        src.set(0, 0, phoneIcon.getWidth(), phoneIcon.getHeight());
        dst.set(src);
        dst.offset(mIconSize - src.width(), 0);
        canvas.drawBitmap(phoneIcon, src, dst, photoPaint);

        canvas.setBitmap(null);

        return icon;
    }

    // round icon algorithm with anti-alias
    public static Bitmap circleMaskBitmap(Bitmap bitmap) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        float roundPx;
        float left,top,right,bottom,dst_left,dst_top,dst_right,dst_bottom;
        if (width <= height) {
            roundPx = width / 2;
            top = 0;
            bottom = width;
            left = 0;
            right = width;
            height = width;
            dst_left = 0;
            dst_top = 0;
            dst_right = width;
            dst_bottom = width;
        } else {
            roundPx = height / 2;
            float clip = (width - height) / 2;
            left = clip;
            right = width - clip;
            top = 0;
            bottom = height;
            width = height;
            dst_left = 0;
            dst_top = 0;
            dst_right = height;
            dst_bottom = height;
        }
        Bitmap output = Bitmap.createBitmap(width,
                height, Config.ARGB_8888);
        Canvas canvas = new Canvas(output);

        final int color = 0xff424242;
        final Paint paint = new Paint();
        final Rect src = new Rect((int)left, (int)top, (int)right, (int)bottom);
        final Rect dst = new Rect((int)dst_left, (int)dst_top, (int)dst_right, (int)dst_bottom);
        final RectF rectF = new RectF(dst);

        paint.setAntiAlias(true);

        canvas.drawARGB(0, 0, 0, 0);
        paint.setColor(color);
        canvas.drawRoundRect(rectF, roundPx, roundPx, paint);

        paint.setXfermode(new PorterDuffXfermode(Mode.SRC_IN));
        canvas.drawBitmap(bitmap, src, dst, paint);
        return output;
    }
}
