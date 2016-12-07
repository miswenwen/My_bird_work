/*
 * Copyright (C) 2009 The Android Open Source Project
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

package com.yunos.alicontacts.model.account;

import android.app.Activity;
import android.app.LoaderManager;
import android.app.LoaderManager.LoaderCallbacks;
import android.content.AsyncTaskLoader;
import android.content.Context;
import android.content.Loader;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;

import com.yunos.alicontacts.ContactsUtils;
import com.yunos.alicontacts.R;
import com.yunos.alicontacts.list.CustomContactListFilterActivity;
import com.yunos.alicontacts.list.CustomContactListFilterActivity.AccountDisplay;
import com.yunos.alicontacts.list.CustomContactListFilterActivity.AccountSet;
import com.yunos.alicontacts.model.AccountTypeManager;
import com.yunos.alicontacts.sim.SimContactUtils;
import com.yunos.alicontacts.sim.SimUtil;

import java.util.List;

public class AccountTypeLoaderManager implements LoaderCallbacks<CustomContactListFilterActivity.AccountSet> {

    private static final int ACCOUNT_SET_LOADER_ID = 1;
    public interface LoadCallBack {
        void onLoadFinished(AccountSet data);
    }
    private LoadCallBack mLoadCallBack;
    private Context mContext;
    private AccountSet mAccountSetData;

    public AccountTypeLoaderManager(Context context) {
        mContext = context;
    }

    @Override
    public Loader<AccountSet> onCreateLoader(int id, Bundle args) {
        return new CustomFilterConfigurationLoader(mContext);
    }

    @Override
    public void onLoadFinished(Loader<AccountSet> loader, AccountSet data) {
        mAccountSetData = data;
        if (mLoadCallBack != null) {
            mLoadCallBack.onLoadFinished(mAccountSetData);
        }
    }

    @Override
    public void onLoaderReset(Loader<AccountSet> loader) {
        mAccountSetData = null;
    }

    public void loadAccountSet(Activity activity, LoadCallBack callBack) {
        mLoadCallBack = callBack;
        LoaderManager lm = activity.getLoaderManager();
        lm.initLoader(ACCOUNT_SET_LOADER_ID, null, this);
    }

    public void reloadAccountSet(Activity activity) {
        LoaderManager lm = activity.getLoaderManager();
        lm.restartLoader(ACCOUNT_SET_LOADER_ID, null, this);
    }

    public static class CustomFilterConfigurationLoader extends AsyncTaskLoader<AccountSet> {

        private static final String TAG = "CustomFilterConfigurationLoader";

        public CustomFilterConfigurationLoader(Context context) {
            super(context);
        }

        @Override
        public AccountSet loadInBackground() {
            final Context context = getContext();
            final AccountTypeManager accountTypes = AccountTypeManager.getInstance(context);
            final List<AccountWithDataSet> listAccountsWithDataSet = accountTypes.getAccounts(false);

            final AccountSet workingAccountSet = new AccountSet();

            /** 1. add local account. */
            AccountDisplay accountDisplay;
            if (!ContactsUtils.getLoginState(getContext())) {
                accountDisplay = new AccountDisplay(context, AccountType.LOCAL_ACCOUNT_NAME,
                        AccountType.LOCAL_ACCOUNT_TYPE, null, R.string.contact_account_local,
                        R.drawable.ic_group_normal);
                workingAccountSet.add(context, accountDisplay);
                Log.i(TAG, "loadInBackground: add local account "+accountDisplay);
            }

            /** 2. add YunOS account. */
            for (AccountWithDataSet account : listAccountsWithDataSet) {
                final AccountType accountType = accountTypes.getAccountTypeForAccount(account);
                if (accountType == null) {
                    continue;
                }

                if (AccountType.YUNOS_ACCOUNT_TYPE.equals(accountType.accountType)) {
                    accountDisplay = new AccountDisplay(context, account.name, account.type, account.dataSet,
                            R.string.contact_account_local, R.drawable.ic_group_normal);
                    // NOTE: In some cases, the test tool can make the YunOS account login state wrong.
                    // So we might have already added phone local account in step 1 above.
                    // As we need only one entry for (phone local account + YunOS account),
                    // so delete the phone local account in the account set before add YunOS account.
                    if (workingAccountSet.size() == 1) {
                        workingAccountSet.clear();
                    }
                    workingAccountSet.add(context, accountDisplay);
                    Log.i(TAG, "loadInBackground: add yun account "+accountDisplay);
                    break;
                }
            }

            /** 3. add SIM account. */
            if (!SimUtil.isAirplaneModeOn(context)) {
                if (SimUtil.MULTISIM_ENABLE) {
                    if (SimUtil.hasIccCard(SimUtil.SLOT_ID_1) && SimUtil.isSimAvailable(SimUtil.SLOT_ID_1)) {
                        accountDisplay = new AccountDisplay(context, SimContactUtils.SIM_ACCOUNT_NAME_SIM1,
                                SimContactUtils.SIM_ACCOUNT_TYPE, null, R.string.contact_account_sim1,
                                R.drawable.ic_list_card1_normal);
                        workingAccountSet.add(context, accountDisplay);
                        Log.i(TAG, "loadInBackground: add sim1 account "+accountDisplay);
                    }

                    if (SimUtil.hasIccCard(SimUtil.SLOT_ID_2) && SimUtil.isSimAvailable(SimUtil.SLOT_ID_2)) {
                        accountDisplay = new AccountDisplay(context, SimContactUtils.SIM_ACCOUNT_NAME_SIM2,
                                SimContactUtils.SIM_ACCOUNT_TYPE, null, R.string.contact_account_sim2,
                                R.drawable.ic_list_card2_normal);
                        workingAccountSet.add(context, accountDisplay);
                        Log.i(TAG, "loadInBackground: add sim2 account "+accountDisplay);
                    }

                } else if (SimUtil.hasIccCard() && SimUtil.isSimAvailable()) {
                    accountDisplay = new AccountDisplay(context, SimContactUtils.SIM_ACCOUNT_NAME,
                            SimContactUtils.SIM_ACCOUNT_TYPE, null, R.string.contact_account_sim,
                            R.drawable.ic_list_card_normal);
                    workingAccountSet.add(context, accountDisplay);
                    Log.i(TAG, "loadInBackground: add sim account "+accountDisplay);
                }
            }

            /** 4. add external account. such as WeChat, QQ, etc */
            for (AccountWithDataSet account : listAccountsWithDataSet) {
                final AccountType accountType = accountTypes.getAccountTypeForAccount(account);
                if (accountType.isExtension() && !account.hasData(context)) {
                    // Extension with no data -- skip.
                    continue;
                }

                // Skip YunOS account.
                if (AccountType.YUNOS_ACCOUNT_TYPE.equals(accountType.accountType)) {
                    continue;
                }

                CharSequence displayLabel = accountType.getDisplayLabel(context);
                String title = context.getString(R.string.contact_account_format, displayLabel);
                Drawable icon = accountType.getDisplayIcon(context);
                accountDisplay = new AccountDisplay(context, account.name, account.type, account.dataSet, title, icon);
                workingAccountSet.add(context, accountDisplay);
                Log.i(TAG, "loadInBackground: add other account "+accountDisplay);
            }

            return workingAccountSet;
        }

        @Override
        public void deliverResult(AccountSet data) {
            if (isReset()) {
                Log.i(TAG, "deliverResult: loader reset.");
                return;
            }

            if (isStarted()) {
                Log.i(TAG, "deliverResult: loader started. data="+data);
                super.deliverResult(data);
            } else {
                Log.w(TAG, "deliverResult: loader NOT started. skip deliver result.");
            }
        }

        @Override
        protected void onStartLoading() {
            Log.i(TAG, "onStartLoading:");
            forceLoad();
        }

        @Override
        protected void onStopLoading() {
            Log.i(TAG, "onStopLoading:");
            cancelLoad();
        }

        @Override
        protected void onReset() {
            Log.i(TAG, "onReset:");
            super.onReset();
            onStopLoading();
        }
    }

}
