
package com.yunos.alicontacts.plugin.duplicateremove.widget;

import android.content.Context;
import android.content.res.Resources;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.yunos.alicontacts.R;
import com.yunos.alicontacts.plugin.duplicateremove.BaseContactEntry;

import hwdroid.widget.item.Item;
import hwdroid.widget.itemview.ItemView;

/**
 * View representation of the {@link SubtitleItem}.
 *
 */
public class Text2ButtonView extends LinearLayout implements ItemView {

    private static final String TAG = Text2ButtonView.class.getSimpleName();

    private static int COLOR_COUNT = 0;
    private Context mContext;
    private Text2ButtonItem mItem;
    private ViewGroup mViewParent;

    //private TextView mTextView;
    //private TextView mSubTextView;
    //private LinearLayout mRightWidgetFrame;


    //private CheckBox mCheckBox;
    //private RadioButton mRadioButton;

    public Text2ButtonView(Context context) {
        this(context, null, null);
    }

    public Text2ButtonView(Context context, ViewGroup parent) {
        this(context, null, parent);
    }

    public Text2ButtonView(Context context, AttributeSet attrs, ViewGroup parent) {
        super(context, attrs);
        mContext = context;
        mViewParent = parent;

        this.setOrientation(VERTICAL);
    }

    @Override
    public void prepareItemView() {
        // remove all child views
        this.removeAllViews();
    }

    @Override
    public void setObject(Item object) {
        mItem = (Text2ButtonItem) object;
        this.removeAllViews();

        BaseContactEntry entry = mItem.mContactEntry;
        LayoutInflater inflater = LayoutInflater.from(mContext);

        if(entry != null) {
            // new a row
            LinearLayout entryView = (LinearLayout)inflater.inflate(R.layout.hw_text_2_button_item_view, mViewParent);
            TextView textView = (TextView)(entryView.findViewById(R.id.hw_text));
            //TextView subTextView = (TextView)(entryView.findViewById(R.id.hw_subtext));

            textView.setText(entry.mDisplayName);
            Resources res = mContext.getResources();
            int resColorGray = res.getColor(R.color.contact_primary_text_color_black);
            textView.setTextColor(resColorGray);
            //subTextView.setText(entry.mPrimaryPhoneNumber);

            Button bt = (Button)(entryView.findViewById(R.id.bt_list_operation));
            bt.setVisibility(View.VISIBLE);
            bt.setText(R.string.bt_edit_incomplet_contact);
            // click listener
            bt.setOnClickListener(new OnClickListener(){

                @Override
                public void onClick(View arg0) {
                    // TODO Auto-generated method stub
                    if(mItem.mListener != null) {
                        mItem.mListener.onClick(mItem);
                    }
                }
            });

            addView(entryView);
        }

    }

    /**
     * get color indication information for current group item view
     * @return 0 for read name text or 1 for read phone number text
     */
    private static int getTextColor() {
         if(COLOR_COUNT == Integer.MAX_VALUE ) {
             COLOR_COUNT = 0;
         } else {
             COLOR_COUNT++;
         }

         return  COLOR_COUNT%2;
     }

    @Override
    public void setSubTextSingleLine(boolean enabled) {
        // TODO Auto-generated method stub

    }


}
