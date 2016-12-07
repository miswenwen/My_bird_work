
package com.yunos.alicontacts.servicehotline;

import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.OperationApplicationException;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;

import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.CommonDataKinds.Photo;
import android.provider.ContactsContract.CommonDataKinds.StructuredName;
import android.provider.ContactsContract.Data;
import android.provider.ContactsContract.RawContacts;

import com.yunos.alicontacts.R;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.IOException;
import java.util.ArrayList;

import org.json.JSONObject;


public class ServiceHotlineManager {
    private static final String TAG = "ServiceHotlineManager";
    private static final String SERVICE_HOTLINE_INIT = "service_hotline_init";

    public static final String SERVICE_HOTLINE_FILE_PATH = "/system/etc/custom/service_hotline.json";
    
    private Context mContext;
    private String mContent;
    private static ServiceHotlineManager INSTANCE = null;

    private ServiceHotlineManager(Context context) {
        mContext = context;
    }

    public static synchronized ServiceHotlineManager getInstance(Context context) {
        if (INSTANCE == null) {
            INSTANCE = new ServiceHotlineManager(context);
        }
        return INSTANCE;
    }

    public void checkServiceHotline() {
        if (!hasInitialised()) {
            mContent = getContent(SERVICE_HOTLINE_FILE_PATH);
            if (mContent != null && !mContent.isEmpty()) {
                initServiceHotline();
            }
        }
    }

    private boolean hasInitialised() {
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
        return prefs.getBoolean(SERVICE_HOTLINE_INIT, false);
    }

    public String getContent(String filePath) {
        String result = null;
        InputStream is = null;
        ByteArrayOutputStream os = null;
        byte[] buf = null;
        try {
            File f = new File(filePath);
            if (!f.exists()) {
                Log.e("waga", "getContent: file not exists");
                return null;
            }
            is = new FileInputStream(f);
            os = new ByteArrayOutputStream(1024);
            buf = new byte[1024];
            int n;
            while ((n = is.read(buf)) >= 0) {
                os.write(buf, 0, n);
            }
            result = new String(os.toByteArray(), "UTF-8");
        } catch (Exception e) {
            Log.e(TAG, e.toString());
        } finally {
            closeCloseable(is);
            closeCloseable(os);
        }
        Log.e("waga", "getContent result = " + result);
        return result;
    }

    private static void closeCloseable(Closeable c) {
        if (c != null) {
            try {
                c.close();
            } catch (Exception e) {
                Log.e(TAG, "closeCloseable: got exception.", e);
            }
        }
    }

    private void initServiceHotline() {
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                boolean result = insertContact();
                if (result == true) {
            	    final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
                    Editor editor = prefs.edit();
                    editor.putBoolean(SERVICE_HOTLINE_INIT, true);
                    editor.apply();
                }
                return null;
            }

        }.execute();
    }

	public boolean insertContact() {
	    String name = "";
	    ArrayList<String> numberList = new ArrayList<String>();
        /// bird: TASK #7674,custom contacts readonly attr,chengting,@20160301 {
        boolean readonly = false;
        /// @}
	    try {
	        JSONObject jsonObject = new JSONObject(mContent);
	        name = jsonObject.getString("name");
	        String numbers = jsonObject.getString("numbers");
	        Log.e("waga", "name = " + name);
            Log.e("waga", "numbers = " + numbers);
            /// bird: TASK #7674,custom contacts readonly attr,chengting,@20160301 {
            readonly = "1".equals(jsonObject.getString("readonly"));
            Log.e("waga", "readonly = " + readonly);
            /// @}
	        while(numbers.indexOf(",") > 0) {
	            String number = numbers.substring(0, numbers.indexOf(","));
	            numbers = numbers.substring(numbers.indexOf(",")+1, numbers.length());
	            numberList.add(number);
	        }

	        if (!numbers.isEmpty()) {
	            numberList.add(numbers);
	        }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
	    
        ArrayList<ContentProviderOperation> ops = new ArrayList<ContentProviderOperation>();
        ContentProviderOperation.Builder builder = ContentProviderOperation
    		.newInsert(RawContacts.CONTENT_URI);

        /// bird: TASK #7674,custom contacts readonly attr,chengting,@20160301 {
        if(readonly){
            builder.withValue(android.provider.ContactsContract.RawContacts.IS_SDN_CONTACT, -2);
        }
        /// @}
        builder.withValue(RawContacts.AGGREGATION_MODE, RawContacts.AGGREGATION_MODE_DISABLED);
        ops.add(builder.build());

        ops.add(ContentProviderOperation.newInsert(Data.CONTENT_URI)
            .withValueBackReference(StructuredName.RAW_CONTACT_ID, 0)
            .withValue(Data.MIMETYPE, StructuredName.CONTENT_ITEM_TYPE)
            .withValue(StructuredName.GIVEN_NAME, name).build());

        for (int i=0; i<numberList.size(); i++) {
            ops.add(ContentProviderOperation.newInsert(Data.CONTENT_URI)
                .withValueBackReference(Phone.RAW_CONTACT_ID, 0)
                .withValue(Data.MIMETYPE, Phone.CONTENT_ITEM_TYPE)
                .withValue(Phone.TYPE, Phone.TYPE_WORK)
                .withValue(Phone.NUMBER, numberList.get(i))
                .withValue(Data.IS_PRIMARY, 1).build());
        }
        try {
            ContentProviderResult[] result = mContext.getContentResolver()
                .applyBatch(ContactsContract.AUTHORITY, ops);
            if (result != null && result.length == ops.size()) {
                return true;
            } else {
                return false;
            }
        } catch (RemoteException e) {
            Log.e(TAG, String.format("%s: %s", e.toString(), e.getMessage()));
            return false;
        } catch (OperationApplicationException e) {
            Log.e(TAG, String.format("%s: %s", e.toString(), e.getMessage()));
            return false;
        }
    }
}
