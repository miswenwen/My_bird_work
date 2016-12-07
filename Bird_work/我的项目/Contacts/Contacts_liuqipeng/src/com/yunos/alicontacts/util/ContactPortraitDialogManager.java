
package com.yunos.alicontacts.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.yunos.alicontacts.R;

import hwdroid.dialog.AlertDialog;
import hwdroid.dialog.DialogInterface.OnClickListener;

public class ContactPortraitDialogManager {

    //private Context mContext;
    private LayoutInflater mLayoutInflater;
    //private OnClickListener mOtherListener;
    private OnItemClickListener mAliListener;
    private AlertDialog mDialog;

    private static final int[] aliPortraitId = new int[] {
            R.drawable.aui_ic_contacts_default, R.drawable.aui_ic_contacts_student_1,
            R.drawable.aui_ic_contacts_student_2, R.drawable.aui_ic_contacts_handsome,
            R.drawable.aui_ic_contacts_beauty, R.drawable.aui_ic_contacts_workmate,
            R.drawable.aui_ic_contacts_woman, R.drawable.aui_ic_contacts_business2,
            R.drawable.aui_ic_contacts_business1, R.drawable.aui_ic_contacts_family_1,
            R.drawable.aui_ic_contacts_family_2, R.drawable.aui_ic_contacts_cat,
            R.drawable.aui_ic_contacts_dog
    };

    public ContactPortraitDialogManager(Context context, OnItemClickListener aliListener,
            OnClickListener otherListener) {

        //mContext = context;
        mLayoutInflater = LayoutInflater.from(context);
        mAliListener = aliListener;
        //mOtherListener = otherListener;

        View scrollView = createHorizonScrollView();

        CharSequence[] options = new CharSequence[] {
                context.getString(R.string.take_photo), context.getString(R.string.pick_photo)
        };

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setView(scrollView);
        builder.setItems(options, otherListener);
        builder.setNegativeButton(R.string.no, null);
        // builder.setNegativeButton(R.string.negative, mOtherListener);
        mDialog = builder.create();
        // mDialog.setSlideDirection(DropDownDialog.SLIDE_UP);
    }

    private View createHorizonScrollView() {
        View mScrollView = mLayoutInflater.inflate(R.layout.portrait_chooser_ali_view, null);
        LinearLayout layout = (LinearLayout) mScrollView.findViewById(R.id.portrait_layout_id);
        for (int i = 0; i < aliPortraitId.length; i++) {
            View v = getItemView(i);
            v.setTag(i);
            v.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View v) {
                    int position = (Integer) v.getTag();
                    if (mAliListener != null) {
                        mAliListener.onItemClick(null, null, position, 0);
                    }
                }
            });
            layout.addView(v);
        }
        return mScrollView;
    }

    public void show() {
        if (mDialog != null) {
            mDialog.show();
        }
    }

    public void dismiss() {
        if (mDialog != null) {
            mDialog.dismiss();
        }
    }

    public int getAliPortraitId(int position) {
        int portraitId = 0;
        if (position >= 0 && position < aliPortraitId.length) {
            portraitId = aliPortraitId[position];
        }
        return portraitId;
    }

    private View getItemView(int position) {
        View itemView;
        itemView = mLayoutInflater.inflate(R.layout.portrait_chooser_ali_item, null);
        ImageView imageview = (ImageView) itemView.findViewById(R.id.item);

        //TODO
        //load portrait in background

        //image resource default is circle
        //Bitmap headbitmap = BitmapFactory.decodeResource(mContext.getResources(),
        //        aliPortraitId[position]);
        //Bitmap avatarMask = BitmapUtil.circleMaskBitmap(headbitmap);
        //imageview.setImageBitmap(avatarMask);
        imageview.setImageResource(aliPortraitId[position]);
        return itemView;
    }
}
