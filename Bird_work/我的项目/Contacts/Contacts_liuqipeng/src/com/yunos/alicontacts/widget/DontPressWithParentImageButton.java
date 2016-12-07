package com.yunos.alicontacts.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageButton;

public class DontPressWithParentImageButton extends ImageButton {
    public DontPressWithParentImageButton(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public void setPressed(boolean pressed) {
        // If the parent is pressed, do not set to pressed.
        if (pressed && ((View) getParent()).isPressed()) {
            super.setPressed(false);
            return;
        }
        super.setPressed(pressed);
    }
}
