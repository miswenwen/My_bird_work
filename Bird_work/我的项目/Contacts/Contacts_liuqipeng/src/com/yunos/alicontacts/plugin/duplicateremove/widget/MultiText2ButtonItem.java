
package com.yunos.alicontacts.plugin.duplicateremove.widget;

import hwdroid.widget.item.Item;
import hwdroid.widget.itemview.ItemView;
import java.util.List;
import android.content.Context;
import android.view.ViewGroup;

import com.yunos.alicontacts.plugin.duplicateremove.BaseContactEntry;

/**
 * A MutliTextButtonItem is a item that contains several rows of Text
 * and one button on the right of first text. The text and subtitle
 * will be displayed on two line on screen.
 *
 */
public class MultiText2ButtonItem extends Item {

    /**
     * The item's text.
     */
    public String mText;
    public String mSubText;
    public List<? extends BaseContactEntry> mContactEntries;
    public ButtonClickListener mListener;
    public boolean mLastOne;

    /**
     * @hide
     */
    public MultiText2ButtonItem() {
    }


    public MultiText2ButtonItem(List<? extends BaseContactEntry> entries, ButtonClickListener l, boolean isLastOne) {
        this.mContactEntries = entries;
        mListener = l;
        mLastOne = isLastOne;
    }

    @Override
    public boolean isEnabled() {
        return !mLastOne;
    }

    @Override
    public ItemView newView(Context context, ViewGroup parent) {
        //return createCellFromXml(context, R.layout.hw_multi_text2_button_item_view, parent);
        MultiText2ButtonView v = new MultiText2ButtonView(context, parent);
        return (ItemView) v;
    }


   /* @Override
    public void inflate(Resources r, XmlPullParser parser, AttributeSet attrs) throws XmlPullParserException, IOException {
        super.inflate(r, parser, attrs);

        TypedArray a = r.obtainAttributes(attrs, R.styleable.TextItem);
        mText = a.getString(R.styleable.TextItem_text);
        a.recycle();
    }*/

}
