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
package com.yunos.alicontacts.vcard;

import android.accounts.Account;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;

import com.yunos.alicontacts.aliutil.alivcard.VCardSourceDetector;

/**
 * Class representing one request for importing vCard (given as a Uri).
 *
 * Mainly used when {@link ImportVCardActivity} requests {@link VCardService}
 * to import some specific Uri.
 *
 * Note: This object's accepting only One Uri does NOT mean that
 * there's only one vCard entry inside the instance, as one Uri often has multiple
 * vCard entries inside it.
 */
public class ImportRequest implements Parcelable {
    /**
     * Can be null (typically when there's no Account available in the system).
     */
    public final Account account;

    /**
     * Uri to be imported. May have different content than originally given from users, so
     * when displaying user-friendly information (e.g. "importing xxx.vcf"), use
     * {@link #displayName} instead.
     *
     * If this is null {@link #data} contains the byte stream of the vcard.
     */
    public final Uri uri;

    /**
     * Holds the byte stream of the vcard, if {@link #uri} is null.
     */
    public final byte[] data;

    /**
     * String to be displayed to the user to indicate the source of the VCARD.
     */
    public final String displayName;

    /**
     * Can be {@link VCardSourceDetector#PARSE_TYPE_UNKNOWN}.
     */
    public final int estimatedVCardType;

    /**
     * Can be null, meaning no preferable charset is available.
     */
    public final String estimatedCharset;

    /**
     * Assumes that one Uri contains only one version, while there's a (tiny) possibility
     * we may have two types in one vCard.
     *
     * e.g.
     * BEGIN:VCARD
     * VERSION:2.1
     * ...
     * END:VCARD
     * BEGIN:VCARD
     * VERSION:3.0
     * ...
     * END:VCARD
     *
     * We've never seen this kind of a file, but we may have to cope with it in the future.
     */
    public final int vcardVersion;

    /**
     * The count of vCard entries in {@link #uri}. A receiver of this object can use it
     * when showing the progress of import. Thus a receiver must be able to torelate this
     * variable being invalid because of vCard's limitation.
     *
     * vCard does not let us know this count without looking over a whole file content,
     * which means we have to open and scan over {@link #uri} to know this value, while
     * it may not be opened more than once (Uri does not require it to be opened multiple times
     * and may become invalid after its close() request).
     */
    public final int entryCount;

    /** Judge if it comes from QR Code URI source. */
    public final boolean isQRCodeUri;

    public ImportRequest(Account account,
            byte[] data, Uri uri, String displayName, int estimatedType, String estimatedCharset,
            int vcardVersion, int entryCount) {
        this(account, data, uri, displayName, estimatedType, estimatedCharset, vcardVersion,
                entryCount, false);
    }

    public ImportRequest(Account account,
            byte[] data, Uri uri, String displayName, int estimatedType, String estimatedCharset,
            int vcardVersion, int entryCount, final boolean isQRCodeUri) {
        this.account = account;
        this.data = data;
        this.uri = uri;
        this.displayName = displayName;
        this.estimatedVCardType = estimatedType;
        this.estimatedCharset = estimatedCharset;
        this.vcardVersion = vcardVersion;
        this.entryCount = entryCount;

        this.isQRCodeUri = isQRCodeUri;
    }

    public static final Parcelable.Creator<ImportRequest> CREATOR =
            new Parcelable.Creator<ImportRequest>() {

                @Override
                public ImportRequest createFromParcel(Parcel source) {
                    return new ImportRequest(source);
                }

                @Override
                public ImportRequest[] newArray(int size) {
                    return new ImportRequest[size];
                }

    };
    @Override
    public int describeContents() {
        return 0;
    }

    public ImportRequest(Parcel in) {
        ClassLoader loader = getClass().getClassLoader();
        account = in.readParcelable(loader);
        uri = in.readParcelable(null);
        data = in.createByteArray();
        displayName = in.readString();
        estimatedVCardType = in.readInt();
        estimatedCharset = in.readString();
        vcardVersion = in.readInt();
        entryCount = in.readInt();
        isQRCodeUri = in.readInt() != 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeParcelable(account, flags);
        dest.writeParcelable(uri, flags);
        dest.writeByteArray(data);
        dest.writeString(displayName);
        dest.writeInt(estimatedVCardType);
        dest.writeString(estimatedCharset);
        dest.writeInt(vcardVersion);
        dest.writeInt(entryCount);
        dest.writeInt(isQRCodeUri ? 1 : 0);
    }
}
