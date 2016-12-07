
package com.yunos.alicontacts;

import android.database.Cursor;
import android.database.MatrixCursor;
import android.graphics.Bitmap;

import com.yunos.alicontacts.list.fisheye.FishEyeContactsCache;

public class ContactsDataCache {
    private FishEyeContactsCache mContactsListCache;
    private MatrixCursor mFavoriteContactListCursor;
    private MatrixCursor mConversationListCursor;
    private boolean[] mFishEyeMask;

    static private Bitmap mDialpadBitmap;
    static private Bitmap mDialpadFooterBarBitmap;

//    private static ContactsDataCache mInstances;
//
//    public static synchronized ContactsDataCache getInstances() {
//        if (mInstances == null) {
//            mInstances = new ContactsDataCache();
//        }
//        return mInstances;
//    }

    public void cacheFishEyeMask(boolean[] mask) {
        mFishEyeMask = mask;
    }

    public boolean[] getFishEyeMask() {
        return mFishEyeMask;
    }

    public void cacheContactsListCursor(final Cursor cursor, int nameColIdx) {
        mContactsListCache = new FishEyeContactsCache(cursor, nameColIdx);
    }

    public void clearContactsListCache() {
        mContactsListCache = null;
    }

    public FishEyeContactsCache getContactsListCache() {
        return mContactsListCache;
    }

    public void cacheConversationListCursor(final Cursor cursor) {
        if (mConversationListCursor != null) {
            mConversationListCursor.close();
        }

        if (cursor == null) {
            return;
        }

        mConversationListCursor = coloneCursor(cursor);

    }

    public Cursor getConversationListCursor() {
        if (mConversationListCursor != null && mConversationListCursor.isClosed()) {
            mConversationListCursor = null;
        }
        return mConversationListCursor;
    }

    public void cacheFavoriteContactListCursor(final Cursor cursor) {
        if (mFavoriteContactListCursor != null) {
            mFavoriteContactListCursor.close();
        }

        if (cursor == null) {
            return;
        }
        mFavoriteContactListCursor = coloneCursor(cursor);

    }

    public Cursor getFavoriteContactListCursor() {
        if (mFavoriteContactListCursor != null && mFavoriteContactListCursor.isClosed()) {
            mFavoriteContactListCursor = null;
        }
        return mFavoriteContactListCursor;
    }

    private MatrixCursor coloneCursor(Cursor cursor) {
        if (cursor == null) {
            return null;
        }

        String[] column = cursor.getColumnNames();
        MatrixCursor newCursor = new MatrixCursor(column);
        cursor.moveToPosition(-1);
        int count = column.length;
        while (cursor.moveToNext()) {
            Object[] rowContent = new Object[count];
            for (int i = 0; i < count; i++) {
                rowContent[i] = cursor.getString(i);
            }
            newCursor.addRow(rowContent);
        }
        cursor.moveToFirst();
        return newCursor;
    }

    public Bitmap getDialpadBitmap() {
        return mDialpadBitmap;
    }

    public Bitmap getDialpadFooterBarBitmap() {
        return mDialpadFooterBarBitmap;
    }

	public void clean() {
		if (mFavoriteContactListCursor != null) {
			mFavoriteContactListCursor.close();
		}
		if (mConversationListCursor != null) {
			mConversationListCursor.close();
		}
	}
}
