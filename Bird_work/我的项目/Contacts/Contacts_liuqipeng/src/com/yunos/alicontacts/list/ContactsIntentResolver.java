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
import android.app.SearchManager;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcelable;
import android.provider.Contacts.ContactMethods;
import android.provider.Contacts.People;
import android.provider.Contacts.Phones;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds.Email;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.CommonDataKinds.StructuredPostal;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.Intents;
import android.provider.ContactsContract.Intents.Insert;
import android.text.TextUtils;
import android.util.Log;

import com.yunos.alicontacts.ContactsSearchManager;
import com.yunos.alicontacts.R;
import com.yunos.alicontacts.activities.ContactSelectionActivity;
import com.yunos.alicontacts.activities.GroupContactSelectionActivity;
import com.yunos.alicontacts.util.Constants;

/**
 * Parses a Contacts intent, extracting all relevant parts and packaging them
 * as a {@link ContactsRequest} object.
 */
@SuppressWarnings("deprecation")
public class ContactsIntentResolver {

    private static final String TAG = "ContactsIntentResolver";

    private static final String PEOPLE_DIALPAD_ALIAS = "com.yunos.alicontacts.activities.DialtactsContactsActivity";
    private static final String ACTION_TOUCH_DIALER = "com.android.phone.action.TOUCH_DIALER";
    private static final String ACTION_RECENT_CALLS = "com.android.phone.action.RECENT_CALLS";

    private final Activity mContext;

    public ContactsIntentResolver(Activity context) {
        this.mContext = context;
    }

    public ContactsRequest resolveIntent(Intent intent) {
        ContactsRequest request = new ContactsRequest();

        String action = intent.getAction();

        Log.i(TAG, "Called with action: " + action);

        if (UiIntentActions.LIST_DEFAULT.equals(action) ) {
            request.setActionCode(ContactsRequest.ACTION_DEFAULT);
        } else if (UiIntentActions.LIST_ALL_CONTACTS_ACTION.equals(action)) {
            request.setActionCode(ContactsRequest.ACTION_ALL_CONTACTS);
        } else if (UiIntentActions.LIST_CONTACTS_WITH_PHONES_ACTION.equals(action)) {
            request.setActionCode(ContactsRequest.ACTION_CONTACTS_WITH_PHONES);
        } else if (UiIntentActions.LIST_STARRED_ACTION.equals(action)) {
            request.setActionCode(ContactsRequest.ACTION_STARRED);
        } else if (UiIntentActions.LIST_FREQUENT_ACTION.equals(action)) {
            request.setActionCode(ContactsRequest.ACTION_FREQUENT);
        } else if (UiIntentActions.LIST_STREQUENT_ACTION.equals(action)) {
            request.setActionCode(ContactsRequest.ACTION_STREQUENT);
        } else if (UiIntentActions.LIST_GROUP_ACTION.equals(action)) {
            request.setActionCode(ContactsRequest.ACTION_GROUP);
            // We no longer support UI.GROUP_NAME_EXTRA_KEY
        } else if (Intent.ACTION_PICK.equals(action)) {
            final String resolvedType = intent.resolveType(mContext);
            if (Contacts.CONTENT_TYPE.equals(resolvedType)) {
                request.setActionCode(ContactsRequest.ACTION_PICK_CONTACT);
            } else if (People.CONTENT_TYPE.equals(resolvedType)) {
                request.setActionCode(ContactsRequest.ACTION_PICK_CONTACT);
                // Do NOT support legacy compatibility mode any more.
                // The legacy compatibility mode supports APIs that are deprecated in level 5.
                //request.setLegacyCompatibilityMode(true);
                request.setValid(false);
            } else if (Phone.CONTENT_TYPE.equals(resolvedType)) {
                request.setActionCode(ContactsRequest.ACTION_PICK_PHONE);
            } else if (Phones.CONTENT_TYPE.equals(resolvedType)) {
                request.setActionCode(ContactsRequest.ACTION_PICK_PHONE);
                // Do NOT support legacy compatibility mode any more.
                // The legacy compatibility mode supports APIs that are deprecated in level 5.
                //request.setLegacyCompatibilityMode(true);
                request.setValid(false);
            } else if (StructuredPostal.CONTENT_TYPE.equals(resolvedType)) {
                request.setActionCode(ContactsRequest.ACTION_PICK_POSTAL);
            } else if (ContactMethods.CONTENT_POSTAL_TYPE.equals(resolvedType)) {
                request.setActionCode(ContactsRequest.ACTION_PICK_POSTAL);
                // Do NOT support legacy compatibility mode any more.
                // The legacy compatibility mode supports APIs that are deprecated in level 5.
                //request.setLegacyCompatibilityMode(true);
                request.setValid(false);
            } else if (Email.CONTENT_TYPE.equals(resolvedType)) {
                request.setActionCode(ContactsRequest.ACTION_PICK_EMAIL);
            }
        } else if (Intent.ACTION_CREATE_SHORTCUT.equals(action)) {
            String component = intent.getComponent().getClassName();
            if ("alias.DialShortcut".equals(component)) {
                request.setActionCode(ContactsRequest.ACTION_CREATE_SHORTCUT_CALL);
            } else if ("alias.MessageShortcut".equals(component)) {
                request.setActionCode(ContactsRequest.ACTION_CREATE_SHORTCUT_SMS);
            } else {
                request.setActionCode(ContactsRequest.ACTION_CREATE_SHORTCUT_CONTACT);
            }
        } else if (Intent.ACTION_GET_CONTENT.equals(action)) {
            String type = intent.getType();
            if (Contacts.CONTENT_ITEM_TYPE.equals(type)) {
                request.setActionCode(ContactsRequest.ACTION_PICK_OR_CREATE_CONTACT);
            } else if (Phone.CONTENT_ITEM_TYPE.equals(type)) {
                request.setActionCode(ContactsRequest.ACTION_PICK_PHONE);
            } else if (Phones.CONTENT_ITEM_TYPE.equals(type)) {
                request.setActionCode(ContactsRequest.ACTION_PICK_PHONE);
                // Do NOT support legacy compatibility mode any more.
                // The legacy compatibility mode supports APIs that are deprecated in level 5.
                //request.setLegacyCompatibilityMode(true);
                request.setValid(false);
            } else if (StructuredPostal.CONTENT_ITEM_TYPE.equals(type)) {
                request.setActionCode(ContactsRequest.ACTION_PICK_POSTAL);
            } else if (ContactMethods.CONTENT_POSTAL_ITEM_TYPE.equals(type)) {
                request.setActionCode(ContactsRequest.ACTION_PICK_POSTAL);
                // Do NOT support legacy compatibility mode any more.
                // The legacy compatibility mode supports APIs that are deprecated in level 5.
                //request.setLegacyCompatibilityMode(true);
                request.setValid(false);
            }  else if (People.CONTENT_ITEM_TYPE.equals(type)) {
                request.setActionCode(ContactsRequest.ACTION_PICK_OR_CREATE_CONTACT);
                // Do NOT support legacy compatibility mode any more.
                // The legacy compatibility mode supports APIs that are deprecated in level 5.
                //request.setLegacyCompatibilityMode(true);
                request.setValid(false);
            }
        } else if (Intent.ACTION_INSERT_OR_EDIT.equals(action)) {
            request.setActionCode(ContactsRequest.ACTION_INSERT_OR_EDIT_CONTACT);
        } else if (Intent.ACTION_SEARCH.equals(action)) {
            String query = intent.getStringExtra(SearchManager.QUERY);
            // If the {@link SearchManager.QUERY} is empty, then check if a phone number
            // or email is specified, in that priority.
            if (TextUtils.isEmpty(query)) {
                query = intent.getStringExtra(Insert.PHONE);
            }
            if (TextUtils.isEmpty(query)) {
                query = intent.getStringExtra(Insert.EMAIL);
            }
            request.setQueryString(query);
            request.setSearchMode(true);
        } else if (Intent.ACTION_VIEW.equals(action)) {
            String component = intent.getComponent().getClassName();
            if (component.equals(PEOPLE_DIALPAD_ALIAS)) {
                request.setActionCode(ContactsRequest.ACTION_VIEW_DIALPAD);
            } else {
                final String resolvedType = intent.resolveType(mContext);
                if (ContactsContract.Contacts.CONTENT_TYPE.equals(resolvedType)
                        || android.provider.Contacts.People.CONTENT_TYPE.equals(resolvedType)) {
                    request.setActionCode(ContactsRequest.ACTION_ALL_CONTACTS);
                } else {
                    request.setActionCode(ContactsRequest.ACTION_VIEW_CONTACT);
                    request.setContactUri(intent.getData());
                    intent.setAction(Intent.ACTION_DEFAULT);
                    intent.setData(null);
                }
            }
        } else if (UiIntentActions.FILTER_CONTACTS_ACTION.equals(action)) {
            // When we get a FILTER_CONTACTS_ACTION, it represents search in the context
            // of some other action. Let's retrieve the original action to provide proper
            // context for the search queries.
            request.setActionCode(ContactsRequest.ACTION_DEFAULT);
            Bundle extras = intent.getExtras();
            if (extras != null) {
                request.setQueryString(extras.getString(UiIntentActions.FILTER_TEXT_EXTRA_KEY));

                ContactsRequest originalRequest =
                        (ContactsRequest)extras.get(ContactsSearchManager.ORIGINAL_REQUEST_KEY);
                if (originalRequest != null) {
                    request.copyFrom(originalRequest);
                }
            }

            request.setSearchMode(true);

        // Since this is the filter activity it receives all intents
        // dispatched from the SearchManager for security reasons
        // so we need to re-dispatch from here to the intended target.
        } else if (Intents.SEARCH_SUGGESTION_CLICKED.equals(action)) {
            Uri data = intent.getData();
            request.setActionCode(ContactsRequest.ACTION_VIEW_CONTACT);
            request.setContactUri(data);
            intent.setAction(Intent.ACTION_DEFAULT);
            intent.setData(null);
        } else if (ContactSelectionActivity.ACTION_PICK_MULTIPLE.equals(action)) {
            String content = intent.getStringExtra(GroupContactSelectionActivity.PICK_CONTENT);
            if (content == null) {
                request.setValid(false);
            } else if (content.equals(ContactSelectionActivity.PICK_CONTACT_TO_DELETE)) {
                request.setActionCode(ContactsRequest.ACTION_PICK_MULTIPLE_CONTACT_TO_DELETE);
                request.setPosition(intent.getIntExtra(
                        ContactSelectionActivity.EXTRA_LIST_POSITION, 0));
            } else if (content.equals(ContactSelectionActivity.PICK_CONTACT_ADD_TO_GROUP)) {
                String[] ids = intent.getStringArrayExtra(ContactSelectionActivity.EXTRA_GROUP_MEM_IDS);
                long groupID = intent.getLongExtra(ContactSelectionActivity.EXTRA_GROUP_ID, -1);

                setGroupsData(request, groupID, ids);
                request.setActionCode(ContactsRequest.ACTION_PICK_MULTIPLE_CONTACT_ADD_TO_GROUP);
            } else if (content.equals(ContactSelectionActivity.PICK_CONTACT_IN_GROUP_TO_RM)) {
                String[] ids = intent.getStringArrayExtra(ContactSelectionActivity.EXTRA_GROUP_MEM_IDS);
                long groupID = intent.getLongExtra(ContactSelectionActivity.EXTRA_GROUP_ID, -1);

                setGroupsData(request, groupID, ids);
                request.setActionCode(ContactsRequest.ACTION_PICK_MULTIPLE_CONTACT_RM_FROM_GROUP);
                /*YunOS BEGIN PB*/
                //##module:Contacts##author:xingnuan.cxn@alibaba-inc.com
                //##BugID:(8466294) ##date:2016-7-22 09:00
                //##description:suppot export some contacts to vcard
            }else if (content.equals(ContactSelectionActivity.PICK_CONTACT_TO_EXPORT)) {
                request.setActionCode(ContactsRequest.ACTION_PICK_MULTIPLE_CONTACT_TO_EXPORT);
                request.setPosition(intent.getIntExtra(
                        ContactSelectionActivity.EXTRA_LIST_POSITION, 0));
            }
            /*YUNOS END PB*/
        } else if (GroupContactSelectionActivity.ACTION_PICK_MULTIPLE.equals(action)) {
            String content = intent.getStringExtra(GroupContactSelectionActivity.PICK_CONTENT);
            if (content == null) {
                request.setValid(false);
            } else if (content.equals(GroupContactSelectionActivity.PICK_PHONE_NUMBER)) {
                Parcelable[] uris = intent.getParcelableArrayExtra(Constants.EXTRA_PHONE_URIS);
                if (uris != null) {
                    request.setContactUriArray(uris);
                }

                request.setActionCode(ContactsRequest.ACTION_PICK_MULTIPLE_PHONE_NUMBER);
            }else if (content.equals(GroupContactSelectionActivity.PICK_PHONE_NUMBER_IN_GROUP)) {
                String ids = intent.getStringExtra(GroupContactSelectionActivity.EXTRA_GROUP_MEM_IDS);
                long groupID = intent.getLongExtra(ContactSelectionActivity.EXTRA_GROUP_ID, -1);
                if (groupID < 0) {
                    request.setValid(false);
                } else {
                    request.setGroupID(groupID);
                    request.setQueryString(ids);
                    request.setActionCode(ContactsRequest.ACTION_PICK_MULTIPLE_PHONE_NUMBER_IN_GOURP);
                }
            }

        } else if(Intent.ACTION_MAIN.equals(action)) {
            String component = intent.getComponent().getClassName();
            if (component.equals(PEOPLE_DIALPAD_ALIAS)) {
                request.setActionCode(ContactsRequest.ACTION_VIEW_DIALPAD);
            } else {
                request.setActionCode(ContactsRequest.ACTION_ALL_CONTACTS);
            }
        } else if (Intent.ACTION_DIAL.equals(action) || Intent.ACTION_CALL_BUTTON.equals(action)
                || ACTION_TOUCH_DIALER.equals(action) || ACTION_RECENT_CALLS.equals(action)) {
            String component = intent.getComponent().getClassName();
            if (component.equals(PEOPLE_DIALPAD_ALIAS)) {
                request.setActionCode(ContactsRequest.ACTION_VIEW_DIALPAD);
            }
        } else {
            request.setActivityTitle(mContext.getResources().getString(R.string.contactsList));
        }

        // Allow the title to be set to a custom String using an extra on the intent
        String title = intent.getStringExtra(UiIntentActions.TITLE_EXTRA_KEY);
        if (title != null) {
            request.setActivityTitle(title);
        }
        return request;
    }

    private void setGroupsData(ContactsRequest request, long groupId, String[] groupMemberIds) {
        if (groupId < 0) {
            request.setValid(false);
        } else {
            request.setGroupID(groupId);
            if (groupMemberIds == null) {
                groupMemberIds = new String[0];
            }
            request.setGroupMemIDs(groupMemberIds);
        }
    }

}
