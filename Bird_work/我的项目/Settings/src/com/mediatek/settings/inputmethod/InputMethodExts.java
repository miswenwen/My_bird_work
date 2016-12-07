package com.mediatek.settings.inputmethod;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.media.AudioManager;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.util.Log;

import com.android.settings.R;
import com.mediatek.settings.FeatureOption;

import java.util.List;

public class InputMethodExts {

    private static final String TAG = "InputMethodAndLanguageSettings";
    private static final String KEY_VOICE_UI_ENTRY = "voice_ui";

    private Context mContext;
    private boolean mIsOnlyImeSettings;
    private PreferenceCategory mVoiceCategory;
    private Preference mVoiceUiPref;
    private Intent mVoiceControlIntent;

    private static final String MTK_VOW_SUPPORT_State = "MTK_VOW_SUPPORT";
    private static final String MTK_VOW_SUPPORT_on = "MTK_VOW_SUPPORT=true";

    public InputMethodExts(Context context, boolean isOnlyImeSettings,
            PreferenceCategory voiceCategory, PreferenceCategory pointCategory) {
        mContext = context;
        mIsOnlyImeSettings = isOnlyImeSettings;
        mVoiceCategory = voiceCategory;
    }

    // init input method extends items
    public void initExtendsItems() {
        // For voice control @ {
        mVoiceUiPref = new Preference(mContext);
        mVoiceUiPref.setKey(KEY_VOICE_UI_ENTRY);
        mVoiceUiPref.setTitle(mContext.getString(R.string.voice_ui_title));
        if (mVoiceCategory != null) {
            mVoiceCategory.addPreference(mVoiceUiPref);
        }
        if (mIsOnlyImeSettings
                || (!FeatureOption.MTK_VOICE_UI_SUPPORT && !isWakeupSupport(mContext))) {
            Log.d("@M_" + TAG, "going to remove voice ui feature ");
            if (mVoiceUiPref != null && mVoiceCategory != null) {
                Log.d("@M_" + TAG, "removed done");
                mVoiceCategory.removePreference(mVoiceUiPref);
            }
        }
        // @ }
    }

    // on resume input method extends items
    public void resumeExtendsItems() {
        // { @ ALPS00823791
        mVoiceControlIntent = new Intent("com.mediatek.voicecommand.VOICE_CONTROL_SETTINGS");
        mVoiceControlIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        List<ResolveInfo> apps = mContext.getPackageManager().queryIntentActivities(
                mVoiceControlIntent, 0);
        if (apps == null || apps.size() == 0) {
            Log.d("@M_" + TAG, "going to remove voice ui feature ");
            if (mVoiceUiPref != null && mVoiceCategory != null) {
                Log.d("@M_" + TAG, "removed done");
                mVoiceCategory.removePreference(mVoiceUiPref);
            }
        } else {
            if (!mIsOnlyImeSettings && FeatureOption.MTK_VOICE_UI_SUPPORT) {
                Log.d("@M_" + TAG, "going to add voice ui feature ");
                if (mVoiceUiPref != null && mVoiceCategory != null) {
                    mVoiceCategory.addPreference(mVoiceUiPref);
                }
            }
        }
        // @ }
    }

    /*
     * on resume input method extends items
     *
     * @param preferKey: clicled preference's key
     */
    public void onClickExtendsItems(String preferKey) {
        if (KEY_VOICE_UI_ENTRY.equals(preferKey)) {
            mContext.startActivity(mVoiceControlIntent);
        }
    }

    /**
     * Check if support voice wakeup feature.
     *
     * @param context
     *            context
     * @return true if support, otherwise false
     */
    public static boolean isWakeupSupport(Context context) {
        AudioManager am = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        if (am == null) {
            Log.e("@M_" + TAG, "isWakeupSupport get audio service is null");
            return false;
        }
        String state = am.getParameters(MTK_VOW_SUPPORT_State);
        if (state != null) {
            return state.equalsIgnoreCase(MTK_VOW_SUPPORT_on);
        }
        return false;
    }
}
