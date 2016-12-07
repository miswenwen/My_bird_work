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

package com.yunos.alicontacts.util;

import android.app.Service; //~~aliyun add start
//import com.android.internal.telephony.ISms;
//import android.os.ServiceManager;

/**
 * Background {@link Service} that is used to keep our process alive long enough
 * for background threads to finish. Started and stopped directly by specific
 * background tasks when needed.
 */ //~~aliyun add end
public class Constants {
    public static final String MIME_TYPE_VIDEO_CHAT = "vnd.android.cursor.item/video-chat-address";

    public static final String LOOKUP_URI_PREFIX = "content://com.android.contacts/contacts/lookup/";

    public static final String ACTION_INSTALL_SHORTCUT = "com.android.launcher.action.INSTALL_SHORTCUT";

    public static final String SCHEME_TEL = "tel";
    public static final String SCHEME_SMSTO = "smsto";
    public static final String SCHEME_MAILTO = "mailto";
    public static final String SCHEME_IMTO = "imto";
    public static final String SCHEME_SIP = "sip";

    /**
     * Log tag for performance measurement.
     * To enable: adb shell setprop log.tag.ContactsPerf VERBOSE
     */
    public static final String PERFORMANCE_TAG = "ContactsPerf";

    public static final String EXTRA_PHONE_URIS = "com.android.contacts.extra.PHONE_URIS";

    public static final String PHONE_LOOKUP_QUERY_PARAM_IN_VISIBLE_CONTACTS = "yunos_ext_in_visible_contacts";

    /**
     * When the system is first start, we might get Unknown URL exception on writing ContactsProvider,
     * We want to wait some time and have some retries.
     */
    public static final int RETRY_COUNT_FOR_WAIT_DB_READY = 5;

}
