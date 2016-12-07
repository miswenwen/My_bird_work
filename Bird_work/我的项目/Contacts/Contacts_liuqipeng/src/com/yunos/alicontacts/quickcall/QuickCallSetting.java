
package com.yunos.alicontacts.quickcall;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.database.Cursor;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.PhoneLookup;
import android.telephony.PhoneNumberUtils;
import android.text.TextUtils;
import android.util.Log;
import android.util.Pair;
import android.view.KeyEvent;

import com.yunos.alicontacts.sim.SimUtil;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

public class QuickCallSetting {

    public static final String TAG = QuickCallSetting.class.getSimpleName();

    private static QuickCallSetting mQuickCallSetting;
    private SharedPreferences mPrefs;
    private HashMap<Integer, String> mSpeedDialMap = new HashMap<Integer, String>();
    private HashMap<Integer, Integer> mDefaultSimMap = new HashMap<Integer, Integer>();
    private static final String PREFIX_NUM = "SpeedNum_";
    private static final String PREFIX_SIM = "SpeedSim_";
    private final boolean mIsMultiSim;
    public static final String EXTRAPOS = "pos";
    public static final String PICMETHOD = "picmethod";

    private static final int[] mKeyCodeArray = new int[] {
            KeyEvent.KEYCODE_1, KeyEvent.KEYCODE_2, KeyEvent.KEYCODE_3, KeyEvent.KEYCODE_4,
            KeyEvent.KEYCODE_5, KeyEvent.KEYCODE_6, KeyEvent.KEYCODE_7, KeyEvent.KEYCODE_8,
            KeyEvent.KEYCODE_9
    };

    private QuickCallSetting(Context c) {
        mPrefs = PreferenceManager.getDefaultSharedPreferences(c);
        mIsMultiSim = SimUtil.MULTISIM_ENABLE;
        initQuickCallSetting();
    }

    private void initQuickCallSetting() {
        synchronized (mSpeedDialMap) {
            for (int i = 0; i < mKeyCodeArray.length; i++) {
                String key = PREFIX_NUM + String.valueOf(mKeyCodeArray[i]);
                String phoneNum = mPrefs.getString(key, null);
                if (!TextUtils.isEmpty(phoneNum)) {
                    mSpeedDialMap.put(mKeyCodeArray[i], phoneNum);
                }
            }
        }
        if (mIsMultiSim) {
            synchronized (mDefaultSimMap) {
                for (int i = 0; i < mKeyCodeArray.length; i++) {
                    String key = PREFIX_SIM + String.valueOf(mKeyCodeArray[i]);
                    int defaultSim = mPrefs.getInt(key, -1);
                    mDefaultSimMap.put(mKeyCodeArray[i], defaultSim);
                }
            }
        }
    }

    public static QuickCallSetting getQuickCallInstance(Context c) {
        if (mQuickCallSetting == null) {
            mQuickCallSetting = new QuickCallSetting(c);
        }
        return mQuickCallSetting;
    }

    public String getPhoneNumber(int pos) {
        synchronized (mSpeedDialMap) {
            return mSpeedDialMap.get(pos);
        }
    }

    public void addQuickDialSetting(int pos, String num) {
        if (!TextUtils.isEmpty(num)) {
            synchronized (mSpeedDialMap) {
                String key = PREFIX_NUM + pos;
                mSpeedDialMap.put(pos, num);
                Editor e = mPrefs.edit();
                e.putString(key, num);
                e.commit();
            }
        }
    }

    public void deleteQuickDialSetting(int pos) {
        String key = PREFIX_NUM + pos;
        synchronized (mSpeedDialMap) {
            mSpeedDialMap.remove(pos);
        }
        synchronized (mDefaultSimMap) {
            mDefaultSimMap.remove(pos);
        }
        Editor e = mPrefs.edit();
        e.remove(key);
        if (mIsMultiSim) {
            key = PREFIX_SIM + pos;
            e.remove(key);
        }
        e.commit();
    }

    public void setDefaultQuickDialSim(int pos, int simid) {
        if (mIsMultiSim) {
            synchronized (mDefaultSimMap) {
                mDefaultSimMap.put(pos, simid);
            }
            String key = PREFIX_SIM + pos;
            Editor e = mPrefs.edit();
            e.putInt(key, simid);
            e.commit();
        }
    }

    public Pair<String, String> getNameAndPhoneNumber(Context context, char keyChar) {
        int numberKey = keyChar - '0' + KeyEvent.KEYCODE_0;
        String number = getPhoneNumber(numberKey);
        Pair<String, String> nameAndPhoneNumber = Pair.create("", number);
        if (!TextUtils.isEmpty(number)) {
            Cursor phoneCursor = null;
            Uri phoneUri = Uri.withAppendedPath(PhoneLookup.CONTENT_FILTER_URI, Uri.encode(number));
            try {
                phoneCursor = context.getContentResolver().query(phoneUri,
                        new String[] {Phone.DISPLAY_NAME}, null, null, null);

                if (phoneCursor != null && phoneCursor.moveToFirst()) {
                    nameAndPhoneNumber = Pair.create(phoneCursor.getString(0), number);
                /* YUNOS BEGIN PB */
                //##email:caixiang.zcx@alibaba-inc.com
                //##BugID:(5883538) ##date:2015/05/14
                //##description:add edit contact function when set the quick call
                } else {
                    nameAndPhoneNumber = Pair.create("", number);
                }
                /* YUNOS END PB */
            } catch (Exception e) {
                Log.e(TAG, "phone query exception", e);
            } finally {
                if (phoneCursor != null) {
                    phoneCursor.close();
                }
            }
        }
        return nameAndPhoneNumber;
    }

    public boolean hasQuickCallByNumber(String num, int pos) {
        synchronized (mSpeedDialMap) {
            Iterator<Entry<Integer, String>> iter = mSpeedDialMap.entrySet().iterator();
            while (iter.hasNext()) {
                Map.Entry<Integer, String> entry = (Map.Entry<Integer, String>) iter.next();
                if ((Integer) entry.getKey() == pos) {
                    continue;
                }
                String phoneNum = (String) entry.getValue();
                if (PhoneNumberUtils.compare(num, phoneNum)) {
                    return true;
                }
            }
            return false;
        }
    }

    public int getDefaultQuickDialSim(int pos) {
        synchronized (mDefaultSimMap) {
            if (mDefaultSimMap.containsKey(pos)) {
                return mDefaultSimMap.get(pos);
            } else {
                return -1;
            }
        }
    }

}
