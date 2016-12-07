
package com.yunos.alicontacts.activities;

import android.content.Intent;
import android.os.Bundle;

import com.yunos.alicontacts.R;
import com.yunos.alicontacts.list.ContactsSearchFragment;
import com.yunos.alicontacts.list.UiIntentActions;

public class ContactsSearchActivity extends BaseActivity {

    private ContactsSearchFragment mSearchFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setActivityContentView(R.layout.contacts_search_activity);
        mSearchFragment = (ContactsSearchFragment) getSupportFragmentManager().findFragmentById(R.id.search_fragment);
        processIntent();
        setSearchBar();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        processIntent();
    }

    private void processIntent() {
        Intent intent = getIntent();
        if (UiIntentActions.FILTER_CONTACTS_ACTION.equals(intent.getAction())) {
            String searchStr = intent.getStringExtra(UiIntentActions.FILTER_TEXT_EXTRA_KEY);
            mSearchFragment.setQueryString(searchStr);
        }
    }
}
