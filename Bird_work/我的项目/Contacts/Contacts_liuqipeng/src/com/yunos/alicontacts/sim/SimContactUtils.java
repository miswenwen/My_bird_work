
package com.yunos.alicontacts.sim;

import android.content.ContentProviderOperation;
import android.content.ContentProviderOperation.Builder;
import android.content.ContentProviderResult;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.RemoteException;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds.Email;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.CommonDataKinds.StructuredName;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.Data;
import android.provider.ContactsContract.Intents.Insert;
import android.provider.ContactsContract.RawContacts;
import android.telephony.PhoneNumberUtils;
import android.text.TextUtils;
import android.util.Log;

import com.google.android.collect.Sets;
import com.yunos.alicontacts.CallUtil;
import com.yunos.alicontacts.R;
import com.yunos.alicontacts.model.account.AccountType;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

public class SimContactUtils {
    private static final String TAG = "SimContactUtils";

    /** Split char for anrs and emails. */
    public static final String SPLIT_COMMA = ",";

    /** SIM account in database of ContactsProvider. */
    public static final String SIM_ACCOUNT_NAME = "sim";
    public static final String SIM_ACCOUNT_NAME_SIM1 = "sim1";
    public static final String SIM_ACCOUNT_NAME_SIM2 = "sim2";
    public static final String SIM_ACCOUNT_TYPE = "com.android.contact.sim";

    public static final int SIM1_ACCOUNT_HASHCODE = (SIM_ACCOUNT_NAME_SIM1.hashCode() ^ SIM_ACCOUNT_TYPE.hashCode()) & 0xFFF;
    public static final int SIM2_ACCOUNT_HASHCODE = (SIM_ACCOUNT_NAME_SIM2.hashCode() ^ SIM_ACCOUNT_TYPE.hashCode()) & 0xFFF;
    public static final int SIM_ACCOUNT_HASHCODE = (SIM_ACCOUNT_NAME.hashCode() ^ SIM_ACCOUNT_TYPE.hashCode()) & 0xFFF;

    /** SIM state changed action for MTK platform only. */
    public static final String ACTION_PHB_STATE_CHANGED = "android.intent.action.PHB_STATE_CHANGED";
    /** SIM state changed action for MTK/Qcomm/Spreadtrum platform. */
    public static final String ACTION_SIM_STATE_CHANGED = "android.intent.action.SIM_STATE_CHANGED";

    /** ready state for ACTION_PHB_STATE_CHANGED and ACTION_SIM_CONTACT_DATA_CHANGED */
    public static final String STATE_READY = "ready";

    /** SIM state changed key, used to mark SIM states. */
    public static final String INTENT_VALUE_ICC_READY = "READY";
    /** SIM state changed key, used to mark SIM states. */
    public static final String INTENT_KEY_ICC_STATE = "ss";
    /** SIM state changed key, used to mark subscription. */
    public static final String SUBSCRIPTION_KEY = "subscription";

    /** NOT_READY means ICC is not initial or not ready. */
    public static final String INTENT_VALUE_ICC_NOT_READY = "NOT_READY";
    /** LOADED means ICC is ready to r/w. */
    public static final String INTENT_VALUE_ICC_LOADED = "LOADED";
    /** ABSENT means ICC is missing. */
    public static final String INTENT_VALUE_ICC_ABSENT = "ABSENT";
    /** CARD_IO_ERROR means for three consecutive times there was SIM IO error. */
    public static final String INTENT_VALUE_ICC_CARD_IO_ERROR = "CARD_IO_ERROR";
    /** IMSI means ICC IMSI is ready in property. */
    public static final String INTENT_VALUE_ICC_IMSI = "IMSI";

    /** The key for sim1 icc is ready or not. */
    public static final String KEY_SIM1_ICC_READY = "key_SIM1_icc_ready";
    /** The key for sim2 icc is ready or not. */
    public static final String KEY_SIM2_ICC_READY = "key_SIM2_icc_ready";

    private static int mPhoneContactCount;

    /** Actions for SIM features. */
    public static final String ACTION_INSERT_SIM_CONTACTS = "android.intent.action.INSERT_SIM_CONTACTS";
    public static final String ACTION_EDITOR_SIM_CONTACTS = "android.intent.action.EDITOR_SIM_CONTACTS";
    public static final String ACTION_IMPORT_CONTACTS = "android.intent.action.IMPORT_CONTACTS";
    public static final String ACTION_EXPORT_CONTACTS = "android.intent.action.EXPORT_CONTACTS";

    public static final String SIM_MULTI_SELECT_ACTION = "SimMultiSelectAction";
    public static final String SIM_MULTI_SELECT_DATAS = "SimMultiSelectData";

    public static final int SIM_MULTI_SELECT_ACTION_IMPORT = 100;
    public static final int SIM_MULTI_SELECT_ACTION_EXPORT = 101;
    public static final int SIM_MULTI_SELECT_ACTION_IMPORT_FROM_SIM = 103;

    public static final String EMAIL_REGEX = "(\\w+([-+.]\\w+)*@\\w+([-.]\\w+)*\\.\\w+([-.]\\w+)*,?)+[^,]";
    public static final Pattern EMAIL_PATTERN = Pattern.compile(EMAIL_REGEX);
    public static final int SIM_NAME_MAX_EN_LENGTH = 14;
    public static final int SIM_NAME_MAX_CN_LENGTH = 6;
    public static final int SIM_NUMBER_MAX_LENGTH = 20;
    public static final int SIM_EMAIL_MAX_LENGTH = 40;

    public static final String SAVE_SIM_CONTACTS_ERROR = "error";

    public static final String QUERY_LOCAL_ACCOUNT_CONTACTS_SELECTION = RawContacts.ACCOUNT_TYPE + "='"
            + AccountType.LOCAL_ACCOUNT_TYPE + "' OR " + RawContacts.ACCOUNT_TYPE + "='"
            + AccountType.YUNOS_ACCOUNT_TYPE + "'";

    public static boolean hasMultiByteChar(String str) {
        if (TextUtils.isEmpty(str) || str.getBytes(Charset.defaultCharset()).length == str.length()) {
            return false;
        }
        return true;
    }

    public static boolean isEmailValid(String email) {
        if (!TextUtils.isEmpty(email)) {
            if (email.endsWith(SPLIT_COMMA)) {
                email = email.substring(0, email.length() - 1);
            }
            return EMAIL_PATTERN.matcher(email).matches();
        }

        return true;
    }

    public static String trimSimName(String displayName) {
        if (!TextUtils.isEmpty(displayName)) {
            displayName = displayName.trim();
            if (hasMultiByteChar(displayName) && displayName.length() > SIM_NAME_MAX_CN_LENGTH) {
                displayName = displayName.substring(0, SIM_NAME_MAX_CN_LENGTH);
            } else if (displayName.length() > SIM_NAME_MAX_EN_LENGTH) {
                displayName = displayName.substring(0, SIM_NAME_MAX_EN_LENGTH);
            }
        }
        return displayName;
    }

    public static String trimSimNumber(String number) {
        if (!TextUtils.isEmpty(number)) {
            number = number.trim();
            number = PhoneNumberUtils.stripSeparators(number);

            if (!TextUtils.isEmpty(number) && number.length() > SIM_NUMBER_MAX_LENGTH) {
                number = number.substring(0, SIM_NUMBER_MAX_LENGTH);
            }
        }

        return number;
    }

    public static String trimSimEmail(String email) {
        if (!TextUtils.isEmpty(email)) {
            email = email.trim();
        }

        return email;
    }

    public static String getSimListItemTitle(Context context, int subscription) {
        StringBuilder title = new StringBuilder();

        if (SimUtil.MULTISIM_ENABLE) {
            title.append(context
                    .getString((subscription == SimUtil.SLOT_ID_1) ? R.string.xxsim_sim_card_1
                            : R.string.xxsim_sim_card_2));

            String displayName = SimUtil.getSimCardDisplayName(context, subscription);
            if (!TextUtils.isEmpty(displayName)) {
                title.append(':').append(displayName);
            }
        } else {
            title.append(context.getString(R.string.title_sim));

            String displayName = SimUtil.getSimOperatorName(context);
            if (!TextUtils.isEmpty(displayName)) {
                title.append(':').append(displayName);
            }
        }

        return title.toString();
    }

    public static String getSimCardName(Context context, int subscription) {
        String displayName;
        if (SimUtil.MULTISIM_ENABLE) {
            displayName = SimUtil.getSimCardDisplayName(context, subscription);

            if (TextUtils.isEmpty(displayName)) {
                displayName = context
                        .getString((subscription == SimUtil.SLOT_ID_1) ? R.string.title_sim1
                                : R.string.title_sim2);
            }
        } else {
            displayName = SimUtil.getSimOperatorName(context);

            if (TextUtils.isEmpty(displayName)) {
                displayName = context.getString(R.string.title_sim);
            }
        }

        return displayName;
    }

    public static long insertSimContactToPhoneDb(
            Context context, String name, String phoneNumber,
            String anr, String email, int slot) {

        ArrayList<ContentProviderOperation> ops = new ArrayList<ContentProviderOperation>();

        final String accountName;
        if (SimUtil.MULTISIM_ENABLE) {
            accountName = (slot == SimUtil.SLOT_ID_1) ? SIM_ACCOUNT_NAME_SIM1
                    : SIM_ACCOUNT_NAME_SIM2;
        } else {
            accountName = SIM_ACCOUNT_NAME;
        }
        buildInsertSimContactToDbOps(ops, accountName, name, phoneNumber, anr, email);

        ContentProviderResult[] results = null;
        try {
            results = context.getContentResolver().applyBatch(ContactsContract.AUTHORITY, ops);
            if ((results != null) && (results.length > 0)) {
                return Long.parseLong(results[0].uri.getLastPathSegment());
            }
        } catch (RemoteException e) {
            Log.e(TAG, "insertSimContactToPhoneDb RemoteException", e);
        } catch (OperationApplicationException e) {
            Log.e(TAG, "insertSimContactToPhoneDb OperationApplicationException", e);
        } catch (NumberFormatException nfe) {
            Log.w(TAG, "insertSimContactToPhoneDb: can NOT parse raw contact id from result.", nfe);
        }
        return -1;
    }

    public static void buildInsertSimContactToDbOps(List<ContentProviderOperation> ops,
            String accountName, String name, String phoneNumber, String anrs, String emails) {
        ContentProviderOperation.Builder builder = ContentProviderOperation
                .newInsert(RawContacts.CONTENT_URI);

        int backReferenceIndex = ops.size();

        builder.withValue(RawContacts.ACCOUNT_TYPE, SIM_ACCOUNT_TYPE);
        builder.withValue(RawContacts.ACCOUNT_NAME, accountName);

        builder.withValue(RawContacts.AGGREGATION_MODE, RawContacts.AGGREGATION_MODE_DISABLED);
        ops.add(builder.build());

        if (!TextUtils.isEmpty(name)) {
            ops.add(ContentProviderOperation.newInsert(Data.CONTENT_URI)
                    .withValueBackReference(StructuredName.RAW_CONTACT_ID, backReferenceIndex)
                    .withValue(Data.MIMETYPE, StructuredName.CONTENT_ITEM_TYPE)
                    .withValue(StructuredName.DISPLAY_NAME, name).build());
        }

        if (!TextUtils.isEmpty(phoneNumber)) {
            ops.add(ContentProviderOperation.newInsert(Data.CONTENT_URI)
                    .withValueBackReference(Phone.RAW_CONTACT_ID, backReferenceIndex)
                    .withValue(Data.MIMETYPE, Phone.CONTENT_ITEM_TYPE)
                    .withValue(Phone.TYPE, Phone.TYPE_MOBILE)
                    .withValue(Phone.NUMBER, phoneNumber)
                    .withValue(Data.IS_PRIMARY, 1).build());
        }

        if (!TextUtils.isEmpty(anrs)) {
            ops.add(ContentProviderOperation.newInsert(Data.CONTENT_URI)
                    .withValueBackReference(Phone.RAW_CONTACT_ID, backReferenceIndex)
                    .withValue(Data.MIMETYPE, Phone.CONTENT_ITEM_TYPE)
                    .withValue(Phone.TYPE, Phone.TYPE_HOME)
                    .withValue(Phone.NUMBER, anrs).build());
        }

        if (!TextUtils.isEmpty(emails)) {
            ops.add(ContentProviderOperation.newInsert(Data.CONTENT_URI)
                    .withValueBackReference(Email.RAW_CONTACT_ID, backReferenceIndex)
                    .withValue(Data.MIMETYPE, Email.CONTENT_ITEM_TYPE)
                    .withValue(Email.TYPE, Email.TYPE_WORK)
                    .withValue(Email.DATA, emails).build());
        }

        /* NOTE: do NOT save photo for sim contact in phone db.
         * Or the photo will be exported to vcf or shared via alishare app.
         * The sim contact icon shall be only used when it is really a sim contact.
         * So when it is copied to other storage, never combine with a photo.
        byte[] photo = null;
        if (SimUtil.MULTISIM_ENABLE) {
            photo = (subscription == SimUtil.SUBSCRIPTION_1) ? getSIM1Photo(context)
                    : getSIM2Photo(context);
        } else {
            photo = getSIMPhoto(context);
        }
        builder = ContentProviderOperation.newInsert(Data.CONTENT_URI);
        builder.withValueBackReference(Photo.RAW_CONTACT_ID, backReferenceIndex);
        builder.withValue(Data.MIMETYPE, Photo.CONTENT_ITEM_TYPE);
        builder.withValue(Photo.PHOTO, photo);
        builder.withValue(Photo.IS_PRIMARY, 1);
        ops.add(builder.build());
        */

    }

    public static int getPhoneContactCount() {
        return mPhoneContactCount;
    }

    public static void setPhoneContactCount(int count) {
        mPhoneContactCount = count;
    }

    public static void launch(Context context, int actionMode) {
        if (!SimUtil.MULTISIM_ENABLE
                && actionMode == SIM_MULTI_SELECT_ACTION_IMPORT_FROM_SIM) {
            Intent intent = new Intent(ACTION_IMPORT_CONTACTS);
            try {
                context.startActivity(intent);
            } catch (Exception e) {
                Log.e(TAG, "launch(ACTION_IMPORT_CONTACTS) Exception", e);
            }
            return;
        }

        Intent intent = new Intent(context, SimMultiSelectActivity.class);
        intent.putExtra(SIM_MULTI_SELECT_ACTION, actionMode);
        context.startActivity(intent);
    }

    /**
     * Update one sim contacts to database. we compare SIM records with SIM
     * contacts values, if different, we update it.
     */
    public static boolean actuallyUpdateOneSimContactForEditor(ContentResolver resolver,
            final ContentValues before, final ContentValues after,
            long rawContactId, int slot) {
        final ArrayList<ContentProviderOperation> operationList =
                new ArrayList<ContentProviderOperation>();
        ContentProviderOperation.Builder builder;

        builder = buildDiff(before, after, SimUtil.SIM_NAME, rawContactId);
        if (builder != null) {
            operationList.add(builder.build());
        }

        builder = buildDiff(before, after, SimUtil.SIM_NUMBER, rawContactId);
        if (builder != null) {
            operationList.add(builder.build());
        }

        builder = buildDiff(before, after, SimUtil.SIM_ANR, rawContactId);
        if (builder != null) {
            operationList.add(builder.build());
        }

        builder = buildDiff(before, after, SimUtil.SIM_EMAILS, rawContactId);
        if (builder != null) {
            operationList.add(builder.build());
        }

        Log.d(TAG, "actuallyUpdateOneSimContact() update on slot" + slot);

        try {
            resolver.applyBatch(ContactsContract.AUTHORITY, operationList);
            return true;
        } catch (RemoteException e) {
            Log.e(TAG, "RemoteException:" + e, e);
        } catch (OperationApplicationException e) {
            Log.e(TAG, "OperationApplicationException:" + e, e);
        }
        return false;
    }

    private static ContentProviderOperation.Builder buildDiff(ContentValues before,
            ContentValues after, String key, long rawContactId) {
        Builder builder = null;
        if (isInserted(before, after, key)) {
            if (SimUtil.SIM_NAME.equals(key)) {
                builder = buildInsertedName(after, rawContactId);
            } else if (SimUtil.SIM_NUMBER.equals(key)) {
                builder = buildInsertedNumber(after, rawContactId);
            } else if (SimUtil.SIM_ANR.equals(key)) {
                builder = buildInsertedAnr(after, rawContactId);
            } else if (SimUtil.SIM_EMAILS.equals(key)) {
                builder = buildInsertedEmail(after, rawContactId);
            }
        } else if (isDeleted(before, after, key)) {
            if (SimUtil.SIM_NAME.equals(key)) {
                builder = buildDeletedName(after, rawContactId);
            } else if (SimUtil.SIM_NUMBER.equals(key)) {
                builder = buildDeletedNumber(after, rawContactId);
            } else if (SimUtil.SIM_ANR.equals(key)) {
                builder = buildDeletedAnr(after, rawContactId);
            } else if (SimUtil.SIM_EMAILS.equals(key)) {
                builder = buildDeletedEmail(after, rawContactId);
            }
        } else if (isUpdated(before, after, key)) {
            if (SimUtil.SIM_NAME.equals(key)) {
                builder = buildUpdatedName(after, rawContactId);
            } else if (SimUtil.SIM_NUMBER.equals(key)) {
                builder = buildUpdatedNumber(after, rawContactId);
            } else if (SimUtil.SIM_ANR.equals(key)) {
                builder = buildUpdatedAnr(after, rawContactId);
            } else if (SimUtil.SIM_EMAILS.equals(key)) {
                builder = buildUpdatedEmail(after, rawContactId);
            }
        }
        return builder;
    }

    private static boolean isInserted(final ContentValues before, final ContentValues after,
            String key) {
        return TextUtils.isEmpty(before.getAsString(key))
                && !TextUtils.isEmpty(after.getAsString(key));
    }

    private static boolean isDeleted(final ContentValues before, final ContentValues after,
            String key) {
        return !TextUtils.isEmpty(before.getAsString(key))
                && TextUtils.isEmpty(after.getAsString(key));
    }

    private static boolean isUpdated(final ContentValues before, final ContentValues after,
            String key) {
        final String beforeValue = before.getAsString(key);
        final String afterValue = after.getAsString(key);

        return beforeValue != null && afterValue != null && !beforeValue.equals(afterValue);
    }

    private static Builder buildInsertedName(final ContentValues after, final long rawContactId) {
        Builder builder = ContentProviderOperation.newInsert(Data.CONTENT_URI);
        builder.withValue(StructuredName.RAW_CONTACT_ID, rawContactId);
        builder.withValue(Data.MIMETYPE, StructuredName.CONTENT_ITEM_TYPE);
        builder.withValue(StructuredName.DISPLAY_NAME, after.getAsString(SimUtil.SIM_NAME));
        return builder;
    }

    private static Builder buildInsertedNumber(final ContentValues after, final long rawContactId) {
        Builder builder = ContentProviderOperation.newInsert(Data.CONTENT_URI);
        builder.withValue(Phone.RAW_CONTACT_ID, rawContactId);
        builder.withValue(Data.MIMETYPE, Phone.CONTENT_ITEM_TYPE);
        builder.withValue(Phone.TYPE, Phone.TYPE_MOBILE);
        builder.withValue(Data.IS_PRIMARY, 1);
        builder.withValue(Phone.NUMBER, after.getAsString(SimUtil.SIM_NUMBER));
        return builder;
    }

    private static Builder buildInsertedAnr(final ContentValues after, final long rawContactId) {
        Builder builder = ContentProviderOperation.newInsert(Data.CONTENT_URI);
        builder.withValue(Phone.RAW_CONTACT_ID, rawContactId);
        builder.withValue(Data.MIMETYPE, Phone.CONTENT_ITEM_TYPE);
        builder.withValue(Phone.TYPE, Phone.TYPE_HOME);
        builder.withValue(Phone.NUMBER, after.getAsString(SimUtil.SIM_ANR));
        return builder;
    }

    private static Builder buildInsertedEmail(final ContentValues after, final long rawContactId) {
        Builder builder = ContentProviderOperation.newInsert(Data.CONTENT_URI);
        builder.withValue(Email.RAW_CONTACT_ID, rawContactId);
        builder.withValue(Data.MIMETYPE, Email.CONTENT_ITEM_TYPE);
        builder.withValue(Email.TYPE, Email.TYPE_HOME);
        builder.withValue(Email.ADDRESS, after.getAsString(SimUtil.SIM_EMAILS));
        return builder;
    }

    private static Builder buildDeletedName(final ContentValues after, final long rawContactId) {
        Builder builder = ContentProviderOperation.newDelete(Data.CONTENT_URI);
        String nameSelection = StructuredName.RAW_CONTACT_ID + "=? AND " + Data.MIMETYPE + "=?";
        String[] nameSelectionArg =
                new String[] {
                        String.valueOf(rawContactId), StructuredName.CONTENT_ITEM_TYPE
                };
        builder.withSelection(nameSelection, nameSelectionArg);
        return builder;
    }

    private static Builder buildDeletedNumber(final ContentValues after, final long rawContactId) {
        Builder builder = ContentProviderOperation.newDelete(Data.CONTENT_URI);
        String selection = Phone.RAW_CONTACT_ID + "=? AND " + Data.MIMETYPE + "=? AND "
                + Phone.TYPE + "=?";
        String[] selectionArg =
                new String[] {
                        String.valueOf(rawContactId), Phone.CONTENT_ITEM_TYPE,
                        String.valueOf(Phone.TYPE_MOBILE)
                };
        builder.withSelection(selection, selectionArg);
        return builder;
    }

    private static Builder buildDeletedAnr(final ContentValues after, final long rawContactId) {
        Builder builder = ContentProviderOperation.newDelete(Data.CONTENT_URI);
        String selection = Phone.RAW_CONTACT_ID + "=? AND " + Data.MIMETYPE + "=? AND "
                + Phone.TYPE + "=?";
        String[] selectionArg =
                new String[] {
                        String.valueOf(rawContactId), Phone.CONTENT_ITEM_TYPE,
                        String.valueOf(Phone.TYPE_HOME)
                };
        builder.withSelection(selection, selectionArg);
        return builder;
    }

    private static Builder buildDeletedEmail(final ContentValues after, final long rawContactId) {
        Builder builder = ContentProviderOperation.newDelete(Data.CONTENT_URI);
        String selection = Email.RAW_CONTACT_ID + "=? AND " + Data.MIMETYPE + "=?";
        String[] selectionArg =
                new String[] {
                        String.valueOf(rawContactId), Email.CONTENT_ITEM_TYPE
                };
        builder.withSelection(selection, selectionArg);
        return builder;
    }

    private static Builder buildUpdatedName(final ContentValues after, final long rawContactId) {
        Builder builder = ContentProviderOperation.newUpdate(Data.CONTENT_URI);
        String nameSelection = StructuredName.RAW_CONTACT_ID + "=? AND " + Data.MIMETYPE + "=?";
        String[] nameSelectionArg =
                new String[] {
                        String.valueOf(rawContactId), StructuredName.CONTENT_ITEM_TYPE
                };
        builder.withSelection(nameSelection, nameSelectionArg);
        builder.withValue(StructuredName.GIVEN_NAME, null);
        builder.withValue(StructuredName.FAMILY_NAME, null);
        builder.withValue(StructuredName.PREFIX, null);
        builder.withValue(StructuredName.MIDDLE_NAME, null);
        builder.withValue(StructuredName.SUFFIX, null);
        builder.withValue(StructuredName.DISPLAY_NAME, after.getAsString(SimUtil.SIM_NAME));

        return builder;
    }

    private static Builder buildUpdatedNumber(final ContentValues after, final long rawContactId) {
        Builder builder = ContentProviderOperation.newUpdate(Data.CONTENT_URI);
        String selection = Phone.RAW_CONTACT_ID + "=? AND " + Data.MIMETYPE + "=? AND "
                + Phone.TYPE + "=?";
        String[] selectionArg =
                new String[] {
                        String.valueOf(rawContactId), Phone.CONTENT_ITEM_TYPE,
                        String.valueOf(Phone.TYPE_MOBILE)
                };
        builder.withSelection(selection, selectionArg);
        builder.withValue(Data.IS_PRIMARY, 1);
        builder.withValue(Phone.NUMBER, after.getAsString(SimUtil.SIM_NUMBER));
        return builder;
    }

    private static Builder buildUpdatedAnr(final ContentValues after, final long rawContactId) {
        Builder builder = ContentProviderOperation.newUpdate(Data.CONTENT_URI);
        String selection = Phone.RAW_CONTACT_ID + "=? AND " + Data.MIMETYPE + "=? AND "
                + Phone.TYPE + "=?";
        String[] selectionArg =
                new String[] {
                        String.valueOf(rawContactId), Phone.CONTENT_ITEM_TYPE,
                        String.valueOf(Phone.TYPE_HOME)
                };
        builder.withSelection(selection, selectionArg);
        builder.withValue(Phone.NUMBER, after.getAsString(SimUtil.SIM_ANR));
        return builder;
    }

    private static Builder buildUpdatedEmail(final ContentValues after, final long rawContactId) {
        Builder builder = ContentProviderOperation.newUpdate(Data.CONTENT_URI);
        String selection = Email.RAW_CONTACT_ID + "=? AND " + Data.MIMETYPE + "=?";
        String[] selectionArg =
                new String[] {
                        String.valueOf(rawContactId), Email.CONTENT_ITEM_TYPE
                };
        builder.withSelection(selection, selectionArg);
        builder.withValue(Email.TYPE, Email.TYPE_HOME);
        builder.withValue(Email.ADDRESS, after.getAsString(SimUtil.SIM_EMAILS));
        return builder;
    }

    public static int getContactsTotalCount(Context context) {
        int total = 0;
        Cursor cursor = null;
        try {
            cursor = context.getContentResolver().query(Contacts.CONTENT_URI, new String[] {
                    Contacts._COUNT
            }, null, null, null);

            if (cursor != null && cursor.moveToFirst()) {
                total = cursor.getInt(0);
            }
        } catch (Exception e) {
            Log.d(TAG, "getContactsTotalCount() Exception", e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        Log.d(TAG, "getContactsTotalCount() total:" + total);
        return total;
    }

    public static int getSlotIdFromCacheByRawContactId(long rawContactId) {
        SimContactCache.SimContact cached = SimContactCache.getSimContactByRawContactIdWithoutSimId(rawContactId);
        if (cached == null) {
            return SimUtil.INVALID_SLOT_ID;
        }
        return cached.slotId;
    }

    public static Intent makeEditIntentFromCachedSimContact(SimContactCache.SimContact cached) {
        Intent intent = new Intent(SimContactUtils.ACTION_EDITOR_SIM_CONTACTS);
        intent.putExtra(Data.RAW_CONTACT_ID, cached.rawContactId);
        intent.putExtra(SimUtil.SLOT_KEY, (int) cached.slotId);

        if (SimUtil.IS_PLATFORM_MTK || SimUtil.IS_PLATFORM_SPREADTRUM) {
            // it is required to cast to int when put to ContentValues.
            intent.putExtra(SimUtil.SIM_INDEX, (int) cached.simIndex);
        }

        if (!TextUtils.isEmpty(cached.name)) {
            intent.putExtra(SimUtil.SIM_NAME, cached.name);
        }

        if (!TextUtils.isEmpty(cached.number)) {
            intent.putExtra(SimUtil.SIM_NUMBER, cached.number);
        }

        if (!TextUtils.isEmpty(cached.anrs)) {
            intent.putExtra(SimUtil.SIM_ANR, cached.anrs);
        }

        if (!TextUtils.isEmpty(cached.emails)) {
            intent.putExtra(SimUtil.SIM_EMAILS, cached.emails);
        }

        return intent;
    }

    /**
     * Returns true if is a single email or single phone number provided in the {@link Intent}
     * extras bundle so that we might save them in a SIM contact.
     * Otherwise return false if there are other intent extras that require launching
     * the full editor for phone contact. Ignore extras with the key {@link Insert.NAME} because names
     * are a special case and we typically don't want to replace the name of an existing
     * contact.
     */
    public static boolean canAddToSimContact(Bundle extras) {
        if (extras == null) {
            return false;
        }

        // Copy extras because the set may be modified in the next step
        Set<String> intentExtraKeys = Sets.newHashSet();
        intentExtraKeys.addAll(extras.keySet());

        // Ignore name key because this is an existing contact.
        if (intentExtraKeys.contains(Insert.NAME)) {
            intentExtraKeys.remove(Insert.NAME);
        }

        int numIntentExtraKeys = intentExtraKeys.size();
        if (numIntentExtraKeys == 2) {
            return intentExtraKeys.contains(Insert.PHONE) && intentExtraKeys.contains(Insert.EMAIL);
        } else if (numIntentExtraKeys == 1) {
            return intentExtraKeys.contains(Insert.PHONE) ||
                    intentExtraKeys.contains(Insert.EMAIL);
        }
        // Having 0 or more than 2 intent extra keys means that we should launch
        // the full contact editor to properly handle the intent extras.
        return false;
    }

    private static final String INTENT_KEY_AIRPLANE_MODE_STATE = "state";
    public static final int OPERATION_NOTHING = 0;
    public static final int OPERATION_LOAD_SIM_CONTACTS_TO_PHONE_DB = 1;
    public static final int OPERATION_DELETE_SIM_CONTACTS_FROM_PHONE_DB = 2;

    /**
     * Check if it is time to load sim contacts to phone db.
     * For MTK, most of the time, we will get the following sequence
     * before we can access sim contacts:
     *     seq n. action: android.intent.action.SIM_STATE_CHANGED; ss: LOADED
     *     seq n+1. action: android.intent.action.PHB_STATE_CHANGED; ready: true
     * But on A800, we turn on/off the sim card from dual card settings,
     * we can only get the former one, and we can load sim contacts without the later one.
     * So to make sure we can load sim contacts correctly, load sim contacts at both intent.
     * For QCOM, we can load sim contacts at the following intent:
     *     action: android.intent.action.SIM_STATE_CHANGED; ss: LOADED
     * But on CM810, it seems the air plane mode does NOT send such intent.
     * So maybe we need to check Intent.ACTION_AIRPLANE_MODE_CHANGED on QCOM.
     *
     * When activate/deactivate sim card from settings, we will get:
     *     action: android.intent.action.SIM_STATE_CHANGED; ss: NOT_READY/ABSENT
     * @param intent
     * @param action
     * @return what operation to do.
     */
    public static int needToLoadSimContactsToPhone(Intent intent, String action) {
        if (SimUtil.IS_PLATFORM_MTK) {
            if (SimContactUtils.ACTION_PHB_STATE_CHANGED.equals(action)) {
                boolean ready = intent.getBooleanExtra(SimContactUtils.STATE_READY, false);
                Log.i(TAG, "needToLoadSimContactsToPhone: ACTION_PHB_STATE_CHANGED, ready="+ready);
                return ready ? OPERATION_LOAD_SIM_CONTACTS_TO_PHONE_DB
                        : OPERATION_DELETE_SIM_CONTACTS_FROM_PHONE_DB;
            }
            if (SimContactUtils.ACTION_SIM_STATE_CHANGED.equals(action)) {
                String state = intent.getStringExtra(SimContactUtils.INTENT_KEY_ICC_STATE);
                Log.i(TAG, "needToLoadSimContactsToPhone: ACTION_SIM_STATE_CHANGED, state="+state);
                if (SimContactUtils.INTENT_VALUE_ICC_LOADED.equals(state)) {
                    return OPERATION_LOAD_SIM_CONTACTS_TO_PHONE_DB;
                } else if (SimContactUtils.INTENT_VALUE_ICC_NOT_READY.equals(state)
                        || SimContactUtils.INTENT_VALUE_ICC_ABSENT.equals(state)
                        || SimContactUtils.INTENT_VALUE_ICC_CARD_IO_ERROR.equals(state)) {
                    return OPERATION_DELETE_SIM_CONTACTS_FROM_PHONE_DB;
                }
            }
        } else if (SimUtil.IS_PLATFORM_QCOMM) {
            if (SimContactUtils.ACTION_SIM_STATE_CHANGED.equals(action)) {
                String state = intent.getStringExtra(SimContactUtils.INTENT_KEY_ICC_STATE);
                Log.i(TAG, "needToLoadSimContactsToPhone: ACTION_SIM_STATE_CHANGED, state="+state);
                if (SimContactUtils.INTENT_VALUE_ICC_LOADED.equals(state)) {
                    return OPERATION_LOAD_SIM_CONTACTS_TO_PHONE_DB;
                } else if (SimContactUtils.INTENT_VALUE_ICC_NOT_READY.equals(state)
                        || SimContactUtils.INTENT_VALUE_ICC_ABSENT.equals(state)
                        || SimContactUtils.INTENT_VALUE_ICC_CARD_IO_ERROR.equals(state)) {
                    return OPERATION_DELETE_SIM_CONTACTS_FROM_PHONE_DB;
                }
            }
            if (Intent.ACTION_AIRPLANE_MODE_CHANGED.equals(action)) {
                if (!intent.hasExtra(INTENT_KEY_AIRPLANE_MODE_STATE)) {
                    Log.w(TAG, "needToLoadSimContactsToPhone: no airPlaneMode status in intent.");
                    return OPERATION_NOTHING;
                }
                boolean airPlaneMode = intent.getBooleanExtra(INTENT_KEY_AIRPLANE_MODE_STATE, false);
                Log.i(TAG, "needToLoadSimContactsToPhone: ACTION_AIRPLANE_MODE_CHANGED, airPlaneMode="+airPlaneMode);
                return airPlaneMode ? OPERATION_DELETE_SIM_CONTACTS_FROM_PHONE_DB
                        : OPERATION_LOAD_SIM_CONTACTS_TO_PHONE_DB;
            }
        } else if (SimUtil.IS_PLATFORM_SPREADTRUM) {
            /*
             * codebase does not have spreadtrum phone, the spreadtrum code comes from:
             * https://k3.alibaba-inc.com/issue/6737776?versionId=1234626
             */
            if (SimContactUtils.ACTION_SIM_STATE_CHANGED.equals(action)) {
                String state = intent.getStringExtra(SimContactUtils.INTENT_KEY_ICC_STATE);
                Log.i(TAG, "IS_PLATFORM_SPREADTRUM needToLoadSimContactsToPhone,state="+state);
                if (SimContactUtils.INTENT_VALUE_ICC_LOADED.equals(state)
                        || INTENT_VALUE_ICC_READY.equals(state)) {
                    return OPERATION_LOAD_SIM_CONTACTS_TO_PHONE_DB;
                } else if (SimContactUtils.INTENT_VALUE_ICC_NOT_READY.equals(state)
                        || SimContactUtils.INTENT_VALUE_ICC_ABSENT.equals(state)
                        || SimContactUtils.INTENT_VALUE_ICC_CARD_IO_ERROR.equals(state)) {
                    return OPERATION_DELETE_SIM_CONTACTS_FROM_PHONE_DB;
                }
            }
            if (Intent.ACTION_AIRPLANE_MODE_CHANGED.equals(action)) {
                if (!intent.hasExtra(INTENT_KEY_AIRPLANE_MODE_STATE)) {
                    Log.w(TAG, "needToLoadSimContactsToPhone: no airPlaneMode status in intent.");
                    return OPERATION_NOTHING;
                }
                boolean airPlaneMode = intent.getBooleanExtra(INTENT_KEY_AIRPLANE_MODE_STATE, false);
                Log.i(TAG, "needToLoadSimContactsToPhone: ACTION_AIRPLANE_MODE_CHANGED, airPlaneMode="+airPlaneMode);
                return airPlaneMode ? OPERATION_DELETE_SIM_CONTACTS_FROM_PHONE_DB
                        : OPERATION_LOAD_SIM_CONTACTS_TO_PHONE_DB;
            }
        } else {
            Log.e(TAG, "readToLoadSimContacts: the platform "+SimUtil.PLATFORM+" is NOT supported.");
        }
        return OPERATION_NOTHING;
    }

    public static int getSlotIdFromSimStateChangedIntent(Intent intent) {
        String action = intent.getAction();
        int slotId = SimUtil.INVALID_SLOT_ID;
        if (SimUtil.MULTISIM_ENABLE && SimUtil.IS_PLATFORM_MTK && ACTION_PHB_STATE_CHANGED.equals(action)) {
            int subId = intent.getIntExtra(SUBSCRIPTION_KEY, SimUtil.INVALID_SUB_ID);
            if (subId != SimUtil.INVALID_SUB_ID) {
                slotId = SimUtil.getSlotId(subId);
            } else {
                Log.e(TAG, "getSlotIdFromSimStateChangedIntent: can NOT get subId from intent "+intent);
            }
            return slotId;
        }
        if (SimUtil.MULTISIM_ENABLE) {
            slotId = intent.getIntExtra(SimUtil.SIM_STATE_CHANGE_SLOT_KEY, SimUtil.INVALID_SLOT_ID);
        }
        return slotId;
    }

    // On QCom M, when the phone is just powered up, and we call VendorSimImpl.getAnrCount(),
    // we will read wrong value on 3g/4g card.
    // If the user start sim contact edit page at this time,
    // then we might display less edit fields then sim supports.
    // So we remember if we have received LOADED state of a sim,
    // if true, then we can use the return value of VendorSimImpl.getAnrCount().
    // Otherwise, we shall notify user about this state.
    private static final boolean sSimLoaded[] = new boolean[] {
        false, false
    };
    public static void handleSimState(Intent intent) {
        if (SimUtil.IS_PLATFORM_QCOMM) {
            int slotId = SimUtil.MULTISIM_ENABLE
                    ? intent.getIntExtra(SimUtil.SIM_STATE_CHANGE_SLOT_KEY, SimUtil.INVALID_SLOT_ID)
                            : SimUtil.SLOT_ID_1;
            if (SimUtil.INVALID_SLOT_ID == slotId) {
                Log.e(TAG, "handleSimState: can NOT get valid int slotId from intent. quit.");
                return;
            }
            String state = intent.getStringExtra(SimContactUtils.INTENT_KEY_ICC_STATE);
            if (INTENT_VALUE_ICC_NOT_READY.equals(state) || INTENT_VALUE_ICC_ABSENT.equals(state)) {
                sSimLoaded[slotId] = false;
            } else if (INTENT_VALUE_ICC_LOADED.equals(state)) {
                sSimLoaded[slotId] = true;
            }
        }
    }

    public static boolean isSimLoaded(int slotId) {
        if (SimUtil.IS_PLATFORM_QCOMM) {
            if ((SimUtil.SLOT_ID_1 != slotId) && (SimUtil.SLOT_ID_2 != slotId)) {
                slotId = SimUtil.SLOT_ID_1;
            }
            return sSimLoaded[slotId];
        }
        return true;
    }

    public static void observeVolteAttachChangeByPlatform(Context context, Handler handler, int what) {
        if (SimUtil.IS_PLATFORM_SPREADTRUM) {
            SimUtil.observeVolteAttachChanged(context, handler, what);
        }
    }

    public static void unObserveVolteAttachChangedByPlatform(Context context) {
        if (SimUtil.IS_PLATFORM_SPREADTRUM) {
            SimUtil.unObserveVolteAttachChanged(context);
        }
    }

    public static final String MTK_EXTRA_VOLTE_CONF_CALL_DIAL = "com.mediatek.volte.ConfCallDial";
    public static final String MTK_EXTRA_VOLTE_CONF_CALL_NUMBERS = "com.mediatek.volte.ConfCallNumbers";
    public static final String SPRD_EXTRA_PLACE_GROUP_CALL = "com.sprd.phone.placeGroupGall";
    public static final String SPRD_EXTRA_GROUP_CALL_LIST = "com.sprd.phone.groupGallList";

    public static Intent makeConferenceCallIntent(Context context, ArrayList<String> numbers) {
        Intent confCallIntent = null;
        if (SimUtil.IS_PLATFORM_MTK) {
            confCallIntent = CallUtil.getCallIntent(context, numbers.get(0), null, null);
            confCallIntent.putExtra(MTK_EXTRA_VOLTE_CONF_CALL_DIAL, true);
            confCallIntent.putStringArrayListExtra(MTK_EXTRA_VOLTE_CONF_CALL_NUMBERS, numbers);
        } else if (SimUtil.IS_PLATFORM_SPREADTRUM) {
            confCallIntent = new Intent(Intent.ACTION_CALL_PRIVILEGED);
            StringBuilder numbersBuilder = new StringBuilder();
            numbersBuilder.append("tel:");
            for (String number : numbers) {
                numbersBuilder.append(number).append(',');
            }
            confCallIntent.setData(Uri.parse(numbersBuilder.toString()));
            confCallIntent.putExtra(SPRD_EXTRA_PLACE_GROUP_CALL, true);
            confCallIntent.putExtra(SPRD_EXTRA_GROUP_CALL_LIST, numbers);
        }
        return confCallIntent;
    }

    /* YUNOS BEGIN PB */
    //##email:caixiang.zcx@alibaba-inc.com
    //##BugID:(8206447) ##date:2016/05/12
    //##description:get the capacity of sim card
    public static int getSimContactsCount(Context context, int slotId) {
        int count = 0;
        Cursor cursor = SimUtil.query(context,slotId);
        if (cursor != null) {
            Log.d(TAG, "getSimContactsCount = " + cursor.getCount());
            count = cursor.getCount();
            cursor.close();
            return count;
        }
        return count;
    }
    /* YUNOS END PB */
}
