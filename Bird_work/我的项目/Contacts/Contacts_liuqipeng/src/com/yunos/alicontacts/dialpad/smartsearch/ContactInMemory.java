package com.yunos.alicontacts.dialpad.smartsearch;

import java.util.ArrayList;

public class ContactInMemory {
    String pinyinData;
    String allFistCharacter;
    /** The length of searchable part (from start) in contactName. */
    public int length;
    public long version;
    ArrayList<String> retArray;
    public String contactName;
    public String phoneNumber;
    public String area;

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(128);
        sb.append("{pinyinData:\"").append(pinyinData)
                .append("\"; allFirstCharacter:\"").append(allFistCharacter)
                .append("\"; Name:\"").append(contactName)
                .append("\"; phoneNumber:\"").append(phoneNumber)
                .append("\"; area:\"").append(area).append("\"; length:")
                .append(length).append("; version:").append(version)
                .append("; retArray:[");
        if (retArray == null) {
            sb.append("null");
        } else {
            String[] array = retArray.toArray(new String[retArray.size()]);
            int length = array.length;
            for (int i = 0; i < length; i++) {
                if (i > 0) {
                    sb.append(',');
                }
                sb.append('\"').append(array[i]).append('\"');
            }
        }
        sb.append("]}");
        return sb.toString();
    }
}
