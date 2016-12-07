package com.yunos.alicontacts.dialpad.smartsearch;

import android.content.Context;
import android.content.res.Resources.NotFoundException;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;
import android.net.Uri;
import android.os.Build;
import android.text.TextUtils;
import android.util.Log;

import com.yunos.alicontacts.R;
import com.yunos.alicontacts.database.util.NumberServiceHelper;
import com.yunos.alicontacts.dialpad.smartsearch.SearchResult.SearchResultBuilder;
import com.yunos.alicontacts.util.AliTextUtils;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PinyinSearch {

    private static final String TAG = "PinyinSearch";
    private static final boolean DEBUG = false;

    public ContactInMemory newContactInMemory(){
        return new ContactInMemory();
    }

    /**
     * because the complexity of searching by T9 pinyin is very high, so if the
     * pinyin length is too long, then the search time will last for several
     * minutes. we have to limit the max length of the input pinyin string.
     */
    public static final int MAX_SEARCH_PINYIN_LENGTH = 20;

    /**
     * Limit multi pinyin characters, if beyond this limit count, then cache the first
     * pinyin string.
     */
    // YunOS BEGIN PB
    // ##module:(Contacts)  ##author:shihuai.wg@alibaba-inc.com
    // ##BugID:(8267335)  ##date:2016-05-16
    public static final int MULTI_PINYIN_LIMIT = 3;
    // YunOS END PB

    // 用来存放所有的key到contactId的映射
    //ConcurrentHashMap<String, LinkedHashSet<Long>> sT9KeyContactId = new ConcurrentHashMap<String, LinkedHashSet<Long>>();

    //key is rawContactId+phoneNumber
    private ConcurrentHashMap<String, ContactInMemory> mSearchTable = new ConcurrentHashMap<String, ContactInMemory>(512);
    //shadow search table
    private ConcurrentHashMap<String, ContactInMemory> mSearchTableShadow = new ConcurrentHashMap<String, ContactInMemory>(512);
    private PinyinSearchMap mSearchMap = new PinyinSearchMap();
    //shadow search map
    private PinyinSearchMap mSearchMapShadow = new PinyinSearchMap();
    //shadow highLight format
    public static final char KEY_SPLIT = 'T';
    private AtomicBoolean mReady;
    private PinyinSearchStateChangeListener pyListener;

    public void swapSearchTable() {
        ConcurrentHashMap<String, ContactInMemory> temp = mSearchTable;
        mSearchTable = mSearchTableShadow;
        mSearchTableShadow = temp;
    }

    public ConcurrentHashMap<String, ContactInMemory>  getSearchTable() {
        return mSearchTable;
    }

    /**
     * Get the shadow search table to batch merge into real search table
     * @return
     */
    public ConcurrentHashMap<String, ContactInMemory>  getSearchTableShadow() {
        return mSearchTableShadow;
    }

    /**
     * Get search map for real data
     * @return
     */
    public PinyinSearchMap getSearchMap() {
        return mSearchMap;
    }

    /**
     * Get shadow search map for bulk put into real search map
     * @return
     */
    public PinyinSearchMap getSearchMapShadow() {
        return mSearchMapShadow;
    }

    Dictionary<String, ArrayList<ArrayList<String>>> myPinyinRegCacheDict;// =

    static HashSet<String> myRegPinyinArray = new HashSet<String>();

    public static boolean mHaveNewInput = false;

    /**
     * mPinyinIndexes is loaded from res/raw/data_hzpinyin_bin,
     * which contains index (in HanziUtil.PINYIN_TOKENS) of
     * pinyin data for all Chinese characters.
     * When we want to get pinyin data for a Chinese character,
     * we will use the char code (minus a fixed minimal char code) as offset,
     * and get all possible pinyin data (index in HanziUtil.PINYIN_TOKENS) for it.
     * Each Chinese character has PINYIN_CANDIDATE_PER_CHAR indexes in this array.
     */
    private static short[] mPinyinIndexes;

    /**
     * This is the size (in bytes) of res/raw/data_hzpinyin_bin.
     */
    private static final int PINYIN_DATA_SIZE = 125412;
    /** The count of pinyin candidate for each Chinese character. */
    private static final int PINYIN_CANDIDATE_PER_CHAR = 3;

    public static synchronized void setHaveNewInput(boolean value) {
        mHaveNewInput = value;
    }

    public static volatile boolean mPersistInterrupted = false;

    public static void setPersistInterrupted(boolean value) {
        mPersistInterrupted = value;
    }

    ArrayList<String> myPinyinRegCacheList;

    private static PinyinSearch mInstance = new PinyinSearch();

    public StringBuilder mFirstLetterStr;

    // regular variable
    private StringBuilder sExp = new StringBuilder();

    public static synchronized PinyinSearch getInstance(Context context) {
        if (mInstance == null) {
            mInstance = new PinyinSearch();
        }
        return mInstance;
    }

    protected PinyinSearch() {
        mReady = new AtomicBoolean(false);
        initMyRegPinyinArray();
    }

    /*
     * 初始化拼音规则数据
     */
    final private void initMyRegPinyinArray() {
        HashSet<String> set = myRegPinyinArray;
        byte[] p = new byte[6];
        for (int index = 0; index < 217; index++) {
            int position = index * 6;
            for (int i = 0; i < 6; i++) {
                p[i] = (byte) HanziUtil.Data_Reg_Pinyin.charAt(position + i);
            }
            try {
                String pyr = new String(p, "UTF-8").trim();
                set.add(pyr);
                for (int i = 1; i < pyr.length(); i++) {
                    set.add(pyr.substring(0, i));
                }
            } catch (UnsupportedEncodingException e) {
                Log.e(TAG, "initMyRegPinyinArray throw UnsupportedEncodingException", e);
            }
        }
    }

    // 将数字字符串首字母变形 -32
    String getFirstCharTransform(String str) {
        if (TextUtils.isEmpty(str)) {
            return str;
        }

        // char firstChar = [str characterAtIndex:0];
        char firstChar = str.charAt(0);
        String leftString = str.substring(1);
        StringBuilder sb = new StringBuilder();

        if ((firstChar >= '2') && (firstChar <= '9')) {
            char c = (char) (firstChar - 32);
            sb.append(c);
            sb.append(leftString);
            return sb.toString();
        } else {
            return str;
        }
    }

    /*
     * 根据字母，得到对应的T9数字 注意姓名中每个字拼音的第一个字母为大写：如小梅：XiaoMei
     * 为了区分汉字的边界，如果字母为大写，则则将该字符减去32，得到一个控制字符，比如： X对应的数字为9，ASCII值为0x39，减去32为0x19，
     */
    /*String getT9StrFromLetters(String strLetters) {
        StringBuilder retStr = new StringBuilder();

        int strLettersLength = strLetters.length();
        // char[] Data_Letters_To_T9 =
        // HanziUtil.Data_Letters_To_T9.toCharArray();
        for (int i = 0; i < strLettersLength; i++) {
            char c = strLetters.charAt(i);
            if ((c >= 'a') && (c <= 'z')) {
                c = HanziUtil.Data_Letters_To_T9[c - 'a'];
            } else if ((c >= 'A') && (c <= 'Z')) {
                char temp = HanziUtil.Data_Letters_To_T9[c - 'A'];
                c = (char) ((int) temp - 32);
            } else if ((c >= '2') && (c <= '9')) {
                c -= 32;
            }

            retStr.append(c);
        }

        return retStr.toString();
    }*/

    // 将字符串首字母大写
    /*String getFirstCharUppercaseString(String str) {
        if (TextUtils.isEmpty(str)) {
            return str;
        }

        // char firstChar = [str characterAtIndex:0];
        char firstChar = str.charAt(0);
        String leftString = str.substring(1);

        if (((firstChar >= 'a') && (firstChar <= 'z'))
                || ((firstChar >= '2') && (firstChar <= '9'))) {
            return (firstChar - 32) + leftString;
        } else {
            return str;
        }

    }*/

    class PinyinParm {
        String pinyinSeg;

        ArrayList<String> pinyinNodeList;
    }

    ArrayList<ArrayList<String>> getPinyinListByRegReal(String pinyin) {
        if (TextUtils.isEmpty(pinyin)) {
            return null;
        }

        if(DEBUG){
            Log.v(TAG,"getPinyinListByRegReal string is "+pinyin);
        }

        // 获取符合拼音规则的列表(包含完整拼音和部分拼音)
        ArrayList<PinyinParm> pySegStatck = new ArrayList<PinyinParm>();
        ArrayList<ArrayList<String>> retPyList = new ArrayList<ArrayList<String>>();

        PinyinParm defaultPym = new PinyinParm();
        defaultPym.pinyinSeg = pinyin;
        defaultPym.pinyinNodeList = new ArrayList<String>();
        pySegStatck.add(defaultPym);
        // pinyin: 576, 首先576不包含在myRegPinyinArray中，即不存在构成一个汉字的可能，从头扫描576，取第一个字符5
        while (!pySegStatck.isEmpty()) {
            if (mHaveNewInput) {
                break;
            }
            int size = pySegStatck.size();
            PinyinParm pym = pySegStatck.get(size - 1);
            pySegStatck.remove(size - 1);

            String ps = pym.pinyinSeg;
            if (myRegPinyinArray.contains(ps) || ps.length() == 1) {
                String transform = getFirstCharTransform(ps);
                ArrayList<String> ml = new ArrayList<String>();
                ml.addAll(pym.pinyinNodeList);
                ml.add(transform);
                retPyList.add(ml);
            }

            // 对首字母的各种组合进行处理
            // const unsigned int psLength = [ps length];
            int psLength = ps.length();
            if (psLength > 1) {
                for (int i = 1; i < psLength; i++) {
                    if (mHaveNewInput) {
                        break;
                    }
                    boolean isNotRegPinyin = false;
                    // NSString *prefixPy = [ps substringToIndex:i];
                    String prefixPy = ps.substring(0, i);
                    if (!myRegPinyinArray.contains(prefixPy)) {
                        char curChar = ps.charAt(i - 1);
                        if ((curChar >= '2') && (curChar <= '9'))
                            break;
                        // 如果prefixPy中包含一些特殊字母，不在T9键盘中
                        // prefixPy = [NSString stringWithFormat:@"%c", curChar];
                        prefixPy = Character.toString(curChar);
                        if (i > 1)
                            isNotRegPinyin = true;
                    }

                    ArrayList<String> newArray = new ArrayList<String>();
                    newArray.addAll(pym.pinyinNodeList);
                    if (isNotRegPinyin) {
                        newArray.add(getFirstCharTransform(ps.substring(0, i - 1)));
                    }
                    newArray.add(getFirstCharTransform(prefixPy));
                    // pym.pinyinNodeList.add(getFirstCharTransform(prefixPy));
                    // [newArray addObject:[self getFirstCharTransform:prefixPy]];

                    PinyinParm newPm = new PinyinParm();
                    newPm.pinyinSeg = ps.substring(i);// [ps substringFromIndex:i];
                    newPm.pinyinNodeList = newArray;
                    // [pySegStatck addObject:newPm];
                    pySegStatck.add(newPm);
                }
            }
        }
        /*
         * ArrayList<String> lastObject = new ArrayList<String>(); if
         * (retPyList.size() > 0) { // ArrayList<String> lastObject =
         * (ArrayList<String>) retPyList.get(retPyList.size() - 1);
         * lastObject.addAll(retPyList.get(retPyList.size()-1)); }
         */
        int size = retPyList.size();
        if ((size == 0) || retPyList.get(size - 1).size() < pinyin.length()) {
            String upperPinyin = pinyin.toUpperCase(Locale.US);
            ArrayList<String> egSpList = new ArrayList<String>();
            for (int i = 0; i < upperPinyin.length(); i++) {
                if (mHaveNewInput) {
                    break;
                }
                String subString = Character.toString(upperPinyin.charAt(i));
                egSpList.add(subString);
                // egSpList.add(upperPinyin.substring(i, i + 1));
            }

            // [retPyList addObject:egSpList];
            retPyList.add(egSpList);
        }

        return retPyList;
    }

    // getPinyinListByRegReal 的代理函数，增加了Cache机制
    /*ArrayList<ArrayList<String>> getPinyinListByReg(String pinyin) {

        // NSArray *retList;
        ArrayList<ArrayList<String>> retList = null;
        /*
         * retList = getPinyinListByRegReal(pinyin); if (retList == null ||
         * retList.size() == 0) { return null; } return retList;
         * /

        if (myPinyinRegCacheDict == null) {
            myPinyinRegCacheDict = new Hashtable<String, ArrayList<ArrayList<String>>>();
        }
        if (myPinyinRegCacheList == null) {
            myPinyinRegCacheList = new ArrayList<String>();
        }

        retList = myPinyinRegCacheDict.get(pinyin);
        // if (retList != null) {
        if (false) {
            myPinyinRegCacheList.remove(pinyin);
        } else {
            retList = getPinyinListByRegReal(pinyin);
            if (retList == null || retList.size() == 0) {
                return null;
            }

            /*
             * for (ArrayList<String> pinyinArray: retList) { for (String str:
             * pinyinArray) { LogSystem.v(TAG, "retList" + str); } }
             * /
            myPinyinRegCacheDict.put(pinyin, retList);
        }

        myPinyinRegCacheList.add(0, pinyin);

        // TODO MAX_CACHE_PL_SIZE
        if (myPinyinRegCacheList.size() > 100) {
            String pyKey = myPinyinRegCacheList.get(myPinyinRegCacheList.size() - 1);
            myPinyinRegCacheList.remove(myPinyinRegCacheList.size() - 1);
            myPinyinRegCacheDict.remove(pyKey);
        }

        return retList;
    }*/

    // 获取拼音首字母的组合列表
    // Moved to PinyinSearchMap.addDigiPYStringAndSubStrings()
    /*HashSet<String> getPyFcSegs(String pyFc) {
        HashSet<String> result = new HashSet<String>();
        if (TextUtils.isEmpty(pyFc)) {
            return result;
        }

        // const int pyFcLength = [pyFc length];
        int pyFcLength = pyFc.length();

        if (pyFcLength <= 1) {
            result.add(pyFc);
            return result;
        }

        if (pyFcLength == 2) {
            result.add(pyFc);
            result.add(pyFc.substring(0, 1));
            result.add(pyFc.substring(1));

            return result;
        }

        if (pyFcLength == 3) {
            result.add(pyFc);
            result.add(pyFc.substring(0, 2));
            result.add(pyFc.substring(1));
            result.add(pyFc.substring(0, 1));
            result.add(pyFc.substring(1, 2));
            result.add(pyFc.substring(2));

            return result;
        }

        for (int i = pyFcLength; i > 0; i--) {
            for (int j = 0; j <= pyFcLength - i; j++) {
                result.add(pyFc.substring(j, j + i));
            }
        }

        // return [retList allObjects];
        return result;
    }*/

    /*
     * 获取单个字符(汉字/字母/其他字符)的拼音 UNICODE 中, UNICODE 汉字从 \u3400 到 \u9FFF 中间, \uF900 到
     * \uFAFF 也有一些, 但是 GB2312 和 Big5 的汉字和字符都是在 \u4E00 到 \u9FFF 中间.
     */
    /*synchronized ArrayList<String> getPinyinReal(char ch, Context context) {

        ArrayList<String> retArray = new ArrayList<String>();

        boolean needSetNull = false;
        if (mHanziPinyin == null) {
            mHanziPinyin = getHanziPinyin(context, null);
            needSetNull = true;
        }
        int chCode = (int) ch;
        byte[] p = new byte[6];
        if ((chCode >= 19968) && (chCode < 40870)) {
            for (int count = 0; count < 3; count++) {
                int startPostion = (chCode - 19968) * 18 + count * 6;
                for (int i = 0; i < 6; i++) {
                    p[i] = (byte) mHanziPinyin.charAt(startPostion + i);
                }

                String pinyin = EncodingUtils.getAsciiString(p);

                if (pinyin.equals("      "))
                    break;
                // mContactName = contactName.replaceAll(StringUtil.SAPCE_REGEX,
                // "");
                retArray.add(pinyin.replaceAll(" ", ""));
            }
        } else {

            String letter = Character.toString(ch);
            if (letter.length() == 0) {
                retArray.add("_");
            } else {
                if ((ch >= '2') && (ch <= '9')) {
                    retArray.add(Character.toString((char) (ch - 32)));
                } else {
                    retArray.add(letter.toUpperCase());
                }
            }
            // 不可用的字符，返回@ // 返回_ 方便正则表达式匹配
            /*
             * if ([letter length] == 0) { [retArray addObject:@"_"]; } else
             * [retArray addObject:((ch>='2')&&(ch<='9'))? [NSString
             * stringWithFormat:@"%c", ch-32]: [letter uppercaseString]];//0-9 :
             * -32
             * /
        }

        if (needSetNull) {
            mHanziPinyin = null;
        }

        return retArray;
    }*/

    HashSet<String> getKeyFromPinyinList(ArrayList<String> pyList) {
        HashSet<String> retList = new HashSet<String>();
        for (String py : pyList) {
            StringBuilder key = new StringBuilder();
            for (int i = 0; i < py.length(); i++) {
                char c = py.charAt(i);
                // When logic goes here, no char will be above '9'.
                //if ((c < '2') || ((c > '9') && (c < 'a')) || (c > 'z')) {
                if (c < '2') {
                    key.append(c);
                }
            }

            retList.add(key.toString());

        }

        return retList;

    }

    private static byte[] readHanziPinyinDataToByteArray(Context context) {
        byte[] result = new byte[PINYIN_DATA_SIZE];
        InputStream is = null;
        try {
            is = context.getResources().openRawResource(R.raw.data_hzpinyin_bin);
            int n, cnt = 0;
            while (cnt < PINYIN_DATA_SIZE) {
                n = is.read(result, cnt, PINYIN_DATA_SIZE - cnt);
                if (n < 0) {
                    Log.e(TAG, "readHanziPinyinDataToByteArray: got unexpected EOF at count "+cnt+" with return value "+n+".");
                    break;
                }
                cnt += n;
            }
        } catch (NotFoundException e) {
            Log.e(TAG, "Resource not found for R.raw.data_hzpinyin ("+R.raw.data_hzpinyin_bin+")."+e.getMessage(), e);
        } catch (IOException e) {
            Log.e(TAG, "Cannot read from resource R.raw.data_hzpinyin ("+R.raw.data_hzpinyin_bin+")."+e.getMessage(), e);
        } finally {
            try {
                if (is != null) {
                    is.close();
                }
            } catch (IOException e) {
                Log.e(TAG, "readHanziPinyinDataToByteArray() finally clause throw IOException", e);
            }
        }
        return result;
    }

    public synchronized static void initHanziPinyinForAllChars(Context context) {
        if (mPinyinIndexes != null) {
            return;
        }
        byte[] result = readHanziPinyinDataToByteArray(context);

        int totalPinyinCount = HanziUtil.HANZI_COUNT * PINYIN_CANDIDATE_PER_CHAR;
        short[] pinyinIndexes = new short[totalPinyinCount];
        int resultIndex = 0;
        for (int pinyinIndex = 0; pinyinIndex < totalPinyinCount; pinyinIndex++) {
            pinyinIndexes[pinyinIndex] = (short)((result[resultIndex] & 0xFF) | ((result[resultIndex+1] & 0xFF) << 8));
            resultIndex += 2;
        }
        mPinyinIndexes = pinyinIndexes;
    }

    /**
     * Get the hanzi pinyin data for a given Chinese character.
     * Each Chinese character will have up to 3 pinyin candidates.
     * The index will specify which candidate to get.
     * If the character have less pinyin candidate(s),
     * null will be returned for the remainder.
     * @param chCode The char code for the Chinese character.
     * @param index The 0-based index of pinyin data for the Chinese character.
     * @return The pinyin data of the specified Chinese character in the give index.
     *          Or null for no pinyin data for this index.
     */
    public static String getHanziPinyinDataForCharCode(int chCode, int index) {
        int pinyinIndex = getHanziPinyinIndexForCharCode(chCode, index);
        if (pinyinIndex < 0) {
            return null;
        }
        return HanziUtil.PINYIN_TOKENS[pinyinIndex];
    }

    /**
     * Get the index in HanziUtil.PINYIN_TOKENS for hanzi pinyin for a given Chinese character.
     * Each Chinese character will have up to 3 pinyin candidates.
     * The parameter index will specify which candidate to get.
     * If the character have less pinyin candidate(s),
     * -1 will be returned for the remainder.
     * @param chCode The char code for the Chinese character.
     * @param index The 0-based index of pinyin data for the Chinese character.
     * @return The pinyin index of the specified Chinese character in the give index.
     *          Or -1 for no pinyin data for this index.
     */
    public static int getHanziPinyinIndexForCharCode(int chCode, int index) {
        int chIndex = chCode - HanziUtil.HANZI_START;
        int pinyinIndex = mPinyinIndexes[(PINYIN_CANDIDATE_PER_CHAR * chIndex) + index];
        return pinyinIndex;
    }

    /**
     * Put the contact info into the temporary search table and search map.
     * The searchName must be a prefix of displayName, or the highlight in result might be wrong.
     * @param searchName The part of name that can be searched by dialpad.
     * @param displayName The full display name in the result.
     * @param rawContactId _id column in raw_contacts table.
     * @param phoneNumber The phone number.
     * @param version The name version.
     * @param key The key to find the contact info in search table.
     * @param context
     * @return A list of search map keys that can find this contact.
     */
    public ArrayList<String> initSearchTable(String searchName, String displayName, long rawContactId,
            String phoneNumber, long version, String key, Context context) {
        if (TextUtils.isEmpty(searchName)) {
            return null;
        }

        /* pinyin4Char saves the Pinyin strings for each Chinese character in name.
         * Because we might have multiple pinyin strings for one Chinese char,
         * so use list here. */
        ArrayList<String> pinyin4Char = new ArrayList<String>();
        /* cachePyList saves the full Pinyin strings for Chinese name or original letters for English name. */
        ArrayList<String> cachePyList = new ArrayList<String>();

        int iLength = searchName.length();

        int multiPinyinCount = 0;
        for (int namePosition = 0; namePosition < iLength; namePosition++) {
            char ch = searchName.charAt(namePosition);
            int chCode = ch;
            pinyin4Char.clear();
            if (HanziUtil.isHanziCharCode(chCode)) {
                for (int count = 0; count < PINYIN_CANDIDATE_PER_CHAR; count++) {
                    String pinyin = getHanziPinyinDataForCharCode(chCode, count);
                    if (pinyin == null) {
                        break;
                    }
                    pinyin4Char.add(pinyin);
                }
                int polyCount = pinyin4Char.size();
                if (polyCount == 0) {
                    // can not find pinyin for this char, skip it.
                    continue;
                }
                if (cachePyList.size() == 0) {
                    cachePyList.addAll(pinyin4Char);
                } else {
                    int prevCount = cachePyList.size();
                    boolean handleMultiPinyin = true;

                    if(polyCount > 1) {
                        multiPinyinCount ++;
                    }
                    if (multiPinyinCount > MULTI_PINYIN_LIMIT) {
                        handleMultiPinyin = false;
                        polyCount = 1;
                    }

                    ArrayList<String> tempArray = new ArrayList<String>(prevCount * polyCount);
                    for (String pinyinItem : cachePyList) {
                        if (handleMultiPinyin) {
                            for (String pinyin : pinyin4Char) {
                                tempArray.add(pinyinItem + pinyin);
                            }
                        } else {
                            tempArray.add(pinyinItem + pinyin4Char.get(0));
                        }
                    }
                    cachePyList.clear();
                    cachePyList.addAll(tempArray);
                }
            } else {
                String letter = Character.toString(ch);
                if (letter.length() == 0) {
                    if (Build.TYPE.equals(Build.USER)) {
                        Log.w(TAG, "initSearchTable: got 0 length char code: "+chCode+" in name **** at "+namePosition);
                    } else {
                        Log.w(TAG, "initSearchTable: got 0 length char code: "+chCode+" in name "+searchName+" at "+namePosition);
                    }
                    continue;
                } else if ((ch >= '2') && (ch <= '9')) {
                    letter = Character.toString((char) (ch - 32));
                } else if ((ch >= 'a') && (ch <= 'z')) {
                    letter = letter.toUpperCase(Locale.US);
                } else if (((ch >= 'A') && (ch <= 'Z')) || (ch == '0') || (ch == '1')) {
                    // keep letter unchanged, but go through the code after the if-statement.
                } else {
                    // not digits and letters, this can be symbols,
                    // like '#', '%', etc.
                    // ignore the characters that cannot map to pinyin or digits
                    continue;
                }
                if (cachePyList.isEmpty()) {
                    cachePyList.add(letter);
                } else {
                    int prevCount = cachePyList.size();
                    ArrayList<String> tempArray = new ArrayList<String>(prevCount);
                    for (String pinyinItem : cachePyList) {
                        tempArray.add(pinyinItem + letter);
                    }
                    cachePyList.clear();
                    cachePyList.addAll(tempArray);
                }
            }

        }
        ArrayList<String> retArray = new ArrayList<String>();
        StringBuilder firstChar = new StringBuilder();
        StringBuilder t9Str = new StringBuilder();
        for (String cachePy : cachePyList) {
            t9Str.setLength(0);

            int strLettersLength = cachePy.length();
            boolean addedFirstChar = false;
            for (int i = 0; i < strLettersLength; i++) {
                char c = cachePy.charAt(i);
                if ((c >= 'a') && (c <= 'z')) {
                    c = HanziUtil.Data_Letters_To_T9[c - 'a'];
                } else if ((c >= 'A') && (c <= 'Z')) {
                    char temp = HanziUtil.Data_Letters_To_T9[c - 'A'];
                    c = (char) (temp - 32);
                    firstChar.append(c).append(i);
                    addedFirstChar = true;
                    /*} else if ((c >= '2') && (c <= '9')) {
                    c -= 32;
                    firstChar.append(c).append(i);
                    addedFirstChar = true;
                    useless code, no '2' to '9' will be left here. */
                } else {
                    firstChar.append('\u0001').append(i);
                    addedFirstChar = true;
                }
                t9Str.append(c);
            }
            if (addedFirstChar) {
                firstChar.append(',');
            }

            retArray.add(t9Str.toString());
        }
        // If firstChar has contents, then it must end with ',',
        // we need to remove the ',' at tail.
        if (firstChar.length() > 0) {
            firstChar.deleteCharAt(firstChar.length() - 1);
        }
        ContactInMemory myContactInfo = getSearchTableShadow().get(key);
        if (myContactInfo == null) {
            myContactInfo = new ContactInMemory();
            getSearchTableShadow().put(key, myContactInfo);
        }

        Iterator<String> it = retArray.iterator();
        StringBuilder sb = new StringBuilder();
        while (it.hasNext()) {
            String element = it.next();
            sb.append(element);
            sb.append(',');
        }
        if (sb.length() != 0) {
            sb.setLength(sb.length() -1);
        }

        myContactInfo.pinyinData = sb.toString();
        myContactInfo.length = searchName.length();
        myContactInfo.version = version;
        myContactInfo.contactName = displayName;
        myContactInfo.phoneNumber = phoneNumber;
        myContactInfo.area = getArea(phoneNumber, context);

        if (myContactInfo.allFistCharacter == null) {
            myContactInfo.allFistCharacter = firstChar.toString();
        } else if (myContactInfo.allFistCharacter.length() > 0) {
            myContactInfo.allFistCharacter += ",";
            myContactInfo.allFistCharacter += firstChar.toString();
        }
        myContactInfo.retArray = retArray;
        addToSearchMap(getSearchMapShadow(),retArray,key);
        return retArray;
    }

    public void addToSearchMap(PinyinSearchMap searchMap,
            ArrayList<String> retArray, String key)    {
        if(DEBUG){
            Log.v(TAG,"retArray is "+retArray.toString());
        }
        // 为所有key分段建立BitSet和contactinfo dictionary
        HashSet<String> keySet = getKeyFromPinyinList(retArray);
        Iterator<String> keySetIt = keySet.iterator();
        while (keySetIt.hasNext()) {
            String ks = keySetIt.next();
            searchMap.addDigiPYStringAndSubStrings(ks, key);
        }
    }

    public String getArea(String phoneNumber, Context context) {
        /*
         * If the phonenumber does not start with +86, 0, 13~19, then it is not
         * a valid number. In this case, even though we pass them to
         * ContentProvider, we will get nothing. And it is cost several ms for
         * calling the ContentProvider.
         */
        if (phoneNumber.charAt(0) == '0' || phoneNumber.charAt(0) == '1'
                || phoneNumber.startsWith("+86")) {
            Uri queryUri = NumberServiceHelper.getSingleLocationQueryUriForNumber(phoneNumber);
            Cursor cursor = null;
            try {
                Log.d(TAG, "getArea() : uniform query up. phoneNumber = " + AliTextUtils.desensitizeNumber(phoneNumber));
                cursor = context.getContentResolver().query(queryUri, null, null, null, null);
                Log.d(TAG, "getArea() : uniform query down. cursor = " + cursor);
                if (cursor != null && cursor.moveToFirst()) {
                    String province = cursor.getString(NumberServiceHelper.LOCATION_SINGLE_COLUMN_PROVINCE);
                    String area = cursor.getString(NumberServiceHelper.LOCATION_SINGLE_COLUMN_AREA);
                    String location = AliTextUtils.makeLocation(province, area);
                    return location;
                }
            } catch (SQLiteException sqle) {
                Log.e(TAG, "getArea: failed to query location.", sqle);
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }
        }
        return "";
    }

    public void searchByT9(String pinyin, SearchResultBuilder builder, HashSet<String> matchedNumbers) {
        boolean isSearchResultsEnough = false;

        if (TextUtils.isEmpty(pinyin) || (pinyin.length() > MAX_SEARCH_PINYIN_LENGTH)) {
            return;
        }

        int pinyinLen = pinyin.length();
        ArrayList<ArrayList<String>> retList = getPinyinListByRegReal(pinyin);
        StringBuilder sb = new StringBuilder();
        int count = retList.size();

        Collections.reverse(retList);
        for (int i = 0; i < count; i++) {
            if (mHaveNewInput) {
                break;
            }
            sb.setLength(0);
            ArrayList<String> item = retList.get(i);
            for (String str : item) {
                sb.append(str.charAt(0));
            }
            ArrayList<String> match = getSearchMap().getContactKeys(sb.toString());
            if (match != null) {
                if (mHaveNewInput) {
                    break;
                }
                sExp.setLength(0);
                for (int keyLen = 0; keyLen < item.size(); keyLen++) {
                    String str = item.get(keyLen);
                    sExp.append(str.charAt(0));
                    if (str.length() > 1) {
                        sExp.append(str.substring(1));
                    }
                    sExp.append("[^\u0001\u0012\u0013\u0014\u0015\u0016\u0017\u0018\u0019]*?");
                }
                Pattern pattern = Pattern.compile(sExp.toString());
                Iterator<String> it = match.iterator();
                while (it.hasNext()) {
                    if (mHaveNewInput) {
                        break;
                    }
                    String key = it.next();
                    ContactInMemory memoryNode = getSearchTable().get(key);
                    if ((memoryNode == null) || builder.isKeyExists(key)) {
                        continue;
                    }
                    String pinyinData = memoryNode.pinyinData;
                    /*
                     * 过滤掉不满足条件的情况： 即汉字的首字符相同，但后面的拼音不一致
                     */
                    int polyPos = -1;
                    int startPosition = -1;
                    if (pinyinData.contains(",")) {
                        String[] charArray = pinyinData.split(",");
                        for (polyPos = 0; polyPos < charArray.length; polyPos++) {
                            String pinyinItem = charArray[polyPos];
                            Matcher patternMatch = pattern.matcher(pinyinItem);
                            if (patternMatch.find()) {
                                String str = patternMatch.group(0);
                                startPosition = pinyinItem.indexOf(str);
                                break;
                            }
                        }
                    } else {
                        Matcher patternMatch = pattern.matcher(pinyinData);
                        if (patternMatch.find()) {
                            String str = patternMatch.group(0);
                            startPosition = pinyinData.indexOf(str);
                        }
                    }

                    if (startPosition != -1) {
                        if (startPosition > 0) {
                            String firstCharPosition = memoryNode.allFistCharacter;
                            if (polyPos != -1) {
                                try{
                                    firstCharPosition = firstCharPosition.split(",")[polyPos];
                                }catch(ArrayIndexOutOfBoundsException e){
                                    firstCharPosition = firstCharPosition.split(",")[0];
                                }
                            }
                            StringBuilder length = new StringBuilder();
                            int firstCharCount = 0;
                            int firstCharLength = firstCharPosition.length();
                            for (int position=0; position < firstCharLength; position++) {
                                char firstChar = firstCharPosition.charAt(position);
                                if (firstChar < 48 || firstChar > 57) {
                                    if (position > 0) {
                                        int len = Integer.parseInt(length.toString());
                                        if (len == startPosition) {
                                            startPosition = firstCharCount - 1;
                                            break;
                                        } else {
                                            length.setLength(0);
                                        }
                                    }
                                    firstCharCount++;
                                } else {
                                    length.append(firstChar);
                                    if (position == (firstCharLength - 1)) {
                                        int len = Integer.parseInt(length.toString());
                                        if (len == startPosition) {
                                            startPosition = firstCharCount - 1;
                                            break;
                                        } else {
                                            length.setLength(0);
                                        }
                                    }
                                }
                            }
                        }
                        int matchLength = sb.length();
                        MatchResult mr = new MatchResult();
                        // key starts with '-' means a yellow page contact, otherwise, phone contact.
                        mr.type = key.charAt(0) == '-' ? MatchResult.TYPE_YELLOWPAGE : MatchResult.TYPE_CONTACTS;
                        mr.matchPart = MatchResult.MATCH_PART_NAME;
                        mr.key = key;
                        mr.name = memoryNode.contactName;
                        mr.phoneNumber = memoryNode.phoneNumber;
                        mr.setNameMatchRange(startPosition, matchLength);
                        mr.calculateWeight();
                        builder.addMatchResult(mr);
                        if (!isSearchResultsEnough) {
                            matchedNumbers.add(mr.phoneNumber);
                            isSearchResultsEnough = ContactsSearchEngine.isSearchResultsEnough(pinyinLen, builder.getMatchResultCount());
                        }
                    }
                }
            }

        }

        return;
    }

    public void setReady(boolean isReady){
        if(mReady==null){
            mReady = new AtomicBoolean();
        }
        mReady.set(isReady);
        if(DEBUG){
            Log.d(TAG, "PinyinSearch status is:"+isReady);
        }
        if(pyListener!=null){
            pyListener.onStateChanged(isReady);
            if(DEBUG){
                Log.d(TAG, "Notified listener");
            }
        }
    }

    public boolean isReady(){
        if(mReady==null)
            return false;
        else{
            return mReady.get();
        }
    }

    public void notifySearchTableChanged() {
        if (pyListener != null) {
            pyListener.onSearchTableChanged();
        }
    }

    public void setOnStateChangeListener(PinyinSearchStateChangeListener pyListener){
        this.pyListener = pyListener;
    }

    public void dumpSearchTable(String dumpFilePath) {
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(dumpFilePath);
            String[] keysArray = mSearchTable.keySet().toArray(new String[0]);
            int count = keysArray.length;
            fos.write(("The keys count in mSearchTable is "+count+".\n").getBytes("UTF-8"));
            StringBuilder line = new StringBuilder();
            String key;
            ContactInMemory contact;
            for (int i = 0; i < count; i++) {
                key = keysArray[i];
                line.setLength(0);
                line.append("key [").append(i).append("]: ").append(key).append("\n\t");
                contact = mSearchTable.get(key);
                if (contact == null) {
                    line.append("null\n");
                } else {
                    line.append(contact.toString()).append('\n');
                }
                fos.write(line.toString().getBytes("UTF-8"));
            }
        } catch (Exception e) {
            Log.d(TAG, "dumpSearchTable: got exception. "+e.getMessage(), e);
        } finally {
            if (fos != null) {
                try { fos.close(); } catch (Exception e) {
                    Log.e(TAG, "dumpSearchTable() finally clause throw Exception", e);
                }
            }
        }
    }

}
