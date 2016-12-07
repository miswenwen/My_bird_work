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
 * limitations under the License
 */

package com.yunos.alicontacts.editor;

import android.content.Context;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.ListPopupWindow;

import com.yunos.alicontacts.R;

import java.util.ArrayList;

/**
 * Shows a popup asking the user what to do for a photo. The result is passed back to the Listener
 */
public class PhotoActionPopup {
    public static final String TAG = "PhotoActionPopup";

    public static ListPopupWindow createPopupMenu(Context context, View anchorView,
            final Listener listener, int mode) {
        final ArrayList<ChoiceListItem> choices = new ArrayList<ChoiceListItem>(4);
        final int takePhotoResId = R.string.take_photo;
        final String takePhotoString = context.getString(takePhotoResId);
        final int pickPhotoResId = R.string.pick_photo;
        final String pickPhotoString = context.getString(pickPhotoResId);
        choices.add(new ChoiceListItem(ChoiceListItem.ID_TAKE_PHOTO, takePhotoString));
        choices.add(new ChoiceListItem(ChoiceListItem.ID_PICK_PHOTO, pickPhotoString));

        final ListAdapter adapter = new ArrayAdapter<ChoiceListItem>(context,
                R.layout.select_dialog_item, choices);

        final ListPopupWindow listPopupWindow = new ListPopupWindow(context);
        final OnItemClickListener clickListener = new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                final ChoiceListItem choice = choices.get(position);
                switch (choice.getId()) {
                    /*case ChoiceListItem.ID_USE_AS_PRIMARY:
                        listener.onUseAsPrimaryChosen();
                        break;
                    case ChoiceListItem.ID_REMOVE:
                        listener.onRemovePictureChosen();
                        break;*/
                    case ChoiceListItem.ID_TAKE_PHOTO:
                        listener.onTakePhotoChosen();
                        break;
                    case ChoiceListItem.ID_PICK_PHOTO:
                        listener.onPickFromGalleryChosen();
                        break;
                }

                listPopupWindow.dismiss();
            }
        };

        listPopupWindow.setAnchorView(anchorView);
        listPopupWindow.setAdapter(adapter);
        listPopupWindow.setOnItemClickListener(clickListener);
        listPopupWindow.setModal(true);
        listPopupWindow.setInputMethodMode(ListPopupWindow.INPUT_METHOD_NOT_NEEDED);
        final int minWidth = context.getResources().getDimensionPixelSize(
                R.dimen.photo_action_popup_min_width);
        if (anchorView.getWidth() < minWidth) {
            listPopupWindow.setWidth(minWidth);
        }
        return listPopupWindow;
    }

    private static final class ChoiceListItem {
        private final int mId;
        private final String mCaption;

        public static final int ID_USE_AS_PRIMARY = 0;
        public static final int ID_TAKE_PHOTO = 1;
        public static final int ID_PICK_PHOTO = 2;
        public static final int ID_REMOVE = 3;

        public ChoiceListItem(int id, String caption) {
            mId = id;
            mCaption = caption;
        }

        @Override
        public String toString() {
            return mCaption;
        }

        public int getId() {
            return mId;
        }
    }

    public interface Listener {
        void onUseAsPrimaryChosen();
        void onRemovePictureChosen();
        void onTakePhotoChosen();
        void onPickFromGalleryChosen();
    }
}
