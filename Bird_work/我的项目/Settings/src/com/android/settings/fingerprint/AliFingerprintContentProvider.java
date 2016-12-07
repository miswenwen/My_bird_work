package com.android.settings.fingerprint;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.content.ContentUris;
import android.content.UriMatcher;
import android.database.sqlite.SQLiteDatabase;

public class AliFingerprintContentProvider extends ContentProvider {
    private static final String TAG = "AliFingerprintContentProvider";

    public static final Uri MOBILE_DATA_URI = Uri.parse("content://authentication.information");
    private static final String AUTHORITY = "authentication.information";
    public static final String DEFAULT_PHONE_MODEL = "model";
    public static final String FINGERPRINT_SUPPORT  = "fingerprint";
    public static final String PROTOCAL_TYPE        = "protocalType";
    public static final String PROTOCAL_VERSION     = "protocalVersion";
    public static final String DEFAULT_PHONE_VERDOR = "vendor";

/*******finger quick settings start***************/
    private static final UriMatcher matcher;
    private AliFingerDBHelper mDBHelper;
    private static final int PERSON_ALL = 0;
    private static final int PERSON_ONE = 1;
    public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.birdfinger.fingerquick";
    public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.birdfinger.fingerquick";
    private static final Uri NOTIFY_URI = Uri.parse("content://" + AUTHORITY + "/fingerquick");

    static {
        matcher = new UriMatcher(UriMatcher.NO_MATCH);

        matcher.addURI(AUTHORITY, "fingerquick", PERSON_ALL);
        matcher.addURI(AUTHORITY, "fingerquick/#", PERSON_ONE);
    }
/*******finger quick settings end***************/

    @Override
    public boolean onCreate() {
        // TODO Auto-generated method stub
        /*******finger quick settings***************/
        mDBHelper = new AliFingerDBHelper(getContext());
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection,
            String[] selectionArgs, String sortOrder) {
        // TODO Auto-generated method stub
        /*******finger quick settings start***************/
        SQLiteDatabase db = mDBHelper.getReadableDatabase();
        int match = matcher.match(uri);
        switch (match) {
        case PERSON_ALL:
            //doesn't need any code in my provider.
            break;
        case PERSON_ONE:
            long _id = ContentUris.parseId(uri);
            selection = "_id = ?";
            selectionArgs = new String[]{String.valueOf(_id)};
            break;
        default:
            throw new IllegalArgumentException("Unknown URI: " + uri);
        }
        return db.query(AliFingerDBHelper.FINGER_QUICK_TABLE_NAME, projection, selection, selectionArgs, null, null, sortOrder);
        /*******finger quick settings end***************/
    }

    @Override
    public String getType(Uri uri) {
        // TODO Auto-generated method stub
        /*******finger quick settings start***************/
        int match = matcher.match(uri);
        switch (match) {
        case PERSON_ALL:
            return CONTENT_TYPE;
        case PERSON_ONE:
            return CONTENT_ITEM_TYPE;
        default:
            return null;
        }
        /*******finger quick settings end***************/
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        // TODO Auto-generated method stub
        /*******finger quick settings start***************/
        int match = matcher.match(uri);
        if (match != PERSON_ALL) {
            throw new IllegalArgumentException("Wrong URI: " + uri);
        }
        SQLiteDatabase db = mDBHelper.getWritableDatabase();
        if (values == null) {
            return null;
        }
        long rowId = db.insert(AliFingerDBHelper.FINGER_QUICK_TABLE_NAME, null, values);
        if (rowId > 0) {
            notifyDataChanged();
            return ContentUris.withAppendedId(uri, rowId);
        }
        /*******finger quick settings end***************/
        return null;

    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        // TODO Auto-generated method stub
        /*******finger quick settings start***************/
        SQLiteDatabase db = mDBHelper.getWritableDatabase();
        int match = matcher.match(uri);
        switch (match) {
        case PERSON_ALL:
            //doesn't need any code in my provider.
            break;
        case PERSON_ONE:
            long _id = ContentUris.parseId(uri);
            selection = "_id = ?";
            selectionArgs = new String[]{String.valueOf(_id)};
        }
        int count = db.delete(AliFingerDBHelper.FINGER_QUICK_TABLE_NAME, selection, selectionArgs);
        if (count > 0) {
            notifyDataChanged();
            return count;
        }
        /*******finger quick settings end***************/
        return 0;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection,
            String[] selectionArgs) {
        // TODO Auto-generated method stub
        /*******finger quick settings start***************/
        SQLiteDatabase db = mDBHelper.getWritableDatabase();
        int match = matcher.match(uri);
        switch (match) {
        case PERSON_ALL:
            //doesn't need any code in my provider.
            break;
        case PERSON_ONE:
            long _id = ContentUris.parseId(uri);
            selection = "_id = ?";
            selectionArgs = new String[]{String.valueOf(_id)};
            break;
        default:
            throw new IllegalArgumentException("Unknown URI: " + uri);
        }
        int count = db.update(AliFingerDBHelper.FINGER_QUICK_TABLE_NAME, values, selection, selectionArgs);
        if (count > 0) {
            notifyDataChanged();
            return count;
        }
        /*******finger quick settings end***************/
        return 0;
    }
    private void notifyDataChanged() {
        getContext().getContentResolver().notifyChange(NOTIFY_URI, null);
    }

}

