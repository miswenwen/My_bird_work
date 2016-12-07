
package com.yunos.alicontacts.dialpad.calllog;

import android.content.Context;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ImageView;
import android.widget.TextView;

import com.yunos.alicontacts.R;

import java.io.IOException;
import java.util.ArrayList;

public class CallLogRecordAdapter extends BaseAdapter {
    private static final String TAG = "CallLogRecordAdapter";
    private CallLogRecordItem[] mCallLogRecordItems;
    private CallLogRecordActivity mActivity;
    private Handler mHandler;
    public int mCurrPlayPosition = -1;

    private MediaPlayer mMediaPlay = null;
    private boolean mIsplay;

    private final static int RECORDING_TIME_OFFSET = 1000;

    private Handler mUpdateTimeHandler = new Handler();

    @Override
    public int getCount() {
        return mCallLogRecordItems.length;
    }

    @Override
    public Object getItem(int position) {
        return mCallLogRecordItems[position];
    }

    public CallLogRecordAdapter(CallLogRecordActivity activity, CallLogRecordItem[] items, Handler handler) {
        mActivity = activity;
        setCallLogRecordItems(items);
        mHandler = handler;
    }

    public void setCallLogRecordItems(CallLogRecordItem[] items) {
        // When called from constructor, mCallLogRecordItems is null.
        // In all other cases, mCallLogRecordItems shall not be null.
        if (mCallLogRecordItems != null) {
            mergeDeleteCheckMarkFromExistingItems(items);
        }
        mCallLogRecordItems = items;
    }

    private void mergeDeleteCheckMarkFromExistingItems(CallLogRecordItem[] newItems) {
        int existingIndex = 0;
        int newIndex = 0;
        CallLogRecordItem existingItem, newItem;
        int compareResult;
        while ((existingIndex < mCallLogRecordItems.length) && (newIndex < newItems.length)) {
            existingItem = mCallLogRecordItems[existingIndex];
            newItem = newItems[newIndex];
            compareResult = existingItem.mRecordName.compareTo(newItem.mRecordName);
            if (compareResult == 0) {
                newItem.mIsDel = existingItem.mIsDel;
                existingIndex++;
                newIndex++;
            } else if(compareResult > 0) {
                newIndex++;
            } else {
                existingIndex++;
            }
        }
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder viewHolder = null;
        if (convertView == null) {
            convertView = LayoutInflater.from(mActivity).inflate(R.layout.call_log_record_item, parent, false);
            viewHolder = new ViewHolder();
            viewHolder.mNameView = (TextView) convertView.findViewById(R.id.txt_record_name);
            viewHolder.mCheckBox = (CheckBox) convertView.findViewById(R.id.cb_del_selete);
            viewHolder.mTimeView = (TextView) convertView.findViewById(R.id.txt_record_time);
            viewHolder.mPlayView = (ImageView) convertView.findViewById(R.id.btn_play_record);
            viewHolder.mCheckBox.setOnCheckedChangeListener(mCheckedChangeListener);
            viewHolder.mPlayView.setOnClickListener(mAudioPlayerListener);
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }

        CallLogRecordItem item = mCallLogRecordItems[position];
        viewHolder.mPlayView.setVisibility(View.GONE);
        viewHolder.mTimeView.setVisibility(View.GONE);

        if (mActivity.isDelMode()) {
            viewHolder.mCheckBox.setVisibility(View.VISIBLE);
        } else {
            viewHolder.mCheckBox.setVisibility(View.GONE);
            if (mCurrPlayPosition == position) {
                viewHolder.mPlayView.setVisibility(View.VISIBLE);
                viewHolder.mTimeView.setVisibility(View.VISIBLE);
                if (mMediaPlay != null) {
                    String time = getTimeViewText(mMediaPlay.getCurrentPosition(), mMediaPlay.getDuration());
                    viewHolder.mTimeView.setText(time);
                }
                if (mIsplay) {
                    viewHolder.mPlayView.setImageResource(R.drawable.call_record_pause_selector);
                } else {
                    viewHolder.mPlayView.setImageResource(R.drawable.call_record_play_selector);
                }
            }
        }

        viewHolder.mCheckBox.setTag(position);
        viewHolder.mPlayView.setTag(position);
        viewHolder.mCheckBox.setChecked(item.mIsDel);
        viewHolder.mNameView.setText(item.mRecordName);
        return convertView;
    }

    public int getDeleteCount() {
        int i = 0;
        for (CallLogRecordItem item : mCallLogRecordItems) {
            if (item.mIsDel) {
                i++;
            }
        }
        return i;
    }

    public ArrayList<CallLogRecordItem> getDeleteItems() {
        ArrayList<CallLogRecordItem> result = new ArrayList<CallLogRecordItem>();
        for (CallLogRecordItem item : mCallLogRecordItems) {
            if (item.mIsDel) {
                result.add(item);
            }
        }
        return result;
    }

    public void cancelAllRecordDelTag() {
        for (CallLogRecordItem item : mCallLogRecordItems) {
            item.mIsDel = false;
        }
        notifyDataSetChanged();
    }

    public void setAllRecordDelTag() {
        for (CallLogRecordItem item : mCallLogRecordItems) {
            item.mIsDel = true;
        }
        notifyDataSetChanged();
    }

    private void updateTimerView() {
        if (mIsplay) {
            notifyDataSetChanged();
            mUpdateTimeHandler.postDelayed(mUpdateTimer, RECORDING_TIME_OFFSET);
        } else {
            mUpdateTimeHandler.removeCallbacks(mUpdateTimer);
        }
    }

    private String getTimeViewText(long current, long total) {
        StringBuffer sb = new StringBuffer();
        sb.append(formatTime(current)).append('/').append(formatTime(total));
        return sb.toString();
    }

    private Runnable mUpdateTimer = new Runnable() {
        @Override
        public void run() {
            updateTimerView();
        }
    };

    private Runnable mHideTimeTimer = new Runnable() {
        @Override
        public void run() {
            stopPlay();
            notifyDataSetChanged();
        }
    };

    public void clearPlayingRecordItem() {
        Log.i(TAG, "clearGlobalVariable");
        mIsplay = false;
        mCurrPlayPosition = -1;
        notifyDataSetChanged();
    }

    private void pause() {
        try {
            if (mMediaPlay == null) {
                return;
            }
            mMediaPlay.pause();
            mIsplay = false;
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "pause: throw IllegalArgumentException", e);
        } catch (IllegalStateException e) {
            Log.e(TAG, "pause: throw IllegalStateException", e);
        }
    }

    private void play(Context context, Uri msgUri) {
        Log.i(TAG, "play msgUri:" + msgUri);
        try {
            mUpdateTimeHandler.removeCallbacks(mHideTimeTimer);
            if (mMediaPlay == null) {
                mMediaPlay = new MediaPlayer();
                mMediaPlay.setOnCompletionListener(mCompletionListener);
                mMediaPlay.setOnErrorListener(mErrorListener);
                mMediaPlay.setDataSource(context, msgUri);
                mMediaPlay.prepare();
            }
            if (!mMediaPlay.isPlaying()) {
                mMediaPlay.start();
            }
            mIsplay = true;
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "play: throw IllegalArgumentException", e);
        } catch (IllegalStateException e) {
            Log.e(TAG, "play: throw IllegalStateException", e);
        } catch (IOException e) {
            Log.e(TAG, "play: throw IOException", e);
        }
    }

    public void stopPlay() {
        if (mMediaPlay != null && mMediaPlay.isPlaying()) {
            mMediaPlay.stop();
            mMediaPlay.release();
        }
        mMediaPlay = null;
        mCurrPlayPosition = -1;
        mIsplay = false;
    }

    private MediaPlayer.OnCompletionListener mCompletionListener = new MediaPlayer.OnCompletionListener() {
        @Override
        public void onCompletion(MediaPlayer player) {
            mUpdateTimeHandler.postDelayed(mHideTimeTimer, RECORDING_TIME_OFFSET);
            if (mHandler != null) {
                Message msg = mHandler.obtainMessage(CallLogRecordActivity.MSG_TYPE_STOP_RECORD);
                mHandler.dispatchMessage(msg);
            }
            Log.i(TAG, "onCompletion");
        }
    };

    private MediaPlayer.OnErrorListener mErrorListener = new MediaPlayer.OnErrorListener() {
        @Override
        public boolean onError(MediaPlayer player, int what, int extra) {
            stopPlay();
            notifyDataSetChanged();
            if (mHandler != null) {
                Message msg = mHandler.obtainMessage(CallLogRecordActivity.MSG_TYPE_STOP_RECORD);
                mHandler.dispatchMessage(msg);
            }
            Log.v(TAG, "mErrorListener/onError: playerror");
            return true;
        }
    };

    private String formatTime(long time) {
        StringBuffer sb = new StringBuffer();
        long originTime = time / 1000;
        long second, minute, hour;

        second = originTime % 60;
        minute = (originTime / 60) % 60;
        hour = (originTime / (60 * 60)) % 60;

        if (hour > 0) {
            if (hour < 10) {
                sb.append('0');
            }
            sb.append(hour).append(':');
        }
        if (minute < 10) {
            sb.append('0');
        }
        sb.append(minute).append(':');
        if (second < 10) {
            sb.append('0');
        }
        sb.append(second);

        return sb.toString();
    }

    public void onRecordListItemClick(int position, boolean isDel) {
        Log.d(TAG, "onRecordListItemClick: isDel = " + isDel);
        CallLogRecordItem item = mCallLogRecordItems[position];
        if (isDel) {
            item.mIsDel = !item.mIsDel;
        } else {
            onResponsePlayClicked(position, item.mRecordPath);
        }
        notifyDataSetChanged();
    }

    private OnCheckedChangeListener mCheckedChangeListener = new OnCheckedChangeListener() {

        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
             if (buttonView.getTag() == null) {
                return;
            }
            if (buttonView instanceof CheckBox) {
                int position = (Integer) buttonView.getTag();
                CallLogRecordItem item = mCallLogRecordItems[position];
                item.mIsDel = isChecked;

                if (mHandler != null) {
                    Message msg = mHandler.obtainMessage(CallLogRecordActivity.MSG_TYPE_REFRESH_VIEWS_BY_DEL);
                    mHandler.dispatchMessage(msg);
                }
                notifyDataSetChanged();
            }
        }
    };

    private OnClickListener mAudioPlayerListener = new OnClickListener() {

        @Override
        public void onClick(View v) {
            if (v.getTag() == null) {
                return;
            }
            int position = (Integer) v.getTag();
            CallLogRecordItem item = mCallLogRecordItems[position];
            onResponsePlayClicked(position, item.mRecordPath);
            notifyDataSetChanged();
        }
    };

    private void onResponsePlayClicked(int position, String filename) {
        Log.i(TAG, "onResponsePlayClicked filename = " + filename);
        if (mCurrPlayPosition != position) {
            stopPlay();
            play(mActivity, Uri.parse(filename));
            mCurrPlayPosition = position;
        } else {
            if (mIsplay) {
                pause();
            } else {
                play(mActivity, Uri.parse(filename));
            }
        }

        if (mHandler != null) {
            int msgType = mIsplay ? CallLogRecordActivity.MSG_TYPE_PLAY_RECORD : CallLogRecordActivity.MSG_TYPE_STOP_RECORD;
            Message msg = mHandler.obtainMessage(msgType);
            mHandler.dispatchMessage(msg);
        }
        updateTimerView();
    }

    static class CallLogRecordItem {
        public String mRecordPath;
        public String mRecordName;
        public String mDuration;
        public boolean mIsDel;
    }

    static class ViewHolder {
        public TextView mNameView;
        public CheckBox mCheckBox;
        public TextView mTimeView;
        public ImageView mPlayView;
    }
}
