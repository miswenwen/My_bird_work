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
 * limitations under the License
 */

package com.yunos.alicontacts.aliutil.provider;

import android.content.res.Resources;
import android.provider.ContactsContract;
import android.text.TextUtils;

import com.yunos.alicontacts.R;

/**
 * <p>
 * This is a modification and extension to android.provider.ContactsContract.
 * </p>
 * <p>
 * We add WANGWANG to Im protocol, but the android.provider.ContactsContract.CommonDataKinds.Im is final.
 * So we write a new Im inner class here and copy most of the original Im.
 * </p>
 * <p>
 * We also add a mimetype for sns data. So we add CommonDataKinds.Sns here.
 * </p>
 */
public final class AliContactsContract {

    /**
     * Container for definitions of common data types stored in the {@link ContactsContract.Data}
     * table.
     */
    public static final class CommonDataKinds {
        /**
         * This utility class cannot be instantiated
         */
        private CommonDataKinds() {}

        /**
         * Columns common across the specific types.
         */
        protected interface CommonColumns extends ContactsContract.CommonDataKinds.BaseTypes {
            /**
             * The data for the contact method.
             * <P>Type: TEXT</P>
             */
            public static final String DATA = ContactsContract.Data.DATA1;

            /**
             * The type of data, for example Home or Work.
             * <P>Type: INTEGER</P>
             */
            public static final String TYPE = ContactsContract.Data.DATA2;

            /**
             * The user defined label for the the contact method.
             * <P>Type: TEXT</P>
             */
            public static final String LABEL = ContactsContract.Data.DATA3;
        }

        /**
         * <p>
         * A data kind representing an SocialNetwork type
         * </p>
         * <p>
         * You can use all columns defined for {@link ContactsContract.Data} as
         * well as the following aliases.
         * </p>
         * <h2>Column aliases</h2>
         * <table class="jd-sumtable">
         * <tr>
         * <th>Type</th>
         * <th>Alias</th><th colspan='2'>Data column</th>
         * </tr>
         * <tr>
         * <td>String</td>
         * <td>{@link #DATA}</td>
         * <td>{@link #DATA1}</td>
         * <td></td>
         * </tr>
         * <tr>
         * <td>int</td>
         * <td>{@link #TYPE}</td>
         * <td>{@link #DATA2}</td>
         * <td>Allowed values are:
         * <p>
         * <ul>
         * <li>{@link #TYPE_CUSTOM}. Put the actual type in {@link #LABEL}.</li>
         * <li>{@link #TYPE_HOME}</li>
         * <li>{@link #TYPE_WORK}</li>
         * <li>{@link #TYPE_OTHER}</li>
         * </ul>
         * </p>
         * </td>
         * </tr>
         * <tr>
         * <td>String</td>
         * <td>{@link #LABEL}</td>
         * <td>{@link #DATA3}</td>
         * <td></td>
         * </tr>
         * <tr>
         * <td>String</td>
         * <td>{@link #COMMUNITY}</td>
         * <td>{@link #DATA5}</td>
         * <td>
         * <p>
         * Allowed values:
         * <ul>
         * <li>{@link #COMMUNITY_CUSTOM}. Also provide the actual protocol name
         * as {@link #CUSTOM_COMMUNITY}.</li>
         * <li>{@link #COMMUNITY_RENREN}</li>
         * <li>{@link #COMMUNITY_KAIXIN}</li>
         * <li>{@link #COMMUNITY_MICROBLOGGING_SINA}</li>
         * </ul>
         * </p>
         * </td>
         * </tr>
         * <tr>
         * <td>String</td>
         * <td>{@link #CUSTOM_COMMUNITY}</td>
         * <td>{@link #DATA6}</td>
         * <td></td>
         * </tr>
         * </table>
         */
        public static final class Sns implements CommonColumns {
            /**
             * This utility class cannot be instantiated
             */
            private Sns() {}

            /** MIME type used when storing this in data table. */
            public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/sns";

            public static final int TYPE_HOME = 1;
            public static final int TYPE_WORK = 2;
            public static final int TYPE_OTHER = 3;

            /**
             * This column should be populated with one of the defined
             * constants, e.g. {@link #COMMUNITY_RENREN}. If the value of this
             * column is {@link #COMMUNITY_CUSTOM}, the {@link #CUSTOM_COMMUNITY}
             * should contain the name of the custom protocol.
             */
            public static final String COMMUNITY = ContactsContract.Data.DATA5;

            public static final String CUSTOM_COMMUNITY = ContactsContract.Data.DATA6;

            /**
             * The SNS UID, for example uid of weibo user. Use data7 field for
             * uid.
             *
             * @author yingchun.zyc
             * @date 2014-06-13
             *       <P>
             *       Type: TEXT
             *       </P>
             */
            public static final String UID = ContactsContract.Data.DATA7;

            /*
             * The predefined SNS types.
             */
            public static final int COMMUNITY_CUSTOM = -1;
            public static final int COMMUNITY_RENREN = 0;
            public static final int COMMUNITY_KAIXIN = 1;
            public static final int COMMUNITY_MICROBLOG_SINA = 2;

            /**
             * Return the string resource that best describes the given
             * {@link #TYPE}. Will always return a valid resource.
             */
            public static final int getTypeLabelResource(int type) {
                switch (type) {
                    case TYPE_HOME: return R.string.snsTypeHome;
                    case TYPE_WORK: return R.string.snsTypeWork;
                    case TYPE_OTHER: return R.string.snsTypeOther;
                    default: return R.string.snsTypeCustom;
                }
            }

            /**
             * Return a {@link CharSequence} that best describes the given type,
             * possibly substituting the given {@link #LABEL} value
             * for {@link #TYPE_CUSTOM}.
             */
            public static final CharSequence getTypeLabel(Resources res, int type,
                    CharSequence label) {
                if (type == TYPE_CUSTOM && !TextUtils.isEmpty(label)) {
                    return label;
                } else {
                    final int labelRes = getTypeLabelResource(type);
                    return res.getText(labelRes);
                }
            }

            /**
             * Return the string resource that best describes the given
             * {@link #PROTOCOL}. Will always return a valid resource.
             */
            public static final int getProtocolLabelResource(int type) {
                switch (type) {
                    case COMMUNITY_RENREN: return R.string.snsCommunityRenRen;
                    case COMMUNITY_KAIXIN: return R.string.snsCommunityKaixin;
                    case COMMUNITY_MICROBLOG_SINA: return R.string.snsCommunitySinaWeibo;
                    default: return R.string.snsCommunityCustom;
                }
            }

            /**
             * Return a {@link CharSequence} that best describes the given
             * protocol, possibly substituting the given
             * {@link #CUSTOM_PROTOCOL} value for {@link #PROTOCOL_CUSTOM}.
             */
            public static final CharSequence getProtocolLabel(Resources res, int type,
                    CharSequence label) {
                if (type == COMMUNITY_CUSTOM && !TextUtils.isEmpty(label)) {
                    return label;
                } else {
                    final int labelRes = getProtocolLabelResource(type);
                    return res.getText(labelRes);
                }
            }
        }

        /**
         * <p>
         * A data kind representing an IM address
         * </p>
         * <p>
         * You can use all columns defined for {@link ContactsContract.Data} as
         * well as the following aliases.
         * </p>
         * <h2>Column aliases</h2>
         * <table class="jd-sumtable">
         * <tr>
         * <th>Type</th>
         * <th>Alias</th><th colspan='2'>Data column</th>
         * </tr>
         * <tr>
         * <td>String</td>
         * <td>{@link #DATA}</td>
         * <td>{@link #DATA1}</td>
         * <td></td>
         * </tr>
         * <tr>
         * <td>int</td>
         * <td>{@link #TYPE}</td>
         * <td>{@link #DATA2}</td>
         * <td>Allowed values are:
         * <p>
         * <ul>
         * <li>{@link #TYPE_CUSTOM}. Put the actual type in {@link #LABEL}.</li>
         * <li>{@link #TYPE_HOME}</li>
         * <li>{@link #TYPE_WORK}</li>
         * <li>{@link #TYPE_OTHER}</li>
         * </ul>
         * </p>
         * </td>
         * </tr>
         * <tr>
         * <td>String</td>
         * <td>{@link #LABEL}</td>
         * <td>{@link #DATA3}</td>
         * <td></td>
         * </tr>
         * <tr>
         * <td>String</td>
         * <td>{@link #PROTOCOL}</td>
         * <td>{@link #DATA5}</td>
         * <td>
         * <p>
         * Allowed values:
         * <ul>
         * <li>{@link #PROTOCOL_CUSTOM}. Also provide the actual protocol name
         * as {@link #CUSTOM_PROTOCOL}.</li>
         * <li>{@link #PROTOCOL_AIM}</li>
         * <li>{@link #PROTOCOL_MSN}</li>
         * <li>{@link #PROTOCOL_YAHOO}</li>
         * <li>{@link #PROTOCOL_SKYPE}</li>
         * <li>{@link #PROTOCOL_QQ}</li>
         * <li>{@link #PROTOCOL_GOOGLE_TALK}</li>
         * <li>{@link #PROTOCOL_ICQ}</li>
         * <li>{@link #PROTOCOL_JABBER}</li>
         * <li>{@link #PROTOCOL_NETMEETING}</li>
         * </ul>
         * </p>
         * </td>
         * </tr>
         * <tr>
         * <td>String</td>
         * <td>{@link #CUSTOM_PROTOCOL}</td>
         * <td>{@link #DATA6}</td>
         * <td></td>
         * </tr>
         * </table>
         */
        public static final class Im implements CommonColumns {
            /**
             * This utility class cannot be instantiated
             */
            private Im() {}

            /** MIME type used when storing this in data table. */
            public static final String CONTENT_ITEM_TYPE = ContactsContract.CommonDataKinds.Im.CONTENT_ITEM_TYPE;

            public static final int TYPE_HOME = ContactsContract.CommonDataKinds.Im.TYPE_HOME;
            public static final int TYPE_WORK = ContactsContract.CommonDataKinds.Im.TYPE_WORK;
            public static final int TYPE_OTHER = ContactsContract.CommonDataKinds.Im.TYPE_OTHER;

            /**
             * This column should be populated with one of the defined
             * constants, e.g. {@link #PROTOCOL_YAHOO}. If the value of this
             * column is {@link #PROTOCOL_CUSTOM}, the {@link #CUSTOM_PROTOCOL}
             * should contain the name of the custom protocol.
             */
            public static final String PROTOCOL = ContactsContract.CommonDataKinds.Im.PROTOCOL;

            public static final String CUSTOM_PROTOCOL = ContactsContract.CommonDataKinds.Im.CUSTOM_PROTOCOL;

            /*
             * The predefined IM protocol types.
             */
            public static final int PROTOCOL_CUSTOM = ContactsContract.CommonDataKinds.Im.PROTOCOL_CUSTOM;
            public static final int PROTOCOL_AIM = ContactsContract.CommonDataKinds.Im.PROTOCOL_AIM;
            public static final int PROTOCOL_MSN = ContactsContract.CommonDataKinds.Im.PROTOCOL_MSN;
            public static final int PROTOCOL_YAHOO = ContactsContract.CommonDataKinds.Im.PROTOCOL_YAHOO;
            public static final int PROTOCOL_SKYPE = ContactsContract.CommonDataKinds.Im.PROTOCOL_SKYPE;
            public static final int PROTOCOL_QQ = ContactsContract.CommonDataKinds.Im.PROTOCOL_QQ;
            public static final int PROTOCOL_GOOGLE_TALK = ContactsContract.CommonDataKinds.Im.PROTOCOL_GOOGLE_TALK;
            public static final int PROTOCOL_ICQ = ContactsContract.CommonDataKinds.Im.PROTOCOL_ICQ;
            public static final int PROTOCOL_JABBER = ContactsContract.CommonDataKinds.Im.PROTOCOL_JABBER;
            public static final int PROTOCOL_NETMEETING = ContactsContract.CommonDataKinds.Im.PROTOCOL_NETMEETING;
            public static final int PROTOCOL_WANGWANG = 9;

            /**
             * Return the string resource that best describes the given
             * {@link #TYPE}. Will always return a valid resource.
             */
            public static final int getTypeLabelResource(int type) {
                switch (type) {
                    case TYPE_HOME: return R.string.imTypeHome;
                    case TYPE_WORK: return R.string.imTypeWork;
                    case TYPE_OTHER: return R.string.imTypeOther;
                    default: return R.string.imTypeCustom;
                }
            }

            /**
             * Return a {@link CharSequence} that best describes the given type,
             * possibly substituting the given {@link #LABEL} value
             * for {@link #TYPE_CUSTOM}.
             */
            public static final CharSequence getTypeLabel(Resources res, int type,
                    CharSequence label) {
                if (type == TYPE_CUSTOM && !TextUtils.isEmpty(label)) {
                    return label;
                } else {
                    final int labelRes = getTypeLabelResource(type);
                    return res.getText(labelRes);
                }
            }

            /**
             * Return the string resource that best describes the given
             * {@link #PROTOCOL}. Will always return a valid resource.
             */
            public static final int getProtocolLabelResource(int type) {
                switch (type) {
                    case PROTOCOL_WANGWANG: return R.string.imProtocolAliWangwang;
                    case PROTOCOL_AIM: return R.string.imProtocolAim;
                    case PROTOCOL_MSN: return R.string.imProtocolMsn;
                    case PROTOCOL_YAHOO: return R.string.imProtocolYahoo;
                    case PROTOCOL_SKYPE: return R.string.imProtocolSkype;
                    case PROTOCOL_QQ: return R.string.imProtocolQq;
                    case PROTOCOL_GOOGLE_TALK: return R.string.imProtocolGoogleTalk;
                    case PROTOCOL_ICQ: return R.string.imProtocolIcq;
                    case PROTOCOL_JABBER: return R.string.imProtocolJabber;
                    case PROTOCOL_NETMEETING: return R.string.imProtocolNetMeeting;
                    default: return R.string.imProtocolCustom;
                }
            }

            /**
             * Return a {@link CharSequence} that best describes the given
             * protocol, possibly substituting the given
             * {@link #CUSTOM_PROTOCOL} value for {@link #PROTOCOL_CUSTOM}.
             */
            public static final CharSequence getProtocolLabel(Resources res, int type,
                    CharSequence label) {
                if (type == PROTOCOL_CUSTOM && !TextUtils.isEmpty(label)) {
                    return label;
                } else {
                    final int labelRes = getProtocolLabelResource(type);
                    return res.getText(labelRes);
                }
            }
        }

    }

}
