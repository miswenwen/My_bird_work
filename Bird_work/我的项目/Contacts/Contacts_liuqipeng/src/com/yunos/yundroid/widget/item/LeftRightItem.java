/*
 * Copyright (C) 2013 YunOS (http://www.yunos.com)
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
package com.yunos.yundroid.widget.item;

import android.content.Context;
import android.view.ViewGroup;

import com.yunos.alicontacts.R;
import com.yunos.yundroid.widget.itemview.ItemView;
import com.yunos.yundroid.widget.itemview.LeftRightItemView;

public class LeftRightItem extends Item {

    public String mLeftText;
    public String mRightText;
    public LeftRightItemView mView;

    /**
     * Create a new LeftRightItem with the specified text.
     *
     * @param right The text used to create this item.
     * @param on The toggle item is on or off.
     */
    public LeftRightItem(String left, String right) {
        this.mLeftText = left;
        this.mRightText = right;
    }

    public void destroy() {
        if (mView != null) {
            mView.destroy();
            mView = null;
        }
    }

    public void setRightItemText(String text) {
        mRightText = text;
        if(mView != null) {
            mView.setSubtextView(text);
        }
    }

    @Override
    public ItemView newView(Context context, ViewGroup parent) {
        ItemView view = createCellFromXml(context, R.layout.gd_left_right_item_view, parent);
        view.prepareItemView();
        mView = (LeftRightItemView) view;
        return view;
    }
}
