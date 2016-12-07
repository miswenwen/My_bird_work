
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
public class Text2ButtonItem extends Item {

    /**
     * The item's text.
     */
    public String mText;
    public String mSubText;
    public BaseContactEntry mContactEntry;
    public ButtonClickListener mListener;


    /**
     * @hide
     */
    public Text2ButtonItem() {
    }


    public Text2ButtonItem(BaseContactEntry entry, ButtonClickListener l) {
        this.mContactEntry = entry;
        mListener = l;
    }

    @Override
    public ItemView newView(Context context, ViewGroup parent) {
        //return createCellFromXml(context, R.layout.hw_multi_text2_button_item_view, parent);
        Text2ButtonView v = new Text2ButtonView(context, parent);
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

