
package com.yunos.alicontacts.sim;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.yunos.alicontacts.R;
import com.yunos.alicontacts.activities.BaseActivity;

import yunos.support.v4.app.Fragment;

public class SimContactActivity extends BaseActivity {
    private static final String TAG = "SimContactActivity";
    private Fragment mFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final Intent intent = getIntent();
        if (intent == null) {
            finish();
            return;
        }
        String action = intent.getAction();
        Log.d(TAG, "onCreate() intent:" + intent);

        if (SimContactUtils.ACTION_INSERT_SIM_CONTACTS.equals(action)
                || SimContactUtils.ACTION_EDITOR_SIM_CONTACTS.equals(action)
                || SimContactUtils.ACTION_IMPORT_CONTACTS.equals(action)
                || SimContactUtils.ACTION_EXPORT_CONTACTS.equals(action)) {
            int slot = intent.getIntExtra(SimUtil.SLOT_KEY, SimUtil.SLOT_ID_1);
            if (SimContactListFragment.isActing(slot)) {
                Log.d(TAG, "SIM card is reading/writing on slot" + slot + ", return!!!");
                Toast.makeText(this, R.string.accessing_sim_card, Toast.LENGTH_SHORT).show();
                finish();
                return;
            }
        }

        setActivityContentView(R.layout.sim_contact_list);

        configureFragment(action);
        showBackKey(true);
    }

    private void configureFragment(String action) {
        if (SimContactUtils.ACTION_IMPORT_CONTACTS.equals(action)
                || SimContactUtils.ACTION_EXPORT_CONTACTS.equals(action)) {
            mFragment = new SimContactListFragment();
        } else if (SimContactUtils.ACTION_INSERT_SIM_CONTACTS.equals(action)
                || SimContactUtils.ACTION_EDITOR_SIM_CONTACTS.equals(action)) {
            mFragment = new SimContactEditorFragment();
        } else {
            Log.e(TAG, "configureFragment() action:" + action);
            finish();
            return;
        }

        getSupportFragmentManager().beginTransaction().replace(R.id.sim_container, mFragment)
                .commitAllowingStateLoss();
    }

    @Override
    public void onBackPressed() {
        if (mFragment instanceof SimContactEditorFragment) {
            ((SimContactEditorFragment) mFragment).onCancel();
            return;
        }
        super.onBackPressed();
    }

    @Override
    public void onBackKey() {
        if (mFragment instanceof SimContactEditorFragment) {
            ((SimContactEditorFragment) mFragment).onCancel();
            return;
        }
        super.onBackKey();
    }
}
