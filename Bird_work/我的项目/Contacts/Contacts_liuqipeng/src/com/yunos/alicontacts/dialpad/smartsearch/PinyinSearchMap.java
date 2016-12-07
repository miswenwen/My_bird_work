package com.yunos.alicontacts.dialpad.smartsearch;

import java.util.ArrayList;

import android.os.Build;
import android.text.TextUtils;
import android.util.Log;

/**
 * <p>
 * The key for search map is a string, that made from digital characters, '0' to '9'.
 * But in some cases, '2' to '9' (with letters on these digital keys) will be
 * converted to chars with code '2' - 32 to '9' - 32 according to PinyinSearch logic,
 * so the possible characters for search map keys are:
 * '0' to '9' and '2' - 32 to '9' - 32.
 * The char code for these characters are:
 * 48 (0x30) - 57 (0x39) and 18 (0x12) - 25 (0x19).
 * </p>
 * <p>
 * To avoid too many key string objects in memory, we use a tree structure
 * to store the key - value pairs for search map.
 * </p>
 * <p>
 * The key is described as above.
 * The value is an ArrayList<String> that contains "{raw_contact_id}T{number}" of all matched contacts information.
 * </p>
 * <p>
 * The data storage will like the following:
 * <pre>
 * +-----------+
 * | root node |
 * +---+-------+
 *     |   +----------------+
 *     +---+ 1st char: 0x12 |
 *     |   +---+------------+
 *     |       |   +----------------+               +------+------+
 *     |       +---+ 2nd char: 0x12 | // this means | 0x12 | 0x12 |
 *     |       |   +---+------------+               +------+------+
 *     |       |       | // more levels and nodes for 3rd char, fourth char, etc...
 *     |       |       | //      +------+------+------+
 *     |       |       | // e.g. | 0x12 | 0x12 | 0x?? |
 *     |       |       | //      +------+------+------+
 *     |       |   +----------------+               +------+------+
 *     |       +---+ 2nd char: 0x13 | // this means | 0x12 | 0x13 |
 *     |       |   +---+------------+               +------+------+
 *     |       |       | //     +------+------+------+
 *     |       |       | // for | 0x12 | 0x13 | 0x?? |
 *     |       |       | //     +------+------+------+
 *     |       | // more char code values for 2nd char, 18 to 25 and 48 to 57.
 *     |   // more char code values for 1st char...
 * </pre>
 * </p>
 */
public class PinyinSearchMap {
    private static final String TAG = "PinyinSearchMap";
    /**
     * The root node is an entrance to travel all nodes.
     */
    private DigiNode root = new DigiNode();

    public void addDigiPYStringAndSubStrings(String py, String contact) {
        byte[] chars = py.getBytes();
        int length = chars.length;
        for (int i = length; i > 0; i--) {
            for (int j = 0; j + i <= length; j++) {
                addDigiPYByteArray(chars, j, i, contact);
            }
        }
    }

    public ArrayList<String> getContactKeys(String digiPY) {
        if (TextUtils.isEmpty(digiPY)) {
            return null;
        }
        DigiNode current = root;
        int depth = digiPY.length() - 1;
        byte digi;
        for (int i = 0; i < depth; i++) {
            digi = (byte) digiPY.charAt(i);
            current = current.getNextCharNode(digi, false);
            if (current == null) {
                return null;
            }
        }
        if (current == null) {
            return null;
        }
        return current.getContactList((byte)digiPY.charAt(depth), false);
    }

    public synchronized void transferDataTo(PinyinSearchMap map) {
        map.root = this.root;
        this.root = new DigiNode();
    }

    public synchronized void clear() {
        root = new DigiNode();
    }

    /**
     * Add the contact info into the search map, so that the pinyin digits keys can filter the contact.
     * @param chars The chars are the pinyin digits (2-9) sequence, which contains the first pinyin characters of a name.
     *               Each call of this method will use a part of the chars as key to put contact info.
     * @param offset The key will start from the offset.
     * @param length The key length, must be larger than 0.
     * @param contact The contact info to be put in the search map.
     */
    private void addDigiPYByteArray(byte[] chars, int offset, int length, String contact) {
        DigiNode current = root;
        for (int i = 0; i < length - 1; i++) {
            current = current.getNextCharNode(chars[offset + i], true);
            if (current == null) {
                if (!Build.USER.equals(Build.TYPE)) {
                    Log.w(TAG, "addDigiPYByteArray: got null node for "+dumpByteArray(chars, offset, length)+" at "+(offset+i)+", ignore it.");
                }
                break;
            }
        }
        if (current != null) {
            ArrayList<String> contactArray = current.getContactList(chars[offset + length - 1], true);
            if (contactArray != null) {
                int size = contactArray.size();
                /* NOTE: Use != instead of !xx.equals(),
                 * because we compare the instance of string contacts, not value.
                 * The keys for same contact number is added in neighbor rounds,
                 * so if the contact is added multiple times,
                 * the last instance must be the same as the adding one. */
                if ((size == 0) || (contactArray.get(size - 1) != contact)) {
                    contactArray.add(contact);
                }
            }
        }
    }

    private static final char[] hex_chars = {
        '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'
    };

    private String dumpByteArray(byte[] ba, int offset, int length) {
        StringBuilder result = new StringBuilder(32 + 6*length);
        byte b;
        result.append("byte[").append(ba.length).append("] : [").append(offset).append(':').append(length).append("] = {");
        for (int i = 0; i < length; i++) {
            if (i > 0) {
                result.append(", ");
            }
            b = ba[offset+i];
            result.append("0x").append(hex_chars[(b & 0xF0) >> 4]).append(hex_chars[b & 0x0F]);
        }
        result.append('}');
        return result.toString();
    }

    private static class DigiNode {
        private static final int CHARS_NODE_LENGTH = 18;
        private static final byte LOW_RANGE_START = 18; // 0x12
        private static final byte LOW_RANGE_END = 25; // 0x19
        private static final byte HIGH_RANGE_START = 48; // 0x30
        private static final byte HIGH_RANGE_END = 57; // 0x39

        /**
         * This array stores nodes to match next chars.
         * Index 0 to 7 are for char code 18 to 25.
         * Index 8 to 17 are for char code 48 to 57.
         */
        private DigiNode[] nextCharsNode = null;

        /**
         * This array stores the {raw_contact_id}T{number} values in ArrayList<String>.
         * The index in this array is corresponding to the index in nextCharsNode.
         * The char sequence composed from the char codes from root node down to
         * the same index in nextCharsNode is the key,
         * the object stored in the same index in this list is value.
         * Thus we make a map in a tree structure.
         */
        private ArrayList<String>[] contactLists = null;

        public DigiNode() {
        }

        public DigiNode getNextCharNode(byte chCode, boolean createIfNotExist) {
            int idx = getIndexFromCharCode(chCode);
            if (idx < 0) {
                Log.w(TAG, "DigiNode.getNextCharNode: got negative index "+idx+" for char code "+chCode+", ignore it.");
                return null;
            }
            if (nextCharsNode == null) {
                if (!createIfNotExist) {
                    return null;
                }
                nextCharsNode = new DigiNode[CHARS_NODE_LENGTH];
            }
            if ((nextCharsNode[idx] == null) && createIfNotExist) {
                nextCharsNode[idx] = new DigiNode();
            }
            return nextCharsNode[idx];
        }

        private int getIndexFromCharCode(byte code) {
            if ((code >= LOW_RANGE_START) && (code <= LOW_RANGE_END)) {
                return code - LOW_RANGE_START;
            } else if ((code >= HIGH_RANGE_START) && (code <= HIGH_RANGE_END)) {
                return code - HIGH_RANGE_START + (LOW_RANGE_END - LOW_RANGE_START + 1);
            } else {
                return -1;
            }
        }

        private ArrayList<String> getContactList(byte chCode, boolean createIfNotExist) {
            int idx = getIndexFromCharCode(chCode);
            if (idx < 0) {
                Log.w(TAG, "DigiNode.getNextCharNode: got negative index "+idx+" for char code "+chCode+", ignore it.");
                return null;
            }
            if (contactLists == null) {
                if (!createIfNotExist) {
                    return null;
                }
                contactLists = new ArrayList[CHARS_NODE_LENGTH];
            }
            if ((contactLists[idx] == null) && createIfNotExist) {
                contactLists[idx] = new ArrayList<String>();
            }
            return contactLists[idx];
        }

    }
}
