package com.yunos.alicontacts.conference;

import android.annotation.NonNull;
import android.content.Context;
import android.net.Uri;
import android.os.Parcelable;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.yunos.alicontacts.R;
import com.yunos.alicontacts.activities.ConferenceRecipientsPickerActivity;
import com.yunos.alicontacts.database.util.NumberNormalizeUtil;

import java.util.ArrayList;
import java.util.Set;

public class RecipientsListAdapter extends BaseAdapter {
    private static final String TAG = "RecipientsListAdapter";
    private final ConferenceRecipientsPickerActivity mActivity;
    private LayoutInflater mInflater;
    private ArrayList<Recipient> mRecipients;
    private Set<String> mPinnedNumbers;

    public RecipientsListAdapter(
            @NonNull ArrayList<Recipient> recipients,
            @NonNull Set<String> pinnedNumbers,
            ConferenceRecipientsPickerActivity activity) {
        mActivity = activity;
        mInflater = (LayoutInflater) mActivity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mRecipients = recipients;
        mPinnedNumbers = pinnedNumbers;
    }

    public void updateRecipientsData(@NonNull final ArrayList<Recipient> recipients) {
        if (mActivity.isFinishing() || mActivity.isDestroyed()) {
            Log.i(TAG, "updateListData: activity is not active. quit.");
            return;
        }
        mRecipients = recipients;
        mActivity.enableMakeCall(recipients.size() > mPinnedNumbers.size());
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return mRecipients.size();
    }

    @Override
    public Recipient getItem(int position) {
        return mRecipients.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        ViewHolder holder = null;
        Recipient recipient = getItem(position);
        if (convertView == null) {
            convertView = mInflater.inflate(R.layout.conference_recipient_item, parent, false);
            holder = new ViewHolder(convertView);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }
        holder.mRemoveView.setTag(recipient);
        if (mPinnedNumbers.contains(recipient.formattedNumber)) {
            holder.mRemoveView.setVisibility(View.INVISIBLE);
        } else {
            holder.mRemoveView.setVisibility(View.VISIBLE);
        }

        String loc = recipient.getLocation();
        if (TextUtils.isEmpty(recipient.name)) {
            holder.mTitleView.setText(recipient.number);
            if (TextUtils.isEmpty(loc)) {
                holder.mNumberView.setText("");
            } else {
                holder.mNumberView.setText(loc);
            }
        } else {
            holder.mTitleView.setText(recipient.name);
            if (TextUtils.isEmpty(loc)) {
                holder.mNumberView.setText(recipient.number);
            } else {
                holder.mNumberView.setText(recipient.number + ' ' + loc);
            }
        }
        return convertView;
    }

    public void addPhoneNumber(String number) {
        if (TextUtils.isEmpty(number) || containsNumber(number)) {
            return;
        }
        ArrayList<Recipient> recipients = new ArrayList<Recipient>(
                ConferenceRecipientsPickerActivity.MAX_RECIPIENTS_COUNT);
        recipients.addAll(mRecipients);
        recipients.add(new Recipient(null, null, number));
        updateRecipientsData(recipients);
    }

    public boolean containsNumber(String number) {
        for (Recipient r : mRecipients) {
            if (TextUtils.equals(r.formattedNumber,
                    NumberNormalizeUtil.normalizeNumber(number, true))) {
                return true;
            }
        }
        return false;
    }

    public Parcelable[] getSelectedPhoneNumbersForPickContactsIntent() {
        ArrayList<Parcelable> urisArray = new ArrayList<Parcelable>(
                ConferenceRecipientsPickerActivity.MAX_RECIPIENTS_COUNT);
        for (Recipient recipient : mRecipients) {
            if (recipient.uri != null) {
                urisArray.add(recipient.uri);
            }
        }
        return urisArray.toArray(new Parcelable[urisArray.size()]);
    }

    public long[] getPinnedNumberDataIds() {
        ArrayList<Long> pinnedIds = new ArrayList<Long>(
                ConferenceRecipientsPickerActivity.MAX_RECIPIENTS_COUNT);
        for (Recipient recipient : mRecipients) {
            if (mPinnedNumbers.contains(recipient.formattedNumber)) {
                Uri phoneUri = recipient.uri;
                String idStr = phoneUri == null ? null : phoneUri.getLastPathSegment();
                if (idStr == null) {
                    continue;
                }
                try {
                    pinnedIds.add(Long.parseLong(idStr));
                } catch (NumberFormatException nfe) {
                    // ignore non-number ids.
                    Log.e(TAG, "getPinnedNumberDataIds: invalid id in uri "+phoneUri, nfe);
                }
            }
        }
        long[] result = new long[pinnedIds.size()];
        for (int i = 0; i < result.length; i++) {
            result[i] = pinnedIds.get(i);
        }
        return result;
    }

    public Recipient[] getAllRecipients() {
        return mRecipients.toArray(new Recipient[mRecipients.size()]);
    }

    public ArrayList<String> getNumbersForMakeConferenceCall() {
        ArrayList<String> numbers = new ArrayList<String>(
                ConferenceRecipientsPickerActivity.MAX_RECIPIENTS_COUNT);
        for (Recipient recipient : mRecipients) {
            if (!mPinnedNumbers.contains(recipient.formattedNumber)) {
                numbers.add(recipient.formattedNumber);
            }
        }
        return numbers;
    }

    public void removeRecipient(Recipient recipient) {
        if ((getCount() == 0) || (recipient == null)) {
            return;
        }
        if (mPinnedNumbers.contains(recipient.formattedNumber)) {
            Log.w(TAG, "removeRecipient: Try to remove a pinned number, skip.");
            return;
        }
        int index = mRecipients.indexOf(recipient);
        if (index < 0) {
            return;
        }
        ArrayList<Recipient> recipients = new ArrayList<Recipient>(
                ConferenceRecipientsPickerActivity.MAX_RECIPIENTS_COUNT);
        recipients.addAll(mRecipients);
        recipients.remove(index);
        updateRecipientsData(recipients);
    }

    public void mergeContactRecipients(Recipient[] mergeRecipients) {
        Recipient[] existingRecipients = mRecipients.toArray(new Recipient[mRecipients.size()]);
        // 1. search each existingRecipient in mergeRecipients:
        // If find, replace existingRecipient and update mLocation.
        // If not find, remove existingRecipient.
        for (int i = 0; i < existingRecipients.length; i++) {
            Recipient r = existingRecipients[i];
            int mergeIndex = getIndexOfRecipient(mergeRecipients, r);
            if (mergeIndex > -1) {
                mergeRecipients[mergeIndex].setLocation(existingRecipients[i].getLocation());
                existingRecipients[i] = mergeRecipients[mergeIndex];
                mergeRecipients[mergeIndex] = null;
            } else if (existingRecipients[i].uri != null) {
                existingRecipients[i] = null;
            }
        }
        // 2. put remaining existingRecipients and mergeRecipients together.
        ArrayList<Recipient> result = new ArrayList<Recipient>(
                ConferenceRecipientsPickerActivity.MAX_RECIPIENTS_COUNT);
        for (int i = 0; i < existingRecipients.length; i++) {
            if (existingRecipients[i] != null) {
                result.add(existingRecipients[i]);
            }
        }
        for (int i = 0; i < mergeRecipients.length; i++) {
            if (mergeRecipients[i] != null) {
                result.add(mergeRecipients[i]);
            }
        }
        updateRecipientsData(result);
    }

    private int getIndexOfRecipient(Recipient[] recipients, Recipient recipient) {
        for (int i = 0; i < recipients.length; i++) {
            if (recipients[i] == null) {
                continue;
            }
            if (TextUtils.equals(recipients[i].formattedNumber, recipient.formattedNumber)) {
                return i;
            }
        }
        return -1;
    }

    private class ViewHolder {
        final TextView mTitleView;
        final TextView mNumberView;
        final ImageView mRemoveView;

        public ViewHolder(View view) {
            mTitleView = (TextView) view.findViewById(R.id.title_line);
            mNumberView = (TextView) view.findViewById(R.id.number_line);
            mRemoveView = (ImageView) view.findViewById(R.id.remove_icon);
            mActivity.setRemoveClickListener(mRemoveView);
        }
    }

}

