/**
 *
 */

package com.yunos.alicontacts.dialpad.calllog;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.CallLog.Calls;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.yunos.alicontacts.R;
import com.yunos.alicontacts.activities.BaseActivity;
import com.yunos.alicontacts.database.CallLogManager;
import com.yunos.common.UsageReporter;

import hwdroid.dialog.AlertDialog;
import hwdroid.dialog.DialogInterface;
import hwdroid.dialog.ProgressDialog;

import java.util.ArrayList;


/**
 * @author lxd
 */
public class CallLogMultiSelectActivity extends BaseActivity implements
        CallLogQueryHandler.Listener, OnItemClickListener {

    private static final String TAG = "CallLogMultiSelectActivity";

    public static final String EXTRA_KEY_FILTER_TYPE = "extra_key_filter_type";

    //private ListView mListView;
    private ContentResolver mResolver;

    private int mFilterType;
    private ProgressDialog mProgressDialog ;
    private AlertDialog mDeleteConformDialog;

    private CallLogAdapter mCallLogAdapter;
    private CallLogQueryHandler mCallLogQueryHandler;
    private CallLogManager mCallLogManager;
    private CallLogChangeListenerForMultiSelect mCallLogListener;

    private Context mContext;
    private TextView mDelteBtn;

    private boolean mCancelWorkFlag;

    //private int mFilterType = CallLogQueryHandler.QUERY_CALLS_ALL;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setActivityContentView(R.layout.ali_calllog_delete);
        mContext = getApplicationContext();

        mResolver = getContentResolver();
        //mIdsBuffer = new StringBuffer();

        ListView  listView = (ListView) findViewById(R.id.call_log_delete_list);
        mCallLogAdapter = new CallLogAdapter(this);
        mCallLogAdapter.setModeCurrent(CallLogAdapter.MODE_SELECT);
        mCallLogQueryHandler = new CallLogQueryHandler(this);

        listView.setAdapter(mCallLogAdapter);
        listView.setOnItemClickListener(this);

        mFilterType = getIntent().getIntExtra(EXTRA_KEY_FILTER_TYPE, CallLogQueryHandler.QUERY_CALLS_ALL);

        mCallLogManager = CallLogManager.getInstance(getApplicationContext());
        mCallLogListener = new CallLogChangeListenerForMultiSelect();
        mCallLogManager.registCallsTableChangeListener(mCallLogListener);
        startCallsQuery();

        showAllCheckBox(new OnAllCheckedListener() {
            @Override
            public void onAllChecked(boolean checked) {
                handleAllChecked(checked);
            }
        });
        showBackKey(true);
        mDelteBtn = (TextView) findViewById(R.id.footer_delete_btn);
        mDelteBtn.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                deleteBtnClick();
            }
        });
        mDelteBtn.setEnabled(false);
    }

    @Override
    protected void onDestroy() {
        // BugID:61842:StrictMode policy violation: com.yunos.alicontacts
        // release resources when quit
        mCallLogAdapter.releaseCursor();
        if (mDeleteConformDialog != null) {
            mDeleteConformDialog.dismiss();
            mDeleteConformDialog = null;
        }
        mCallLogManager.unRegistCallsTableChangeListener(mCallLogListener);
        super.onDestroy();
    }

    @Override
    protected void onResume() {
        super.onResume();
        overridePendingTransition(0, 0);
        UsageReporter.onResume(this, null);
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(0, 0);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if(mProgressDialog != null) {
            mProgressDialog.dismiss();
        }
        UsageReporter.onPause(this, null);
    }

    @Override
    public boolean onCallsFetched(Cursor cursor) {
        if (isFinishing()) {
            return false;
        }

        if(cursor == null || cursor.getCount() == 0) {
            Toast.makeText(this, R.string.dialpad_callog_empty_text, Toast.LENGTH_SHORT).show();
            finish();
            return false;
        }

        mCallLogAdapter.setLoading(false);
        mCallLogAdapter.changeCursor(cursor);
        updateHeaderCheckStatus();
        return true;
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Log.i(TAG, "onItemClick: position="+position+"; id="+id);
        mCallLogAdapter.toggleChecked(position, id, view);
        updateHeaderCheckStatus();
    }

    private void startCallsQuery() {
        if (mCallLogAdapter != null && !mCallLogAdapter.isLoading()) {
            mCallLogAdapter.setLoading(true);
            mCallLogQueryHandler.queryCalls(mFilterType);
        }
    }

    private void deleteBtnClick() {
        int checkedCount = mCallLogAdapter.getCheckedCount();
        if (checkedCount <= 0) {
            Toast.makeText(getApplicationContext(), R.string.dialpad_callog_empty_text, Toast.LENGTH_SHORT).show();
            return ;
        }
        String message;
        final AlertDialog.Builder build = new AlertDialog.Builder(this);
        if (checkedCount == mCallLogAdapter.getCount()) {
            message = getString(R.string.calllog_delete_all_dialog_confirm_msg);

        } else {
            message = checkedCount == 1 ? getResources().getString(R.string.calllog_delete_some_dialog_confirm_msg_for_one) :
                    getResources().getString(R.string.calllog_delete_some_dialog_confirm_msg, checkedCount);
        }
        build.setMessage(message);
        build.setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                deleteCallLog();
            }
        });
        build.setNegativeButton(R.string.no, null);
        mDeleteConformDialog = build.create();
        mDeleteConformDialog.show();
        mDeleteConformDialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(Color.RED);
    }

    private void deleteCallLog() {
        final ArrayList<Long> checkedIds = mCallLogAdapter.getCheckedIds();
        if ((checkedIds == null) || (checkedIds.size() == 0)) {
            return;
        }

        new AsyncTask<Void, String, Integer>() {
            @Override
            protected Integer doInBackground(Void... params) {
                StringBuilder ids = new StringBuilder(100);
                int deleteCount = 0;
                int idCount = 0;
                int totalDeleteCount = checkedIds.size();
                for (int i = 0; i < totalDeleteCount; i++) {
                    if (mCancelWorkFlag) {
                        return deleteCount;
                    }

                    if (ids.length() > 0) {
                        ids.append(',');
                    }
                    ids.append(checkedIds.get(i));
                    if ((++idCount) >= 5) {
                        // Delete selected contacts.
                        int count = mResolver.delete(
                                Calls.CONTENT_URI,
                                Calls._ID + " IN (" + ids.toString() + ")",
                                null);
                        deleteCount += count;

                        idCount = 0;
                        ids.setLength(0);
                        if (mProgressDialog != null) {
                            mProgressDialog.incrementProgressBy(count);
                        }
                    }
                }

                if (mCancelWorkFlag) {
                    return deleteCount;
                }

                if (idCount > 0) {
                    int count = mResolver.delete(Calls.CONTENT_URI,
                            Calls._ID + " IN (" + ids.toString() + ")",
                            null);
                    deleteCount += count;

                    if (mProgressDialog != null) {
                        mProgressDialog.incrementProgressBy(count);
                    }
                }

                UsageReporter
                        .onClick(
                                CallLogMultiSelectActivity.this,
                                null,
                                UsageReporter.DialpadPage.DP_DELETE_CALLLOG_FROM_SELECT);
                return deleteCount;
            }

            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                mProgressDialog = new ProgressDialog(CallLogMultiSelectActivity.this);
                mProgressDialog.setCancelable(true);
                mProgressDialog
                        .setMessage(getString(R.string.calllog_delete_dialog_msg));
                mProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
                mProgressDialog.setMax(checkedIds.size());

                mProgressDialog
                        .setOnCancelListener(new DialogInterface.OnCancelListener() {

                            @Override
                            public void onCancel(DialogInterface dialog) {

                                mCancelWorkFlag = true;
                                finish();
                            }
                        });

                mProgressDialog
                        .setOnDismissListener(new DialogInterface.OnDismissListener() {

                            @Override
                            public void onDismiss(DialogInterface dialog) {
                                finish();
                            }
                        });

                mProgressDialog.show();
            }

            @Override
            protected void onPostExecute(Integer result) {
                super.onPostExecute(result);
                if (mProgressDialog != null) {
                    mProgressDialog.dismiss();
                    mProgressDialog = null;
                }

                if (result <= 0) {
                    Toast.makeText(mContext,
                            getString(R.string.calllog_delete_fail),
                            Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(mContext,
                            getString(R.string.calllog_delete_success),
                            Toast.LENGTH_SHORT).show();
                    finish();
                }
            }

        }.execute();
    }

    private void updateHeaderCheckStatus() {
        int checkedCount = mCallLogAdapter.getCheckedCount();
        int totalCount = mCallLogAdapter.getCount();
        setTitle2(getResources().getString(R.string.calllog_picker_title, checkedCount));
        setAllCheckBoxChecked(checkedCount == totalCount);
        mDelteBtn.setEnabled(checkedCount != 0);
    }

    public void handleAllChecked(boolean checked) {
        mCallLogAdapter.setAllChecked(checked);
        updateHeaderCheckStatus();
    }

    private class CallLogChangeListenerForMultiSelect
            implements CallLogManager.CallLogChangeListener {
        @Override
        public void onCallLogChange(int changedPart) {
            Log.d(TAG, "CallLogChangeListenerForMultiSelect onCallLogChange.");
            startCallsQuery();
        }
    }

}
