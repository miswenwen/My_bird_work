package com.yunos.alicontacts.conference;

import android.annotation.NonNull;
import android.content.ContentUris;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.PhoneLookup;
import android.text.TextUtils;
import android.util.Log;

import com.google.common.io.Closeables;
import com.yunos.alicontacts.activities.ConferenceRecipientsPickerActivity;
import com.yunos.alicontacts.database.util.NumberServiceHelper;
import com.yunos.alicontacts.util.AliTextUtils;
import com.yunos.alicontacts.util.Constants;

import java.util.ArrayList;
import java.util.HashMap;

public class QueryPhoneNumberHandler extends Handler {
    public static final int WHAT_QUERY_PHONE_URIS = 1;
    public static final int WHAT_QUERY_LOCATIONS = 2;
    public static final int WHAT_QUERY_PHONE_NAMES = 3;
    private static final String TAG = "QueryPhoneNumberHandler";

    private final ConferenceRecipientsPickerActivity mActivity;
    private volatile int mWorkingMessageType = -1;

    public QueryPhoneNumberHandler(ConferenceRecipientsPickerActivity activity, HandlerThread thread) {
        super(thread.getLooper());
        mActivity = activity;
    }

    @Override
    public void handleMessage(Message msg) {
        Log.i(TAG, "handleMessage: what="+msg.what);
        mWorkingMessageType = msg.what;
        try {
            handleMessageInternal(msg);
        } finally {
            mWorkingMessageType = -1;
        }
    }

    public boolean isWorkingOnMessage(int what) {
        return mWorkingMessageType == what;
    }

    private void handleMessageInternal(Message msg) {
        switch (msg.what) {
            case WHAT_QUERY_PHONE_URIS:
                Log.i(TAG, "handleMessageInternal: query phone uris.");
                // after query phone uris, we will send another WHAT_QUERY_LOCATIONS.
                removeMessages(WHAT_QUERY_LOCATIONS);
                // Do NOT remove WHAT_QUERY_PHONE_URIS,
                // because the recipients in latter message might be different.
                handleQueryPhoneUris((Uri[]) msg.obj);
                break;
            case WHAT_QUERY_LOCATIONS:
                Log.i(TAG, "handleMessageInternal: query locations.");
                removeMessages(WHAT_QUERY_LOCATIONS);
                if (hasMessages(WHAT_QUERY_PHONE_URIS)) {
                    // after query phone uris, we will send another WHAT_QUERY_LOCATIONS.
                    return;
                }
                handleQueryLocations((Recipient[]) msg.obj);
                break;
            case WHAT_QUERY_PHONE_NAMES:
                Log.i(TAG, "handleMessageInternal: query name for phone numbers.");
                handleQueryPhoneNumbersName((Recipient[]) msg.obj);
                break;
            default:
                Log.w(TAG, "handleMessageInternal: unrecognized message.");
                break;
        }
    }

    private static final String[] PHONE_URIS_QUERY_PROJECTION = new String[] {
        Phone._ID, Phone.NUMBER, Phone.DISPLAY_NAME_PRIMARY,
    };
    private static final int PHONE_URI_QUERY_COLUMN_ID = 0;
    private static final int PHONE_URI_QUERY_COLUMN_NUMBER = 1;
    private static final int PHONE_URI_QUERY_COLUMN_NAME = 2;

    private void handleQueryPhoneUris(Uri[] uris) {
        Cursor cursor = null;
        if ((uris == null) || (uris.length == 0)) {
            Log.i(TAG, "handleQueryPhoneUris: empty uris.");
            cursor = null;
        } else {
            cursor = queryPhoneUris(uris);
        }
        final Recipient[] recipients;
        try {
            if ((cursor != null) && (cursor.getCount() > 0)) {
                recipients = makeRecipientsFromCursor(cursor);
            } else {
                recipients = new Recipient[0];
            }
        } finally {
            Closeables.closeQuietly(cursor);
        }
        mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mActivity.onQueryNumberInfoCompleted(recipients);
            }
        });
    }

    private Cursor queryPhoneUris(Uri[] uris) {
        Uri queryBaseUri = Phone.CONTENT_URI;
        Uri.Builder builder = queryBaseUri.buildUpon()
                .appendQueryParameter(ContactsContract.REMOVE_DUPLICATE_ENTRIES, "true");
        Uri queryUri = builder.build();
        StringBuilder selection = new StringBuilder(Phone._ID + " IN (");
        for (Uri phoneUri : uris) {
            String idStr = phoneUri == null ? null : phoneUri.getLastPathSegment();
            try {
                selection.append(Long.parseLong(idStr)).append(',');
            } catch (NumberFormatException nfe) {
                Log.w(TAG, "queryPhoneUris: ignore invalid phone uri: "+phoneUri);
            }
        }
        selection.deleteCharAt(selection.length() - 1);
        selection.append(')');
        Cursor cursor = null;
        try {
            cursor = mActivity.getContentResolver().query(
                    queryUri, PHONE_URIS_QUERY_PROJECTION, selection.toString(), null, null);
        } catch (SQLiteException e) {
            Log.e(TAG, "queryPhoneUris: failed to query phone uris.", e);
        }
        return cursor;
    }

    private Recipient[] makeRecipientsFromCursor(Cursor cursor) {
        ArrayList<Recipient> recipients = new ArrayList<Recipient>(cursor.getCount());
        long id;
        Uri uri;
        String name, number;
        Recipient recipient;
        while (cursor.moveToNext()) {
            id = cursor.getLong(PHONE_URI_QUERY_COLUMN_ID);
            uri = ContentUris.withAppendedId(Phone.CONTENT_URI, id);
            number = cursor.getString(PHONE_URI_QUERY_COLUMN_NUMBER);
            name = cursor.getString(PHONE_URI_QUERY_COLUMN_NAME);
            recipient = new Recipient(uri, name, number);
            recipients.add(recipient);
        }
        return recipients.toArray(new Recipient[recipients.size()]);
    }

    private void handleQueryLocations(@NonNull Recipient[] recipients) {
        // As we have only up to 5 numbers, so it is safe to use batch location query uri.
        // The limit of batch location query uri is 10 numbers.
        ArrayList<String> numbers = new ArrayList<String>(
                ConferenceRecipientsPickerActivity.MAX_RECIPIENTS_COUNT);
        for (Recipient recipient : recipients) {
            if (recipient.getLocation() == null) {
                numbers.add(recipient.formattedNumber);
            }
        }
        Cursor cursor = queryLocations(numbers);
        boolean locationUpdated = false;
        try {
            if ((cursor == null) || (cursor.getCount() == 0)) {
                return;
            }
            locationUpdated = fillLocations(recipients, cursor);
        } finally {
            Closeables.closeQuietly(cursor);
        }
        if (locationUpdated) {
            mActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mActivity.onQueryLocationCompleted();
                }
            });
        }
    }

    private Cursor queryLocations(ArrayList<String> numbers) {
        Uri batchUri = NumberServiceHelper.getBatchLocationQueryUriForNumbers(numbers.toString());
        Cursor cursor = null;
        try {
            cursor = mActivity.getContentResolver().query(batchUri, null, null, null, null);
        } catch (SQLiteException e) {
            Log.e(TAG, "queryLocations: failed to query locations.", e);
        }
        return cursor;
    }

    private boolean fillLocations(Recipient[] recipients, Cursor cursor) {
        boolean updated = false;
        HashMap<String, String> locationsMap = readLocationsToMap(cursor);
        String loc;
        for (Recipient recipient : recipients) {
            loc = locationsMap.get(recipient.formattedNumber);
            if (loc != null) {
                recipient.setLocation(loc);
                updated = true;
            }
        }
        return updated;
    }

    private HashMap<String, String> readLocationsToMap(Cursor cursor) {
        HashMap<String, String> locationsMap =
                new HashMap<String, String>(ConferenceRecipientsPickerActivity.MAX_RECIPIENTS_COUNT);
        String num, prov, area, loc;
        while (cursor.moveToNext()) {
            num = cursor.getString(NumberServiceHelper.LOCATION_BATCH_COLUMN_NUMBER);
            prov = cursor.getString(NumberServiceHelper.LOCATION_BATCH_COLUMN_PROVINCE);
            area = cursor.getString(NumberServiceHelper.LOCATION_BATCH_COLUMN_AREA);
            loc = AliTextUtils.makeLocation(prov, area);
            Log.i(TAG, "readLocationsToMap: got location "+loc+" for number "+AliTextUtils.desensitizeNumber(num));
            locationsMap.put(num, loc);
        }
        return locationsMap;
    }

    private void handleQueryPhoneNumbersName(@NonNull Recipient[] recipients) {
        final ArrayList<Recipient> results = new ArrayList<Recipient>(recipients.length);
        boolean changed = false;
        Recipient queryResult;
        for (Recipient recipient : recipients) {
            queryResult = queryPhoneName(recipient);
            // do NOT use equals() to compare. We need to check if they are the same instance.
            if (queryResult != recipient) {
                changed = true;
            }
            results.add(queryResult);
        }
        if (!changed) {
            return;
        }
        mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mActivity.onQueryNumberInfoCompleted(results.toArray(new Recipient[results.size()]));
            }
        });
    }

    public static final String[] PHONE_NUMBER_NAME_PROJECTION = new String[] {
        // PhoneLookup does not provide a constant with value "data_id".
        // But the column does exist in the table phone_lookup.
        "data_id",
        PhoneLookup.DISPLAY_NAME,
    };

    private Recipient queryPhoneName(Recipient recipient) {
        if (!TextUtils.isEmpty(recipient.name)) {
            return recipient;
        }
        Recipient result = recipient;
        Uri queryUri = PhoneLookup.CONTENT_FILTER_URI.buildUpon()
                .appendEncodedPath(Uri.encode(recipient.formattedNumber))
                .appendQueryParameter(Constants.PHONE_LOOKUP_QUERY_PARAM_IN_VISIBLE_CONTACTS, "true")
                .build();
        long id;
        Uri phoneUri;
        String name;
        Cursor cursor = null;
        try {
            cursor = mActivity.getContentResolver().query(
                    queryUri,
                    PHONE_NUMBER_NAME_PROJECTION,
                    null,
                    null,
                    null);
            if ((cursor != null) && cursor.moveToFirst()) {
                id = cursor.getLong(0);
                phoneUri = ContentUris.withAppendedId(Phone.CONTENT_URI, id);
                name = cursor.getString(1);
                result = new Recipient(phoneUri, name, recipient.number);
            }
        } catch (SQLiteException sqle) {
            Log.e(TAG, "queryPhoneName: got exception.", sqle);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return result;
    }

}

