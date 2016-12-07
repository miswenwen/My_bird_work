package com.yunos.alicontacts.conference;

import android.net.Uri;

import com.yunos.alicontacts.database.util.NumberNormalizeUtil;

public class Recipient {
    public final Uri uri;
    public final String name;
    public final String number;
    public final String formattedNumber;
    private String mLocation = null;

    public Recipient(Uri uri, String name, String number) {
        this.uri = uri;
        this.name = name;
        this.number = number;
        this.formattedNumber = NumberNormalizeUtil.normalizeNumber(number, true);
    }

    public void setLocation(String location) {
        mLocation = location;
    }

    public String getLocation() {
        return mLocation;
    }

}
