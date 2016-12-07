
package com.yunos.alicontacts.plugin.duplicateremove;

import android.text.TextUtils;

import java.util.HashSet;
import java.util.Set;

public class Utils {
    public final static String TAG = Utils.class.getSimpleName();
    public static final boolean DBG = true;

    public static long hash(String s) {
        long seed = 131; // 31 131 1313 13131 131313 etc.. BKDRHash
        long hash = 0;

        if (s == null) {
            return hash;
        }

        int len = s.length();
        for (int i = 0; i < len; i++) {
            hash = (hash * seed) + s.charAt(i);
        }

        return hash;
    }

    public static String phoneNormalize(String number) {
        if (number == null || number.length() == 0)
            return number;

        // ???if phone numbers compare with normalized form,
        // here should be implemented.
        // now we use identical method
        return number;
    }

    public static <T> boolean isTwoSetIntersect(Set<T> first, Set<T> second) {
        if (first == null || second == null) {
            // Log.i(TAG, "isTwoSetIntersect return false for null reference");
            return false;
        }

        HashSet<T> copy_set = new HashSet<T>(first);
        copy_set.retainAll(second);

        return (!copy_set.isEmpty());
    }

    /**
     * Delete all whitespace and hyphen in phone number string.
     *
     * @param str input string
     * @return string
     */
    public static String trimHyphenAndSpaceInNumberString(String str) {
        if (TextUtils.isEmpty(str))
            return str;

        final int sz = str.length();
        final char[] chs = new char[sz];
        int count = 0;
        for (int i = 0; i < sz; i++) {
            final char ch = str.charAt(i);
            if (!Character.isWhitespace(ch) && ch != '-') {
                chs[count++] = ch;
            }
        }
        if (count == sz) {
            return str;
        }
        return new String(chs, 0, count);
    }

}
