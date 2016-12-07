package com.yunos.alicontacts.util;

import android.text.TextUtils;

public final class AliTextUtils {

    private AliTextUtils() { }

    private static final String CN_CITY_EQUALS_EXCEPTION = "吉林";
    public static String makeLocation(String prov, String area) {
        if (TextUtils.isEmpty(prov)) {
            if (area == null) {
                return "";
            }
            return area;
        }
        if (TextUtils.isEmpty(area)) {
            return prov;
        }
        // This is for the case: province=="北京" and area=="北京",
        // we will use only one of province / area, except the values are both "吉林".
        if (prov.equals(area) && (!CN_CITY_EQUALS_EXCEPTION.equals(area))
                && (!FeatureOptionAssistant.isInternationalSupportted())) {
            return prov;
        }
        return prov + area;
    }

    public static CharSequence desensitizeNumber(CharSequence number) {
        if (TextUtils.isEmpty(number)) {
            return "[empty]";
        }
        int len = number.length();
        if (len <= 4) {
            return number;
        }
        return "**" + number.subSequence(len - 4, len);
    }

    public static boolean equalsLoosely(String s1, String s2) {
        // compare same instance for quick return.
        if (s1 == s2) {
            return true;
        }
        if (TextUtils.isEmpty(s1)) {
            return TextUtils.isEmpty(s2);
        } else if (TextUtils.isEmpty(s2)) {
            return false;
        }
        return TextUtils.equals(s1.trim(), s2.trim());
    }

    public static int getHash(Object obj) {
        return obj == null ? 0 : obj.hashCode();
    }

}
