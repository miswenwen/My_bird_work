
package com.yunos.alicontacts.database.util;

import android.text.TextUtils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility for normalizing various type phone numbers
 *
 * @author tianyuan.ty
 */

public class NumberNormalizeUtil {
    private static final String TAG = NumberNormalizeUtil.class.getSimpleName();

    /**
     * CHINA SPECIAL_NUMBER
     */
    private static final String SPECIAL_NUMBER_CHINA = "110|119|120|122|12110|12395|12121|12117|95119|999|"
            + "95555|95566|95533|95588|95558|95599|95568|95595|95559|95508|95528|95501|95577|95561|"
            + "10086|10010|10000|17951|17911|17900|12593|17909|114|95598|12318|"
            + "12315|12358|12365|12310|12369|12333|12366|12306|"
            + "95518|95519|95511|95500|95522|95567|[48]00[0-9]{7}";

    /**
     * CHINA SMS/MMS Notification/Service Number
     */
    // private static final String CHINA_SMS_NOTIFICATION_CODE =
    // "106(5|9)[0-9]{4}[0-9]*|95[0-9]{3}[0-9]*|100[0-9]{2}[0-9]*|12(1|3)[0-9]{2}[0-9]*|125200[0-9]*";
    // private static final String CHINA_SMS_NOTIFICATION_CODE =
    // "106(5|9)[0-9]{1,3}" +
    // "(95555|95566|95533|95588|95558|95599|95568|95595|95559|95508|95528|95501|95577|95561|95598|95518|95519|95511|95500|95522|95567)$";
    // private static final Pattern sSMServiceNumberPattern = Pattern
    // .compile(CHINA_SMS_NOTIFICATION_CODE);

    /**
     * CHINA TELEPHONE NUMBER REGEX
     */
    public static final String TELEPHONE_CHINA = "^(17951|17911|17900|12593|17909)?((((\\+|00)86)(10|2[0-9]|[3-9][0-9]{2}))|(010|02[0-9]|0[3-9][0-9]{2}))(("
            + SPECIAL_NUMBER_CHINA + ")|([2-9][0-9]{6,7}))$";
    public static final Pattern sTeleNumberPattern = Pattern.compile(TELEPHONE_CHINA);

    /**
     * CHINA MOBILEPHONE NUMBER REGEX
     */
    private static final String MOBILEPHONE_CHINA = "^(17951|17911|17900|12593|17909)?((\\+|00)86)?(((1(3|4|5|8)[0-9])|(17[0-8]))\\d{8})$";
    private static final Pattern sMobileNumberPattern = Pattern.compile(MOBILEPHONE_CHINA);

    /**
     * convert the phone numbers with different forms into one unified form,
     * which can be used by query (for example in yellow page).
     *
     * @param rawNumber Number string that need to be normalized
     * @param isContainSpace if contain space in input string;if {@code true},
     *            this function will trim them.
     * @return Normalize string
     */
    public static String normalizeNumber2(String rawNumber, boolean isContainSpace) {
        if (TextUtils.isEmpty(rawNumber))
            return "";

        if (isContainSpace) {
            rawNumber = trimAllWhiteSpace(rawNumber);
        }

        String result = rawNumber;
        SectionsResult sectionsResult = normalize(rawNumber);
        StringBuilder builder = new StringBuilder();
        switch (sectionsResult.mType) {
            case SectionsResult.NUMBER_TYPE_MOBILE:
                // if (sectionsResult.mCountryCode != null) {
                // builder.append(sectionsResult.mCountryCode);
                // builder.append("-");
                // }
                builder.append(sectionsResult.mPhoneNumber);
                result = builder.toString();
                break;
            case SectionsResult.NUMBER_TYPE_FIX:
                if (sectionsResult.mCountryCode != null) {
                    builder.append(sectionsResult.mCountryCode);
                    builder.append('-');
                }

                if (sectionsResult.mDistrictCode != null) {
                    if ((!TextUtils.isEmpty(sectionsResult.mCountryCode))
                            && (sectionsResult.mDistrictCode.charAt(0) == '0')) {
                        builder.append(sectionsResult.mDistrictCode.substring(1));
                    } else {
                        builder.append(sectionsResult.mDistrictCode);
                    }
                    builder.append('-');
                }
                builder.append(sectionsResult.mPhoneNumber);
                result = builder.toString();
                break;
            case SectionsResult.NUMBER_TYPE_SEPCIAL:
                result = sectionsResult.mPhoneNumber;
                break;
            case SectionsResult.NUMBER_TYPE_SMS_SERVICE:
                result = sectionsResult.mPhoneNumber;
                break;
            case SectionsResult.NUMBER_TYPE_UNKNOWN:
                break;
        }

        return result;
    }

    /**
     * This function is for URI. It has the same function as normalizeNumber2,
     * but removed "-".
     *
     * @param rawNumber
     * @param isContainSpace
     * @return
     */
    public static String normalizeNumber(String rawNumber, boolean isContainSpace) {
        if (TextUtils.isEmpty(rawNumber))
            return "";

        if (isContainSpace) {
            rawNumber = trimAllWhiteSpace(rawNumber);
        }

        String result = rawNumber;
        SectionsResult sectionsResult = normalize(rawNumber);
        StringBuilder builder = new StringBuilder();
        switch (sectionsResult.mType) {
            case SectionsResult.NUMBER_TYPE_MOBILE:
                // if (sectionsResult.mCountryCode != null) {
                // builder.append(sectionsResult.mCountryCode);
                // builder.append("-");
                // }
                builder.append(sectionsResult.mPhoneNumber);
                result = builder.toString();
                break;
            case SectionsResult.NUMBER_TYPE_FIX:
                if (sectionsResult.mCountryCode != null) {
                    builder.append(sectionsResult.mCountryCode);
                    //builder.append("-");
                }

                if (sectionsResult.mDistrictCode != null) {
                    if ((!TextUtils.isEmpty(sectionsResult.mCountryCode))
                            && (sectionsResult.mDistrictCode.charAt(0) == '0')) {
                        builder.append(sectionsResult.mDistrictCode.substring(1));
                    } else {
                        builder.append(sectionsResult.mDistrictCode);
                    }
                    //builder.append("-");
                }
                builder.append(sectionsResult.mPhoneNumber);
                result = builder.toString();
                break;
            case SectionsResult.NUMBER_TYPE_SEPCIAL:
                result = sectionsResult.mPhoneNumber;
                break;
            case SectionsResult.NUMBER_TYPE_SMS_SERVICE:
                result = sectionsResult.mPhoneNumber;
                break;
            case SectionsResult.NUMBER_TYPE_UNKNOWN:
                break;
        }

        return result;
    }

    public static int getSegment(String rawNumber) {
        if (TextUtils.isEmpty(rawNumber))
            return 0;

        rawNumber = trimAllWhiteSpace(rawNumber);

        int result = 0;
        SectionsResult sectionsResult = normalize(rawNumber);
        switch (sectionsResult.mType) {
        case SectionsResult.NUMBER_TYPE_MOBILE:
            result = Integer.valueOf(sectionsResult.mPhoneNumber
                    .substring(0, 7));
            break;
        case SectionsResult.NUMBER_TYPE_FIX:
            result = Integer.valueOf(sectionsResult.mDistrictCode);
            break;
        default:
            break;
        }
        return result;
    }

    public static String getNumberSegment(String rawNumber) {
        String result = null;
        if (TextUtils.isEmpty(rawNumber))
            return result;

        rawNumber = trimAllWhiteSpace(rawNumber);

        SectionsResult sectionsResult = normalize(rawNumber);
        switch (sectionsResult.mType) {
        case SectionsResult.NUMBER_TYPE_MOBILE:
            result = sectionsResult.mPhoneNumber.substring(0, 7);
            break;
        case SectionsResult.NUMBER_TYPE_FIX:
            result = sectionsResult.mDistrictCode;
            break;
        default:
            break;
        }
        return result;
    }

    private static SectionsResult normalize(String rawNumber) {
        String formatedNumber = formatNumber(rawNumber);
        SectionsResult result = new SectionsResult();

        Matcher matcher = sMobileNumberPattern.matcher(formatedNumber);
        if (matcher.find()) {
            String strCountry = matcher.group(2);
            String strNumber = matcher.group(4);
            // log("Mobile Match : strCountry = " + strCountry +
            // ", strNumber = " + strNumber);
            result.mCountryCode = strCountry;
            result.mPhoneNumber = strNumber;
            result.mType = SectionsResult.NUMBER_TYPE_MOBILE;
            return result;
        } else if (matcher.usePattern(sTeleNumberPattern).find()) {
            String strCounrty = matcher.group(4);
            String strArea1 = matcher.group(6); // area code behind country code
            String strArea2 = matcher.group(7); // area code without country
                                                // code
            String strSpecialNum = matcher.group(9);
            String strFixNum = matcher.group(10);

            if (strCounrty != null) {
                if (strCounrty.startsWith("00") && strArea1 != null) {
                    result.mCountryCode = "+" + strCounrty.substring(2);
                    result.mDistrictCode = "0" + strArea1;
                } else if (strCounrty.charAt(0) == '+' && strArea1 != null) {
                    result.mCountryCode = strCounrty;
                    result.mDistrictCode = "0" + strArea1;
                }

            } else if (strArea2 != null) {
                result.mDistrictCode = strArea2;
            }

            if (strSpecialNum != null) {
                result.mType = SectionsResult.NUMBER_TYPE_SEPCIAL;
                result.mPhoneNumber = strSpecialNum;

            } else if (strFixNum != null) {
                result.mType = SectionsResult.NUMBER_TYPE_FIX;
                result.mPhoneNumber = strFixNum;
            }
        }
        // else if (matcher.usePattern(sSMServiceNumberPattern).find()) {
        // String strServiceNum = matcher.group(2);
        // if (strServiceNum != null) {
        // result.mType = SectionsResult.NUMBER_TYPE_SMS_SERVICE;
        // result.mPhoneNumber = strServiceNum;
        // }
        // }

        return result;
    }

    /**
     * Delete all whitespace in string. This functions is about 10 times faster
     * than trimAllWhiteSpace2()
     *
     * @param str input string
     * @return string without whitespace
     */
    public static String trimAllWhiteSpace(String str) {
        if (TextUtils.isEmpty(str))
            return str;

        int sz = str.length();
        char[] chs = new char[sz];
        int count = 0;
        for (int i = 0; i < sz; i++) {
            if (!Character.isWhitespace(str.charAt(i))) {
                chs[count++] = str.charAt(i);
            }
        }
        if (count == sz) {
            return str;
        }
        return new String(chs, 0, count);
    }
    //delete all space and '-'
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

    public static String trimAllWhiteSpace2(String str) {
        if (TextUtils.isEmpty(str))
            return str;

        return str.replaceAll("\\s+", "");
    }

    private static String normalizeContryCode(String country) {
        if (TextUtils.isEmpty(country) || '+' == country.charAt(0)) {
            // empty string or "+86" like form
            return country;
        }

        // "0086" like form
        int sz = country.length();
        char[] chs = new char[sz + 1];
        int count = 1;
        chs[count++] = '+';
        for (int i = 0; i < sz; i++) {
            if ('0' == country.charAt(i)) {
                chs[count++] = country.charAt(i);
            }
        }

        return new String(chs, 0, count);
    }

    /**
     * Contain each sections after normalize and other information
     *
     * @author tianyuan.ty
     */
    public static class SectionsResult {
        /**
         * Number type definition
         */
        public static final int NUMBER_TYPE_UNKNOWN = 1000;
        public static final int NUMBER_TYPE_MOBILE = 1001;
        public static final int NUMBER_TYPE_FIX = 1002;
        public static final int NUMBER_TYPE_SEPCIAL = 1003;
        public static final int NUMBER_TYPE_SMS_SERVICE = 1004;

        String mCountryCode; // country code , such as +86, +852
        String mDistrictCode; // district code ,such as 010, 0571
        String mPhoneNumber; // phone number, such as 1381111111, 688988998,
                             // 110, 95555

        int mType;

        public SectionsResult(String country, String district, String phone, int type) {
            mCountryCode = country;
            mDistrictCode = district;
            mPhoneNumber = phone;
            mType = type;
        }

        public SectionsResult() {
            mType = NUMBER_TYPE_UNKNOWN;
        }

        @Override
        public String toString() {
            return "{mCountryCode=" + mCountryCode + ",mDistrictCode=" + mDistrictCode
                    + ",mPhoneNumber=" + mPhoneNumber + ",mType=" + mType + "}";
        }
    }

    /**
     * First replace leading "+" with 00; then remove any non-digital character.
     *
     * @param rawNumber
     *            input Number String
     * @return formatted Number String
     */
    public static String formatNumber(String rawNumber) {
        if (TextUtils.isEmpty(rawNumber))
            return rawNumber;

        int sz = rawNumber.length();
        char[] chs = new char[sz + 1];
        int count = 0;
        if (rawNumber.charAt(0) == '+') {
            chs[count++] = '0';
            chs[count++] = '0';
        }

        for (int i = 0; i < sz; i++) {
            char c = rawNumber.charAt(i);
            if (Character.isDigit(c)) {
                chs[count++] = c;
            }
        }
        return new String(chs, 0, count);
    }

    /*
    private static void log(String logInfo) {
        Log.v(TAG, logInfo);
    }

    public static void test(String[] args) {
        // input number case array
        String[] mobileInputNumbers = {
                "17951008613811166461", "17951+8613811166461", "13811166461", "+8613811166461",
                "008613811166461", "0086-13811166461", "179511381116646"
        };

        String[] fixInputNumbers = {
                "17951+8636587654431", "1795100861087654321", "17951037165432112",
                "0086-25-87654321", "+862587654321", "02587654321", "87654321", "037110086",
                "0371122", "0371110", "021-122", "021-132", "021-95555", "0214001111234",
                "0218001231234", "010 800 001 4456", "+86 800 001 4456"
        };

        String[] smsServiceInputNumbers = {
                "1065795555", "1069795559", "10690095555", "106901295555"
        };

        long tick;

        log("\n\n\n\n-------------------------------------------New Run -----------------------------\n");
        log("-- Mobile Number --");
        for (String number : mobileInputNumbers) {
            log("Before Normalize : " + number);
            tick = System.currentTimeMillis();
            log("After Normalize :  " + normalizeNumber(number, true));
            log("Consume time : " + (System.currentTimeMillis() - tick) + "\n");
        }

        log("\n-- Fix Number --");
        for (String number : fixInputNumbers) {
            log("\nBefore Normalize : " + number);
            tick = System.currentTimeMillis();
            log("After Normalize :  " + normalizeNumber(number, true));
            log("Consume time : " + (System.currentTimeMillis() - tick) + "\n");
        }

        log("\n-- SMS Service Number --");
        for (String number : smsServiceInputNumbers) {
            log("\nBefore Normalize : " + number);
            tick = System.currentTimeMillis();
            log("After Normalize :  " + normalizeNumber(number, true));
            log("Consume time : " + (System.currentTimeMillis() - tick) + "\n");
        }

        log("\n-- Performance Test --");
        int count = 0;
        tick = System.currentTimeMillis();
        int iter = 100;
        while (iter-- != 0) {
            for (String number : fixInputNumbers) {
                count++;
                normalizeNumber(number, true);
            }
            for (String number : mobileInputNumbers) {
                count++;
                normalizeNumber(number, true);
            }
        }
        log("Consume time : " + (System.currentTimeMillis() - tick) + "\n" + " for " + count
                + " Numbers");

        log("\n-- trim space Performance Test 1 --");
        count = 0;
        tick = System.currentTimeMillis();
        iter = 1000;
        String[] testStrs = {
                "879 09 009 99 0000", "  89   00 00 99999999 00 00 "
        };
        while (iter-- != 0) {
            for (String number : testStrs) {
                count++;
                trimAllWhiteSpace(number);
            }
        }
        log("Totle trim space 1 Consume time : " + (System.currentTimeMillis() - tick) + "\n"
                + " for " + count + " Numbers");

        log("\n-- trim space Performance Test 2 --");
        count = 0;
        tick = System.currentTimeMillis();
        iter = 1000;
        while (iter-- != 0) {
            for (String number : testStrs) {
                count++;
                trimAllWhiteSpace2(number);
            }
        }
        log("Totle trim space 2 Consume time : " + (System.currentTimeMillis() - tick) + "\n"
                + " for " + count + " Numbers");

    }*/
}
