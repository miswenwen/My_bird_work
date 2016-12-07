/*
 * Copyright (C) 2009 The Android Open Source Project
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

package com.yunos.alicontacts;

import android.content.Context;
import android.content.Intent;
import android.location.Country;
import android.location.CountryDetector;
import android.net.Uri;
import android.os.storage.StorageManager;
import android.os.storage.StorageVolume;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds.Im;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.Contacts;
import android.telephony.PhoneNumberUtils;
import android.text.TextUtils;
import android.util.Log;
import android.util.Pair;
import android.view.View;
import android.widget.TextView;

import com.aliyun.ams.tyid.TYIDConstants;
import com.aliyun.ams.tyid.TYIDManager;
import com.yunos.alicontacts.model.AccountTypeManager;
import com.yunos.alicontacts.model.account.AccountType;
import com.yunos.alicontacts.model.account.AccountWithDataSet;
import com.yunos.alicontacts.model.dataitem.ImDataItem;
import com.yunos.alicontacts.test.NeededForTesting;

import java.io.File;
import java.util.List;


public class ContactsUtils {
    private static final String TAG = "ContactsUtils";

    // Telecomm related schemes are in CallUtil
    public static final String SCHEME_IMTO = "imto";
    public static final String SCHEME_MAILTO = "mailto";
    public static final String SCHEME_SMSTO = "smsto";

    public static final String MMS_PACKAGE = "com.android.mms";
    public static final String MMS_COMPOSE_ACTIVITY_NAME = "com.android.mms.ui.ComposeMessageActivity";

    // TODO find a proper place for the canonical version of these
    public interface ProviderNames {
        String YAHOO = "Yahoo";
        String GTALK = "GTalk";
        String MSN = "MSN";
        String ICQ = "ICQ";
        String AIM = "AIM";
        String XMPP = "XMPP";
        String JABBER = "JABBER";
        String SKYPE = "SKYPE";
        String QQ = "QQ";
    }

    /**
     * This looks up the provider name defined in
     * ProviderNames from the predefined IM protocol id.
     * This is used for interacting with the IM application.
     *
     * @param protocol the protocol ID
     * @return the provider name the IM app uses for the given protocol, or null if no
     * provider is defined for the given protocol
     * @hide
     */
    public static String lookupProviderNameFromId(int protocol) {
        switch (protocol) {
            case Im.PROTOCOL_GOOGLE_TALK:
                return ProviderNames.GTALK;
            case Im.PROTOCOL_AIM:
                return ProviderNames.AIM;
            case Im.PROTOCOL_MSN:
                return ProviderNames.MSN;
            case Im.PROTOCOL_YAHOO:
                return ProviderNames.YAHOO;
            case Im.PROTOCOL_ICQ:
                return ProviderNames.ICQ;
            case Im.PROTOCOL_JABBER:
                return ProviderNames.JABBER;
            case Im.PROTOCOL_SKYPE:
                return ProviderNames.SKYPE;
            case Im.PROTOCOL_QQ:
                return ProviderNames.QQ;
        }
        return null;
    }

    /**
     * Test if the given {@link CharSequence} contains any graphic characters,
     * first checking {@link TextUtils#isEmpty(CharSequence)} to handle null.
     */
    public static boolean isGraphic(CharSequence str) {
        return !TextUtils.isEmpty(str) && isGraphicInternal(str);
    }

    /**
     * This method is copied from TextUtils.isGraphic(),
     * except we do not check type Character.SURROGATE.
     * Because the emoji characters are in type Character.SURROGATE,
     * and these characters are printable.
     * @param str
     * @return
     */
    private static boolean isGraphicInternal(CharSequence str) {
        final int len = str.length();
        for (int i=0; i<len; i++) {
            int gc = Character.getType(str.charAt(i));
            if (gc != Character.CONTROL
                    && gc != Character.FORMAT
                    // && gc != Character.SURROGATE
                    && gc != Character.UNASSIGNED
                    && gc != Character.LINE_SEPARATOR
                    && gc != Character.PARAGRAPH_SEPARATOR
                    && gc != Character.SPACE_SEPARATOR) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns true if two objects are considered equal.  Two null references are equal here.
     */
    @NeededForTesting
    public static boolean areObjectsEqual(Object a, Object b) {
        return a == b || (a != null && a.equals(b));
    }

    /**
     * Returns true if two {@link Intent}s are both null, or have the same action.
     */
    public static final boolean areIntentActionEqual(Intent a, Intent b) {
        if (a == b) {
            return true;
        }
        if (a == null || b == null) {
            return false;
        }
        return TextUtils.equals(a.getAction(), b.getAction());
    }

    public static boolean areContactWritableAccountsAvailable(Context context) {
        final List<AccountWithDataSet> accounts =
                AccountTypeManager.getInstance(context).getAccounts(true /* writeable */);
        return !accounts.isEmpty();
    }

    public static boolean areGroupWritableAccountsAvailable(Context context) {
        final List<AccountWithDataSet> accounts =
                AccountTypeManager.getInstance(context).getGroupWritableAccounts();
        return !accounts.isEmpty();
    }

    private static Intent getCustomImIntent(ImDataItem im, int protocol) {
        String host = im.getCustomProtocol();
        final String data = im.getData();
        if (TextUtils.isEmpty(data)) {
            return null;
        }
        if (protocol != Im.PROTOCOL_CUSTOM) {
            // Try bringing in a well-known host for specific protocols
            host = ContactsUtils.lookupProviderNameFromId(protocol);
        }
        if (TextUtils.isEmpty(host)) {
            return null;
        }
        final String authority = host.toLowerCase();
        final Uri imUri = new Uri.Builder().scheme(SCHEME_IMTO).authority(
                authority).appendPath(data).build();
        final Intent intent = new Intent(Intent.ACTION_SENDTO, imUri);
        return intent;
    }

    /**
     * Returns the proper Intent for an ImDatItem. If available, a secondary intent is stored
     * in the second Pair slot
     */
    public static Pair<Intent, Intent> buildImIntent(Context context, ImDataItem im) {
        Intent intent = null;
        Intent secondaryIntent = null;
        final boolean isEmail = im.isCreatedFromEmail();

        if (!isEmail && !im.isProtocolValid()) {
            return Pair.create(null, null);
        }

        final String data = im.getData();
        if (TextUtils.isEmpty(data)) {
            return Pair.create(null, null);
        }

        final int protocol = isEmail ? Im.PROTOCOL_GOOGLE_TALK : im.getProtocol();

        if (protocol == Im.PROTOCOL_GOOGLE_TALK) {
            final int chatCapability = im.getChatCapability();
            if ((chatCapability & Im.CAPABILITY_HAS_CAMERA) != 0) {
                intent = new Intent(Intent.ACTION_SENDTO,
                                Uri.parse("xmpp:" + data + "?message"));
                secondaryIntent = new Intent(Intent.ACTION_SENDTO,
                        Uri.parse("xmpp:" + data + "?call"));
            } else if ((chatCapability & Im.CAPABILITY_HAS_VOICE) != 0) {
                // Allow Talking and Texting
                intent =
                    new Intent(Intent.ACTION_SENDTO, Uri.parse("xmpp:" + data + "?message"));
                secondaryIntent =
                    new Intent(Intent.ACTION_SENDTO, Uri.parse("xmpp:" + data + "?call"));
            } else {
                intent =
                    new Intent(Intent.ACTION_SENDTO, Uri.parse("xmpp:" + data + "?message"));
            }
        } else {
            // Build an IM Intent
            intent = getCustomImIntent(im, protocol);
        }
        return Pair.create(intent, secondaryIntent);
    }

    // The following lines are provided and maintained by Mediatek Inc.
    // empty sim id
    public static final int CALL_TYPE_NONE = 0;
    // sim id of sip call in the call log database
    public static final int CALL_TYPE_SIP = -2;
    public static boolean[] isServiceRunning = {false, false};

    private static String sCurrentCountryIso;

    /**
     * this method is used only in the onCreate of DialtactsActivity
     * @return
     */
    public static final String getCurrentCountryIso(Context context) {
        if (sCurrentCountryIso == null) {
            CountryDetector detector = (CountryDetector) context.getSystemService(Context.COUNTRY_DETECTOR);
            if (detector != null) {
                if (detector.detectCountry() != null)
                    sCurrentCountryIso = detector.detectCountry().getCountryIso();
            }
        }

        return sCurrentCountryIso;
    }

    // non-synchronized shall be work, because in concurrent scenario,
    // we will have a background call and a onReceive call.
    // At this time, we will get a correct country iso in either call.
    private static final void setCurrentCountryIsoInternal(Context context) {
        CountryDetector detector = (CountryDetector) context.getSystemService(Context.COUNTRY_DETECTOR);
        if (detector != null) {
            Country country = detector.detectCountry();
            if (country != null) {
                sCurrentCountryIso = country.getCountryIso();
            }
        }
    }

    public static final void updateCurrentCountryIso(Context context) {
        // If sCurrentCountryIso is null, then we can leave it untouched to the first time use it.
        if (sCurrentCountryIso != null) {
            setCurrentCountryIsoInternal(context);
        }
    }

    public static final String formatPhoneNumberWithCurrentCountryIso(String number, Context context) {
        if (number == null) {
            return "";
        }

        if ((sCurrentCountryIso == null) && (context == null)) {
            return number;
        }
        String countryIso = getCurrentCountryIso(context);
        String result = PhoneNumberUtils.formatNumber(number, countryIso);
        if (result == null) {
            return number;
        }
        return result;
    }

    // The previous lines are provided and maintained by Mediatek Inc.

    /**
     * *************************************************************************
     * ******** The code above is copied from orginal codebase_mtk_r2
     * ContactsCommon module. The code below is copied from codebase_mtk_r1
     * Contacts module.
     * *************************************************************************
     */

    public static Intent getAddContactIntent(Context context, Class<?> clazz, String number) {
        Intent createIntent = new Intent(context, clazz);
        createIntent.setAction(ContactsContract.Intents.Insert.ACTION);
        createIntent.putExtra(ContactsContract.Intents.Insert.PHONE, number);
        return createIntent;
    }

    public static Intent getInsertContactIntent(Context context, Class<?> clazz, String number) {
        Intent createIntent2 = new Intent(context, clazz);
        createIntent2.setAction(Intent.ACTION_INSERT_OR_EDIT);
        createIntent2.setType(Contacts.CONTENT_ITEM_TYPE);
        createIntent2.putExtra(ContactsContract.Intents.Insert.PHONE, number);
        return createIntent2;
    }

    public static Intent getViewContactIntent(Context context, Class<?> clazz, Uri uri) {
        Intent intent = new Intent(context, clazz);
        intent.setAction(Intent.ACTION_VIEW);
        intent.setData(uri);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT);
        return intent;
    }

    public static Intent getSendSmsIntent(String number) {
        Intent intent = new Intent();
        intent.setClassName(MMS_PACKAGE, MMS_COMPOSE_ACTIVITY_NAME);
        intent.setAction(Intent.ACTION_SENDTO);
        intent.setData(Uri.fromParts("sms", number, null));
        return intent;
    }

    /**
     * Returns true if two data with mimetypes which represent values in contact
     * entries are considered equal for collapsing in the GUI. For caller-id,
     * use {@link PhoneNumberUtils#compare(Context, String, String)} instead
     */
    public static final boolean shouldCollapse(CharSequence mimetype1, CharSequence data1, CharSequence mimetype2,
            CharSequence data2) {
        // different mimetypes? don't collapse
        if (!TextUtils.equals(mimetype1, mimetype2))
            return false;

        // exact same string? good, bail out early
        if (TextUtils.equals(data1, data2))
            return true;

        // so if either is null, these two must be different
        if (data1 == null || data2 == null)
            return false;

        // if this is not about phone numbers, we know this is not a match (of course, some
        // mimetypes could have more sophisticated matching is the future, e.g. addresses)
        if (!TextUtils.equals(Phone.CONTENT_ITEM_TYPE, mimetype1))
            return false;

        return shouldCollapsePhoneNumbers(data1.toString(), data2.toString());
    }

    private static final boolean shouldCollapsePhoneNumbers(String number1WithLetters, String number2WithLetters) {
        final String number1 = PhoneNumberUtils.convertKeypadLettersToDigits(number1WithLetters);
        final String number2 = PhoneNumberUtils.convertKeypadLettersToDigits(number2WithLetters);

        int index1 = 0;
        int index2 = 0;
        for (;;) {
            // Skip formatting characters.
            while (index1 < number1.length() && !PhoneNumberUtils.isNonSeparator(number1.charAt(index1))) {
                index1++;
            }
            while (index2 < number2.length() && !PhoneNumberUtils.isNonSeparator(number2.charAt(index2))) {
                index2++;
            }
            // If both have finished, match. If only one has finished, not
            // match.
            final boolean number1End = (index1 == number1.length());
            final boolean number2End = (index2 == number2.length());
            if (number1End) {
                return number2End;
            }
            if (number2End)
                return false;

            // If the non-formatting characters are different, not match.
            if (number1.charAt(index1) != number2.charAt(index2))
                return false;

            // Go to the next characters.
            index1++;
            index2++;
        }
    }

    /**
     * Returns a header view based on the R.layout.list_separator, where the
     * containing {@link TextView} is set using the given textResourceId.
     */
    public static View createHeaderView(Context context, int textResourceId) {
        View view = View.inflate(context, R.layout.list_separator, null);
        TextView textView = (TextView) view.findViewById(R.id.title);
        textView.setText(context.getString(textResourceId));
        return view;
    }

    /**
     * Returns the intent to launch for the given invitable account type and
     * contact lookup URI. This will return null if the account type is not
     * invitable (i.e. there is no
     * {@link AccountType#getInviteContactActivityClassName()} or
     * {@link AccountType#syncAdapterPackageName}).
     */
    public static Intent getInvitableIntent(AccountType accountType, Uri lookupUri) {
        String syncAdapterPackageName = accountType.syncAdapterPackageName;
        String className = accountType.getInviteContactActivityClassName();
        if (TextUtils.isEmpty(syncAdapterPackageName) || TextUtils.isEmpty(className)) {
            return null;
        }
        Intent intent = new Intent();
        intent.setClassName(syncAdapterPackageName, className);

        intent.setAction(ContactsContract.Intents.INVITE_CONTACT);

        // Data is the lookup URI.
        intent.setData(lookupUri);
        return intent;
    }

    public static boolean getLoginState(Context context) {
        TYIDManager tidm = null;
        try {
            tidm = TYIDManager.get(context);
            return tidm.yunosGetLoginState() == TYIDConstants.EYUNOS_SUCCESS;
        } catch (Exception e) {
            Log.e(TAG, "getLoginState ", e);
        }
        return false;
    }

    public static String getLoginName(Context context) {
        TYIDManager tidm = null;
        try {
            tidm = TYIDManager.get(context);
            return tidm.yunosGetLoginId();
        } catch (Exception e) {
            Log.e(TAG, "getLoginName ", e);
        }
        return null;
    }

    public static String getLoginId(Context context) {
        TYIDManager tidm = null;
        try {
            tidm = TYIDManager.get(context);
            return tidm.yunosGetKP();
        } catch (Exception e) {
            Log.e(TAG, "getLoginId ", e);
        }
        return null;
    }

    /* Internal SDCard */
    public static String getInternalStorageDirectory(Context context) {
        return getStorageDirectory(context, true);
    }

    /* External SDcard*/
    public static String getExternalStorageDirectory(Context context) {
        return getStorageDirectory(context, false);
    }

    public static String getStorageDirectory(Context context, boolean isInternal) {
        String directory = null;
        String directoryTemp = null;
        StorageManager storageManager = (StorageManager)context.getSystemService(Context.STORAGE_SERVICE);
        StorageVolume[] storageVolumes = storageManager.getVolumeList();

        for (StorageVolume storageVolume : storageVolumes) {
            if (storageVolume.isRemovable() && !isInternal) {
                directoryTemp = storageVolume.getPath();
            } else if (!storageVolume.isRemovable() && isInternal) {
                directoryTemp = storageVolume.getPath();
            }
            if (directoryTemp != null && new File(directoryTemp).canWrite()) {
                directory = directoryTemp;
                break;
            }
        }

        return directory;
    }
}
