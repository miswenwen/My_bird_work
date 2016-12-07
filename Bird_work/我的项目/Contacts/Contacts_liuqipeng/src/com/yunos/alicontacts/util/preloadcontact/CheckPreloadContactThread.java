package com.yunos.alicontacts.util.preloadcontact;

import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.os.SystemProperties;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.CommonDataKinds.Photo;
import android.provider.ContactsContract.CommonDataKinds.StructuredName;
import android.provider.ContactsContract.Contacts.Entity;
import android.provider.ContactsContract.Data;
import android.provider.ContactsContract.RawContacts;
import android.provider.ContactsContract.RawContactsEntity;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import android.util.LongSparseArray;

import com.alibaba.fastjson.JSON;
import com.yunos.alicontacts.model.account.AccountType;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import java.io.File;
import java.io.FileInputStream;


public class CheckPreloadContactThread extends Thread {
    private static final String TAG = "CheckPreloadContactThread";

    /*
     * For each preload contact, we will first search by name to get the contact id.
     * Then query the numbers for the contact id.
     * CONTACT_ID_PROJECTION is used to query the raw contact id.
     * PHONE_NUMBER_PROJECTION is used to query the phone numbers.
     */
    private static final String[] CONTACT_ID_PROJECTION = new String[] {
        RawContacts.CONTACT_ID,
    };
    private static final int COLUMN_ID = 0;
    private static final String CONTACT_ID_SELECTION =
            RawContacts.DISPLAY_NAME_PRIMARY + "=? OR " + RawContacts.DISPLAY_NAME_ALTERNATIVE + "=?";

    private static final String[] PHONE_NUMBER_PROJECTION = new String[] {
        Entity.CONTACT_ID,
        Entity.DATA1,
    };
    private static final int COLUMN_CONTACT_ID = 0;
    private static final int COLUMN_NUMBER = 1;
    private static final String PHONE_NUMBER_SELECTION_PREFIX
        = Entity.MIMETYPE + "='" + Phone.CONTENT_ITEM_TYPE + "' AND " + Entity.CONTACT_ID + " in ";

    /**
     * If the user starts the thread at first use a new phone,
     * and system time changed from default time to current,
     * then the user starts the thread again immediately,
     * we will get two threads insert preload contacts.
     * We don't want the second thread to run before the first quits,
     * so we have to check and set the running flag.
     */
    private static volatile boolean running = false;
    private final Context mContext;
    private Map<String, PreloadContact[]> mCachedExisting = new HashMap<String, PreloadContact[]>();

    public CheckPreloadContactThread(Context context) {
        mContext = context;
    }

    @Override
    public void run() {
        if (running) {
            return;
        }
        try {
            running = true;
            List<PreloadContact> preloads = readPreloadContactsFromFileorAsset();
            if (preloads == null) {
                Log.i(TAG, "run: no preload contacts.");
                return;
            }
            Iterator<PreloadContact> iter = preloads.iterator();
            PreloadContact p;
            ContentResolver resolver = mContext.getContentResolver();
            while (iter.hasNext()) {
                p = iter.next();
                if (findPreloadContactInDb(resolver, p)) {
                    iter.remove();
                }
            }
            // we do NOT need the cache any more.
            mCachedExisting.clear();
            mCachedExisting = null;
            if (preloads.size() > 0) {
                insertPreloadContactsToDb(resolver, preloads);
            }
        } catch (Exception e) {
            Log.e(TAG, "CheckPreloadContactThread.run: got exception.", e);
        } finally {
            running = false;
        }
    }

    private List<PreloadContact> readPreloadContactsFromFileorAsset() {
        List<PreloadContact> result = null;
        String preloadJson = null;
        try {
            if (SystemProperties.getBoolean(PreloadContactUtil.PRELOAD_CONTACT_PROP_KEY_NAME, false)) {
                preloadJson = readFileToString(PreloadContactUtil.PRELOAD_CONTACT_FILE_PATH);
            } else {
                preloadJson = readAssetToString(PreloadContactUtil.PRELOAD_CONTACT_ASSET_PATH);
            }
            if (TextUtils.isEmpty(preloadJson)) {
                return null;
            }
            result = JSON.parseArray(preloadJson, PreloadContact.class);
        } catch (Exception e) {
            Log.e(TAG, "readPreloadContactsFromFileorAsset: got exception.", e);
        }
        Log.d(TAG, "readPreloadContactsFromFileorAsset: size="
                + (result == null ? -1 : result.size()));
        return result;
    }

    private String readAssetToString(String assetPath) throws IOException {
        Log.d(TAG, "readAssetToString: assetPath="+assetPath);
        String result = null;
        InputStream is = null;
        ByteArrayOutputStream os = null;
        byte[] buf = null;
        try {
            is = mContext.getAssets().open(assetPath);
            os = new ByteArrayOutputStream(PreloadContactUtil.ASSET_READ_BUF_SIZE);
            buf = new byte[PreloadContactUtil.ASSET_READ_BUF_SIZE];
            int n;
            while ((n = is.read(buf)) >= 0) {
                os.write(buf, 0, n);
            }
            result = new String(os.toByteArray(), PreloadContactUtil.PRELOAD_CONTACT_ASSET_FILE_CHAR_SET);
        } catch (FileNotFoundException fne) {
            // It seems the AssetManager has no way to detect if a path exists,
            // so we have to catch FileNotFoundException here.
            Log.i(TAG, "readAssetToString: no asset found for preload contacts.");
        } finally {
            closeCloseable(is);
            closeCloseable(os);
        }
        return result;
    }

    private String readFileToString(String filePath) throws IOException {
        Log.d(TAG, "readFileToString: filePath="+filePath);
        String result = null;
        FileInputStream is = null;
        ByteArrayOutputStream os = null;
        byte[] buf = null;
        try {
            File f = new File(filePath);
            if (f.exists()) {
                Log.i(TAG, "preload contact file exists");
                is = new FileInputStream(f);
                os = new ByteArrayOutputStream(PreloadContactUtil.ASSET_READ_BUF_SIZE);
                buf = new byte[PreloadContactUtil.ASSET_READ_BUF_SIZE];
                int n;
                while ((n = is.read(buf)) >= 0) {
                    os.write(buf, 0, n);
                }
                result = new String(os.toByteArray(),
                        PreloadContactUtil.PRELOAD_CONTACT_ASSET_FILE_CHAR_SET);
            }
        } catch (FileNotFoundException fne) {
            Log.e(TAG, "readFileToString: no file found for preload contacts.");
        } finally {
            closeCloseable(is);
            closeCloseable(os);
        }
        return result;
    }

    private boolean findPreloadContactInDb(ContentResolver resolver, PreloadContact p) {
        PreloadContact[] existingArray = mCachedExisting.get(p.name);
        // because queryPreloadContactFromDb will not return null,
        // so if existingArray is null, it means we have never cached value for the name.
        if (existingArray == null) {
            Long[] ids = queryContactIdsByName(resolver, p.name);
            // If the user stores too many contacts with the same name,
            // then we assume the user don't care if there is any duplicate data.
            // The user wants to zuo, and we let him/her die.
            if ((ids == null) || (ids.length > 500)) {
                return false;
            }
            existingArray = queryNumbersByIds(resolver, ids, p.name);
            mCachedExisting.put(p.name, existingArray);
        }
        for (PreloadContact existing : existingArray) {
            if (p.equalsIgnorePhoto(existing)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Query the contact with given ids from db, and return an array of such contacts.
     * The array object is PreloadContact.
     * @param resolver
     * @param ids
     * @param name
     * @return The contacts from db, that has given name.
     *          If no contacts has the given name, then 0-length array is returned.
     *          NEVER return null.
     */
    private PreloadContact[] queryNumbersByIds(ContentResolver resolver, Long[] ids, String name) {
        LongSparseArray<PreloadContact> result = new LongSparseArray<PreloadContact>();
        Cursor c = null;
        try {
            StringBuilder selection = new StringBuilder(PHONE_NUMBER_SELECTION_PREFIX);
            int len = ids.length;
            String[] args = new String[len];
            selection.append('(');
            for (int i = 0; i < len; i++) {
                if (i > 0) {
                    selection.append(',');
                }
                selection.append('?');
                args[i] = String.valueOf(ids[i]);
            }
            selection.append(')');
            c = resolver.query(
                    RawContactsEntity.CONTENT_URI,
                    PHONE_NUMBER_PROJECTION,
                    selection.toString(),
                    args,
                    Entity.CONTACT_ID);
            int count = c == null ? -1 : c.getCount();
            if (count <= 0) {
                return new PreloadContact[0];
            }
            long id;
            PreloadContact pc;
            while (c.moveToNext()) {
                id = c.getLong(COLUMN_CONTACT_ID);
                pc = result.get(id);
                if (pc == null) {
                    pc = new PreloadContact();
                    result.put(id, pc);
                    pc.name = name;
                    pc.numbers = new ArrayList<String>();
                }
                pc.numbers.add(c.getString(COLUMN_NUMBER));
            }
        } finally {
            closeCloseable(c);
        }
        PreloadContact[] ret = new PreloadContact[result.size()];
        for (int i = 0; i < ret.length; i++) {
            ret[i] = result.valueAt(i);
        }
        return ret;
    }

    private Long[] queryContactIdsByName(ContentResolver resolver, String name) {
        Set<Long> ids = new HashSet<Long>();
        Cursor c = null;
        try {
            c = resolver.query(
                    RawContacts.CONTENT_URI,
                    CONTACT_ID_PROJECTION,
                    CONTACT_ID_SELECTION,
                    new String[] { name, name },
                    null);
            int count = c == null ? -1 : c.getCount();
            if (count <= 0) {
                return null;
            }
            while (c.moveToNext()) {
                ids.add(c.getLong(COLUMN_ID));
            }
        } finally {
            closeCloseable(c);
        }
        return ids.toArray(new Long[ids.size()]);
    }

    private void insertPreloadContactsToDb(ContentResolver resolver, List<PreloadContact> pcList) {
        for (PreloadContact pc : pcList) {
            insertPreloadContactToDb(resolver, pc);
        }
    }

    private void insertPreloadContactToDb(ContentResolver resolver, PreloadContact pc) {
        ArrayList<ContentProviderOperation> ops = new ArrayList<ContentProviderOperation>();
        int rawContactInsertIndex = 0;
        ops.add(ContentProviderOperation.newInsert(RawContacts.CONTENT_URI)
                .withValue(RawContacts.ACCOUNT_TYPE, AccountType.LOCAL_ACCOUNT_TYPE)
                .withValue(RawContacts.ACCOUNT_NAME, AccountType.LOCAL_ACCOUNT_NAME).build());

        ops.add(ContentProviderOperation.newInsert(android.provider.ContactsContract.Data.CONTENT_URI)
                .withValueBackReference(Data.RAW_CONTACT_ID, rawContactInsertIndex)
                .withValue(Data.MIMETYPE, StructuredName.CONTENT_ITEM_TYPE)
                .withValue(StructuredName.GIVEN_NAME, pc.name)
                .build());
        if (pc.numbers != null) {
            for (String num : pc.numbers) {
                ops.add(ContentProviderOperation.newInsert(android.provider.ContactsContract.Data.CONTENT_URI)
                        .withValueBackReference(Data.RAW_CONTACT_ID, rawContactInsertIndex)
                        .withValue(Data.MIMETYPE, Phone.CONTENT_ITEM_TYPE)
                        .withValue(Phone.NUMBER, num)
                        .withValue(Phone.TYPE, Phone.TYPE_WORK)
                        .withValue(Phone.LABEL, "")
                        .build());
            }
        }
        if (pc.photo != null) {
            ops.add(ContentProviderOperation.newInsert(android.provider.ContactsContract.Data.CONTENT_URI)
                    .withValueBackReference(Data.RAW_CONTACT_ID, rawContactInsertIndex)
                    .withValue(Data.MIMETYPE, Photo.CONTENT_ITEM_TYPE)
                    .withValue(Photo.PHOTO, decodePhoto(pc.photo))
                    .build());
        }
        try {
            resolver.applyBatch(ContactsContract.AUTHORITY,ops);
        } catch (Exception e) {
            Log.e(TAG, "insertPreloadContactToDb: failed to insert preload contact "+pc.name, e);
        }
    }

    private byte[] decodePhoto(String encoded) {
        return Base64.decode(encoded, Base64.DEFAULT);
    }

    private static void closeCloseable(Closeable c) {
        if (c != null) {
            try {
                c.close();
            } catch (Exception e) {
                // ignore
                Log.e(TAG, "closeCloseable: got exception.", e);
            }
        }
    }

}
