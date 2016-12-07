
package com.yunos.alicontacts.plugin.duplicateremove;

import android.content.ContentValues;
import android.content.Context;
import android.content.res.Resources;
import android.database.Cursor;
import android.media.MediaScannerConnection;
import android.media.MediaScannerConnection.MediaScannerConnectionClient;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.Handler;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.Data;
import android.provider.ContactsContract.RawContacts;
import android.provider.ContactsContract.RawContactsEntity;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.yunos.alicontacts.R;
import com.yunos.alicontacts.aliutil.alivcard.VCardComposer;
import com.yunos.alicontacts.aliutil.alivcard.VCardConfig;
import com.yunos.alicontacts.model.account.AccountType;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public final class CheckContact {
    private static final String TAG = CheckContact.class.getSimpleName();
    private static final Boolean DBG = true;

    private static final int MSG_CHECK_REPEAT_FINISH = 1001;
    private static final int POGRESSBAR_UPDATE_TIMES = 20;

    // private List<ContactRecord> mRecordList = new ArrayList<ContactRecord>();
    private List<ComparableContactEntry> mContactList;
    private List<ComparableContactEntry> mInCompleteContactList = new ArrayList<ComparableContactEntry>();
    private List<ReleatedGroup> mRelatedList = new ArrayList<ReleatedGroup>();
    // private Cursor mCursor;
    private Thread mCheckRepeatThread;
    private Handler mMainThreadHandler;
    private CheckCallback mCallback;
    private Context mContext;

    private boolean mIsBackupTaskNeeded;
    private boolean mIsBackUpTaskStart;
    private boolean mIsCheckRepeatTaskStart;
    private boolean mCancelAllTask;
    private ContactsBackupTask mBackupTask;

    public CheckContact(CheckCallback cb, Context ctx) {
        mCallback = cb;
        mContext = ctx;

        mCheckRepeatThread = new Thread() {

            @Override
            public void run() {
                if (DBG)
                    Log.v(TAG, "CheckRepeat#mThread#run");

                if (mCallback != null) {
                    mCallback.notifyCheckStart();
                }

                checkRepeat();

                if (mCallback != null) {
                    //mCallback.notifyCheckEnd(mInCompleteContactList);
                    setTaskFinished(TaskType.CHECK_REPEAT);
                }

                if (mMainThreadHandler != null) {
                    mMainThreadHandler.sendEmptyMessage(MSG_CHECK_REPEAT_FINISH);
                }

            }

        };
    }

    /**
     * start checking contacts. This task will be done in a new thread, and a
     * message will be sent through handler when finish.
     */
    public void startCheck(boolean isBackupContacts) {
        if (DBG)
            Log.v(TAG, "CheckRepeat#startCheck");

        if (isBackupContacts && mBackupTask == null) {
            Log.v(TAG, "CheckRepeat#startCheck#start backup task");
            mIsBackupTaskNeeded = isBackupContacts;
            mIsBackUpTaskStart = true;
            mBackupTask = new ContactsBackupTask();
            mBackupTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }

        if (mCheckRepeatThread != null) {
            mIsCheckRepeatTaskStart = true;
            mCheckRepeatThread.start();
        }
    }

    /**
     * Cancel inner task threads by outer user.
     * When user exit before finish all check, this API
     * should be called to stop all ongoing thread.
     */
    public void cancelCheck() {
        if(mCheckRepeatThread != null) {
            mCheckRepeatThread.interrupt();
            mCheckRepeatThread = null;
        }

        if(mBackupTask != null) {
            mBackupTask.cancel(true);
            mBackupTask = null;
        }

        mCancelAllTask = true;
    }

    private boolean checkRepeat() {
        // 1.read cursor content to records list
        Log.v(TAG, "checkRepeat -- cursorToList begin at : " + System.currentTimeMillis());
        if (!cursorToList())
            return false;
        Log.v(TAG, "checkRepeat -- cursorToList end at : " + System.currentTimeMillis());

        // 2.get one entry from record list compare with each other in the
        // RecordList

        // contain the CONTACT_ID for contacts that have been added to
        // RelatedEntry or auto merged
        Set<Long> foundIDs = new HashSet<Long>();
//        if(mRelatedList != null) {
//            mRelatedList.clear();
//        }

        if (DBG)
            Log.v(TAG, "CheckContact#check begin at : " + System.currentTimeMillis());
        long checkBeginMill = System.currentTimeMillis();

        ComparableContactEntry current = null;
        ComparableContactEntry contact = null;
        // long current_contact_id = 0;
        ReleatedGroup relatedGroup = null;
        ReleatedGroup identicalGroup = null;
        int size = mContactList.size();

        Log.v(TAG, "CheckContact#list size : " + size);

        int progressStep = Math.max(1, size / POGRESSBAR_UPDATE_TIMES);

        for (int i = 0; i < size; i++) {
            if(mCancelAllTask)
                return false;

            //Log.v(TAG, "CheckContact()#current position : " + i);
            if ((i + 1) % progressStep == 0 && mCallback != null) {
                // mCallback.notifyCheckProgress(i, size);
                updateProgress(TaskType.CHECK_REPEAT, i, size);
            }
            current = mContactList.get(i);
            long current_contact_id = current.mID;
            if (foundIDs.contains(current_contact_id)) {
                // this contact has been handled
                continue;
            }

            for (int j = i + 1; j < size; j++) {
                contact = mContactList.get(j);
                long contact_id = contact.mID;
                if (foundIDs.contains(Long.valueOf(contact_id)) || current_contact_id == contact_id) {
                    // this contact has been handled
                    continue;
                }
                // computerCount ++;
                int compareResult = current.compareTo(contact);

                if (compareResult == ComparableContactEntry.IDENTICAL) {
                    // add to a ReleatedGroup or create a new
                    if (identicalGroup == null) {
                        identicalGroup = new ReleatedGroup();
                        identicalGroup.add(current);
                        foundIDs.add(current_contact_id);
                    }

                    identicalGroup.add(contact);

                    // add the contact_id for this
                    foundIDs.add(Long.valueOf(contact_id));
                } else if (compareResult == ComparableContactEntry.RELATED) {
                    // add to a ReleatedGroup or create a new
                    if (relatedGroup == null) {
                        relatedGroup = new ReleatedGroup();
                        relatedGroup.add(current);
                        foundIDs.add(current_contact_id);
                    }

                    relatedGroup.add(contact);

                    // add the contact_id for this
                    foundIDs.add(Long.valueOf(contact_id));
                }
            }

            // auto merge identical contacts if exists
            if (identicalGroup != null && mCallback != null) {
                // further step for identical check : all data should be the
                // same
                // auto delete the duplicate contacts and delete it's according
                // entry in identical group;
                List<ComparableContactEntry> remains = checkAllColumnsInfo3(identicalGroup);

                // add the new contact_id to relatedEntry if it is not null.
                if (remains != null && remains.size() > 0) {
                    // other info other than name and phone number are not same
                    // Log.v(TAG, "after identical compare, remains : " +
                    // remains);
                    if (relatedGroup == null) {
                        relatedGroup = new ReleatedGroup();
                        relatedGroup.add(current);
                    }
                    relatedGroup.addAll(remains);
                }
            }

            // call callback function for merge related contacts
            if (relatedGroup != null && mCallback != null) {
                mCallback.mergeReleateContacts(relatedGroup.mGroup);
                mRelatedList.add(relatedGroup);
            }

            relatedGroup = null;
            identicalGroup = null;
        }

        Log.v(TAG, "CheckRepeat#checkRepeat end");
        Log.v(TAG, "CheckContact# check consume : "
                + (System.currentTimeMillis() - checkBeginMill));

        // remove contacts in mInCompleteContactList if they exist in foundIDs
        Iterator<ComparableContactEntry> iter = mInCompleteContactList.iterator();
        while (iter.hasNext()) {
            ComparableContactEntry entry = iter.next();
            if (foundIDs.contains(entry.mID)) {
                iter.remove();
            }
        }

        return true;
    }

    private boolean cursorToList() {
        Log.v(TAG, "CheckRepeat#cursorToList--into");
//        if (mInCompleteContactList != null) {
//            mInCompleteContactList.clear();
//        }

        // Only support to manage contacts in local account or YunOS account.
        String selection
                = Data.MIMETYPE + "!='" + ContactsContract.CommonDataKinds.Photo.CONTENT_ITEM_TYPE
                + "' AND (" + RawContacts.ACCOUNT_TYPE + "='" + AccountType.LOCAL_ACCOUNT_TYPE
                + "' OR " + RawContacts.ACCOUNT_TYPE + "='" + AccountType.YUNOS_ACCOUNT_TYPE + "')";

        Cursor cursor = null;
        try {
            if(mContext == null) {
                return false;
            }
            cursor = mContext.getContentResolver().query(AllDataRowsQuery.CONTENT_URI,
                    AllDataRowsQuery.PROJECTION, selection, null, AllDataRowsQuery.SORT);
            Log.v(TAG, "CheckRepeat#cursorToList--after query db");

            if (cursor == null) {
                return false; // query failure
            }

            mContactList = new ArrayList<ComparableContactEntry>(cursor.getCount() / 2);

            // generate the BaseContatct list for each contact in Contact table
            // identify the incomplete contacts simultaneously
            if (mContactList == null) {
                return false;
            }

            if (cursor.moveToFirst()) {
                // int pos = 0;
                List<Long> duplicateDataIds = new ArrayList<Long>();
                long cur_id = cursor.getLong(AllDataRowsQuery.CONTACT_ID);
                ComparableContactEntry cur_contact = new ComparableContactEntry(cur_id,
                        cursor.getString(AllDataRowsQuery.DISPLAY_NAME),
                        cursor.getString(AllDataRowsQuery.LOOKUP_KEY));
                mContactList.add(cur_contact);
                long last_id = cur_id;
                StringBuilder builder = new StringBuilder();
                do {
                    builder.setLength(0);
                    cur_id = cursor.getLong(AllDataRowsQuery.CONTACT_ID);
                    if (last_id != cur_id) {
                        // check if last contact is incomplete
                        if (cur_contact.mCheckType == ComparableContactEntry.CHECK_TYPE_INCOMPLETE_ONLY_NAME
                                || cur_contact.mCheckType == ComparableContactEntry.CHECK_TYPE_INCOMPLETE_ONLY_PHONE
                                || cur_contact.mCheckType == ComparableContactEntry.CHECK_TYPE_INCOMPLETE_ONLY_OTHERS) {
                            this.mInCompleteContactList.add(cur_contact);
                        }/*
                          * else if (cur_contact.mCheckType ==
                          * ComparableContactEntry
                          * .CHECK_TYPE_INCOMPLETE_ONLY_PHONE) {
                          * this.mOnlyPhoneContactList.add(cur_contact); }
                          */

                        // new contact data rows group
                        cur_contact = new ComparableContactEntry(cur_id,
                                cursor.getString(AllDataRowsQuery.DISPLAY_NAME),
                                cursor.getString(AllDataRowsQuery.LOOKUP_KEY));
                        mContactList.add(cur_contact);
                        last_id = cur_id;
                    }

                    // generate ContentValues for one data row
                    // if data mimetype is known and not photo
                    String mimetype = cursor.getString(AllDataRowsQuery.MIMETYPE);
                    Integer index = ComparableContactEntry.MIME_TYPE_MAP.get(mimetype);
                    ContentValues rowValues = new ContentValues();
                    if (index == null || index == 3) {
                        // not standard mimetype or photo mimetype, continue to
                        // next record ???
                        continue;
                    } // else if (index == 3) {
                      // if MIMETYPE is Photo
                      // DATA14(Integer) : ID of the hi-res photo file.
                      // DATA15(BLOB) : By convention, binary data is stored in
                      // DATA15. The thumbnail of the photo is stored in this
                      // column.
                      // Now only copy DATA14
                      // rowValues.put(AllDataRowsQuery.PROJECTION[19],
                      // cursor.getString(19));
                      // rowValues.put(AllDataRowsQuery.PROJECTION[20],
                      // cursor.getBlob(20));
                      // }
                    else {
                        // not photo
                        rowValues.put(Data.MIMETYPE, mimetype);
                        int[] offsets = ComparableContactEntry.COL_OFFSETS[index];
                        int len = offsets.length;
                        for (int i = 0; i < len; i++) {
                            // add each meaningful columns' Key/Value string of
                            // this
                            // row to ContentValue
                            rowValues.put(AllDataRowsQuery.PROJECTION[offsets[i] + 6],
                                    cursor.getString(offsets[i] + AllDataRowsQuery.DATA1));
                        }
                    }

                    if (mimetype.equals(CommonDataKinds.Phone.CONTENT_ITEM_TYPE)) {
                        // add phone number
                        if (cur_contact.addPhoneNumberRow(cursor.getString(AllDataRowsQuery.DATA1),
                                true, rowValues) == 0) {
                            // add to row ids list
                            // Log.v(TAG,"cursorToList() : duplicate phone number with number = "
                            // + cursor.getString(AllDataRowsQuery.DATA1));
                            duplicateDataIds.add(cursor.getLong(AllDataRowsQuery._ID));
                        }
                    } else if (mimetype.equals(CommonDataKinds.Nickname.CONTENT_ITEM_TYPE)
                            || mimetype.equals(CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)) {
                        cur_contact.addNameRow(rowValues);
                    } else {
                        cur_contact.addOthersRow(rowValues);
                    }

                    cur_contact.mCount++;
                    // pos++;
                } while (cursor.moveToNext());

                if (duplicateDataIds.size() > 0 && mCallback != null) {
                    mCallback.removeIdenticalPhoneNumberDataRow(duplicateDataIds);
                }

                if (cur_contact.mCheckType == ComparableContactEntry.CHECK_TYPE_INCOMPLETE_ONLY_NAME
                        || cur_contact.mCheckType == ComparableContactEntry.CHECK_TYPE_INCOMPLETE_ONLY_PHONE
                        || cur_contact.mCheckType == ComparableContactEntry.CHECK_TYPE_INCOMPLETE_ONLY_OTHERS) {
                    this.mInCompleteContactList.add(cur_contact);
                }
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return true;
    }

    // use custom hash function
    private List<ComparableContactEntry> checkAllColumnsInfo3(ReleatedGroup group) {
        if (group == null) {
            return null;
        }

        int size = group.size();
        if (size < 2) {
            // return group.mGroup;
            Log.e(TAG, "ReleatedGroup size < 2");
            return null;
        }

        HashSet<Long> valuesSet = new HashSet<Long>(size);
        List<ComparableContactEntry> rtn = new ArrayList<ComparableContactEntry>();
        //Log.v(TAG, "dupl_ty checkAllColumnsInfo3() begin");
        for (int i = 0; i < size; i++) {
            ComparableContactEntry c = group.mGroup.get(i);
            /*
             * if(cursorMap.containsValue(id)) { continue; }
             */
            if (!valuesSet.add(c.allDataHash())) {
                // add failure, which means that this is duplicate with one in
                // set
                // delete current data record with ID = id
                if (mCallback != null) {
                    List<ComparableContactEntry> identicalList = new ArrayList<ComparableContactEntry>();
                    identicalList.add(c);
                    mCallback.removeIdenticalContacts(identicalList);
                }
            } else if (i > 0) {
                rtn.add(c);
            }

        }

        return rtn;
    }

    /**
     * Interface for handle the check results
     *
     * @author tianyuan.ty
     */
    public static interface CheckCallback {
        /**
         * When find the identical contacts, this function will be called to
         * notify the user object
         *
         * @param identical_set a BaseContactItem set of identical contacts
         */
        void removeIdenticalContacts(List<? extends BaseContactEntry> identical_set);

        /**
         * When find the related contacts, this function will be called to
         * notify the user object
         *
         * @param identical_set a BaseContactItem set of related contacts
         */
        void mergeReleateContacts(List<? extends BaseContactEntry> related_set);

        /**
         * When found the identical phone number rows in one contact, this
         * function will be called to notify user the ids set need to be
         * deleted.
         *
         * @param identical_data_ids_set list of ids of Data rows need to be
         *            deleted
         */
        void removeIdenticalPhoneNumberDataRow(List<Long> identical_data_ids_set);

        /**
         * Notify user of check's start
         */
        void notifyCheckStart();

        /**
         * Notify user of check's end
         */
        // void notifyCheckEnd(List<? extends BaseContactEntry> onlyNameList,
        // List<? extends BaseContactEntry> onlyPhoneList);

        void notifyCheckEnd(List<? extends BaseContactEntry> inCompleteList);

        /**
         * Notify user of check's progress
         */
        void notifyCheckProgress(int current, int total);
    }

    /**
     * @author tianyuan.ty Inner class. Store the related ComparableContactEntry
     *         object as one group.
     */
    static public class ReleatedGroup {
        public List<ComparableContactEntry> mGroup = new ArrayList<ComparableContactEntry>();

        public ReleatedGroup() {

        }

        public boolean addAll(Collection<ComparableContactEntry> contacts) {
            if (contacts != null && contacts.size() > 0) {
                return mGroup.addAll(contacts);
            }

            return false;
        }

        /*
         * public void add(List<Long> idList) { if(idList != null &&
         * idList.size() > 0) { mGroup.addAll(idList); } }
         */

        public boolean add(ComparableContactEntry contact) {
            return mGroup.add(contact);
        }

        public int size() {
            return mGroup.size();
        }

        public Set<Long> getAllContactIDs() {
            if (mGroup == null) {
                return null;
            }

            Set<Long> ids = new HashSet<Long>();
            int cnt = size();

            for (int i = 0; i < cnt; i++) {
                ids.add(mGroup.get(i).mID);
            }

            return ids;
        }
    }

    // Query for all data rows of one contact or all contacts
    public interface AllDataRowsQuery {
        final static Uri CONTENT_URI = Data.CONTENT_URI;
        final static String SELECTION = Data.CONTACT_ID + "=? AND "
                + Data.MIMETYPE + "!='" + ContactsContract.CommonDataKinds.Photo.CONTENT_ITEM_TYPE + "'";
        final static String SORT = Data.CONTACT_ID; // + "," + Data.MIMETYPE;

        final static String[] PROJECTION = {
                Data.CONTACT_ID, // ...........................................0
                Data.DISPLAY_NAME, // .........................................1
                Data.RAW_CONTACT_ID, // .......................................2
                Data.MIMETYPE, // .............................................3
                Data.IS_PRIMARY, // ...........................................4
                // Data.IS_SUPER_PRIMARY, //
                // .....................................5
                Data.LOOKUP_KEY, // ............................................5
                Data.DATA1, // ................................................6
                Data.DATA2, // ................................................7
                Data.DATA3, // ................................................8
                Data.DATA4, // ................................................9
                Data.DATA5, // ................................................10
                Data.DATA6, // ................................................11
                Data.DATA7, // ................................................12
                Data.DATA8, // ................................................13
                Data.DATA9, // ................................................14
                Data.DATA10, // ................................................15
                Data.DATA11, // ................................................16
                Data.DATA12, // ................................................17
                Data.DATA13, // ................................................18
                Data.DATA14, // ................................................19
                Data.DATA15, // ................................................20
                Data._ID, // ................................................21
        };

        final static String[] DATA_PROJECTION = {
                Data.CONTACT_ID, // ...........................................0
                Data.DISPLAY_NAME, // .........................................1
                Data.RAW_CONTACT_ID, // .......................................2
                Data.MIMETYPE, // .............................................3
        };

        public final static int CONTACT_ID = 0;
        public final static int DISPLAY_NAME = 1;
        public final static int RAW_CONTACT_ID = 2;
        public final static int MIMETYPE = 3;
        public final static int IS_PRIMARY = 4;
        // public final static int IS_SUPER_PRIMARY = 5;
        public final static int LOOKUP_KEY = 5;
        public final static int DATA1 = 6;
        public final static int _ID = 21;
    }

    public ArrayList<ContentValues> getDataContentValueList(List<? extends BaseContactEntry> group) {
        ArrayList<ContentValues> dataList = new ArrayList<ContentValues>();
        int size = group.size();
        for (int i = 0; i < size; i++) {
            ComparableContactEntry entry = (ComparableContactEntry) group.get(i);

            entry.addAllData(dataList);
        }
        return dataList;
    }

    public static final boolean isInCompleteContacts(Context context, long contactId) {
        if (context == null || contactId <= 0) {
            Log.e(TAG, "isInCompleteContacts maybe contact is not exist!");
            return true;
        }

        Cursor cursor = null;
        try {
            cursor = context.getContentResolver().query(AllDataRowsQuery.CONTENT_URI,
                    AllDataRowsQuery.DATA_PROJECTION, AllDataRowsQuery.SELECTION, new String[] {
                        String.valueOf(contactId)
                    }, AllDataRowsQuery.SORT);

            int checkType = ComparableContactEntry.CHECK_TYPE_INIT;
            if (cursor != null && cursor.getCount() > 0) {
                // Log.v(TAG, "isInCompleteContacts() contactId:" + contactId +
                // ", cursor.getCount():"
                // + cursor.getCount());
                while (cursor.moveToNext()) {
                    String mimeType = cursor.getString(AllDataRowsQuery.MIMETYPE);
                    if (CommonDataKinds.Nickname.CONTENT_ITEM_TYPE.equals(mimeType)
                            || CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE.equals(mimeType)) {
                        checkType |= ComparableContactEntry.CHECK_TYPE_INCOMPLETE_ONLY_NAME;
                    } else if (CommonDataKinds.Phone.CONTENT_ITEM_TYPE.equals(mimeType)) {
                        checkType |= ComparableContactEntry.CHECK_TYPE_INCOMPLETE_ONLY_PHONE;
                    } else {
                        checkType |= ComparableContactEntry.CHECK_TYPE_INCOMPLETE_ONLY_OTHERS;
                    }
                }

                // Log.v(TAG, "isInCompleteContacts() checkType:" + checkType);
                if (isCheckTypeNormal(checkType)) {
                    return false;
                }
            } else {
                // this statements means contactId is deleted, not existed any
                // more.
                return false;
            }
        } catch (Exception e) {
            Log.e(TAG, "Exception:" + e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return true;
    }

    static boolean isCheckTypeNormal(int checkType) {
        int bitsCount = 0;

        while (checkType != 0) {
            checkType &= checkType - 1;
            bitsCount++;
        }

        return bitsCount >= ComparableContactEntry.CHECK_TYPE_NORMAL_BITS_COUNT;
    }

    private enum TaskType {
        CHECK_REPEAT, BACKUP_CONTACTS
    }

    private int mBackupPercent;
    private int mCheckRepeatPercent;
    private Object mProgressLock = new Object();
    private Object mTaskLock = new Object();

    private void updateProgress(TaskType type, int current, int total) {
        synchronized (mProgressLock) {
            switch (type) {
                case  BACKUP_CONTACTS:
                    if(mIsBackupTaskNeeded)
                        mBackupPercent = current * 100 / total;
                    break;
                case CHECK_REPEAT:
                    mCheckRepeatPercent = current * 100 / total;
                    break;
            }

            if (mIsBackupTaskNeeded) {
                mCallback.notifyCheckProgress(mBackupPercent + mCheckRepeatPercent, 200);
            } else {
                mCallback.notifyCheckProgress(mCheckRepeatPercent, 100);
            }
        }
    }

    private synchronized void setTaskFinished(TaskType type) {
        synchronized (mTaskLock) {
            switch (type) {
                case BACKUP_CONTACTS:
                    mIsBackUpTaskStart = false;
                    break;
                case CHECK_REPEAT:
                    mIsCheckRepeatTaskStart = false;
                    break;
            }

            if(mIsBackupTaskNeeded) {
                if (!mIsBackUpTaskStart && !mIsCheckRepeatTaskStart) {
                    mCallback.notifyCheckEnd(mInCompleteContactList);
                }
            } else if (!mIsCheckRepeatTaskStart) {
                mCallback.notifyCheckEnd(mInCompleteContactList);
            }
        }
    }

    /**
     * AsyncTask which will save all contacts to vcf on storage
     */
    private class ContactsBackupTask extends AsyncTask<String, Integer, Integer> {
        private static final String TASK_TAG = "ContactsBackupTask";

        private int mFileIndexMinimum;
        private int mFileIndexMaximum;
        private File mTargetDirectory;
        private String mFileNamePrefix;
        private String mFileNameSuffix;
        private String mFileNameExtension;
        private Set<String> mExtensionsToConsider;
        private String mSaveToPath = null;

        public ContactsBackupTask() {
            final Resources resources = mContext.getResources();
            mFileIndexMinimum = resources.getInteger(R.integer.config_export_file_min_index);
            mFileIndexMaximum = resources.getInteger(R.integer.config_export_file_max_index);

            mTargetDirectory = Environment.getExternalStorageDirectory();
            mFileNamePrefix = resources.getString(R.string.config_export_file_prefix);
            mFileNameSuffix = resources.getString(R.string.config_export_file_suffix);
            mFileNameExtension = resources.getString(R.string.config_export_file_extension);

            mExtensionsToConsider = new HashSet<String>();
            mExtensionsToConsider.add(mFileNameExtension);
        }

        @Override
        protected Integer doInBackground(String... params) {
            VCardComposer composer = null;
            Writer writer = null;
            Log.i(TASK_TAG, "doInBackground() start");
            try {
                if (isCancelled()) {
                    Log.i(TASK_TAG, "Contacts backup is cancelled before handling the request");
                    return 0;
                }

                final int vcardType;
                final String exportType = "default";
                if (TextUtils.isEmpty(exportType)) {
                    vcardType = VCardConfig.getVCardTypeFromString(mContext
                            .getString(R.string.config_export_vcard_type));
                } else {
                    vcardType = VCardConfig.getVCardTypeFromString(exportType);
                }

                composer = new VCardComposer(mContext,
                        vcardType | VCardConfig.FLAG_REFRAIN_PHONE_NUMBER_FORMATTING, true);

                // for test
                // int vcardType = (VCardConfig.VCARD_TYPE_V21_GENERIC |
                // VCardConfig.FLAG_USE_QP_TO_PRIMARY_PROPERTIES);
                // composer = new VCardComposer(ExportVCardActivity.this,
                // vcardType,
                // true);

                final Uri contentUriForRawContactsEntity = RawContactsEntity.CONTENT_URI
                        .buildUpon().appendQueryParameter(RawContactsEntity.FOR_EXPORT_ONLY, "1")
                        .build();
                String selection = null;
                // need consider this case ???
                // if (!ContactsUtils.isShowSimInList(mService) &&
                // SimUtil.IS_YUNOS) {
                // selection = Contacts.INDEX_IN_SIM + " =-1";
                // }
                if (!composer.init(Contacts.CONTENT_URI, new String[] {
                    Contacts._ID
                }, selection, null, null, contentUriForRawContactsEntity)) {
                    final String errorReason = composer.getErrorReason();
                    Log.e(TASK_TAG, "initialization of vCard composer failed: " + errorReason);
                    return 0;
                }

                final int total = composer.getCount();
                if (total == 0) {
                    Log.i(TASK_TAG, "Backup exit because not contacts can be exported");
                    return 0;
                }

                final String vcfPathStr = getAppropriateDestination(mTargetDirectory);
                if (vcfPathStr == null) {
                    Log.i(TASK_TAG,
                            "Contacts backup exit because not found suitable save path");
                    return 0;
                }
                final Uri uri = Uri.parse("file://" + vcfPathStr);
                Log.i(TASK_TAG, "doInBackground() # Uri = " + uri.toString());
                final OutputStream outputStream;
                try {
                    outputStream = mContext.getContentResolver().openOutputStream(uri);
                } catch (FileNotFoundException e) {
                    Log.w(TASK_TAG, "FileNotFoundException thrown", e);
                    // Need concise title.
                    return 0;
                } catch (Exception e) {
                    Log.w(TASK_TAG, "doInBackground()  throw exception", e);
                    return 0;
                }
                writer = new BufferedWriter(new OutputStreamWriter(outputStream));

                int current = 1; // 1-origin
                while (!composer.isAfterLast()) {
                    if (isCancelled() || mCancelAllTask) {
                        Log.i(TASK_TAG, "Backup is cancelled during composing vCard");
                        return 0;
                    }
                    try {
                        writer.write(composer.createOneEntry());
                    } catch (IOException e) {
                        final String errorReason = composer.getErrorReason();
                        Log.e(TASK_TAG, "Failed to read a contact: " + errorReason);
                        return 0;
                    }

                    // vCard export is quite fast (compared to import), and
                    // frequent
                    // notifications
                    // bother notification bar too much.
                    if (current % 100 == 1) {
                        // doProgressNotification(uri, total, current);
                        publishProgress(new Integer[] {
                                current, total
                        });
                    }
                    current++;
                }
                Log.i(TASK_TAG, "Successfully finished exporting vCard " + uri);

                final CustomMediaScannerConnectionClient client = new CustomMediaScannerConnectionClient(
                        uri.getPath());
                client.start();
            } finally {
                if (composer != null) {
                    composer.terminate();
                }
                if (writer != null) {
                    try {
                        writer.close();
                    } catch (IOException e) {
                        Log.w(TASK_TAG, "IOException is thrown during close(). Ignored. " + e);
                    }
                }
            }
            return null;
        }

        @Override
        protected void onPreExecute() {
            mIsBackUpTaskStart = true;
            super.onPreExecute();
        }

        @Override
        protected void onPostExecute(Integer result) {
            //mIsBackupTaskStart = false;
            if (result == null) { // Here add 100% export case for toast user.
                Toast.makeText(mContext, mContext.getString(R.string.export_vcard_done, mSaveToPath), Toast.LENGTH_LONG).show();
            } else { // Here add fail export case for toast user.
                Toast.makeText(mContext, mContext.getString(R.string.export_vcard_fail), Toast.LENGTH_LONG).show();
            }
            setTaskFinished(TaskType.BACKUP_CONTACTS);
            super.onPostExecute(result);
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            updateProgress(TaskType.BACKUP_CONTACTS, values[0], values[1]);
            super.onProgressUpdate(values);
        }

        /**
         * Returns an appropriate file name for vCard export. Returns null when
         * impossible.
         *
         * @return destination path for a vCard file to be exported. null on
         *         error and mErrorReason is correctly set.
         */
        private String getAppropriateDestination(final File destDirectory) {
            /*
             * Here, file names have 5 parts: directory, prefix, index, suffix,
             * and extension. e.g. "/mnt/sdcard/prfx00001sfx.vcf" ->
             * "/mnt/sdcard", "prfx", "00001", "sfx", and ".vcf" (In default,
             * prefix and suffix is empty, so usually the destination would be
             * /mnt/sdcard/00001.vcf.) This method increments "index" part from
             * 1 to maximum, and checks whether any file name following naming
             * rule is available. If there's no file named
             * /mnt/sdcard/00001.vcf, the name will be returned to a caller. If
             * there are 00001.vcf 00002.vcf, 00003.vcf is returned. There may
             * not be any appropriate file name. If there are 99999 vCard files
             * in the storage, for example, there's no appropriate name, so this
             * method returns null.
             */

            // Count the number of digits of mFileIndexMaximum
            // e.g. When mFileIndexMaximum is 99999, fileIndexDigit becomes 5,
            // as we will count the
            int fileIndexDigit = 0;
            {
                // Calling Math.Log10() is costly.
                int tmp;
                for (fileIndexDigit = 0, tmp = mFileIndexMaximum; tmp > 0; fileIndexDigit++, tmp /= 10) {
                }
            }

            // %s05d%s (e.g. "p00001s")
            final StringBuilder bodyFormatBuilder = new StringBuilder();
            bodyFormatBuilder.append("%s%0").append(fileIndexDigit).append("d%s");
            final String bodyFormat = bodyFormatBuilder.toString();

            for (int i = mFileIndexMinimum; i <= mFileIndexMaximum; i++) {
                boolean numberIsAvailable = true;
                final String body = String.format(bodyFormat, mFileNamePrefix, i, mFileNameSuffix);
                // Make sure that none of the extensions of
                // mExtensionsToConsider matches. If this
                // number is free, we'll go ahead with mFileNameExtension (which
                // is included in
                // mExtensionsToConsider)
                for (String possibleExtension : mExtensionsToConsider) {
                    final File file = new File(destDirectory, body + '.' + possibleExtension);
                    if (file.exists()) {
                        numberIsAvailable = false;
                        break;
                    }
                }
                if (numberIsAvailable) {
                    mSaveToPath = body + '.' + mFileNameExtension;
                    return new File(destDirectory, mSaveToPath)
                            .getAbsolutePath();
                }
            }

            Log.w(TASK_TAG,
                    "Reached vCard number limit. Maybe there are too many vCard in the storage");
            // mErrorReason = getString(R.string.fail_reason_too_many_vcard);
            return null;
        }

    }

    private class CustomMediaScannerConnectionClient implements MediaScannerConnectionClient {
        final MediaScannerConnection mConnection;
        final String mPath;

        public CustomMediaScannerConnectionClient(String path) {
            mConnection = new MediaScannerConnection(mContext, this);
            mPath = path;
        }

        public void start() {
            mConnection.connect();
        }

        @Override
        public void onMediaScannerConnected() {
            // if (DEBUG) { Log.d(LOG_TAG,
            // "Connected to MediaScanner. Start scanning."); }
            mConnection.scanFile(mPath, null);
        }

        @Override
        public void onScanCompleted(String path, Uri uri) {
            // if (DEBUG) { Log.d(LOG_TAG, "scan completed: " + path); }
            mConnection.disconnect();
        }
    }

}
