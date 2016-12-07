package com.yunos.alicontacts.platform;

import android.provider.CallLog.Calls;
import android.provider.ContactsContract.Intents;
import android.telecom.VideoProfile;

/**
 * This class holds some platform dependent constants.
 * For different platforms, e.g. platform 22 and platform 23,
 * the source of the constants might be different.
 * So we need to assign the constants from different source in different platforms.
 */
public final class PDConstants {

    /** Call is currently in an audio-only mode with no video transmission or receipt. */
    public static final int VIDEO_PROFILE_STATE_AUDIO_ONLY
            = VideoProfile.VideoState.AUDIO_ONLY;

    /** Video signal is bi-directional. */
    public static final int VIDEO_PROFILE_STATE_BIDIRECTIONAL
            = VideoProfile.VideoState.BIDIRECTIONAL;

    public static final String INTENTS_INSERT_EXTRA_ACCOUNT = Intents.Insert.ACCOUNT;

    public static final String INTENTS_INSERT_EXTRA_DATA_SET = Intents.Insert.DATA_SET;

    // In api level 22, we do not have column photo_uri in calls table from ContactsProvider.
    // We use photo_id in query projection instead.
    public static final String CALLS_TABLE_COLUMN_PHOTO_URI = Calls.CACHED_PHOTO_ID;

    private PDConstants() {}

}
