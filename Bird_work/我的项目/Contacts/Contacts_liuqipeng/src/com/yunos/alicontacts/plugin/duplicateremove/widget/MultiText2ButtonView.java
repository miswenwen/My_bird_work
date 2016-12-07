
package com.yunos.alicontacts.plugin.duplicateremove.widget;

import android.content.Context;
import android.content.res.Resources;
import android.text.TextUtils;
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

import java.util.List;

/**
 * View representation of the {@link SubtitleItem}.
 *
 */
public class MultiText2ButtonView extends LinearLayout implements ItemView {

    //private static final String TAG = MultiText2ButtonView.class.getSimpleName();

    private static int COLOR_COUNT = 0;
    private Context mContext;
    private MultiText2ButtonItem mItem;
    private ViewGroup mViewParent;

    //private TextView mTextView;
    //private TextView mSubTextView;
    //private LinearLayout mRightWidgetFrame;


    //private CheckBox mCheckBox;
    //private RadioButton mRadioButton;

    public MultiText2ButtonView(Context context) {
        this(context, null, null);
    }

    public MultiText2ButtonView(Context context, ViewGroup parent) {
        this(context, null, parent);
    }

    public MultiText2ButtonView(Context context, AttributeSet attrs, ViewGroup parent) {
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
        mItem = (MultiText2ButtonItem) object;
        this.removeAllViews();

        List<? extends BaseContactEntry> entries = mItem.mContactEntries;
        int size = entries.size();
        if(size < 2) {
            return; // not a effective group
        }

        LayoutInflater inflater = LayoutInflater.from(mContext);
        // for clause for populate each entry in group
        //int colorType = getTextColor();
        Resources res = mContext.getResources();
        int resColorRed = res.getColor(R.color.color_list_item_text_red);
        int resColorBlack = res.getColor(R.color.contact_primary_text_color_black);
        boolean isLastOne = mItem.mLastOne;
        for(int i = 0; i < size; i++) {
            BaseContactEntry entry = entries.get(i);
            if(entry != null) {
                // new a row
                LinearLayout entryView = (LinearLayout) inflater.inflate(
                        R.layout.hw_text_2_button_item_view, mViewParent);
                TextView textView = (TextView) (entryView.findViewById(R.id.hw_text));
                TextView subTextView = (TextView) (entryView.findViewById(R.id.hw_subtext));

                textView.setText(entry.mDisplayName);
                subTextView.setText(entry.mPrimaryPhoneNumber);
                if (!TextUtils.isEmpty(entry.mPrimaryPhoneNumber)) {
                    subTextView.setVisibility(View.VISIBLE);
                }

                if (entry.isNamePhoneSame()) {
                    textView.setTextColor(resColorRed);
                    subTextView.setTextColor(resColorRed);
                } else if (entry.IsPhoneSame()) {
                    textView.setTextColor(resColorBlack);
                    subTextView.setTextColor(resColorRed);
                } else if (entry.IsNameSame()) {
                    textView.setTextColor(resColorRed);
                    subTextView.setTextColor(resColorBlack);
                }

                if (i == 0) {
                    Button bt = (Button) entryView.findViewById(R.id.bt_list_operation);
                    bt.setVisibility(View.VISIBLE);
                    // click listener
                    bt.setOnClickListener(new OnClickListener() {

                        @Override
                        public void onClick(View arg0) {
                            if (mItem.mListener != null) {
                                mItem.mListener.onClick(mItem);
                            }
                        }
                    });
                }
                addView(entryView);

                // add separator
                View separator = null;
                if(i < size - 1 ) {
                    // set inner separator view drawable
                    //separator.setBackgroundResource(R.drawable.dial_parting_line);
                    separator = inflater.inflate(R.layout.list_separator_view_inner, mViewParent);
                    addView(separator);
                } else if (!isLastOne){
                    // set outer separator view drawable
                    //separator.setBackgroundResource(R.drawable.dial_prompt_bg);
                    separator = inflater.inflate(R.layout.list_separator_view_outer, mViewParent);
                    addView(separator);
                }
                //addView(separator);

            }

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
