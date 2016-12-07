package com.yunos.alicontacts.editor;

import android.content.Context;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;

public class RingtoneUtils {

    public static String getDefaultRingtoneTitle(Context context) {
        Uri uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
        return getRingtoneTitle(context, uri);
    }

    public static String getRingtoneTitle(Context context, Uri uri) {
        String name = null;//context.getString(R.string.contact_edit_ringtone_type);
        Ringtone ringtone = RingtoneManager.getRingtone(context, uri);
        name = ringtone.getTitle(context);

        // Fix bugid 55118, added by Fangjun Lin.
        // If getTitle return uri.getLastPathSegment(), then use default ring tone.
        if(name.equals(uri.getLastPathSegment())) {
            name = getDefaultRingtoneTitle(context);
        }

        return name;
    }

}
