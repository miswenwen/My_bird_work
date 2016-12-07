package com.yunos.alicontacts.util.preloadcontact;

import android.text.TextUtils;

import java.util.Arrays;
import java.util.List;

public class PreloadContact {
    public String name;
    public List<String> numbers;
    public String photo;

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("{name:\"").append(name)
          .append("\";numbers:[");
        if (numbers == null) {
            sb.append("null");
        } else {
            for (String num : numbers) {
                sb.append('\"').append(num).append("\",");
            }
        }
        sb.append("];photo:\"").append(photo).append("\"}");
        return sb.toString();
    }

    public boolean equalsIgnorePhoto(PreloadContact pc) {
        if (compareString(name, pc.name) != 0) {
            return false;
        }
        int size = numbers == null ? 0 : numbers.size();
        int pcSize = pc.numbers == null ? 0 : pc.numbers.size();
        if (size != pcSize) {
            return false;
        }
        if (size > 0) {
            String[] formattedNumbers = numbers.toArray(new String[size]);
            String[] pcFormattecNumbers = pc.numbers.toArray(new String[size]);
            if (!compareNumbersArray(formattedNumbers, pcFormattecNumbers)) {
                return false;
            }
        }
        return true;
    }

    private static int compareString(String str1, String str2) {
        if (TextUtils.isEmpty(str1)) {
            if (TextUtils.isEmpty(str2)) {
                return 0;
            }
            return -1;
        } else {
            if (TextUtils.isEmpty(str2)) {
                return 1;
            }
            return str1.compareTo(str2);
        }
    }

    private static boolean compareNumbersArray(String[] array1, String[] array2) {
        int len1 = array1 == null ? 0 : array1.length;
        int len2 = array2 == null ? 0 : array2.length;
        if (len1 != len2) {
            return false;
        }
        normallizeNumbersArray(array1);
        normallizeNumbersArray(array2);
        for (int i = 0; i < len1; i++) {
            if (compareString(array1[i], array2[i]) != 0) {
                return false;
            }
        }
        return true;
    }

    private static void normallizeNumbersArray(String[] numbers) {
        if (numbers == null) {
            return;
        }
        Arrays.sort(numbers);
    }

}
