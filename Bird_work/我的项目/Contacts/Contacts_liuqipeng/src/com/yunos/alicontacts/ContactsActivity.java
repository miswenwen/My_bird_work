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

package com.yunos.alicontacts;

import android.content.ContentResolver;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;

import com.yunos.alicontacts.activities.ContactEditorActivity;
import com.yunos.alicontacts.activities.TransactionSafeActivity;
import com.yunos.alicontacts.test.InjectedServices;

import yunos.support.v4.app.Fragment;
import yunos.support.v4.app.FragmentManager;
import yunos.support.v4.app.FragmentTransaction;

/**
 * A common superclass for Contacts activities that handles application-wide services.
 */
public abstract class ContactsActivity extends TransactionSafeActivity
    implements ContactSaveService.Listener
{

    private ContentResolver mContentResolver;

    @Override
    public ContentResolver getContentResolver() {
        if (mContentResolver == null) {
            InjectedServices services = ContactsApplication.getInjectedServices();
            if (services != null) {
                mContentResolver = services.getContentResolver();
            }
            if (mContentResolver == null) {
                mContentResolver = super.getContentResolver();
            }
        }
        return mContentResolver;
    }

    @Override
    public SharedPreferences getSharedPreferences(String name, int mode) {
        InjectedServices services = ContactsApplication.getInjectedServices();
        if (services != null) {
            SharedPreferences prefs = services.getSharedPreferences();
            if (prefs != null) {
                return prefs;
            }
        }

        return super.getSharedPreferences(name, mode);
    }

    @Override
    public Object getSystemService(String name) {
        Object service = super.getSystemService(name);
        if (service != null) {
            return service;
        }

        return getApplicationContext().getSystemService(name);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ContactSaveService.registerListener(this);
        super.onCreate(savedInstanceState);
    }

    @Override
    protected void onDestroy() {
        ContactSaveService.unregisterListener(this);
        super.onDestroy();
    }

    @Override
    public void onServiceCompleted(Intent callbackIntent) {

        String ACTION_JOIN_CONTACTS = "joinContacts";
        String action = callbackIntent.getAction();

        if (ACTION_JOIN_CONTACTS.equals(action)) {
            Uri uri = callbackIntent.getData();
            Intent intent = new Intent(Intent.ACTION_EDIT, uri);
            intent.setClass(this, ContactEditorActivity.class);
            intent.putExtra(
                    ContactEditorActivity.INTENT_KEY_NOT_VIEW_DETAIL_ON_SAVE_COMPLETED, true);
            // Don't finish the detail activity after launching the editor because when the
            // editor is done, we will still want to show the updated contact details using
            // this activity.
            startActivity(intent);
            finish();
        } else {
            onNewIntent(callbackIntent);
        }
    }

    /**
     * Convenient version of {@link FragmentManager#findFragmentById(int)}, which throws
     * an exception if the fragment doesn't exist.
     */
    @SuppressWarnings("unchecked")
    public <T extends Fragment> T getFragment(int id) {
        T result = (T)getSupportFragmentManager().findFragmentById(id);
        if (result == null) {
            throw new IllegalArgumentException("fragment 0x" + Integer.toHexString(id)
                    + " doesn't exist");
        }
        return result;
    }

    /**
     * Convenient version of {@link #findViewById(int)}, which throws
     * an exception if the view doesn't exist.
     */
    @SuppressWarnings("unchecked")
    public <T extends View> T getView(int id) {
        T result = (T)findViewById(id);
        if (result == null) {
            throw new IllegalArgumentException("view 0x" + Integer.toHexString(id)
                    + " doesn't exist");
        }
        return result;
    }

    protected static void showFragment(FragmentTransaction ft, Fragment f) {
        if ((f != null) && f.isHidden()) ft.show(f);
    }

    protected static void hideFragment(FragmentTransaction ft, Fragment f) {
        if ((f != null) && !f.isHidden()) ft.hide(f);
    }
}
