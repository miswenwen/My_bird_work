
package com.yunos.alicontacts.util;

import android.text.TextUtils;

import com.yunos.alicontacts.R;

import java.util.ArrayList;

public class ContactsTextUtils {
    public static final String STRING_EMPTY = "";

    private static final int NAME_COUNT = 2;
    private static ArrayList<Integer> mColorList;
    private static ArrayList<Integer> mColorBgList;

    public static boolean isChineseChar(char c) {
        Character.UnicodeBlock ub = Character.UnicodeBlock.of(c);
        if (ub == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS || ub == Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS
                || ub == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A) {
            return true;
        }
        return false;
    }

    public static String getPortraitText(CharSequence strName) {
        if (!TextUtils.isEmpty(strName)) {
            String trimString = strName.toString().trim();
            if (trimString.length() <= 1) {
                return trimString;
            }
            boolean isCheseString = true;
            StringBuilder sb = new StringBuilder();
            for (int i = trimString.length() - 1; i >= 0; i--) {
                char c = trimString.charAt(i);
                if (!isChineseChar(c)) {
                    isCheseString = false;
                    break;
                }
                if (sb.length() < NAME_COUNT) {
                    sb.insert(0, c);
                }
            }
            if (!isCheseString) {
                sb.setLength(0);
                sb.append(trimString.charAt(0));
                sb.append(trimString.charAt(1));
            }
            return sb.toString();
        }
        return STRING_EMPTY;
    }

    public static int getColor(String nameString) {
        if (mColorList == null) {
            initColorList();
        }
        int idx = Math.abs(nameString.hashCode() % mColorList.size());
        return mColorList.get(idx);
    }

    public static int getColorBg(String nameString) {
        if (mColorBgList == null) {
            initColorBgList();
        }
        int idx = Math.abs(nameString.hashCode() % mColorBgList.size());
        return mColorBgList.get(idx);
    }

    private static void initColorList() {
        mColorList = new ArrayList<Integer>(6);
        mColorList.add(R.color.portrait_color1);
        mColorList.add(R.color.portrait_color2);
        mColorList.add(R.color.portrait_color3);
        mColorList.add(R.color.portrait_color4);
        mColorList.add(R.color.portrait_color5);
        mColorList.add(R.color.portrait_color6);
    }

    private static void initColorBgList() {
        mColorBgList = new ArrayList<Integer>(6);
        mColorBgList.add(R.drawable.quick_call_portrait_bg_color1);
        mColorBgList.add(R.drawable.quick_call_portrait_bg_color2);
        mColorBgList.add(R.drawable.quick_call_portrait_bg_color3);
        mColorBgList.add(R.drawable.quick_call_portrait_bg_color4);
        mColorBgList.add(R.drawable.quick_call_portrait_bg_color5);
        mColorBgList.add(R.drawable.quick_call_portrait_bg_color6);
    }
}
