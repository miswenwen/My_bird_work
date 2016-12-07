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

import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;

public class ExportRequest implements Parcelable {
    public final Uri destUri;
    /**
     * Can be null.
     */
    public final String exportType;

    public ExportRequest(Uri destUri) {
        this(destUri, null);
    }

    public ExportRequest(Uri destUri, String exportType) {
        this.destUri = destUri;
        this.exportType = exportType;
    }

    public ExportRequest(Parcel in) {
        this.destUri = in.readParcelable(null);
        this.exportType = in.readString();
    }

    public static final Parcelable.Creator<ExportRequest> CREATOR =
            new Parcelable.Creator<ExportRequest>() {

                @Override
                public ExportRequest createFromParcel(Parcel source) {
                    return new ExportRequest(source);
                }

                @Override
                public ExportRequest[] newArray(int size) {
                    return new ExportRequest[size];
                }

    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeParcelable(destUri, flags);
        dest.writeString(exportType);
    }
}
