/*
 * Copyright (C) 2010 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.yunos.alicontacts.list;

import android.app.Activity;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.FrameLayout.LayoutParams;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

/* YUNOS BEGIN */
//##modules(AliContacts) ##author: hongwei.zhw
//##BugID:(8161644) ##date:2016.4.18
//##descrpition: remove some menu which will cause contacts crash without com.aliyun.xiaoyunmi
import android.os.Build;
/* YUNOS END */
import com.yunos.alicontacts.ContactSaveService;
import com.yunos.alicontacts.GroupMemberLoader;
import com.yunos.alicontacts.R;
import com.yunos.alicontacts.activities.ActionBarAdapter2.TabState;
import com.yunos.alicontacts.activities.ContactSelectionActivity;
import com.yunos.alicontacts.activities.GroupContactSelectionActivity;
import com.yunos.alicontacts.activities.PeopleActivity2;
import com.yunos.alicontacts.dialpad.smartsearch.NameConvertWorker;
import com.yunos.alicontacts.group.GroupManager;
import com.yunos.alicontacts.list.ShortcutIntentBuilder.OnShortcutIntentCreatedListener;


import com.yunos.alicontacts.preference.ContactsSettingActivity;
import com.yunos.alicontacts.sim.SimContactCache;
import com.yunos.alicontacts.sim.SimContactUtils;
import com.yunos.alicontacts.sim.SimUtil;
import com.yunos.alicontacts.util.PreferencesUtils;
import com.yunos.alicontacts.vcard.ImportVCardActivity;
import com.yunos.common.UsageReporter;

import hwdroid.dialog.AlertDialog;
import hwdroid.dialog.DialogInterface;
import hwdroid.widget.ActionSheet;
import hwdroid.widget.searchview.SearchView;
import yunos.support.v4.app.LoaderManager;
import yunos.support.v4.app.LoaderManager.LoaderCallbacks;
import yunos.support.v4.content.CursorLoader;
import yunos.support.v4.content.Loader;

import java.util.ArrayList;
import java.util.List;

/**
 * Fragment containing a contact list used for browsing (as compared to picking
 * a contact with one of the PICK intents).
 */
public class DefaultContactBrowseListFragment extends ContactBrowseListFragment {
    public static final String USAGE_REPORTER_PACKAGE_NAME = DefaultContactBrowseListFragment.class.getSimpleName();
    public static final String TAG = USAGE_REPORTER_PACKAGE_NAME;
    // The log with TMPTAG will be deleted several days later.
    // Use it just for finding convenient;
    private static final String TMPTAG = "PERFORMANCE";

    private static final boolean DBG = true;

    // For notify user contact number function. @ {
    private static final String ACTION_NOTIFY_NEW_INSERT_CONTACT = "com.yunos.sync.action.USERTRACK_CONTACTS_CHANGED";
    private static final String NOTIFY_CONTACT_NUMBER = "ContactNumber";

    private int mNotifyContactNumber;
    // } @

    // add by ali.xgy.
    private FavoritesAndContactsLoader mFavoritesAndContactsLoader;
    private static Cursor mCacheCursor= null;
    private SharedPreferences mDefaultSharedPreference;

    private TextView mCounterHeaderView;
    private View mSearchHeaderView;
    // private View mAccountFilterHeader;
    private FrameLayout mFootViewContainer;
    private TextView mNoContactsFound;

    // Group List empty view
    private View mEmptyViewGroup;
    // Common contact List empty view
    private View mEmptyViewCommon;

    private TextView mEmptyImportFromSimView;
    private boolean mContactsEmpty;
    // private int mColumnWidth;
    // private Uri[] mUris;
    private LayoutInflater mInflater;

    private SearchView mSearchView;
    private View mHeaderView;
    private ImageView mAddContact;

    private String mGroupName;
    private AlertDialog mRenameGroupDlg;
    private EditText mGroupNameText;

    // This function can't call twice.
    private boolean mDidCreateView = false;

    private Handler mHandler = new Handler();

    // This count is for cache contacts count from first loading.
    private int mCachedCount;

    private boolean mIsShowFavoriteContacts;
    private boolean mIsAutoFavoriteContacts;

    private ActionSheet mLongClickPopupDialog;
    // Indicate whether this fragment is launched from desktop for the first
    // tab.
    private boolean mIsColdLaunchedFromDesktop;
    // Indicate whether there are some functions need be called delayed
    // We can't use mIsColdLaunched, because it is one-shot.
    private boolean mHaveDelayedCall;

    // Indicate whether fetched call log data from database
    private boolean mFetchedContactData;
    // Indicate whether setUserVisibleHint called
    private boolean mCalledUserVisibleHint;

    // Indicate whether need to be refreshed. when this fragment is stopped,
    // then switched from dialpad fragment, it should be refreshed in
    // setUserVisibleHint()
    private boolean mIsNeedRefresh;
    private static final String CONTACTS_COUNT = "ContactsCount";

    public void setColdLaunched(boolean coldLaunch) {
        mIsColdLaunchedFromDesktop = coldLaunch;
    }

    @Override
    public void onResume() {
        Log.w(TMPTAG, "DefaultContactBrowseListFragment onResumce called");
        super.onResume();

        if (isCurrentTab()) {
            Activity activity = getActivity();
            if (activity == null) {
                Log.w(TAG, "onResume: getActivity() is null.");
                return;
            }

            // Only run this when current tab is
            // DefaultContactBrowseListFragment. For other case, don nothing in
            // onResume.
            doResume();

            mIsNeedRefresh = false;

            UsageReporter.onResume(null, USAGE_REPORTER_PACKAGE_NAME);
        }
        Log.w(TMPTAG, "DefaultContactBrowseListFragment onResumce end");
    }

    private void doResume() {
        Activity activity = getActivity();
        if (activity == null) {
            Log.w(TAG, "doResume: getActivity() is null.");
            return;
        }
        getAdapter().setDisplayPhotos(ContactsSettingActivity.readShowContactsHeadIconPreference(activity));

        if (mSearchView != null) {
            mSearchView.setQuery(null);
        }

        checkForReloadData();
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        super.onLoadFinished(loader, data);
        int loaderId = loader.getId();
        if (loaderId != -1 && !isSearchMode()) {
            cleanCache();
            mCacheCursor = mFavoritesAndContactsLoader.cloneCursor(data, DefaultContactListAdapter.FIRST_LOAD_COUNT);
            if(mCacheCursor instanceof MatrixCursor){
                ((MatrixCursor)mCacheCursor).setExtras(data.getExtras());
            }
        }
    }

    @Override
    public void onStart() {
        Log.w(TMPTAG, "DefaultContactBrowseListFragment onStart called");
        // super.onStart() will run doStart(). And doStart() is overridden. Do
        // nothing in doStart() for skipping onStart() if current tab is
        // CallLogFragment.
        super.onStart();

        // Use mIsColdLaunchedFromDesktop instead of isCurrentTab
        // We just call doSuperStartAction once
        // Then, if the contact changed, cursorloader will update the data.
        if (mIsColdLaunchedFromDesktop) {
            doSuperStartAction();
            mIsNeedRefresh = false;
            Log.w(TMPTAG, "DefaultContactBrowseListFragment onStart, call doSuperStartAction");
        }
        Log.w(TMPTAG, "DefaultContactBrowseListFragment onStart end");
    }

    @Override
    protected void doStart() {
        // Do nothing. Do not load anything in onStart() cycle.
        if(PeopleActivity2.sFirstLoad && mCacheCursor != null){
            DirectoryPartition partition = (DirectoryPartition) getAdapter().getPartition(0);
            long directoryId = partition.getDirectoryId();
            onPartitionLoaded((int)directoryId, mCacheCursor);
            partition.setStatus(DirectoryPartition.STATUS_NOT_LOADED);
            updateFishEyeData(mCacheCursor);
            PeopleActivity2.sFirstLoad = false;
        }
    }

    public DefaultContactBrowseListFragment() {
        setPhotoLoaderEnabled(true);
        setSectionHeaderDisplayEnabled(true);
        setVisibleScrollbarEnabled(true);
    }

    @Override
    public CursorLoader createCursorLoader() {
        mFavoritesAndContactsLoader = new FavoritesAndContactsLoader(getActivity());
        return mFavoritesAndContactsLoader;
    }

    @Override
    protected void onItemClick(int position, long id) {
        if (!mContactsEmpty) {
            ContactListAdapter adapter = getAdapter();
            viewContact(adapter.getContactUri(position), adapter.isSimContact(position));
            if (getFavoriteContactsCount() > 0 && position < getFavoriteContactsCount()) {
                UsageReporter.onClick(null, TAG, UsageReporter.ContactsListPage.VIEW_CLICK_FAVORITE_CL);
            } else {
                if (isShowGroup()) {
                    UsageReporter.onClick(null, TAG, UsageReporter.ContactsListPage.VIEW_CLICK_GROUP_CL);
                } else if (mSearchView != null && !TextUtils.isEmpty(mSearchView.getQuery())) {
                    UsageReporter.onClick(null, TAG, UsageReporter.ContactsListPage.SEARCH_CL_VIEW_SEARCH_RESULT);
                } else {
                    UsageReporter.onClick(null, TAG, UsageReporter.ContactsListPage.VIEW_CLICK_CL);
                }
            }
        }
    }

    @Override
    public void onItemLongClick(View v, int position, long id) {
        ContactListAdapter adapter = getAdapter();
        Uri uri = adapter.getContactUri(position);
        long rawContactId = adapter.getRawContactId(position);
        boolean isSimContact = adapter.isSimContact(position);
        showPopupMenu(v, uri, rawContactId, isSimContact);
    }

    @Override
    public boolean onTouch(View view, MotionEvent event) {
        boolean result = super.onTouch(view, event);
        hideDialpadFragment();
        return result;
    }

    @Override
    public void onChildClick(int[] partition) {
        super.onChildClick(partition);
        hideDialpadFragment();
    }

    private void hideDialpadFragment() {
        Activity activity = getActivity();
        if (!(activity instanceof PeopleActivity2)) {
            Log.w(TAG, "hideDialpadFragment: not attached to PeopleActivity2. Shall not happen.");
            return;
        }
        ((PeopleActivity2) activity).hideDialpadFragment();
    }

    private static final String ACTION_INSTALL_SHORTCUT = "com.android.launcher.action.INSTALL_SHORTCUT";
   
	// modified by zhouzheng 20160905 begin
	private static final String ACTION_UNINSTALL_SHORTCUT = "com.aliyun.homeshell.action.UNINSTALL_SHORTCUT";
    //modified by zhouzheng  20160905  end 
    /// bird: TASK #7674,BUG #10854,custom contacts readonly attr,chengting,@20160304 {
    int INDEX_VIEW_CONTACT=-1;
    int INDEX_EDIT=-1;
    int INDEX_DELETE=-1;
    int INDEX_FAVORITE=-1;
    int INDEX_SEND_TO_DESK_TOP=-1;
    /// @}
    private void showPopupMenu(View v, final Uri uri, final long rawContactId, final boolean isSimContact) {
        /// bird: TASK #7674,BUG #10854,custom contacts readonly attr,chengting,@20160304 {
        int index=0;
	    INDEX_VIEW_CONTACT=-1;
	    INDEX_EDIT=-1;
	    INDEX_DELETE=-1;
	    INDEX_FAVORITE=-1;
	    INDEX_SEND_TO_DESK_TOP=-1;
        /// @}
		
        final boolean isStarred = ((DefaultContactListAdapter.ViewHolder) v.getTag()).starred;
        mLongClickPopupDialog = new ActionSheet(getActivity());
        Resources resource = getContext().getResources();
        ArrayList<String> titles = new ArrayList<String>();
        titles.add(resource.getString(R.string.view_contact));
		
        /// bird: TASK #7674,BUG #10854,custom contacts readonly attr,chengting,@20160304 begin
        INDEX_VIEW_CONTACT = index++;
        ContactListAdapter.ViewHolder holder = (DefaultContactListAdapter.ViewHolder) v.getTag();
        Log.d(TAG,"ctrng,showPopupMenu,sdn="+holder.sdn);
        if (holder != null && holder.isReadonly()) {
            Log.d(TAG, "cting,showPopupMenu,is read only contact,remove edit & delete menu");
        } else {
        /// bird: TASK #7674,BUG #10854,custom contacts readonly attr,chengting,@20160304 end
		
        titles.add(resource.getString(R.string.edit));
        titles.add(resource.getString(R.string.calllog_delete));
		
        /// bird: TASK #7674,BUG #10854,custom contacts readonly attr,chengting,@20160304 begin
        INDEX_EDIT = index++;
        INDEX_DELETE = index++;
		}
		/// bird: TASK #7674,BUG #10854,custom contacts readonly attr,chengting,@20160304 end
        final int slotId = SimContactUtils.getSlotIdFromCacheByRawContactId(rawContactId);
        if (!isSimContact) {
            titles.add(resource.getString(R.string.send_to_desk_top));
            
			/// bird: TASK #7674,BUG #10854,custom contacts readonly attr,chengting,@20160304 begin
            INDEX_SEND_TO_DESK_TOP = index++;
            /// bird: TASK #7674,BUG #10854,custom contacts readonly attr,chengting,@20160304 begin
			
            final boolean showFavoritesInList = ContactsSettingActivity.readBooleanFromDefaultSharedPreference(
                    getActivity(),
                    ContactsSettingActivity.CONTACT_DISPLAY_FAVORITE_PREFERENCE,
                    ContactsSettingActivity.DEFAULT_DISPLAY_FAVORITE_ON_OFF);
            final boolean autoFavoriteContacts = ContactsSettingActivity.readBooleanFromDefaultSharedPreference(
                    getActivity(),
                    ContactsSettingActivity.CONTACT_AUTO_FAVORITE_PREFERENCE,
                    ContactsSettingActivity.DEFAULT_AUTO_FAVORITE_ON_OFF);

            final boolean showAddFavorite = showFavoritesInList && (!autoFavoriteContacts);
            if (showAddFavorite) {
                titles.add(resource.getString(isStarred ? R.string.contacts_detail_cancel_star : R.string.description_star));
            	/// bird: TASK #7674,BUG #10854,custom contacts readonly attr,chengting,@20160304 begin
            	INDEX_FAVORITE = index++;
            	/// bird: TASK #7674,BUG #10854,custom contacts readonly attr,chengting,@20160304 end
            }

        }
        // Per YaoWei's comments in bug http://k3.alibaba-inc.com/issue/6506061?versionId=1169325
        // we do NOT set title for action sheet any more.
        // mLongClickPopupDialog.setTitle(((DefaultContactListAdapter.ViewHolder) v.getTag()).name);
        mLongClickPopupDialog.setCommonButtons(titles, null, null, new ActionSheet.CommonButtonListener() {

            @Override
            public void onDismiss(ActionSheet arg0) {
            }

            @Override
            public void onClick(int which) {
/*removed by lichengfeng for TASK #7674
                switch (which) {
                    case 0: // view contact
                        viewContact(uri, isSimContact);
                        UsageReporter.onClick(null, TAG, UsageReporter.ContactsListPage.CL_LC_VIEW_CONTACT);
                        break;
                    case 1: // edit
                        if (isSimContact) {
                            editSimContact(rawContactId, slotId);
                        } else {
                            editPhoneContact(uri);
                        }
                        UsageReporter.onClick(null, TAG, UsageReporter.ContactsListPage.CL_LC_EDIT_CONTACT);
                        break;
                    case 2: // delete
                        deleteContact(uri);
                        UsageReporter.onClick(null, TAG, UsageReporter.ContactsListPage.CL_LC_DELETE_CONTACT);
                        break;
                    case 3: // send to desktop
                        ShortcutIntentBuilder builder = new ShortcutIntentBuilder(DefaultContactBrowseListFragment.this
                                .getActivity(), new OnShortcutIntentCreatedListener() {
                            @Override
                            public void onShortcutIntentCreated(Uri uri, Intent shortcutIntent) {
                                shortcutIntent.setAction(ACTION_INSTALL_SHORTCUT);
                                DefaultContactBrowseListFragment.this.getActivity().sendBroadcast(shortcutIntent);
                            }
                        });
                        builder.createContactShortcutIntent(uri, true);
                        UsageReporter.onClick(null, TAG, UsageReporter.ContactsListPage.CL_LC_SEND_DESKTOP);
                        break;
                    case 4: // Favorites or next.
                        if (isStarred) {
                            removeFromFavorites(uri);
                            UsageReporter.onClick(null, TAG, UsageReporter.ContactsListPage.CL_LC_CANCEL_FAVORITE);
                        } else {
                            addToFavorites(uri);
                            UsageReporter.onClick(null, TAG, UsageReporter.ContactsListPage.CL_LC_ADD_FAVORITE);
                        }
                        break;

                    default:
                }
*/
            /// bird: TASK #7674,BUG #10854,custom contacts readonly attr,chengting,@20160304 {	
			if(which == INDEX_VIEW_CONTACT) { // view contact
					viewContact(uri, isSimContact);
					UsageReporter.onClick(null, TAG, UsageReporter.ContactsListPage.CL_LC_VIEW_CONTACT);
			}else if(which == INDEX_EDIT) { // edit
				if (isSimContact) {
                   editSimContact(rawContactId, slotId);
				} else {
                   editPhoneContact(uri);
               }
               UsageReporter.onClick(null, TAG, UsageReporter.ContactsListPage.CL_LC_EDIT_CONTACT);
			}else if(which == INDEX_DELETE) { // delete
				// bird: BUG #15552, [Delete contacts while deleting contacts shortcut],add by zhouzheng,20160908 begin @{
				 ShortcutIntentBuilder detelebuilder = new ShortcutIntentBuilder(DefaultContactBrowseListFragment.this
	                       .getActivity(), new OnShortcutIntentCreatedListener() {
	                   @Override
	                   public void onShortcutIntentCreated(Uri uri, Intent shortcutIntent) {
	                       shortcutIntent.setAction(ACTION_UNINSTALL_SHORTCUT);
	                       DefaultContactBrowseListFragment.this.getActivity().sendBroadcast(shortcutIntent);
	                   }
	               });
			 detelebuilder.createContactShortcutIntent(uri, true);
			//  @ } bird: BUG #15552 ,[Delete contacts while deleting contacts shortcut],add by zhouzheng,20160908 end 
               deleteContact(uri);
               UsageReporter.onClick(null, TAG, UsageReporter.ContactsListPage.CL_LC_DELETE_CONTACT);         
			}else if(which == INDEX_FAVORITE) { // Favorites or next.
               if (isStarred) {
                   removeFromFavorites(uri);
                   UsageReporter.onClick(null, TAG, UsageReporter.ContactsListPage.CL_LC_CANCEL_FAVORITE);
               } else {
                   addToFavorites(uri);
                   UsageReporter.onClick(null, TAG, UsageReporter.ContactsListPage.CL_LC_ADD_FAVORITE);
               }
		}else if(which == INDEX_SEND_TO_DESK_TOP) { // send to desktop
               ShortcutIntentBuilder builder = new ShortcutIntentBuilder(DefaultContactBrowseListFragment.this
                       .getActivity(), new OnShortcutIntentCreatedListener() {
                   @Override
                   public void onShortcutIntentCreated(Uri uri, Intent shortcutIntent) {
                       shortcutIntent.setAction(ACTION_INSTALL_SHORTCUT);
                       DefaultContactBrowseListFragment.this.getActivity().sendBroadcast(shortcutIntent);
                   }
               });
               builder.createContactShortcutIntent(uri, true);
               UsageReporter.onClick(null, TAG, UsageReporter.ContactsListPage.CL_LC_SEND_DESKTOP);
		}
							
				}
        });
        mLongClickPopupDialog.show(v);
    }

    private void editSimContact(final long rawContactId, final int slotId) {
        if (rawContactId <= 0) {
            Log.e(TAG, "editSimContact: rawContactId is invalid:" + rawContactId);
            Toast.makeText(getContext(), R.string.sim_contacts_not_ready, Toast.LENGTH_SHORT).show();
            return;
        }

        final SimContactCache.SimContact cachedSimContact
                = SimContactCache.getSimContactByRawContactIdWithoutSimId(rawContactId);
        if (cachedSimContact == null) {
            Log.e(TAG, "editSimContact: can NOT find cached sim contact for rawContactId "+rawContactId);
            Toast.makeText(getContext(), R.string.sim_contacts_not_ready, Toast.LENGTH_SHORT).show();
            return;
        }

        Intent intent = SimContactUtils.makeEditIntentFromCachedSimContact(cachedSimContact);
        startActivity(intent);
    }

    public void clearDialog() {
        if (mLongClickPopupDialog != null) {
            mLongClickPopupDialog.dismiss();
            mLongClickPopupDialog = null;
        }
    }

    @Override
    protected ContactListAdapter createListAdapter() {
        DefaultContactListAdapter adapter = new DefaultContactListAdapter(getContext(), this);
        adapter.setSectionHeaderDisplayEnabled(isSectionHeaderDisplayEnabled());
        adapter.setDisplayPhotos(ContactsSettingActivity.readShowContactsHeadIconPreference(getActivity()));
        adapter.setShowFavoriteContacts(true);
        // adapter.setListner(new DefaultContactListAdapter.Listener() {
        //
        // @Override
        // public void onAction(int action) {
        // switch (action) {
        // case DefaultContactListAdapter.Listener.Action.START_SLIDE_MODE:
        // ((PinnedHeaderListView) getListView()).lockListMove(true);
        // break;
        // case DefaultContactListAdapter.Listener.Action.STOP_SLIDE_MODE:
        // ((PinnedHeaderListView) getListView()).lockListMove(false);
        // break;
        // }
        // }
        //
        // });
        return adapter;
    }

    @Override
    protected View inflateView(LayoutInflater inflater, ViewGroup container) {
        return inflater.inflate(R.layout.contact_list_content, container, false);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mGM = GroupManager.getInstance(activity);
    }

    @Override
    protected void onCreateView(LayoutInflater inflater, ViewGroup container) {
        //Log.w(TMPTAG, "DefaultContactBrowseListFragment onCreateView enter");
        super.onCreateView(inflater, container);

        mInflater = inflater;

        // If cold launch Contact, doCreateView here
        if (mIsColdLaunchedFromDesktop) {
            doCreateView();
        } else {
            mHaveDelayedCall = true;
        }
        mDefaultSharedPreference = PreferenceManager.getDefaultSharedPreferences(getContext());
        mNotifyContactNumber = mDefaultSharedPreference.getInt(NOTIFY_CONTACT_NUMBER, 0);

        // Note: From bug 8111849, we do not need to launch XiaoYun now.
        // But YunOS is like a box of chocolates. We never know what we're gonna get.
        // So keep the code for some days, in case Yunold Schwarzenegger says: I'll be back.
        //XiaoYunGestureHelper xiaoYunGestureHelper = new XiaoYunGestureHelper(getActivity(), getListView());
        //xiaoYunGestureHelper.monitorGesture();
    }

    private void doCreateView() {
        if (mDidCreateView) {
            return;
        }

        mNoContactsFound = (TextView) getRootView().findViewById(R.id.empty);

        initHeaderView();
        mDidCreateView = true;
    }

    private void initHeaderView() {
        ViewStub vs = (ViewStub) getRootView().findViewById(R.id.contacts_list_header);
        if (vs == null) {
            return;
        }

        mHeaderView = vs.inflate();
        mHeaderView.setVisibility(View.VISIBLE);
        mSearchView = (SearchView) getRootView().findViewById(R.id.search_view);
        mSearchView.setAnchorView(((PeopleActivity2) getActivity()).getTabIndicator());
        mSearchView.setSearchViewListener2(mQueryTextListener);
        mAddContact = (ImageView) getRootView().findViewById(R.id.add_contact);
        mAddContact.setVisibility(View.VISIBLE);
        mSearchView.setBackgroundColor(getActivity().getResources().getColor(R.color.aui_bg_color_white));
        mAddContact.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                createNewContact();
                UsageReporter.onClick(null, DefaultContactBrowseListFragment.TAG,
                        UsageReporter.ContactsListPage.CONTACTS_LIST_NEW_CONTACT);
                hideDialpadFragment();
            }
        });
    }

    private void hideHeaderView() {
        if (mHeaderView != null) {
            mHeaderView.setVisibility(View.GONE);
        }
    }

    private void showHeadView() {
        if (mHeaderView != null) {
            mHeaderView.setVisibility(View.VISIBLE);
        }
    }

    public void updateGroupButtonText(String text) {
        mGroupName = text;
    }

    private SearchView.SearchViewListener2 mQueryTextListener = new SearchView.SearchViewListener2() {
        @Override
        public void startOutAnimationEnd(int time) {
        }

        @Override
        public void startInAnimationEnd(int time) {
        }

        @Override
        public void startOutAnimation(int time) {
            Activity activity = getActivity();
            if (activity instanceof PeopleActivity2) {
                ((PeopleActivity2) activity).setSearchMode(false, TabState.ALL);
                mAddContact.setVisibility(View.VISIBLE);
            }
        }

        @Override
        public void startInAnimation(int time) {
            UsageReporter.onClick(null, TAG, UsageReporter.ContactsListPage.SEARCH_CL);
            Activity activity = getActivity();
            if (activity instanceof PeopleActivity2) {
                ((PeopleActivity2) activity).setSearchMode(true, TabState.ALL);
                mAddContact.setVisibility(View.GONE);
            }
        }

        @Override
        public void doTextChanged(CharSequence s) {
            setQueryString(s.toString(), true);
        }

    };

    @Override
    protected void setSearchMode(boolean flag) {
        super.setSearchMode(flag);
        checkHeaderViewVisibility();
    }

    private void checkHeaderViewVisibility() {
        if (mCounterHeaderView != null) {
            mCounterHeaderView.setVisibility(isSearchMode() ? View.GONE : View.VISIBLE);
        }

        // Hide the search header by default. See showCount().
        if (mSearchHeaderView != null) {
            mSearchHeaderView.setVisibility(View.GONE);
        }
        if (mNoContactsFound != null) {
            mNoContactsFound.setVisibility(View.GONE);
        }
    }

    @Override
    public void setFilter(ContactListFilter filter) {
        super.setFilter(filter);
    }

    /*
     * private void updateFilterHeaderView() { if (mAccountFilterHeader == null)
     * { return; // Before onCreateView -- just ignore it. } final
     * ContactListFilter filter = getFilter(); if (filter != null &&
     * !isSearchMode()) { final boolean shouldShowHeader =
     * AccountFilterUtil.updateAccountFilterTitleForPeople(
     * mAccountFilterHeader, filter, false);
     * mAccountFilterHeader.setVisibility(shouldShowHeader ? View.VISIBLE :
     * View.GONE); } else { mAccountFilterHeader.setVisibility(View.GONE); } }
     */

    @Override
    protected void showCount(int partitionIndex, Cursor data) {
        Activity activity = getActivity();
        if (activity == null || activity.isFinishing()) {
            return;
        }

        updatePopMenuItem();
        mFetchedContactData = true;
        if (mCalledUserVisibleHint) {
            // If the fragment is visible to user, that means we did all thing.
            // so post other
            // fragments runnable to init.
            if (mIsColdLaunchedFromDesktop) {
                mIsColdLaunchedFromDesktop = false;
                ((PeopleActivity2) activity).postOtherFragmentInitialStart(TabState.ALL);
            }
            // The two flags are one-shot, just to post other fragments
            // runnable.
            // So reset them here.
            mFetchedContactData = false;
            mCalledUserVisibleHint = false;
        }

        if (!isSearchMode() && data != null) {
            int count;
            if (PeopleActivity2.sFirstLoad) {
                count = mDefaultSharedPreference.getInt(CONTACTS_COUNT, 0);
                mCachedCount = count;
                if (count == 0) {
                    return;
                }
            } else {
                count = data.getCount();
                if (mCachedCount != count && !((DefaultContactListAdapter) getAdapter()).isShowGroupState()) {
                    PreferencesUtils.commitIntSharedPreferencesInBackground(
                            mDefaultSharedPreference, CONTACTS_COUNT, count);
                    mCachedCount = count;
                }
            }
            if (count != 0) {
                count -= getFavoriteContactsCount();
                // YunOS BEGIN PB
                // ##module:(Contacts)  ##author:shihuai.wg@alibaba-inc.com
                // ##BugID:(8443368)  ##date:2016-07-05
                if (count < 0) {
                    count = getFavoriteContactsCount();
                }
                // YunOS END PB
                showNoContactsEmptyView(false);
                showHeadView();
                String format = getResources().getQuantityText(R.plurals.listTotalAllContacts, count).toString();
                if (mSearchView != null) {
                    mSearchView.setQueryHint(String.format(format, count));
                }

                if (mListener != null) {
                    mListener.onGetContactCount(count);
                }

                // For notify user contact number function. @ {
                int contactNumber = -1;
                if (count >= 200) {
                    contactNumber = 200;
                } else if (count >= 50) {
                    contactNumber = 50;
                } else if (count >= 10) {
                    contactNumber = 10;
                }
                if (contactNumber > mNotifyContactNumber) {
                    mNotifyContactNumber = contactNumber;
                    Log.d(TAG, "send broadcast to notify contact number " + contactNumber);
                    Intent intent = new Intent(ACTION_NOTIFY_NEW_INSERT_CONTACT);
                    intent.putExtra(NOTIFY_CONTACT_NUMBER, contactNumber);
                    activity.sendBroadcast(intent);
                    PreferencesUtils.commitIntSharedPreferencesInBackground(
                            mDefaultSharedPreference, NOTIFY_CONTACT_NUMBER, mNotifyContactNumber);
                }
                // } @
            } else {
                String format = getResources().getQuantityText(R.plurals.listTotalAllContacts, count).toString();
                if (mSearchView != null) {
                    mSearchView.setQueryHint(String.format(format, count));
                }

                if (!((DefaultContactListAdapter) getAdapter()).isShowGroupState()) {
                    setNoContactsEmptyView(mInflater, false);
                    hideHeaderView();
                } else {
                    setNoContactsEmptyView(mInflater, true);
                }
                showNoContactsEmptyView(true);

                mListener.onGetContactCount(count);
            }
        } else {
            ContactListAdapter adapter = getAdapter();
            if (adapter == null) {
                return;
            }

            // In search mode we only display the header if there is nothing
            // found
            if (TextUtils.isEmpty(getQueryString()) || !adapter.areAllPartitionsEmpty()) {
                if (mNoContactsFound != null) {
                    mNoContactsFound.setVisibility(View.GONE);
                }
            } else {
                /*
                 * if (adapter.isLoading()) { } else {
                 */
                if (!adapter.isLoading() && mNoContactsFound != null) {
                    mNoContactsFound.setVisibility(View.VISIBLE);
                    mNoContactsFound.setText(R.string.listFoundAllContactsZero);
                }
            }
            showNoContactsEmptyView(false);
            showHeadView();
        }
    }

    public void updatePopMenuItem() {
        boolean enable = (getAdapter() == null || getAdapter().getCount() == 0) ? false : true;
        ((PeopleActivity2)getActivity()).setMenuItemEnable(PeopleActivity2.FOOTER_BUTTON_CONTACTS_DELETE, enable);
    }

    private void showNoContactsEmptyView(boolean show) {
        mContactsEmpty = show;
        if (mFootViewContainer != null) {
            mFootViewContainer.setVisibility(show ? View.VISIBLE : View.GONE);
        }
    }

    /**
     * This method add footer view for no no contacts in the db.
     */
    private void setNoContactsEmptyView(LayoutInflater inflater, boolean group) {

        if (mFootViewContainer == null) {
            mFootViewContainer = (FrameLayout) this.getRootView().findViewById(R.id.empty_container);
        }
        mFootViewContainer.removeAllViews();

        if (group) {
            if (mEmptyViewGroup == null) {
                mEmptyViewGroup = inflater.inflate(R.layout.group_mem_list_empty, null, false);

                Button addGroupMem = (Button) mEmptyViewGroup.findViewById(R.id.add_group_mem_ll);
                addGroupMem.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        addGroupMembers();
                    }
                });
            }
        } else {
            if (mEmptyViewCommon == null) {
                mEmptyViewCommon = inflater.inflate(R.layout.contacts_list_empty, null, false);
                TextView sepTitle = (TextView) mEmptyViewCommon.findViewById(R.id.gd_text);
                sepTitle.setText(R.string.contact_empty_title);

                /* YUNOS BEGIN */
                //##modules(AliContacts) ##author: hongwei.zhw
                //##BugID:(8161644) ##date:2016.4.18
                //##descrpition: remove some menu which will cause contacts crash without com.aliyun.xiaoyunmi
                if (Build.YUNOS_CARRIER_CMCC) {
                    TextView cloudView = (TextView) mEmptyViewCommon.findViewById(R.id.method_cloud);
                    cloudView.setVisibility(View.GONE);
                }
                /* YUNOS END */

                /*YunOS BEGIN PB*/
               //##module:Contacts##author:xingnuan.cxn@alibaba-inc.com
               //##BugID:(8258946) ##date:2016-5-13 09:00
               //##description:remove  syncContacts which will cause contacts crash without com.aliyun.xiayunmi
               if (!Build.YUNOS_CARRIER_CMCC) {
                    mEmptyViewCommon.findViewById(R.id.method_cloud).setOnClickListener(new OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            syncContacts();
                       }
                    });
                }
               /*YUNOS END PB*/
                mEmptyImportFromSimView = (TextView) mEmptyViewCommon.findViewById(R.id.method_sim);
                mEmptyImportFromSimView.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        SimContactUtils.launch(getActivity(), SimContactUtils.SIM_MULTI_SELECT_ACTION_IMPORT_FROM_SIM);
                        UsageReporter.onClick(null, TAG, UsageReporter.ContactsListPage.EMPTY_IMPORT_FROM_SIM);
                    }
                });
                mEmptyImportFromSimView.setEnabled(!SimUtil.isNoSimCard());

                mEmptyViewCommon.findViewById(R.id.method_vcard).setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent importIntent = new Intent(getActivity(), ImportVCardActivity.class);
                        startActivity(importIntent);

                        UsageReporter.onClick(null, TAG, UsageReporter.ContactsSettingsPage.SETTING_IMPORT_FROM_VCARD);
                    }
                });

                mEmptyViewCommon.findViewById(R.id.create_contacts).setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        createNewContact();
                        UsageReporter.onClick(null, DefaultContactBrowseListFragment.TAG,
                                UsageReporter.ContactsListPage.EMPTY_LIST_NEW_CONTACT);
                    }
                });
            }
        }

        mFootViewContainer.addView(group ? mEmptyViewGroup : mEmptyViewCommon);
    }

    @Override
    public boolean hasFavoriteList() {
        boolean ret = false;
        if (getFavoriteContactsCount() > 0) {
            ret = true;
        }
        return ret;
    }

    @Override
    public void selectedAll(boolean all) {
    }

    /*
     * private void updateGroupPhotoName(DialerMainGroup itemgroup, View view) {
     * View photo; CharSequence name; TextView tv; int[] location = new int[2];
     * tv = (TextView) view.findViewById(R.id.name); photo =
     * view.findViewById(R.id.photo); photo.getLocationOnScreen(location); name
     * = tv.getText(); if(DBG) Log.d(TAG, "updateGroupPhotoName name " + name);
     * itemgroup.setPhotoName(photo, name, location[0]); }
     */

    // add viewGroup operations
    public void onViewGroupAction(Uri groupUri) {
        Log.d(TAG, "onViewGourpAction: groupUri = " + groupUri);
        Context context = getActivity();
        if (context == null) {
            Log.w(TAG, "onViewGourpAction: getActivity() is null.");
            return;
        }
        mGroupUri = groupUri;
        if (groupUri != null) {
            mGroupId = ContentUris.parseId(groupUri);
            getLoaderManager().restartLoader(LOADER_MEMBERS, null, mGroupMemberListLoaderListener);
        } else {
            getAdapter().setDisplayPhotos(ContactsSettingActivity.readShowContactsHeadIconPreference(context));
            mGroupId = -1;
            ((DefaultContactListAdapter) getAdapter()).setShowGroupState(false);
            getLoaderManager().destroyLoader(LOADER_MEMBERS);
            reloadData();
        }

        PeopleActivity2 activity = (PeopleActivity2) getActivity();
        if (activity == null) {
            return;
        }
    }

    private static final int LOADER_MEMBERS = 10;
    private long mGroupId = -1;
    private Uri mGroupUri;
    public static final int MEM_CONTACT_ID = 0;
    public static final int MEM_RAW_CONTACT_ID = 1;
    public static final int MEM_ID_COUNT = 2;
    private String[] mMemIDs = new String[MEM_ID_COUNT];

    private StringBuilder mMemContactIDs;
    private StringBuilder mMemRawContactIDs;

    private GroupManager mGM;

    private final LoaderManager.LoaderCallbacks<Cursor> mGroupMemberListLoaderListener = new LoaderCallbacks<Cursor>() {

        @Override
        public CursorLoader onCreateLoader(int id, Bundle args) {
            return GroupMemberLoader.constructLoaderForGroupDetailQuery(getActivity(), mGroupId);
        }

        @Override
        public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
            Log.v(TAG, "mGroupMemberListLoaderListener.onLoadFinished()");
            mMemContactIDs = new StringBuilder("");
            mMemRawContactIDs = new StringBuilder("");

            while (data.moveToNext()) {
                mMemContactIDs.append(data.getLong(GroupMemberLoader.GroupDetailQuery.CONTACT_ID));
                mMemContactIDs.append(',');
                mMemRawContactIDs.append(data.getLong(GroupMemberLoader.GroupDetailQuery.RAW_CONTACT_ID));
                mMemRawContactIDs.append(',');
            }
            // delete the last ","
            if (data.getCount() > 0 && mMemContactIDs.length() > 0) {
                mMemContactIDs.deleteCharAt(mMemContactIDs.length() - 1);
                mMemRawContactIDs.deleteCharAt(mMemRawContactIDs.length() - 1);
            }

            mMemIDs[MEM_CONTACT_ID] = mMemContactIDs.toString();
            mMemIDs[MEM_RAW_CONTACT_ID] = mMemRawContactIDs.toString();

            ((DefaultContactListAdapter) getAdapter()).setGroupMembersIds(mMemContactIDs.toString(), true);
            reloadData();
        }

        @Override
        public void onLoaderReset(Loader<Cursor> loader) {
        }
    };

    // add viewGroup oprations

    public void addGroupMembers() {
        UsageReporter.onClick(null, TAG, UsageReporter.GroupListPage.GROUP_CONTACTS_ADD_GROUP_MEM);
        Intent intent = new Intent(ContactSelectionActivity.ACTION_PICK_MULTIPLE);
        intent.putExtra(ContactSelectionActivity.PICK_CONTENT, ContactSelectionActivity.PICK_CONTACT_ADD_TO_GROUP);
        intent.putExtra(ContactSelectionActivity.EXTRA_GROUP_MEM_IDS, mMemIDs);
        intent.putExtra(ContactSelectionActivity.EXTRA_GROUP_ID, mGroupId);
        startActivity(intent);
    }

    public void rmGroupMembers() {
        UsageReporter.onClick(null, TAG, UsageReporter.GroupListPage.GROUP_CONTACTS_RE_GROUP_MEM);
        Intent intent = new Intent(ContactSelectionActivity.ACTION_PICK_MULTIPLE);
        intent.putExtra(ContactSelectionActivity.PICK_CONTENT, ContactSelectionActivity.PICK_CONTACT_IN_GROUP_TO_RM);
        intent.putExtra(ContactSelectionActivity.EXTRA_GROUP_MEM_IDS, mMemIDs);
        intent.putExtra(ContactSelectionActivity.EXTRA_GROUP_ID, mGroupId);
        startActivity(intent);
    }

    public void sendSmsToGroup() {
        UsageReporter.onClick(null, TAG, UsageReporter.GroupListPage.GROUP_CONTACTS_SMS_GROUP);
        Intent intent = new Intent(GroupContactSelectionActivity.ACTION_PICK_MULTIPLE);
        intent.putExtra(GroupContactSelectionActivity.PICK_CONTENT, GroupContactSelectionActivity.PICK_PHONE_NUMBER_IN_GROUP);
        intent.putExtra(GroupContactSelectionActivity.EXTRA_GROUP_MEM_IDS, mMemIDs[MEM_CONTACT_ID]);
        intent.putExtra(ContactSelectionActivity.EXTRA_GROUP_ID, mGroupId);
        startActivity(intent);
    }

    public void popupRenameGroupDlg() {
        if (mRenameGroupDlg == null) {
            final Activity activity = this.getActivity();
            if (activity == null) {
                Log.e(TAG, "popupRenameGroupDlg: activity is null. ERROR!!!");
                return;
            }
            if (mGroupUri == null) {
                Log.e(TAG, "popupRenameGroupDlg: mGroupUri is null. ERROR!!!");
                return;
            }
            Uri uri = this.mGroupUri;
            final Long groupId = ContentUris.parseId(uri);
            final LayoutInflater inflater = activity.getLayoutInflater();
            mGroupNameText = (EditText) inflater.inflate(R.layout.create_group_edit, null);

            FrameLayout.LayoutParams p = new FrameLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
            final int margin = getResources().getDimensionPixelSize(R.dimen.custom_tag_dialog_edit_margin_left_right);
            p.gravity = Gravity.CENTER;
            p.setMarginStart(margin);
            p.setMarginEnd(margin);
            mGroupNameText.setLayoutParams(p);
            mGroupNameText.setFocusable(true);
            mGroupNameText.setFocusableInTouchMode(true);

            AlertDialog.Builder builder = new AlertDialog.Builder(activity);
            mRenameGroupDlg = builder.setCancelable(true)
                    .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int whichButton) {
                            String label = mGroupNameText.getText().toString().trim();
                            if (label.equals(mGroupName)) {
                                return;
                            }
                            if (label != null && !label.isEmpty()) {
                                if (mGM != null && mGM.hasGroup(label)) {
                                    Toast.makeText(activity, R.string.create_same_name_group_error, Toast.LENGTH_LONG).show();
                                } else {
                                    renameGroup(groupId, label);
                                    updateGroupButtonText(label);
                                }
                            } else {
                                Toast.makeText(activity, R.string.no_group_name, Toast.LENGTH_LONG).show();
                            }
                        }
                    }).setNegativeButton(R.string.no, null).create();
            builder.setView(mGroupNameText);
            mRenameGroupDlg.setTitle(R.string.editGroupDescription);
            mRenameGroupDlg.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        }
        mGroupNameText.setText(mGroupName);
        // EditText max length 15, when set longer content, will be cut,
        // so when set selection, we should use text after cut.
        Editable text = mGroupNameText.getText();
        if (text != null) {
            mGroupNameText.setSelection(text.length());
        }
        mGroupNameText.requestFocus();
        mRenameGroupDlg.show();
    }

    private void renameGroup(long groupId, String newLabel) {
        final Activity activity = this.getActivity();
        if (activity == null) {
            Log.e(TAG, "renameGroup: activity is null. ERROR!!!");
            return;
        }
        UsageReporter.onClick(null, TAG, UsageReporter.GroupListPage.GROUP_CONTACTS_GROUP_EDIT);

        Intent rename = ContactSaveService.createGroupRenameIntent(activity, groupId, newLabel, getActivity().getClass(),
                UiIntentActions.LIST_GROUP_ACTION);
        activity.startService(rename);
    }

    public void showDeleteGroupDialog() {
        final Activity activity = this.getActivity();
        if (activity == null) {
            Log.e(TAG, "showDeleteGroupDialog: activity is null. ERROR!!!");
            return;
        }
        if (mGroupUri == null) {
            Log.e(TAG, "showDeleteGroupDialog: mGroupUri is null. ERROR!!!");
            return;
        }

        final long groupId = ContentUris.parseId(mGroupUri);
        String message = activity.getString(R.string.delete_group_dialog_message);
        Context context = getContext();
        AlertDialog.Builder build = new AlertDialog.Builder(context);
        build.setPositiveButton(R.string.confirm_delete, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                deleteGroup(groupId);
            }
        });
        build.setNegativeButton(R.string.no, null);
        build.setMessage(message);
        build.create().show();
    }

    protected void deleteGroup(long groupId) {
        final Activity activity = this.getActivity();
        if (activity == null) {
            Log.e(TAG, "showDeleteGroupDialog: activity is null. ERROR!!!");
            return;
        }
        UsageReporter.onClick(null, TAG, UsageReporter.GroupListPage.GROUP_CONTACTS_DELETE_GROUP);
        activity.startService(ContactSaveService.createGroupDeletionIntent(activity, groupId));

        this.onViewGroupAction(null);
        updateGroupButtonText(getString(R.string.group_all_people));
    }

    public boolean isShowGroup() {
        return mGroupUri != null;
    }

    public void handlerGroupFunctionBtn() {
        boolean showGroup = ((DefaultContactListAdapter) getAdapter()).isShowGroupState();
        if (!showGroup) {
            return;
        }

        final Activity activity = this.getActivity();
        if (activity == null) {
            Log.e(TAG, "handlerGroupMoreBtn: activity is null. ERROR!");
            return;
        }

        int count = getAdapter().getCount();
        if (count <= 0 && mGroupUri != null) {
            popupNoMemeberGroupFuntionDlg(activity);
        } else {
            popupGroupFuntionDlg(activity);
        }
    }

    private void popupGroupFuntionDlg(Activity activity) {
        if (activity == null) {
            return;
        }
        CharSequence[] items = getResources().getTextArray(R.array.contacts_group_action_items);

        List<String> item_str = new ArrayList<String>();
        for (int i = 0; i < items.length; i++) {
            item_str.add(items[i].toString());
        }

        final ActionSheet actionSheet = new ActionSheet(activity);
        actionSheet.setTitle(activity.getString(R.string.GroupFounctions));
        actionSheet.setCommonButtons(item_str);
        actionSheet.setCommonButtonListener(new ActionSheet.CommonButtonListener() {

            @Override
            public void onDismiss(ActionSheet arg0) {
            }

            @Override
            public void onClick(int position) {
                switch (position) {
                    case 0: // sms to group
                        sendSmsToGroup();
                        break;
                    case 1: // add_group_members
                        addGroupMembers();
                        break;
                    case 2: // rm_group_members
                        rmGroupMembers();
                        break;
                    case 3: // rename
                        popupRenameGroupDlg();
                        break;
                    case 4: // delete
                        showDeleteGroupDialog();
                    default:
                        break;
                }
            }
        });

        actionSheet.show(mFootViewContainer);
    }

    private void popupNoMemeberGroupFuntionDlg(Activity activity) {
        if (activity == null) {
            return;
        }
        CharSequence[] items = getResources().getTextArray(R.array.contacts_no_memeber_group_action_items);

        List<String> item_str = new ArrayList<String>();
        for (int i = 0; i < items.length; i++) {
            item_str.add(items[i].toString());
        }

        final ActionSheet actionSheet = new ActionSheet(activity);
        actionSheet.setTitle(activity.getString(R.string.GroupFounctions));
        actionSheet.setCommonButtons(item_str);
        actionSheet.setCommonButtonListener(new ActionSheet.CommonButtonListener() {

            @Override
            public void onDismiss(ActionSheet arg0) {
            }

            @Override
            public void onClick(int position) {
                switch (position) {
                    case 0: // add_group_members
                        addGroupMembers();
                        break;
                    case 1: // rename
                        popupRenameGroupDlg();
                        break;
                    case 2: // delete
                        showDeleteGroupDialog();
                    default:
                        break;
                }
            }
        });

        actionSheet.show(mFootViewContainer);
    }

    public void deleteContacts() {
        final Activity activity = this.getActivity();
        if (activity == null) {
            Log.e(TAG, "deleteContacts: activity is null. ERROR!");
            return;
        }
        Intent intent = new Intent(ContactSelectionActivity.ACTION_PICK_MULTIPLE);
        intent.putExtra(ContactSelectionActivity.PICK_CONTENT, ContactSelectionActivity.PICK_CONTACT_TO_DELETE);
        intent.putExtra(ContactSelectionActivity.EXTRA_LIST_POSITION, mListPosition);
        intent.putExtra(ContactSelectionActivity.KEY_EXTRA_CURRENT_FILTER, getFilter());
        startActivity(intent);
    }

    public void syncContacts() {
        UsageReporter.onClick(null, TAG, UsageReporter.ContactsSettingsPage.SETTING_SYNC);
        Intent intent = new Intent("com.aliyun.xiaoyunmi.action.SELECT_SYNC");
        intent.setData(Uri.parse("yunmi://sync_select?type=contact"));
        intent.putExtra("synctype", "sync");
        startActivity(intent);
    }

    public interface OnSearchChangeListener {
        public void onSearchChanged(String query);
    }

    @Override
    public void onStop() {
        super.onStop();
        mIsNeedRefresh = true;
    }

    /**
     * Back to normal state from search state.
     */
    public void backToNormalState() {
        if (mSearchView != null) {
            mSearchView.animateToNormal();
            mSearchView.setQuery("");
            Activity activity = getActivity();
            if (activity instanceof PeopleActivity2) {
                ((PeopleActivity2) activity).setSearchMode(false, TabState.ALL);
            }
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        UsageReporter.onPause(null, USAGE_REPORTER_PACKAGE_NAME);
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        Log.w(TMPTAG, "DefaultContactBrowseListFragment setUserVisibleHint called, isVisibleToUser = " + isVisibleToUser);
        // When this fragment's visibility is changed, it will be invoked.
        super.setUserVisibleHint(isVisibleToUser);

        if (isVisibleToUser) {
            startNameConvertWorker();

            Activity activity = getActivity();
            if (!(activity instanceof PeopleActivity2)) {
                Log.w(TMPTAG,
                        "DefaultContactBrowseListFragment setUserVisibleHint return because activity is not PeopleActivity2");
                return;
            }

            mCalledUserVisibleHint = true;
            if (mFetchedContactData) {
                // if mFetchedContactData is true, it means we got data before
                // the fragment
                // is visible to user, we did all things(onCreateView, onStart,
                // onResume,
                // loadFinished), so post other fragments runnablt to init.
                // else, we will wait for query return, then post other
                // fragments runnable to init.
                if (mIsColdLaunchedFromDesktop) {
                    mIsColdLaunchedFromDesktop = false;
                    ((PeopleActivity2) activity).postOtherFragmentInitialStart(TabState.ALL);
                }
                // The two flags are one-shot, just to post other fragments
                // runnable.
                // So reset them here.
                mFetchedContactData = false;
                mCalledUserVisibleHint = false;
            }

            // Add mDidCreateView condition for bug 5228095, from the
            // phenomenon, we didn't
            // call following functions when the fragment is visible to user
            if (mHaveDelayedCall || mIsNeedRefresh || mInitialStartFailed || !mDidCreateView) {
                // If initial start is failed, it need to reload contacts data.
                doCreateView();
                doSuperStartAction();
                doResume();
                mInitialStartFailed = false;
                mIsNeedRefresh = false;
                mHaveDelayedCall = false;
            }
        }
    }

    private void startNameConvertWorker() {
        Log.d(TAG, "startNameConvertWorker:");
        if (NameConvertWorker.getInstance().isInited()) {
            return;
        }
        mHandler.postDelayed(new Runnable() {

            @Override
            public void run() {
                Activity activity = getActivity();
                if (activity != null) {
                    NameConvertWorker.getInstance().init(activity);
                }

            }
        }, 1500);
        // Cold start on low end phone shall be in 1200ms,
        // here add another 300 for fluctuation.
    }

    private void doSuperStartAction() {
        super.doStart();
    }

    // If getActivity() return null, initial start is failed, the first loading
    // step will be skipped.
    // It need to reload contacts data in setUserVisibleHint(), when user switch
    // to contacts tab.
    private boolean mInitialStartFailed;
    private Runnable mInitialStart = new Runnable() {
        @Override
        public void run() {
            if (getActivity() == null) {
                Log.i(TAG, "mInitialStart getActivity() is null. Initial start is failed.");
                mInitialStartFailed = true;
                return;
            }
            doCreateView();
            doSuperStartAction();
            doResume();
            mHaveDelayedCall = false;
        }
    };

    public void postInitialStart() {
        mHandler.removeCallbacks(mInitialStart);
        mHandler.postDelayed(mInitialStart, 0);
    }

    @Override
    public void onDestroy() {
        mIsColdLaunchedFromDesktop = false;
        mIsNeedRefresh = false;
        super.onDestroy();
        if (mRenameGroupDlg != null) {
            mRenameGroupDlg.dismiss();
            mRenameGroupDlg = null;
        }
    }

    private void cleanCache(){
        if(mCacheCursor != null && !mCacheCursor.isClosed()){
            mCacheCursor.close();
        }
    }

    private void checkForReloadData(){
        if(mIsColdLaunchedFromDesktop){
            if (mFavoritesAndContactsLoader != null){
                mIsShowFavoriteContacts = mFavoritesAndContactsLoader.getIsShowFavoriteContactsFromContactSharedPreferences();
                mIsAutoFavoriteContacts = mFavoritesAndContactsLoader.getIsAutoFavoriteContactsFromContactSharedPreferences();
            }
        } else {
            if (mFavoritesAndContactsLoader != null){
                boolean currentShow = mFavoritesAndContactsLoader.getIsShowFavoriteContactsFromContactSharedPreferences();
                boolean currentAuto = mFavoritesAndContactsLoader.getIsAutoFavoriteContactsFromContactSharedPreferences();
                if ((mIsShowFavoriteContacts != currentShow) || (mIsAutoFavoriteContacts != currentAuto)) {
                    mIsShowFavoriteContacts = currentShow;
                    mIsAutoFavoriteContacts = currentAuto;

                    reloadData();
                }
            }
        }
    }

    private boolean isCurrentTab() {
        Activity activity = getActivity();
        if (!(activity instanceof PeopleActivity2)) {
            Log.w(TAG, "isCurrentTab: activity = " + activity);
            return false;
        }

        int tab = ((PeopleActivity2) activity).getCurrentTab();
        boolean result = (tab == TabState.ALL);
        return result;
    }

    @Override
    public void onSimStateChanged(int slot, String state) {
        super.onSimStateChanged(slot, state);
        if (mContactsEmpty && mEmptyImportFromSimView != null) {
            Log.d(TAG, "onSimStateChanged() slot:" + slot);
            if (SimUtil.DUMMY_SLOT_FOR_AIRPLANE_MODE_ON == slot) {
                mEmptyImportFromSimView.setEnabled(false);
            } else {
                final boolean noSim = SimUtil.isNoSimCard();
                Log.d(TAG, "onSimStateChanged() noSim:" + noSim);
                mEmptyImportFromSimView.setEnabled(!noSim);
            }
        }
    }

    /**
     * Called at very start of launch app, to make sure the class loader has
     * loaded the class. If the class is loaded in background before first
     * create instance, the creation time in main thread will be reduced
     * significantly.
     */
    public static void warmUp() {
        // do nothing;
    }

}
