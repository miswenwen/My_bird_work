package com.yunos.alicontacts.dialpad.smartsearch;

import android.util.Log;

import com.yunos.alicontacts.R;

public class MatchResult {
    private static final String TAG = "MatchResult";

    public static final MatchResult SEPARATOR_PHONE_CONTACTS = makeSeparator(R.string.matched_contacts_separator);
    public static final MatchResult SEPARATOR_CALL_LOGS = makeSeparator(R.string.matched_call_logs_separator);
    public static final MatchResult SEPARATOR_YELLOW_PAGE = makeSeparator(R.string.matched_yellow_page_separator);
    public static final MatchResult SEPARATOR_QUICK_CALL = makeSeparator(R.string.quick_call);

    /*
     * The search results are sorted in the following sections:
     * 1. name match phone contacts.
     * 2. number match phone contacts.
     * 3. name match yellow page contacts.
     * 4. number match yellow page contacts.
     * 5. number match call logs.
     * 6. speed dial add directly
     * For each section above, the results in it are sorted in the following order:
     * 1. match position.
     * 2. match length.
     * 3. match part text.
     * 4. whole name or number text.
     * 5. speed dial match single number
     * We use an int type value to quick sort the search result.
     * The bits in the int value is used as following:
     * bit 31: sign bit, not used by sort.
     * bit 19 - bit 30: reserved.
     * bit 16 - bit 18: 3-bit, section type.
     * bit 8 - bit 15: 8-bit, match position, if the match position is larger than 255, then treat it as 255.
     * bit 0 - bit 7: 8-bit, match length, if match length is larger than 255, then treat it as 255.
     */
    public static final int PHONE_CONTACT_NAME_SECTION = 0 << 16;
    public static final int PHONE_CONTACT_NUMBER_SECTION = 1 << 16;
    public static final int CALL_LOG_NUMBER_SECTION = 2 << 16;
    public static final int YELLOW_PAGE_CONTACT_NAME_SECTION = 3 << 16;
    public static final int YELLOW_PAGE_CONTACT_NUMBER_SECTION = 4 << 16;

    public static final int MAX_MATCH_POSITION = 255;
    public static final int MAX_MATCH_LENGTH = 255;

    public static final byte TYPE_SEPARATOR = -1;
    public static final byte TYPE_CONTACTS = 0;
    public static final byte TYPE_CALLOG = 1;
    public static final byte TYPE_YELLOWPAGE = 2;
    public static final byte TYPE_QUICK_CALL = 3;

    /** The name matches the search input. */
    public static final byte MATCH_PART_NAME            = 0x01;
    /** The phone number matches the search input. */
    public static final byte MATCH_PART_PHONE_NUMBER    = 0x02;

    // The weight uses bit0-bit18, so int type is required.
    public int matchWeight;
    public String key;
    public byte type;
    /** Indicate which part of the contact info is matched.
     * This field is a bit set. If multiple parts are matched,
     * then the field is bit-or result of MATCH_PART_XXX constants. */
    public byte matchPart;
    public boolean numberFullMatch = false;
    public boolean nameFullMatch = false;
    public short numberMatchStart = -1;
    public short numberMatchLength = -1;
    public short nameMatchStart = -1;
    public short nameMatchLength = -1;

    public int separatorNameResId;

    public String phoneNumber;
    public String matchedNumberPart;
    public String mLocation = null;

    public String name;
    public String matchedNamePart;

    public String logoFileName;
    public String logoUri;
    /**
     * This field represents the id column in database for the match item.
     * For yellow page, it is shop / business id.
     * For call log, it is call id.
     * For contacts, it is raw contact id. */
    public long databaseID;

    private static MatchResult makeSeparator(int sepNameResId) {
        MatchResult mr = new MatchResult();
        mr.type = TYPE_SEPARATOR;
        mr.separatorNameResId = sepNameResId;
        return mr;
    }

    public void setNameMatchRange(int start, int length) {
        int nameLen = name.length();
        for (int i = 0; i < nameLen; i++) {
            char ch = name.charAt(i);
            int chCode = ch;
            if (HanziUtil.hasPinyin(chCode)
                    || ((ch >= 'a') && (ch <= 'z'))
                    || ((ch >= 'A') && (ch <= 'Z'))
                    || ((ch >= '0') && (ch <= '9'))) {
                // characters that can find a digit to represent it in pinyin search.
                continue;
            }
            // now the character can not be represented by a digit, e.g. a symbol
            // such character is ignored in pinyin search, but will be displayed in match list.
            // so the match start and length shall be adjusted for this character.
            if (i <= start) {
                // we have a non-digit char before match start.
                // when we first get the start, we do not know this char,
                // so we need to move the start one char toward the tail.
                start++;
            } else if (i < (start+length)) {
                // the non-digit char appears in the match range.
                // so we need to increase the match range to make room for this char.
                length++;
            } else {
                break;
            }
        }
        // If we have no logic error in match process,
        // then the start and (start+length) shall be in the nameLen range.
        // but for fault tolerance, we correct the range here.
        if (start >= nameLen) {
            Log.w(TAG, "setNameMatchRange: got invald start: "+start+" for name ["+name+"].");
            start = nameLen - 1;
        }
        if ((start + length) > nameLen) {
            Log.w(TAG, "setNameMatchRange: got invalid length: "+length+" for name ["+name+"].");
            length = nameLen - start;
        }
        nameMatchStart = (short) (start > MAX_MATCH_POSITION ? MAX_MATCH_POSITION : start);
        nameMatchLength = (short) (length > MAX_MATCH_LENGTH ? MAX_MATCH_LENGTH : length);
        if ((start == 0) && (length == name.length())) {
            nameFullMatch = true;
            matchedNamePart = name;
        } else {
            nameFullMatch = false;
            matchedNamePart = name.substring(start, start + length);
        }
    }

    public void setNumberMatchRange(int start, int length) {
        // assume the phoneNumber is already normalized.
        int numLen = phoneNumber.length();
        if (start >= numLen) {
            Log.w(TAG, "setNumberMatchRange: got invalid start: "+start);
            start = numLen - 1;
        }
        if ((start + length) > numLen) {
            Log.w(TAG, "setNumberMatchRange: got invalid length: "+length);
            length = numLen - start;
        }
        numberMatchStart = (short) (start > MAX_MATCH_POSITION ? MAX_MATCH_POSITION : start);
        numberMatchLength = (short) (length > MAX_MATCH_LENGTH ? MAX_MATCH_LENGTH : length);
        if ((start == 0) && (length == phoneNumber.length())) {
            numberFullMatch = true;
            matchedNumberPart = phoneNumber;
        } else {
            numberFullMatch = false;
            matchedNumberPart = phoneNumber.substring(start, start + length);
        }
    }

    // weight shall be calculated after all fields set, especially the match part related info.
    public void calculateWeight() {
        int weightSectionPart = getSectionWeight();
        int weightMatchPositionPart = getMatchPositionWeight();
        int weightMatchLengthPart = getMatchLengthWeight();
        matchWeight = weightSectionPart | weightMatchPositionPart | weightMatchLengthPart;
    }

    private int getSectionWeight() {
        // the weight for section is already shifted 16-bit in constants,
        // so return directly here.
        switch (type) {
        case TYPE_CONTACTS:
            return (matchPart & MATCH_PART_NAME) == MATCH_PART_NAME ? PHONE_CONTACT_NAME_SECTION : PHONE_CONTACT_NUMBER_SECTION;
        case TYPE_YELLOWPAGE:
            return (matchPart & MATCH_PART_NAME) == MATCH_PART_NAME ? YELLOW_PAGE_CONTACT_NAME_SECTION : YELLOW_PAGE_CONTACT_NUMBER_SECTION;
        case TYPE_CALLOG:
            return CALL_LOG_NUMBER_SECTION;
        default:
            Log.e(TAG, "getSectionWeight: invalid type: "+type+"; this shall not happen.");
            throw new IllegalArgumentException("Invalid MatchResult type: "+type);
        }
    }

    private int getMatchPositionWeight() {
        // the weight for match position shall be shifted 8-bit here.
        if ((matchPart | MATCH_PART_NAME) == MATCH_PART_NAME) {
            return (nameMatchStart >= MAX_MATCH_POSITION ? MAX_MATCH_POSITION : nameMatchStart) << 8;
        } else {
            return (numberMatchStart >= MAX_MATCH_POSITION ? MAX_MATCH_POSITION : numberMatchStart) << 8;
        }
    }

    private int getMatchLengthWeight() {
        // The weight for length 255 is higher than the weight for length 0,
        // so we use MAX_MATCH_LENGTH - match length.
        if ((matchPart | MATCH_PART_NAME) == MATCH_PART_NAME) {
            return nameMatchLength >= MAX_MATCH_LENGTH ? 0 : MAX_MATCH_LENGTH - nameMatchLength;
        } else {
            return numberMatchLength >= MAX_MATCH_LENGTH ? 0 : MAX_MATCH_LENGTH - numberMatchLength;
        }
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder(256);
        result.append("{key=\"").append(key)
              .append("\"; type=").append(type);
        switch (type) {
        case TYPE_SEPARATOR:
            result.append("(TYPE_SEPARATOR)");
            break;
        case TYPE_CONTACTS:
            result.append("(TYPE_CONTACTS)");
            break;
        case TYPE_CALLOG:
            result.append("(TYPE_CALLOG)");
            break;
        case TYPE_YELLOWPAGE:
            result.append("(TYPE_YELLOWPAGE);logoFileName:").append(logoFileName);
            result.append(";logoUri:").append(logoUri);
            result.append(";databaseID:").append(databaseID);
            break;
        default:
            Log.w(TAG, "toString: unrecognized type "+type);
            break;
        }
        result.append("; matchPart=").append(matchPart);
        if ((matchPart & MATCH_PART_NAME) == MATCH_PART_NAME) {
            result.append("(MATCH_PART_NAME)");
        }
        if ((matchPart & MATCH_PART_PHONE_NUMBER) == MATCH_PART_PHONE_NUMBER) {
            result.append("(MATCH_PART_PHONE_NUMBER)");
        }
        result.append("; matchWeight=").append(matchWeight)
              .append("; phoneNumber=\"").append(phoneNumber)
              .append("\"; matchedNumberPart=\"").append(matchedNumberPart)
              .append("\"; numberMatchStart=").append(numberMatchStart)
              .append("; numberMatchLength=").append(numberMatchLength)
              .append("; name=\"").append(name)
              .append("\"; matchedNamePart=\"").append(matchedNamePart)
              .append("\"; nameMatchStart=").append(nameMatchStart)
              .append("; nameMatchLength=").append(nameMatchLength)
              .append("\"}");
        return result.toString();
    }

}
