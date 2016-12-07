package com.yunos.alicontacts.plugin.duplicateremove;

import android.content.Context;
import android.net.Uri;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import yunos.support.v4.content.CursorLoader;

/**
 *
 * @author tianyuan.ty
 *
 * Factory Class to produce loaders for different type
 */
public final class ContactLoaderFactory {

    public static CursorLoader createAllContactsLoader(Context ctx) {
        return new CursorLoader(ctx, AllPhonesQuery.CONTENT_URI, AllPhonesQuery.PROJECTION, null,
                null, null);
    }

    public static CursorLoader createOnlyPhoneContactsLoader(Context ctx) {
        return new CursorLoader(ctx, OnlyPhoneNumberQuery.CONTENT_URI,
                OnlyPhoneNumberQuery.PROJECTION, OnlyPhoneNumberQuery.SELECTION, null, null);
    }

    public static CursorLoader createOnlyNameContactsLoader(Context ctx) {
        return new CursorLoader(ctx, OnlyNameQuery.CONTENT_URI, OnlyNameQuery.PROJECTION,
                OnlyNameQuery.SELECTION, null, null);
    }

    // Query for all phone numbers of all people
    public interface AllPhonesQuery {
        // An identifier for the loader
        final static int QUERY_ID = 1;

        final static Uri CONTENT_URI = Phone.CONTENT_URI;

        final static String[] PROJECTION = {
                Phone.CONTACT_ID, // ..........................................0
                Phone.DISPLAY_NAME, // ........................................1
                Phone.RAW_CONTACT_ID, // ......................................2
                Phone.NUMBER, // ..............................................3
                Phone.TYPE, // ................................................4
        };

        public final static int CONTACT_ID = 0;
        public final static int DISPLAY_NAME = 1;
        public final static int RAW_CONTACT_ID = 2;
        public final static int NUMBER = 3;
        public final static int TYPE = 4;
    }

    // Query for all contacts which have name but not phone number
    public interface OnlyNameQuery {
        // An identifier for the loader
        final static int QUERY_ID = 2;
        final static Uri CONTENT_URI = Contacts.CONTENT_URI;

        final static String[] PROJECTION = {
                Contacts._ID, // .................................................0
                Contacts.DISPLAY_NAME, // ........................................1
        };

        final static String SELECTION = "((" + Contacts.DISPLAY_NAME + " IS NOT NULL) AND ("
                + Contacts.DISPLAY_NAME + " <> '' ) AND (" + Contacts.HAS_PHONE_NUMBER + " =0))";

        public final static int CONTACT_ID = 0;
        public final static int DISPLAY_NAME = 1;

    }

    // Query for all contacts which have phone number not have name
    public interface OnlyPhoneNumberQuery {
        // An identifier for the loader
        final static int QUERY_ID = 3;
        final static Uri CONTENT_URI = Contacts.CONTENT_URI;

        final static String[] PROJECTION = {
                Contacts._ID, // ...............................................0
                Contacts.LOOKUP_KEY, // ........................................1
        };

        final static String SELECTION = "(((" + Contacts.DISPLAY_NAME + " IS NULL) OR ("
                + Contacts.DISPLAY_NAME + " = '' )) AND (" + Contacts.HAS_PHONE_NUMBER + "=1))";

        public final static int CONTACT_ID = 0;
        public final static int LOOKUP_KEY = 1;

    }

}
