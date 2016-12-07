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
 * limitations under the License.
 */

package com.android.settings.deviceinfo;

import android.content.Context;
/*bird add by liuzhenting 20160815 begin*/
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.RectShape;
import com.android.settings.R;
/*bird add by liuzhenting 20160815 end*/
import android.preference.Preference;

public class StorageItemPreference extends Preference {
    public int userHandle;
    /*bird add by liuzhenting 20160815 begin*/
    public final int color;
    public StorageItemPreference(Context context,  int colorRes) {
        super(context);

        if (colorRes != 0) {
            this.color = context.getResources().getColor(colorRes);

            final Resources res = context.getResources();
            final int width = res.getDimensionPixelSize(R.dimen.device_memory_usage_button_width);
            final int height = res.getDimensionPixelSize(R.dimen.device_memory_usage_button_height);
            setIcon(createRectShape(width, height, this.color));
        } else {
            this.color = Color.MAGENTA;
        }

    }

    private static ShapeDrawable createRectShape(int width, int height, int color) {
        ShapeDrawable shape = new ShapeDrawable(new RectShape());
        shape.setIntrinsicHeight(height);
        shape.setIntrinsicWidth(width);
        shape.getPaint().setColor(color);
        return shape;
    }
    /*bird add by liuzhenting 20160815 end*/
}
