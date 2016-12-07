
package com.yunos.alicontacts.list;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.EditText;

import com.yunos.alicontacts.R;
import com.yunos.alicontacts.activities.ContactDetailActivity;
import com.yunos.alicontacts.preference.ContactsSettingActivity;

public class ContactsSearchFragment extends ContactBrowseListFragment implements OnClickListener {

    private EditText mSearchEditor;
    private View mEmptyView;

    private TextWatcher mSearchWatcher = new TextWatcher() {

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void afterTextChanged(Editable s) {
            setQueryString(s.toString(), true);
        }
    };

    public ContactsSearchFragment() {
        setPhotoLoaderEnabled(true);
        setSectionHeaderDisplayEnabled(true);
        setVisibleScrollbarEnabled(true);
    }

    @Override
    protected void onCreateView(LayoutInflater inflater, ViewGroup container) {
        super.onCreateView(inflater, container);
        mSearchEditor = (EditText) getRootView().findViewById(R.id.search_editor);
        mEmptyView = getRootView().findViewById(R.id.empty);
        mSearchEditor.addTextChangedListener(mSearchWatcher);
        getRootView().findViewById(R.id.search_back_id).setOnClickListener(this);
        getRootView().findViewById(R.id.search_clear_btn).setOnClickListener(this);
    }

    @Override
    protected ContactListAdapter createListAdapter() {
        DefaultContactListAdapter adapter = new DefaultContactListAdapter(getContext(), this);
        adapter.setSectionHeaderDisplayEnabled(isSectionHeaderDisplayEnabled());
        adapter.setDisplayPhotos(ContactsSettingActivity.readShowContactsHeadIconPreference(getActivity()));
        adapter.setShowFavoriteContacts(true);
        return adapter;
    }

    @Override
    protected void onItemClick(int position, long id) {
        ContactListAdapter adapter = getAdapter();
        Uri contactUri = adapter.getContactUri(position);
        viewContact(contactUri, adapter.isSimContact(position));
        Intent intent = new Intent(Intent.ACTION_VIEW, contactUri);
        intent.setClass(getActivity(), ContactDetailActivity.class);
        startActivity(intent);
    }

    @Override
    protected View inflateView(LayoutInflater inflater, ViewGroup container) {
        return inflater.inflate(R.layout.contact_search_fragment, container, false);
    }

    @Override
    public void selectedAll(boolean all) {
    }

    @Override
    protected void showCount(int partitionIndex, Cursor data) {
        super.showCount(partitionIndex, data);
        if (data == null || data.getCount() == 0) {
            mEmptyView.setVisibility(View.VISIBLE);
        } else {
            mEmptyView.setVisibility(View.GONE);
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.search_back_id:
                if (getActivity() != null) {
                    getActivity().finish();
                }
                break;
            case R.id.search_clear_btn:
                setQueryString("");
                break;
        }
    }

    public void setQueryString(String queryStr) {
        mSearchEditor.setText(queryStr);
    }
}
