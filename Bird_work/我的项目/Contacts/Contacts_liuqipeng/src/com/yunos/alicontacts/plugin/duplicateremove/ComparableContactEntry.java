
package com.yunos.alicontacts.plugin.duplicateremove;

import android.content.ContentValues;
import android.provider.ContactsContract.CommonDataKinds;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.Data;
import android.text.TextUtils;
import android.util.Log;

import com.yunos.alicontacts.plugin.duplicateremove.CheckContact.AllDataRowsQuery;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

/**
 * This structure contains the following base information for one contact
 * -- contact_id of one contact
 * -- count of data rows in data table
 * -- contact type : normal, incomplete, etc.
 * -- custom hash code set for all the sorted phone numbers string
 * -- custom hash code for display name
 *
 * @author tianyuan.ty
 */
public class ComparableContactEntry extends BaseContactEntry {
    public final static String TAG = ComparableContactEntry.class.getSimpleName();

    // one contact complete check result type
    static final int CHECK_TYPE_INIT = 0;
    static final int CHECK_TYPE_INCOMPLETE_ONLY_NAME = 1;
    static final int CHECK_TYPE_INCOMPLETE_ONLY_PHONE = 2;
    static final int CHECK_TYPE_INCOMPLETE_ONLY_OTHERS = 4;
    static final int CHECK_TYPE_NORMAL = 8;
    static final int CHECK_TYPE_NORMAL_BITS_COUNT = 2;

    // contacts compare results
    static final public int IDENTICAL = 0;
    static final public int UNRELATED = 1;
    static final public int RELATED = 2;

    // MIMETYPE to Integer table
    final static HashMap<String, Integer> MIME_TYPE_MAP = new HashMap<String, Integer>(16);

    // column index numbers' offset to DATA1 of each CONTENT ITEM TYPE in Data Table
    final static int COL_OFFSETS[][] = {
            {
                    0, 1, 2, 3, 4, 5, 6, 7, 8
            }, // StructuredName
            {
                    0, 1, 2
            }, // Phone
            {
                    0, 1, 2
            }, // Email
            {
                    13, 14
            }, // Photo
            {
                    0, 1, 2, 3, 4, 5, 6, 7, 8, 9
            }, // Organization
            {
                    0, 1, 2, 4, 5
            }, // Im
            {
                    0, 1, 2
            }, // Nickname
            {
                0
            }, // Note
            {
                    0, 1, 2, 3, 4, 5, 6, 7, 8, 9
            }, // StructuredPostal
            {
                0
            }, // GroupMembership
            {
                    0, 1, 2
            }, // Website
            {
                    0, 1, 2
            }, // Event
            {
                    0, 1, 2
            }, // Relation
            {
                    0, 1, 2
            }
    // SipAddress
    };

    static {
        MIME_TYPE_MAP.put(CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE, 0);
        MIME_TYPE_MAP.put(CommonDataKinds.Phone.CONTENT_ITEM_TYPE, 1);
        MIME_TYPE_MAP.put(CommonDataKinds.Email.CONTENT_ITEM_TYPE, 2);
        MIME_TYPE_MAP.put(CommonDataKinds.Photo.CONTENT_ITEM_TYPE, 3);
        MIME_TYPE_MAP.put(CommonDataKinds.Organization.CONTENT_ITEM_TYPE, 4);
        MIME_TYPE_MAP.put(CommonDataKinds.Im.CONTENT_ITEM_TYPE, 5);
        MIME_TYPE_MAP.put(CommonDataKinds.Nickname.CONTENT_ITEM_TYPE, 6);
        MIME_TYPE_MAP.put(CommonDataKinds.Note.CONTENT_ITEM_TYPE, 7);
        MIME_TYPE_MAP.put(CommonDataKinds.StructuredPostal.CONTENT_ITEM_TYPE, 8);
        MIME_TYPE_MAP.put(CommonDataKinds.GroupMembership.CONTENT_ITEM_TYPE, 9);
        MIME_TYPE_MAP.put(CommonDataKinds.Website.CONTENT_ITEM_TYPE, 10);
        MIME_TYPE_MAP.put(CommonDataKinds.Event.CONTENT_ITEM_TYPE, 11);
        MIME_TYPE_MAP.put(CommonDataKinds.Relation.CONTENT_ITEM_TYPE, 12);
        MIME_TYPE_MAP.put(CommonDataKinds.SipAddress.CONTENT_ITEM_TYPE, 13);
    }

    int mCount;
    int mCheckType = CHECK_TYPE_INIT;

    List<String> mPhoneStrings = new ArrayList<String>();
    List<String> mPhoneTrimmedStrings = new ArrayList<String>(); // number without hyphen and ws
    //List<Long> mPhonesHashs = new ArrayList<Long>();
    //long mNameHash;
    List<ContentValues> mAllDataValuesList = new ArrayList<ContentValues>();

    ComparableContactEntry(long id, String displayName, String lookupKey) {
        mID = id;
        mCount = 0;
        mDisplayName = displayName;
        mLookupKey = lookupKey;
        //mNameHash = displayName == null ? 0 : Utils.hash(displayName);
    }

    /**
     * 当mCheckType中1的个数大于等于CHECK_TYPE_NORMAL_BITS_COUNT，即>=2时，我们认为该条联系人是完整的。
     */
    private void ensureCheckType() {
        if ((mCheckType & CHECK_TYPE_NORMAL) == CHECK_TYPE_NORMAL) {
            return;
        }

        int bitsCount = 0;
        int checkType = mCheckType;

        while (checkType != 0) {
            checkType &= checkType - 1;
            bitsCount++;
        }

        if (bitsCount >= CHECK_TYPE_NORMAL_BITS_COUNT) {
            mCheckType = CHECK_TYPE_NORMAL;
        }
    }

    /**
     * @param number
     * @param isSuperPrimary
     * @return -1 for no number input
     *          1 for insert successfully
     *          0 for duplicate number
     */
    final int addPhoneNumberRow(String number, boolean isSuperPrimary, ContentValues dataRowValues) {
        if (TextUtils.isEmpty(number))
            return -1;

        Log.v(TAG, "addPhoneNumberRow() : number = " + number);
        int result = 1;
        // String normalizeNumber = Utils.phoneNormalize(number);
        // Log.v(TAG, "Phone Number for hash : " + number);
        if (isSuperPrimary) {
            this.mPrimaryPhoneNumber = number;
        }

        //Long numberHash = Utils.hash(Utils.trimHyphenAndSpaceInNumberString(number));
        String trimmedNumber = Utils.trimHyphenAndSpaceInNumberString(number);
        //if (mPhonesHashs.contains(numberHash)) {
        if(mPhoneTrimmedStrings.contains(trimmedNumber)) {
            // duplicate number in one contact
            result = 0;
        } else {
            // duplicate number will not be a part of all data comparation
            mPhoneStrings.add(number);
            mPhoneTrimmedStrings.add(trimmedNumber);
            //mPhonesHashs.add(numberHash);
            mAllDataValuesList.add(dataRowValues);
        }

        mCheckType |= CHECK_TYPE_INCOMPLETE_ONLY_PHONE;
        ensureCheckType();

        return result;
    }

    final void addNameRow(ContentValues dataRowValues) {
        // this contact has StructureName or NickName rows in data table
        mCheckType |= CHECK_TYPE_INCOMPLETE_ONLY_NAME;
        ensureCheckType();

        mAllDataValuesList.add(dataRowValues);
    }

    final void addOthersRow(ContentValues dataRowValues) {
        // others than Phone or Name rows
        mCheckType |= CHECK_TYPE_INCOMPLETE_ONLY_OTHERS;

        ensureCheckType();
        mAllDataValuesList.add(dataRowValues);
    }

    final int compareTo(ComparableContactEntry other) {
        // name and number are both unequal --> unrelated
        int result = IDENTICAL;

        // now, we do not compare record under same contact_id.
        // ??? If we need to arrange the same information under the one
        // contact_id
        // if(this.mID == other.mID)
        // return result;

        //if (this.mNameHash == other.mNameHash) {
        if ((this.mDisplayName == null && other.mDisplayName == null)
                || (this.mDisplayName != null && this.mDisplayName.equals(other.mDisplayName)) ) {
            // one of name and number is equal
            // result = RELATED;
            this.setSameName();
            other.setSameName();
        } else {
            result = UNRELATED;
        }

        int[] indexs = findFirstSameBetweenTwoList(this.mPhoneTrimmedStrings, other.mPhoneTrimmedStrings);
        if (indexs != null) {
            if (result == UNRELATED) {
                // name and number are both equal -- identical, but need compare other rows
                result = RELATED;
            }
            this.setSamePhone();
            this.mPrimaryPhoneNumber = mPhoneStrings.get(indexs[0]);
            other.setSamePhone();
            other.mPrimaryPhoneNumber = other.mPhoneStrings.get(indexs[1]);
        }

        // Log.v("TAG", "result --3  : " + result);
        return result;
    }

    // Custom hash function which will generate the same hash value(long type)
    // for contacts have the same records
    // without regard of the order in database
    public final long allDataHash( ) {
        long customHash = 0;
        int count = mAllDataValuesList.size();
        ArrayList<Long> values = new ArrayList<Long>((count >= 0 ? count : 0));
        //Log.v(TAG, "allDataHash -- count = " + count + " , pos = " + mStartPos);
        if (count > 0) {
            StringBuilder builder = new StringBuilder();
            for (ContentValues curRow : mAllDataValuesList) {
                //Log.v(TAG, "allDataHash -- iterate 1# for " + count);
                builder.setLength(0);
                String mimetype = curRow.getAsString(Data.MIMETYPE);
                Integer index = MIME_TYPE_MAP.get(mimetype);

                if (index == null || index == 3) {
                    // not standard mimetype or photo column, continue to next
                    // record ???
                    continue;
                }
                builder.append(index);
                if(index == 1) {
                    // special for Phone Mimetype
                    // phone number need to trim whitespace and hyphen sign
                    builder.append(Utils.trimHyphenAndSpaceInNumberString(curRow.getAsString(Phone.NUMBER)));//DATA1
                    builder.append(curRow.getAsString(Phone.TYPE));//DATA2
                    builder.append(curRow.getAsString(Phone.LABEL));//DATA3
                } else {
                    int[] offsets = COL_OFFSETS[index];
                    int len = offsets.length;
                    for (int i = 0; i < len; i++) {
                        // add each meaningful columns'String value to String builder
                        builder.append(curRow.getAsString(AllDataRowsQuery.PROJECTION[offsets[i] + 6]));
                    }
                }
                Log.v(TAG, "allDataHash -- one row for this contact : " + builder.toString());
                // add custom hash value to array
                values.add(Utils.hash(builder.toString()));
                //Log.v(TAG, "allDataHash -- iterate 3# for " + count);
            }
        }

        //Log.v(TAG, "allDataHash -- hash array list  " + values);
        int size = values.size();
        if (size == 0) {
            return customHash;
        }
        // sort the array list, now use the java default sorting
        Collections.sort(values);

        // compute the final hash value by hashing the addition of all value in
        // values list
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < size; i++) {
            sb.append(values.get(i));
        }

        Log.v(TAG, "allDataHash() -- final string : " + sb.toString());

        customHash = Utils.hash(sb.toString());

        return customHash;
    }

    public final void addAllData(ArrayList<ContentValues> dataList) {
//        int count = mCount;
//        if (cursor != null && count > 0 && cursor.moveToPosition(mStartPos)) {
//            do {
//                count--;
//                ContentValues row = new ContentValues();
//                String mimetype = cursor.getString(AllDataRowsQuery.MIMETYPE);
//
//                Integer index = MIME_TYPE_MAP.get(mimetype);
//                if (index == null) {
//                    // not standard mimetype, continue to next record ???
//                    continue;
//                } else if (index == 3) {
//                    // if MIMETYPE is Photo
//                    // DATA14(Integer) : ID of the hi-res photo file.
//                    // DATA15(BLOB) : By convention, binary data is stored in
//                    // DATA15. The thumbnail of the photo is stored in this
//                    // column.
//                    // Now only copy DATA14
//                    row.put(AllDataRowsQuery.PROJECTION[19], cursor.getString(19));
//                    row.put(AllDataRowsQuery.PROJECTION[20], cursor.getBlob(20));
//                } else {
//                    int[] offsets = COL_OFFSETS[index];
//                    int len = offsets.length;
//                    for (int i = 0; i < len; i++) {
//                        // add each meaningful columns' Key/Value string of this
//                        // row to ContentValue
//                        row.put(AllDataRowsQuery.PROJECTION[offsets[i] + 6],
//                                cursor.getString(offsets[i] + AllDataRowsQuery.DATA1));
//                    }
//                }
//
//                // add to list
//                row.put(Data.MIMETYPE, mimetype);
//                dataList.add(row);
//
//            } while (count > 0 && cursor.moveToNext());
//        }
        dataList.addAll(mAllDataValuesList);
    }

    /**
     * @param first first set for find
     * @param second second set for find
     * @return index if same Long in first set, or -1 in case no same
     */
//    private static final int[] findFirstSameBetweenTwoSets(List<Long> first, List<Long> second) {
//        long outer = 0;
//        int first_size = first.size();
//        int second_size = second.size();
//        for (int i = 0; i < first_size; i++) {
//            outer = first.get(i);
//            for (int j = 0; j < second_size; j++) {
//                if (second.get(j) == outer) {
//                    return new int[] {i,j};
//                }
//            }
//        }
//
//        return null;
//    }

    /**
     * @param first first list for find
     * @param second second set for find
     * @return index if same Long in first set, or -1 in case no same
     */
    private static final <T> int [] findFirstSameBetweenTwoList(List<T> first, List<T> second) {
        T outer = null;
        int first_size = first.size();
        int second_size = second.size();
        for (int i = 0; i < first_size; i++) {
            outer = first.get(i);
            for (int j = 0; j < second_size; j++) {
                if (second.get(j).equals(outer)) {
                    return new int[] {i,j};
                }
            }
        }

        return null;
    }

}
