
package com.yunos.alicontacts.list.fisheye;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CopyOnWriteArraySet;

public class FishEyeData {
    /*fish eye depth*/
    public static int FISHEYE_DEPTH = 2;

    private static final String TAG = "FishEyeData";
    public static final String FISH_EYE_INDEX_KEY = "Fish_eye_index_key";
//    private static FishEyeData fed = null;
    private volatile boolean isReady = false;
    private FishEyeContactsCache mContactsCache = null;
    private int favoriteCount = 0;
    private Context mContext;

    /**
     * 字母表索引的联系人列表
     */
    private ConcurrentHashMap<Character, ConcurrentHashMap<String, CopyOnWriteArrayList<Name>>> contactsMap;
    /**
     * 每个字母的姓氏集合
     */
    private ConcurrentHashMap<Character, CopyOnWriteArraySet<String>> xingsMap;
    /**
     * 姓氏索引的联系人列表
     */
    private ConcurrentHashMap<String, CopyOnWriteArraySet<String>> mingsMap;
    /**
     * 每个姓氏对应的名的列表
     */
    private ConcurrentHashMap<String, ConcurrentHashMap<String, CopyOnWriteArrayList<Name>>> xingmingMap;

//    public static synchronized FishEyeData getInstance(Cursor cursor, int displayName, int favoriteCount) {
//        if (fed == null || fed.contacts == null || !fed.contacts.equals(cursor)) {
//            fed = new FishEyeData(cursor, displayName, favoriteCount);
//            return fed;
//        }
//        return fed;
//    }
	public FishEyeData(Context context) {
	    mContext = context;
	}

    public void setFishData(FishEyeContactsCache cache, int favoriteCount) {
        init();
        isReady = false;
        this.mContactsCache = cache;
        this.favoriteCount = favoriteCount;
    }
    private void init() {
        this.contactsMap = new ConcurrentHashMap<Character, ConcurrentHashMap<String, CopyOnWriteArrayList<Name>>>();
        this.xingsMap = new ConcurrentHashMap<Character, CopyOnWriteArraySet<String>>();
        for (char cTemp = 'A'; cTemp <= 'Z'; cTemp++) {
            contactsMap.put(Character.valueOf(cTemp),
                    new ConcurrentHashMap<String, CopyOnWriteArrayList<Name>>());
            xingsMap.put(Character.valueOf(cTemp), new CopyOnWriteArraySet<String>());
        }
        this.xingmingMap = new ConcurrentHashMap<String, ConcurrentHashMap<String, CopyOnWriteArrayList<Name>>>();
        this.mingsMap = new ConcurrentHashMap<String, CopyOnWriteArraySet<String>>();
    }

    public FishEyeContactsCache getContactsCache() {
        return mContactsCache;
    }

    /**
     * This method must be called with isReady == true.
     */
    public String[] getXingList(char c) {
        CopyOnWriteArraySet<String> xingSet = xingsMap.get(Character.valueOf(c));

        if (xingSet == null || xingSet.size() == 0) {
            return null;
        } else {
            String[] saList = new String[xingSet.size()];
            int i = 0;
            for (String xing2 : xingSet) {
                saList[i] = xing2;
                i++;
            }
            return saList;
        }
    }

    public String[] getMingList(char c, int pos){
        String[] mingList = null;
        String xing1 = null;

        String[] xingList = getXingList(c);
        if (xingList == null) {
            Log.w(TAG, "getMingList: xinList is null. isReady = " + isReady);
            return null;
        }
        CopyOnWriteArrayList<Name> nameList = contactsMap.get(Character.valueOf(c)).get(
                xingList[pos]);
        if (nameList == null || nameList.get(0) == null){
            Log.v(TAG,"NameList is null");
            return null;
        }

        else{
            xing1 = nameList.get(0).xing;
            CopyOnWriteArraySet<String> mingSet = mingsMap.get(xing1);
            if(mingSet == null ||mingSet.size()==0){
                return null;
            }
            mingList = new String[mingSet.size()];
            int i = 0;
            for (String ming2 : mingSet) {
                mingList[i] = ming2;
                i++;
            }
        }

        return mingList;
    }

    public int getXingPos(char c, int pos) {
        String[] xingList = getXingList(c);
        if (xingList == null) {
            Log.w(TAG, "getXingPos: xinList is null. isReady = " + isReady);
            return 0;
        }
        CopyOnWriteArrayList<Name> nameList = contactsMap.get(Character.valueOf(c)).get(
                xingList[pos]);
        if (nameList == null || nameList.get(0) == null){
            Log.v(TAG,"NameList is null");
            return 0;
        }
        else{
            for (Name name : nameList) {
                if (name.pos >= favoriteCount) {
                    return name.pos;
                }
            }

            return nameList.get(0).pos;
        }
    }

    public int getMingPos(char c, int pos1, int pos2){
        String xing1 = null;

        String[] xingList = getXingList(c);
        if (xingList == null) {
            Log.w(TAG, "getMingPos: xinList is null. isReady = " + isReady);
            return 0;
        }
        CopyOnWriteArrayList<Name> nameList = contactsMap.get(Character.valueOf(c)).get(
                xingList[pos1]);
        if (nameList == null || nameList.get(0) == null){
            Log.v(TAG,"NameList is null");
            return 0;
        }
        else{
            xing1 = nameList.get(0).xing;
            String[] mingLists = getMingList(c, pos1);
            if (mingLists == null) {
                Log.w(TAG, "getMingPos: mingLists is null. isReady = " + isReady);
                return 0;
            }
            CopyOnWriteArrayList<Name> mingList = xingmingMap.get(xing1).get(mingLists[pos2]);
            if (mingList == null || mingList.get(0) == null){
                Log.v(TAG,"NameList is null");
                return 0;
            }
            else{
                for (Name name : mingList) {
                    if (name.pos >= favoriteCount) {
                        return name.pos;
                    }
                }

                return mingList.get(0).pos;
            }
        }
    }

    public final void getFishEyeData() {
        int size = mContactsCache.getCount();
        mContactsCache.queryPinyinForAll(mContext);
        ConcurrentHashMap<String, CopyOnWriteArrayList<Name>> xingMapTemp;
        ConcurrentHashMap<String, CopyOnWriteArrayList<Name>> mingMapTemp;
        CopyOnWriteArraySet<String> xingSetTemp;
        CopyOnWriteArraySet<String> mingSetTemp;

        for (int i = favoriteCount; i < size; i++) {
            Name contact = mContactsCache.getContact(i);
            String name = contact.nameSource;
            if (TextUtils.isEmpty(name)) {
                continue;
            }

            if (!TextUtils.isEmpty(contact.xing)) {
                xingMapTemp = contactsMap.get(Character.valueOf(contact.pyIndex));
                if (xingMapTemp == null) {
                    Log.e(TAG, "XingMap is incorrect for "+contact.pyIndex);
                    return;
                }
                xingSetTemp = xingsMap.get(Character.valueOf(contact.pyIndex));
                if (xingSetTemp == null) {
                    Log.e(TAG, "XingSet is incorrect for "+contact.pyIndex);
                    return;
                }
                xingSetTemp.add(contact.xing);
                CopyOnWriteArrayList<Name> nameList = xingMapTemp.get(contact.xing);
                // New family Name?
                if (nameList == null) {
                    CopyOnWriteArrayList<Name> nameListTemp = new CopyOnWriteArrayList<Name>();
                    nameListTemp.add(contact);
                    xingMapTemp.put(contact.xing, nameListTemp);
                } else {
                    nameList.add(contact);
                }

                // 此处开始处理第二级
                if (FISHEYE_DEPTH > 2 && name.length() > 1) {
                    String ming1 = String.valueOf(name.charAt(1));
                    mingMapTemp = xingmingMap.get(contact.xing);
                    //新的姓氏?
                    if (mingMapTemp == null) {
                        mingMapTemp = new ConcurrentHashMap<String, CopyOnWriteArrayList<Name>>();
                        xingmingMap.put(contact.xing,mingMapTemp);
                    }
                    mingSetTemp = mingsMap.get(contact.xing);
                    if (mingSetTemp == null) {
                        mingSetTemp = new CopyOnWriteArraySet<String>();
                        mingsMap.put(contact.xing, mingSetTemp);
                    }
                    mingSetTemp.add(ming1);
                    CopyOnWriteArrayList<Name> mingList = mingMapTemp.get(ming1);
                    // 新名字?
                    if (mingList == null) {
                        CopyOnWriteArrayList<Name> mingListTemp = new CopyOnWriteArrayList<Name>();
                        mingListTemp.add(contact);
                        mingMapTemp.put(ming1, mingListTemp);
                    } else {
                        mingList.add(contact);
                    }
                }
            }
        }
        isReady = true;
    }

    public boolean isReady() {
        return isReady;
    }

    public static void saveFishEyeIndexChar(Context context, String[] indexChar) {
        if (indexChar == null) {
            return;
        }
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < indexChar.length; i++) {
            sb.append(indexChar[i]).append(',');
        }
        prefs.edit().putString(FISH_EYE_INDEX_KEY, sb.toString()).commit();
    }
	public static String[] getFishEyeIndexChar(Context context){
	    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
	    String value = prefs.getString(FISH_EYE_INDEX_KEY, "");
	    String[] indexCharArr;
	    if(TextUtils.isEmpty(value)){
	        indexCharArr = new String[]{};
	    }else{
	        indexCharArr = value.split(",");
	    }
	    return indexCharArr;
	}
}
