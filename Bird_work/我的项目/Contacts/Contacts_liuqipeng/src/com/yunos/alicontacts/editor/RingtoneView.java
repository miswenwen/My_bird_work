
package com.yunos.alicontacts.editor;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.yunos.alicontacts.R;

public class RingtoneView extends LinearLayout {

    TextView mRingText;
    View mDelete;
    private Listener mListener;

    public interface Listener {
        void onRingtoneRequest();

        void onRingtoneDeleteRequest();
    }

    public void setListener(Listener listener) {
        mListener = listener;
    }

    public RingtoneView(Context context) {
        super(context);
    }

    public RingtoneView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public RingtoneView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void prepareView() {
        mRingText = (TextView) findViewById(R.id.edit_ringtone_name);

        this.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                if (mListener != null) {
                    mListener.onRingtoneRequest();
                }
            }
        });

        mDelete = findViewById(R.id.delete_button);
        if (mDelete != null) {
            // for RingtoneView in Editor
            mDelete.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mListener != null) {
                        mListener.onRingtoneDeleteRequest();
                    }
                }

            });
        }
    }

    public void setLocalRingtoneName(String name) {
        mRingText.setText(name);
    }

    /**
     * Only show when custom ringtone has been set. click it will delete custom
     * ringtone and show string "default ringtone".
     *
     * @param isShow if show delete button
     */
    public void setDeleteButtonVisible(boolean isShow) {
        Log.v("RingtoneView", "setDeleteButtonVisible() -- isShow = " + isShow + ", mDeleteContainer is " + mDelete);
        if (mDelete == null) {
            return;
        }
        if (isShow) {
            mDelete.setVisibility(View.VISIBLE);
        } else {
            mDelete.setVisibility(View.GONE);
        }
    }

}
