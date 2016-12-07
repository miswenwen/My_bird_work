/*
 * Copyright (C) 2012 The Android Open Source Project
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

package com.yunos.alicontacts.dialpad;

import android.content.Context;
import android.graphics.Rect;
import android.media.AudioManager;
import android.provider.Settings;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.accessibility.AccessibilityManager;
import android.widget.ImageButton;

/*[BIRD_DOOV_DIAPAD_SOUND_SINGLE] add by lichengfeng 20160601 begin*/
import com.bird.contacts.BirdFeatureOption;
/*[BIRD_DOOV_DIAPAD_SOUND_SINGLE] add by lichengfeng 20160601 end*/

/**
 * Custom {@link ImageButton} for dialpad buttons.
 * <p>
 * This class implements lift-to-type interaction when touch exploration is
 * enabled.
 */
public class DialpadImageButton extends ImageButton implements View.OnClickListener{
    /** Accessibility manager instance used to check touch exploration state. */
    private AccessibilityManager mAccessibilityManager;

    /** Bounds used to filter HOVER_EXIT events. */
    private Rect mHoverBounds = new Rect();

    public interface OnPressedListener {
        public void onPressed(View view, boolean pressed);
    }

    private OnPressedListener mOnPressedListener;

    public void setOnPressedListener(OnPressedListener onPressedListener) {
        mOnPressedListener = onPressedListener;
    }

    public DialpadImageButton(Context context) {
        super(context);
        initForAccessibility(context);
        setOnClickListener(this);
        setSoundEffectsEnabled(false);
    }

    public DialpadImageButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        initForAccessibility(context);
        setOnClickListener(this);
        setSoundEffectsEnabled(false);
    }

    public DialpadImageButton(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initForAccessibility(context);
        setOnClickListener(this);
        setSoundEffectsEnabled(false);
    }

    private void initForAccessibility(Context context) {
        mAccessibilityManager = (AccessibilityManager) context.getSystemService(
                Context.ACCESSIBILITY_SERVICE);
    }

    @Override
    public void onClick(View v) {
        if (null != mOnPressedListener) {
            mOnPressedListener.onPressed(v, true);
        }
    }

    @Override
    public void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        mHoverBounds.left = getPaddingLeft();
        mHoverBounds.right = w - getPaddingRight();
        mHoverBounds.top = getPaddingTop();
        mHoverBounds.bottom = h - getPaddingBottom();
    }

    @Override
    public boolean performClick() {
        // When accessibility is on, simulate press and release to preserve the
        // semantic meaning of performClick(). Required for Braille support.
        // FIXME: Comment out here temporary. - QiaoJunqi.
        // We do NOT have settings about Accessibility now,
        // but the test tool might set this to true for preparing environment.
        // When it is true, the digital dialed will not be put to digital edit box.
        // Because the setPressed no longer calls mOnPressedListener.onPressed.
        /*if (mAccessibilityManager.isEnabled()) {
            // Checking the press state prevents double activation.
            if (!isPressed()) {
                setPressed(true);
                setPressed(false);
            }

            return true;
        }*/
        /* Play default sound for view when press this button.
         * In View.performClick() sound is started play before call OnClickListener.onclick(),
         * so call setSoundEffectsEnabled() base on system settings to control sound effect here.  */
        boolean dtmfToneEnabled = Settings.System.getInt(this.getContext().getContentResolver(),
                Settings.System.DTMF_TONE_WHEN_DIALING, 1) == 1;
//      if (dtmfToneEnabled) { //[BIRD_DOOV_SOUND] removed by lichengfeng 20160601 doov sound
		/*[BIRD_DOOV_SOUND] add by lichengfeng 20160601 begin*/
        if (dtmfToneEnabled && !BirdFeatureOption.BIRD_DOOV_SOUND) {
		/*[BIRD_DOOV_SOUND] add by lichengfeng 20160601 end*/
            AudioManager am = (AudioManager) getContext().getSystemService(Context.AUDIO_SERVICE);
            // IMPORTANT:
            // The AudioManager has playSoundEffect(int effectType, int userId)
            // and playSoundEffect(int effectType, float volume).
            // The cast to float can NOT be omitted, or -1 will be used as user ID,
            // and the app will crash for running as user -1.
            am.playSoundEffect(BirdFeatureOption.BIRD_DOOV_DIAPAD_SOUND_SINGLE ? AudioManager.FX_KEYPRESS_DOOV_DIAPAD_SINGLE : AudioManager.FX_KEY_CLICK, (float)-1);
        }
        return super.performClick();
    }

    @Override
    public boolean onHoverEvent(MotionEvent event) {
        // When touch exploration is turned on, lifting a finger while inside
        // the button's hover target bounds should perform a click action.
        if (mAccessibilityManager.isEnabled()
                && mAccessibilityManager.isTouchExplorationEnabled()) {
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_HOVER_ENTER:
                    // Lift-to-type temporarily disables double-tap activation.
                    setClickable(false);
                    break;
                case MotionEvent.ACTION_HOVER_EXIT:
                    if (mHoverBounds.contains((int) event.getX(), (int) event.getY())) {
                        performClick();
                    }
                    setClickable(true);
                    break;
            }
        }

        return super.onHoverEvent(event);
    }
}
