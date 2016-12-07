
package com.yunos.alicontacts.dialpad.calllog;

import android.app.ActivityThread;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Handler.Callback;
import android.os.HandlerThread;
import android.os.Message;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.yunos.alicontacts.R;
import com.yunos.alicontacts.activities.BaseActivity;
import com.yunos.alicontacts.database.CallLogManager;
import com.yunos.alicontacts.database.tables.CallsTable;
import com.yunos.alicontacts.dialpad.calllog.CallLogRecordAdapter.CallLogRecordItem;
import com.yunos.common.UsageReporter;

import hwdroid.dialog.AlertDialog;
import hwdroid.dialog.DialogInterface;
import hwdroid.dialog.DialogInterface.OnClickListener;
import hwdroid.dialog.ProgressDialog;
import hwdroid.widget.FooterBar.FooterBarButton;
import hwdroid.widget.FooterBar.FooterBarType.OnFooterItemClick;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;

public class CallLogRecordActivity extends BaseActivity {
    private static final String TAG = "CallLogRecordActivity";
    public static final String EXTRA_RECORD_PATH = "extra_record_path";
    public static final String EXTRA_CALL_URI = "extra_call_uri";
    public static final int MSG_TYPE_REFRESH_VIEWS_BY_DEL = 0;
    public static final int MSG_TYPE_PLAY_RECORD = 1;
    public static final int MSG_TYPE_STOP_RECORD = 2;
    public static final int MSG_SCAN_RECORD_FILE = 3;
    public static final int MSG_SCAN_RECORD_FILE_COMPLETEED = 4;

    private static final int FOOTER_ID_DELETE = 1;
    private static final int FOOTER_ID_CANCEL = 2;

    private ProgressBar mLoadingView;
    private FooterBarButton mFooterBarButton;
    private CallLogRecordAdapter mRecordAdapter;
    private String mRecordPath;

    private HandlerThread mScanRecordFileThread;
    private Handler mScanRecordFileHandler;
    private DeleteRecordTask mDeleteRecordFileTask;

    private boolean mIsDel = false;

    private ProgressDialog mProgressDialog;
    private AlertDialog mDeleteConfirmDialog;
    private LinearLayout mDeleteFooterLayout;
    private TextView mDeleteBtn;

    private CallLogManager mCallLogManager = null;
    private Uri mCallUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setActivityContentView(R.layout.call_log_record_list_activity);

        mCallLogManager = CallLogManager.getInstance(ActivityThread.currentApplication());
        mRecordPath = this.getIntent().getStringExtra(EXTRA_RECORD_PATH);
        mCallUri = this.getIntent().getParcelableExtra(EXTRA_CALL_URI);
        Log.d(TAG, "onCreate: mRecordPath = " + mRecordPath);
        initViews();

        updateFooterBar();

        this.showBackKey(true);

        setHeader();
        initScanTask();
    }

    public boolean isDelMode() {
        return mIsDel;
    }

    private void refreshRecordListItems() {
        mRecordAdapter.notifyDataSetChanged();
        if (mDeleteBtn != null) {
            mDeleteBtn.setEnabled(!mRecordAdapter.isEmpty());
        }
    }

    private void refreshRecordListView(String recordPath) {
        mLoadingView.setVisibility(View.VISIBLE);
        mScanRecordFileHandler.removeMessages(MSG_SCAN_RECORD_FILE);
        Message msg = Message.obtain();
        msg.what = MSG_SCAN_RECORD_FILE;
        msg.obj = recordPath;
        mScanRecordFileHandler.sendMessage(msg);
    }

    private void initViews() {
        mLoadingView = (ProgressBar) this.findViewById(R.id.loading);
        ListView listView = (ListView) this.findViewById(R.id.list_records);
        TextView tv = (TextView) this.findViewById(R.id.txt_empty);
        listView.setEmptyView(tv);
        mRecordAdapter = new CallLogRecordAdapter(this, new CallLogRecordItem[0], mHandler);
        listView.setAdapter(mRecordAdapter);
        listView.setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Log.d(TAG, "listView.onItemClick: position = " + position);

                mRecordAdapter.onRecordListItemClick(position, mIsDel);
                UsageReporter.onClick(CallLogRecordActivity.this, null, UsageReporter.DialpadPage.CALL_RECORD_LISTEN);

            }
        });
        mDeleteBtn = (TextView) findViewById(R.id.footer_delete_btn);
        mDeleteBtn.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                setDelMode();
            }
        });
        mDeleteFooterLayout = (LinearLayout) findViewById(R.id.footer_layout);
        mDeleteFooterLayout.setVisibility(View.GONE);
    }

    private void initScanTask() {
        mScanRecordFileThread = new HandlerThread("ScanRecordFileThread");
        mScanRecordFileThread.start();
        mScanRecordFileHandler = new Handler(mScanRecordFileThread.getLooper(), new Callback() {

            @Override
            public boolean handleMessage(Message msg) {
                switch (msg.what) {
                    case MSG_SCAN_RECORD_FILE:
                        String path = (String) msg.obj;
                        if (path == null) {
                            return false;
                        }
                        File dir = new File(path);
                        ArrayList<CallLogRecordItem> recordItems = new ArrayList<CallLogRecordItem>();
                        if (dir.exists() && dir.isDirectory()) {
                            // get record files
                            File[] files = dir.listFiles(new FilenameFilter() {
                                @Override
                                public boolean accept(File dir, String filename) {
                                    return filename.endsWith(".m4a") || filename.endsWith(".amr") || filename.endsWith(".3gpp");
                                }
                            });
                            // add files to recordItems
                            for (File file : files) {
                                CallLogRecordItem clrItem = new CallLogRecordItem();
                                clrItem.mRecordPath = file.getAbsolutePath();
                                clrItem.mRecordName = file.getName();
                                recordItems.add(clrItem);
                            }
                        }
                        int recordCount = recordItems.size();
                        CallLogRecordItem[] recordItemsArray = recordItems.toArray(new CallLogRecordItem[recordCount]);
                        if (recordCount == 0) {
                            Log.i(TAG, "mScanRecordFileHandler.handleMessage: records have been deleted.");
                            clearCallRecordFromDB();
                        } else if (recordCount > 1) {
                            Arrays.sort(recordItemsArray, new Comparator<CallLogRecordItem>() {
                                @Override
                                public int compare(CallLogRecordItem lhs, CallLogRecordItem rhs) {
                                    // According to FilenameFilter, the file name must have content.
                                    // So it is not necessary to check null on name.
                                    return lhs.mRecordName.compareTo(rhs.mRecordName);
                                }
                            });
                        }
                        Message uiMessage = Message.obtain();
                        uiMessage.what = MSG_SCAN_RECORD_FILE_COMPLETEED;
                        uiMessage.obj = recordItemsArray;
                        mHandler.sendMessage(uiMessage);
                        break;
                    default:
                        break;
                }

                return true;
            }
        });
    }

    @Override
    protected void onDestroy() {
        mScanRecordFileThread.quit();
        if (mDeleteRecordFileTask != null) {
            mDeleteRecordFileTask.cancel(true);
        }
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        if (mIsDel) {
            this.doCancel();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public void onBackKey() {
        if (mIsDel) {
            this.doCancel();
        } else {
            super.onBackKey();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshRecordListView(mRecordPath);
        UsageReporter.onResume(this, null);
    }

    @Override
    protected void onPause() {
        stopMusicPlay();
        if (mDeleteConfirmDialog != null) {
            mDeleteConfirmDialog.dismiss();
            mDeleteConfirmDialog = null;
        }
        super.onPause();
        UsageReporter.onPause(this, null);

    }

    private void stopMusicPlay() {
        if (mRecordAdapter != null) {
            mRecordAdapter.clearPlayingRecordItem();
            mRecordAdapter.stopPlay();
            if (mDeleteBtn != null) {
                mDeleteBtn.setEnabled(!mRecordAdapter.isEmpty());
            }
        } else {
            if (mDeleteBtn != null) {
                mDeleteBtn.setEnabled(false);
            }
        }
    }

    private void updateFooterBar() {
        if (mIsDel) {
            updateFooterBarButton();
        } else {
            updateFooterBarMenu();
        }
    }

    private void updateFooterBarButton() {
        getFooterBarImpl().removeAllViews();
        getFooterBarImpl().setVisibility(View.VISIBLE);

        if (mFooterBarButton == null) {
            mFooterBarButton = new FooterBarButton(this);

            mFooterBarButton.setOnFooterItemClick(new OnFooterItemClick() {
                @Override
                public void onFooterItemClick(View view, int id) {
                    onButtonClicked(id);
                }
            });

            mFooterBarButton.addItem(FOOTER_ID_CANCEL, getString(R.string.cancel));
            mFooterBarButton.addItem(FOOTER_ID_DELETE, getString(R.string.remove));
            mFooterBarButton.updateItems();
        }

        getFooterBarImpl().addView(mFooterBarButton);
        if (mDeleteFooterLayout != null) {
            mDeleteFooterLayout.setVisibility(View.GONE);
        }
    }

    private void updateFooterBarMenu() {
        getFooterBarImpl().setVisibility(View.GONE);
        if (mDeleteFooterLayout != null) {
            mDeleteFooterLayout.setVisibility(View.VISIBLE);
            if (mRecordAdapter != null) {
                mDeleteBtn.setEnabled(!mRecordAdapter.isEmpty());
            } else {
                mDeleteBtn.setEnabled(false);
            }
        }
    }

    public void onButtonClicked(int id) {
        switch (id) {
            case FOOTER_ID_DELETE:
                popupDeleteConfirmDialog();
                break;
            case FOOTER_ID_CANCEL:
                doCancel();
                break;
        }
    }

    private void doCancel() {
        setHeaderChecked(0);
        mIsDel = false;
        updateFooterBar();
        setHeader();
        mRecordAdapter.cancelAllRecordDelTag();
        mFooterBarButton.setItemEnable(FOOTER_ID_DELETE, !mRecordAdapter.isEmpty());
    }

    private void doDelete() {
        mDeleteRecordFileTask = new DeleteRecordTask(this);
        mDeleteRecordFileTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    private void popupDeleteConfirmDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        String message;
        int deleteCount = mRecordAdapter.getDeleteCount();
        if (deleteCount == mRecordAdapter.getCount()) {
            message = getString(R.string.calllog_record_delete_all_dialog_confirm_msg);
        } else {
            message = deleteCount == 1 ? getString(R.string.calllog_record_delete_dialog_confirm_msg_for_one)
                    : getString(R.string.calllog_record_delete_dialog_confirm_msg, deleteCount);
        }
        mDeleteConfirmDialog = builder.setCancelable(true).setMessage(message)
                .setPositiveButton(R.string.calllog_delete, new OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        doDelete();
                    }
                }).setNegativeButton(R.string.no, new OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                }).create();

        mDeleteConfirmDialog.show();
        mDeleteConfirmDialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(Color.RED);
    }

    private void setDelMode() {
        if (mRecordAdapter.isEmpty()) {
            return;
        }
        mIsDel = true;
        this.updateFooterBar();
        mFooterBarButton.setItemEnable(FOOTER_ID_DELETE, false);
        setHeader();
        mRecordAdapter.notifyDataSetChanged();
        int count = mRecordAdapter.getCount();
        if (count == 1) {
            selectAll();
            setHeaderChecked(count);
        } else {
            cancelSelectAll();
            setHeaderChecked(0);
        }

        stopMusicPlay();
    }

    private void setHeader() {
        if (mIsDel) {
            if (this.getAllCheckBox() != null) {
                this.showAllCheckBox();
            } else {
                this.showAllCheckBox(mAllCheckedListener);
            }
        } else {
            this.setTitle2(getString(R.string.call_log_record_title));
            this.hideAllCheckBox();
        }
    }

    private OnAllCheckedListener mAllCheckedListener = new OnAllCheckedListener() {

        @Override
        public void onAllChecked(boolean checked) {
            if (checked) {
                selectAll();
            } else {
                cancelSelectAll();
            }

        }

    };

    private void selectAll() {
        mRecordAdapter.setAllRecordDelTag();
        mFooterBarButton.setItemEnable(FOOTER_ID_DELETE, true);
    }

    private void setHeaderChecked(int num) {
        int total = mRecordAdapter.getCount();
        // mActionBar.setCheckedCount(num, total);

        if (mIsDel) {
            mFooterBarButton.setItemEnable(FOOTER_ID_DELETE, num != 0);

            setAllCheckBoxChecked(num == total);
            setTitle2(getResources().getString(R.string.multiselect_title, num));
            setAllCheckBoxEnabled(total > 0);
        }
    }

    private Handler mHandler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_TYPE_REFRESH_VIEWS_BY_DEL:
                    int delCount = mRecordAdapter.getDeleteCount();
                    setHeaderChecked(delCount);
                    break;
                case MSG_TYPE_PLAY_RECORD:
                    if (mDeleteBtn != null) {
                        mDeleteBtn.setEnabled(false);
                    }
                    break;
                case MSG_TYPE_STOP_RECORD:
                    if (mDeleteBtn != null) {
                        mDeleteBtn.setEnabled(true);
                    }
                    break;
                case MSG_SCAN_RECORD_FILE_COMPLETEED:
                    CallLogRecordItem[] callLogRecordItems = (CallLogRecordItem[]) msg.obj;
                    if (callLogRecordItems.length == 0) {
                        setResult(RESULT_OK);
                        finish();
                        return;
                    }
                    mLoadingView.setVisibility(View.GONE);
                    mRecordAdapter.setCallLogRecordItems(callLogRecordItems);
                    refreshRecordListItems();
                    break;

            }
            super.handleMessage(msg);
        }

    };

    private void cancelSelectAll() {
        mRecordAdapter.cancelAllRecordDelTag();
        mFooterBarButton.setItemEnable(FOOTER_ID_DELETE, false);
    }

    private void createDialog(Context context, int size) {
        if (mProgressDialog == null) {
            mProgressDialog = new ProgressDialog(context);
            mProgressDialog.setMessage(context.getResources().getText(R.string.message_deleting));
            mProgressDialog.setCancelable(false);
            mProgressDialog.setCanceledOnTouchOutside(false);
        }
        mProgressDialog.setProgress(0);
        mProgressDialog.setMax(size);

        mProgressDialog.show();
    }

    private void dismissDialog() {
        if (mProgressDialog != null) {
            mProgressDialog.dismiss();
            mProgressDialog = null;
        }
    }

    class DeleteRecordTask extends AsyncTask<Void, Integer, Void> {
        private Context mContext;
        private ArrayList<CallLogRecordItem> mDeleteItems;

        public DeleteRecordTask(Context context) {
            mContext = context;
            mDeleteItems = mRecordAdapter.getDeleteItems();
        }

        @Override
        protected Void doInBackground(Void... params) {
            String path;

            int i = 0;
            for (CallLogRecordItem item : mDeleteItems) {
                path = item.mRecordPath;
                if (path != null) {
                    File file = new File(path);
                    if (file.exists()) {
                        notifyMediaStore(file);
                        file.delete();
                        this.publishProgress(i++);
                    }
                }
            }
            return null;
        }

        @Override
        protected void onPreExecute() {
            createDialog(mContext, mRecordAdapter.getCount());
        }

        @Override
        protected void onPostExecute(Void result) {
            dismissDialog();
            doCancel();
            refreshRecordListView(mRecordPath);
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            if (mProgressDialog != null) {
                mProgressDialog.incrementProgressBy(1);
            }
            super.onProgressUpdate(values);
        }
    }

    private void notifyMediaStore(File file) {
        if (file == null) {
            return;
        }

        String path = file.getAbsolutePath();
        String where = MediaStore.Audio.Media.DATA + "='" + path + "'";
        getContentResolver().delete(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, where, null);
    }

    private void clearCallRecordFromDB() {
        long callId;
        try {
            callId = ContentUris.parseId(mCallUri);
        } catch (NumberFormatException e) {
            Log.e(TAG, "clearCallRecordFromDB: can not parse id from uri: "+mCallUri, e);
            return;
        }
        Log.i(TAG, "clearCallRecordFromDB: callId=" + callId);

        ContentValues values = new ContentValues(1);
        values.putNull(CallsTable.COLUMN_PHONE_RECORD_PATH);

        mCallLogManager.updateCallLog(values, CallsTable.COLUMN_ID + "=" + callId, null);
    }

}
