/*
 * Copyright (C) 2011 The Android Open Source Project
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

package com.yunos.alicontacts.util;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Handler;
import android.telephony.PhoneNumberFormattingTextWatcher;
import android.text.Editable;
import android.widget.TextView;

import com.yunos.alicontacts.ContactsUtils;

public final class PhoneNumberFormatter {
    private PhoneNumberFormatter() {}

    /**
     * Load {@link TextWatcherLoadAsyncTask} in a worker thread and set it to a {@link TextView}.
     */
    private static class TextWatcherLoadAsyncTask extends
            AsyncTask<Void, Void, PhoneNumberFormattingTextWatcher> {
        private final String mCountryCode;
        private final TextView mTextView;
        private final Handler mHandler;

        public TextWatcherLoadAsyncTask(String countryCode, TextView textView) {
            mCountryCode = countryCode;
            mTextView = textView;
            mHandler = null;
        }

        public TextWatcherLoadAsyncTask(String countryCode, TextView textView, Handler handler) {
            mCountryCode = countryCode;
            mTextView = textView;
            mHandler = handler;
        }

        @SuppressLint("NewApi")
        @Override
        protected PhoneNumberFormattingTextWatcher doInBackground(Void... params) {
            // using the specified country code will avoid different format
            // rules on Phone number input EditText.
            return new PhoneNumberFormattingTextWatcher(mCountryCode);
        }

        @Override
        protected void onPostExecute(PhoneNumberFormattingTextWatcher watcher) {
            if (watcher == null || isCancelled()) {
                return; // May happen if we cancel the task.
            }
            // Setting a text changed listener is safe even after the view is detached.
            mTextView.addTextChangedListener(watcher);

            // Note changes the user made before onPostExecute() will not be formatted, but
            // once they type the next letter we format the entire text, so it's not a big deal.
            // (And loading PhoneNumberFormattingTextWatcher is usually fast enough.)
            // We could use watcher.afterTextChanged(mTextView.getEditableText()) to force format
            // the existing content here, but that could cause unwanted results.
            // (e.g. the contact editor thinks the user changed the content, and would save
            // when closed even when the user didn't make other changes.)
        }
    }

    /*
     * Delay-set {@link PhoneNumberFormattingTextWatcher} to a {@link TextView}.
     */
    public static final void setPhoneNumberFormattingTextWatcher(Context context,
            TextView textView) {
        new TextWatcherLoadAsyncTask(ContactsUtils.getCurrentCountryIso(context), textView)
                .executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, (Void[]) null);
    }

    public static final void setPhoneNumberFormattingTextWatcher(Context context,
            TextView textView, Handler handler) {
        new TextWatcherLoadAsyncTask(ContactsUtils.getCurrentCountryIso(context), textView, handler)
                .executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, (Void[]) null);
    }

//    public static class PhoneNumberFormattingTextWatcherEx extends PhoneNumberFormattingTextWatcher {
//        protected static boolean mSelfChanged = false;
//
//        protected PhoneNumberFormattingTextWatcherEx() {}
//
//        PhoneNumberFormattingTextWatcherEx(String countryCode){
//            super();
//        }
//
//        public void afterTextChanged(Editable s) {
//            mSelfChanged = true;
//            super.afterTextChanged(s);
//            mSelfChanged = false;
//        }
//    }
}
