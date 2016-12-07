
package com.yunos.alicontacts.dialpad;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Space;

import com.yunos.alicontacts.R;
import com.yunos.alicontacts.sim.SimUtil;

public class DialpadFooterView extends LinearLayout implements OnClickListener {
    private static final String TAG = "DialpadFooterView";

    public interface OnDailpadFooterBarViewClickListener {
        final int ID_BTN_ENTRY_CONTACTS = 0;
        final int ID_BTN_SYNC = 1;
        public final int ID_BTN_SWITCH = 3;
        public final int ID_BTN_SIM = 5;
        public final int ID_BTN_SIM1 = 6;
        public final int ID_BTN_SIM2 = 7;

        void onFooterBarClick(int id);
    }

    private boolean mMutiSimEnable;
    private boolean mSim1Enable;
    private boolean mSim2Enable;

    private IconTextButton mSim1TextBtn;
    private IconTextButton mSim2TextBtn;
    private Space mSimBtnSpace;
    private View mSimBtn;

    private OnDailpadFooterBarViewClickListener mOnDailpadFooterBarViewClickListener;

    public DialpadFooterView(Context context) {
        super(context);
    }

    public DialpadFooterView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public DialpadFooterView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        initViews();
    }

    private void initViews() {
        mSim1TextBtn = (IconTextButton) this.findViewById(R.id.btn_call_sim1);
        mSim2TextBtn = (IconTextButton) this.findViewById(R.id.btn_call_sim2);
        mSimBtn = findViewById(R.id.icon_sim);
        mSim1TextBtn.setOnClickListener(this);
        mSim2TextBtn.setOnClickListener(this);
        mSimBtn.setOnClickListener(this);
        // mSim1TextBtn.setIconImage(R.drawable.aui_dialpad_action_call_sim1);
        // mSim2TextBtn.setIconImage(R.drawable.aui_dialpad_action_call_sim2);
        mSimBtnSpace = (Space) this.findViewById(R.id.space);
        findViewById(R.id.dialpad_footer).setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        Log.d(TAG, v + " clicked.");
        if (v.equals(mSim1TextBtn)) {
            mOnDailpadFooterBarViewClickListener.onFooterBarClick(OnDailpadFooterBarViewClickListener.ID_BTN_SIM1);
        } else if (v.equals(mSim2TextBtn)) {
            mOnDailpadFooterBarViewClickListener.onFooterBarClick(OnDailpadFooterBarViewClickListener.ID_BTN_SIM2);
        } else if (v.equals(mSimBtn)) {
            int id = OnDailpadFooterBarViewClickListener.ID_BTN_SIM;
            if (mSim1Enable) {
                id = OnDailpadFooterBarViewClickListener.ID_BTN_SIM1;
            } else if (mSim2Enable) {
                id = OnDailpadFooterBarViewClickListener.ID_BTN_SIM2;
            }
            mOnDailpadFooterBarViewClickListener.onFooterBarClick(id);
        }
    }

    public void setFooterBarSimMode(boolean mutiSimEnable, boolean sim1Enable, boolean sim2Enable) {
        if (mMutiSimEnable == mutiSimEnable && mSim1Enable == sim1Enable && mSim2Enable == sim2Enable) {
            updateSimCallButton();
            return;
        }
        mMutiSimEnable = mutiSimEnable;
        mSim1Enable = sim1Enable;
        mSim2Enable = sim2Enable;
        updateSimCallButton();
        // relayoutFooterButtons();
        this.invalidate();
    }

    public void setFooterBarDisplayMode(boolean foldFlag) {
        invalidate();
    }

    private void updateSimCallButton() {
        // draw call button
        int bgRes = R.drawable.dial_keyborad_dialer_selector;
        if (!mMutiSimEnable || (!mSim1Enable && !mSim2Enable)) {
            mSimBtn.setVisibility(View.VISIBLE);
            mSimBtn.setBackgroundResource(bgRes);
            mSim1TextBtn.setVisibility(View.GONE);
            mSim2TextBtn.setVisibility(View.GONE);
            mSimBtnSpace.setVisibility(View.GONE);
            return;
        }

        if (mSim1Enable && mSim2Enable) {
            if (mSimBtn != null) {
                mSimBtn.setVisibility(View.GONE);
            }
            mSim1TextBtn.setVisibility(View.VISIBLE);
            mSim2TextBtn.setVisibility(View.VISIBLE);
            mSimBtnSpace.setVisibility(View.VISIBLE);
            updateDualSimBtnText(SimUtil.SLOT_ID_1);
            updateDualSimBtnText(SimUtil.SLOT_ID_2);
        } else {
            mSimBtn.setVisibility(View.VISIBLE);
            mSim1TextBtn.setVisibility(View.GONE);
            mSimBtnSpace.setVisibility(View.GONE);
            mSim2TextBtn.setVisibility(View.GONE);
            if (mSim1Enable) {
                bgRes = R.drawable.dial_keyborad_dialer_left_selector;
            } else if (mSim2Enable) {
                bgRes = R.drawable.dial_keyborad_dialer_right_selector;
            }
            mSimBtn.setBackgroundResource(bgRes);
        }
    }

    public void setOnDailpadFooterBarViewClickListener(OnDailpadFooterBarViewClickListener l) {
        mOnDailpadFooterBarViewClickListener = l;
    }

    private void updateDualSimBtnText(int simId) {
        String simOpratorName = SimUtil.getSimCardDisplayName(getContext(), simId);
        if (simOpratorName == null) {
            simOpratorName = "";
        }

        if (simId == SimUtil.SLOT_ID_1) {
            mSim1TextBtn.setText(simOpratorName);
        } else if (simId == SimUtil.SLOT_ID_2) {
            mSim2TextBtn.setText(simOpratorName);
        }
    }
}
