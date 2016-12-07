package com.yunos.alicontacts.plugin.duplicateremove;

public class BaseContactEntry {
    public static int RELATED_FLAG_NONE = 0;
    public static int RELATED_FLAG_SAME_NAME = 1;
    public static int RELATED_FLAG_SAME_PHONE = 2;
    public static int RELATED_FLAG_SAME_TWO = 3;
    public static int RELATED_FLAG_SMAX = 4;

    public long mID;
    public String mDisplayName;
    public String mPrimaryPhoneNumber;
    public String mLookupKey;
    // if name or/and phone are same for related contacts
    private boolean mRelatedSameName;
    private boolean mRelatedSamePhone;

    public BaseContactEntry() {

    }

    public void setSameName( ) {
        mRelatedSameName = true;
    }

    public void setSamePhone( ) {
        mRelatedSamePhone = true;
    }

    public boolean isNamePhoneSame() {
        return (mRelatedSameName && mRelatedSamePhone);
    }

    public boolean IsNameSame() {
        return mRelatedSameName;
    }

    public boolean IsPhoneSame() {
        return mRelatedSamePhone;
    }
}
