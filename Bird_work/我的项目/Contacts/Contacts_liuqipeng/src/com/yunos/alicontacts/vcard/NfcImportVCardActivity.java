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
 * limitations under the License
 */

package com.yunos.alicontacts.vcard;

import android.app.Activity;
import android.content.Intent;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;

import com.google.common.io.Closeables;
import com.yunos.alicontacts.R;
import com.yunos.alicontacts.aliutil.alivcard.VCardEntryCounter;
import com.yunos.alicontacts.aliutil.alivcard.VCardParser;
import com.yunos.alicontacts.aliutil.alivcard.VCardParser_V21;
import com.yunos.alicontacts.aliutil.alivcard.VCardParser_V30;
import com.yunos.alicontacts.aliutil.alivcard.VCardSourceDetector;
import com.yunos.alicontacts.aliutil.alivcard.exception.VCardException;
import com.yunos.alicontacts.aliutil.alivcard.exception.VCardNestedException;
import com.yunos.alicontacts.aliutil.alivcard.exception.VCardVersionException;
import com.yunos.alicontacts.model.AccountTypeManager;
import com.yunos.alicontacts.model.account.AccountWithDataSet;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class NfcImportVCardActivity extends Activity {
    private static final String TAG = "NfcImportVCardActivity";

    private static final int SELECT_ACCOUNT = 1;

    private NdefRecord mRecord;
    private AccountWithDataSet mAccount;

    /* package */ class ImportTask extends AsyncTask<Void, Void, Void> {
        @Override
        public Void doInBackground(Void... args) {
            ImportRequest request = createImportRequest();
            if (request == null) {
                return null;
            }

            ArrayList<ImportRequest> requests = new ArrayList<ImportRequest>();
            requests.add(request);
            // send import intent to VCardIntentService
            Intent intent = new Intent(NfcImportVCardActivity.this, VCardIntentService.class);
            intent.setAction(VCardIntentService.ACTION_IMPORT_VCARD);
            intent.putExtra(VCardIntentService.EXTRAS_IMPORT_REQUESTS_LIST, requests);
            startService(intent);
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            finish();
        }

    }

    /* package */ ImportRequest createImportRequest() {
        VCardParser parser;
        VCardEntryCounter counter = null;
        VCardSourceDetector detector = null;
        int vcardVersion = ImportVCardActivity.VCARD_VERSION_V21;
        try {
            ByteArrayInputStream is = new ByteArrayInputStream(mRecord.getPayload());
            is.mark(0);
            parser = new VCardParser_V21();
            try {
                counter = new VCardEntryCounter();
                detector = new VCardSourceDetector();
                parser.addInterpreter(counter);
                parser.addInterpreter(detector);
                parser.parse(is);
            } catch (VCardVersionException e1) {
                is.reset();
                vcardVersion = ImportVCardActivity.VCARD_VERSION_V30;
                parser = new VCardParser_V30();
                try {
                    counter = new VCardEntryCounter();
                    detector = new VCardSourceDetector();
                    parser.addInterpreter(counter);
                    parser.addInterpreter(detector);
                    parser.parse(is);
                } catch (VCardVersionException e2) {
                    return null;
                }
            } finally {
//                try {
//                    if (is != null) is.close();
//                } catch (IOException e) {
//                }
                Closeables.closeQuietly(is);
            }
        } catch (IOException e) {
            Log.e(TAG, "Failed reading vcard data", e);
            return null;
        } catch (VCardNestedException e) {
            Log.w(TAG, "Nested Exception is found (it may be false-positive).");
            // Go through without throwing the Exception, as we may be able to detect the
            // version before it
        } catch (VCardException e) {
            Log.e(TAG, "Error parsing vcard", e);
            return null;
        }

        return new ImportRequest(mAccount, mRecord.getPayload(), null,
                getString(R.string.nfc_vcard_file_name), detector.getEstimatedType(),
                detector.getEstimatedCharset(), vcardVersion, counter.getCount());
    }

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);

        Intent intent = getIntent();
        if (!NfcAdapter.ACTION_NDEF_DISCOVERED.equals(intent.getAction())) {
            Log.w(TAG, "Unknowon intent " + intent);
            finish();
            return;
        }

        String type = intent.getType();
        if (type == null ||
                (!"text/x-vcard".equals(type) && !"text/vcard".equals(type))) {
            Log.w(TAG, "Not a vcard");
            //setStatus(getString(R.string.fail_reason_not_supported));
            finish();
            return;
        }
        NdefMessage msg = (NdefMessage) intent.getParcelableArrayExtra(
                NfcAdapter.EXTRA_NDEF_MESSAGES)[0];
        mRecord = msg.getRecords()[0];

        final AccountTypeManager accountTypes = AccountTypeManager.getInstance(this);
        final List<AccountWithDataSet> accountList = accountTypes.getAccounts(true);
        if (accountList.isEmpty()) {
            mAccount = null;
        } else if (accountList.size() == 1) {
            mAccount = accountList.get(0);
        } else {
            startActivityForResult(new Intent(this, SelectAccountActivity.class), SELECT_ACCOUNT);
            return;
        }

        startImport();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if (requestCode == SELECT_ACCOUNT) {
            if (resultCode == RESULT_OK) {
                mAccount = new AccountWithDataSet(
                        intent.getStringExtra(SelectAccountActivity.ACCOUNT_NAME),
                        intent.getStringExtra(SelectAccountActivity.ACCOUNT_TYPE),
                        intent.getStringExtra(SelectAccountActivity.DATA_SET));
                startImport();
            } else {
                finish();
            }
        }
    }

    private void startImport() {
        new ImportTask().execute();
    }
}
