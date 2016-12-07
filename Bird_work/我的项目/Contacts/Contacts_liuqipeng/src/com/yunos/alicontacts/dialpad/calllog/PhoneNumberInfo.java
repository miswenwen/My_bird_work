package com.yunos.alicontacts.dialpad.calllog;

import android.database.Cursor;

import com.yunos.alicontacts.database.CallDetailQuery;
import com.yunos.alicontacts.util.AliTextUtils;

public class PhoneNumberInfo {

    public final String formattedNumber;
    public final String tagName;
    public final int markedCount;
    public final String shopName;
    public final String location;

    public PhoneNumberInfo(String formattedNumber,
            String tagName, int markedCount,
            String shopName,
            String location) {
        this.formattedNumber = formattedNumber;
        this.tagName = tagName;
        this.markedCount = markedCount;
        this.shopName = shopName;
        this.location = location;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(256);
        sb.append("PhoneNumberInfo:{formattedNumber=\"").append(AliTextUtils.desensitizeNumber(formattedNumber))
          .append("\"; tagName=\"").append(tagName)
          .append("\"; markedCount=").append(markedCount)
          .append("; shopName=\"").append(shopName)
          .append("\"; location=\"").append(location)
          .append("\"}");
        return sb.toString();
    }

    public static PhoneNumberInfo fromLocalCallDetailCursor(Cursor c) {
        String formattedNumber = c.getString(CallDetailQuery.FORMATTED_NUM_COLUMN_INDEX);
        String tagName = c.getString(CallDetailQuery.TAG_NAME_COLUMN_INDEX);
        int markedCount = c.getInt(CallDetailQuery.MARKED_COUNT_COLUMN_INDEX);
        String shopName = c.getString(CallDetailQuery.YP_NAME_COLUMN_INDEX);
        String prov = c.getString(CallDetailQuery.LOC_PROVINCE_COLUMN_INDEX);
        String area = c.getString(CallDetailQuery.LOC_AREA_COLUMN_INDEX);
        return new PhoneNumberInfo(formattedNumber, tagName, markedCount, shopName, AliTextUtils.makeLocation(prov, area));
    }

}
