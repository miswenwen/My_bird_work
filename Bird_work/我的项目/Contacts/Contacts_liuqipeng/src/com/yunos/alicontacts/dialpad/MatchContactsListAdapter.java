
package com.yunos.alicontacts.dialpad;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.ContactsContract.RawContacts;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.util.Pair;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ImageView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.yunos.alicontacts.CallDetailActivity;
import com.yunos.alicontacts.CallUtil;
import com.yunos.alicontacts.ContactsUtils;
import com.yunos.alicontacts.R;
import com.yunos.alicontacts.activities.ContactDetailActivity;
import com.yunos.alicontacts.dialpad.DialpadView.OnSpeedDialListener;
import com.yunos.alicontacts.dialpad.calllog.AliCallLogExtensionHelper;
import com.yunos.alicontacts.dialpad.calllog.CallLogAdapter.DialListener;
import com.yunos.alicontacts.dialpad.calllog.CallLogAdapter.ShowLongClickActionMenuListener;
import com.yunos.alicontacts.dialpad.smartsearch.ContactInMemory;
import com.yunos.alicontacts.dialpad.smartsearch.MatchResult;
import com.yunos.alicontacts.dialpad.smartsearch.PersistWorker;
import com.yunos.alicontacts.dialpad.smartsearch.PinyinSearch;
import com.yunos.alicontacts.dialpad.smartsearch.SearchResult;
import com.yunos.alicontacts.quickcall.QuickCallPickerActivity;
import com.yunos.alicontacts.quickcall.QuickCallSetting;
import com.yunos.alicontacts.sim.SimUtil;
import com.yunos.alicontacts.util.ClipboardUtils;
import com.yunos.alicontacts.util.YunOSFeatureHelper;
import com.yunos.common.DebugLog;
import com.yunos.common.UsageReporter;
import com.yunos.yundroid.widget.itemview.SeparatorItemView;

import hwdroid.widget.ActionSheet;

import java.util.ArrayList;
import java.util.List;

public class MatchContactsListAdapter extends SimpleAdapter implements OnClickListener {
    public static final String TAG = "MatchContactsListAdapter";

    // handle click for speed dial phone number
    private QuickCallSetting mQuickCallSetting;
    private OnSpeedDialListener mSpeedDialListener;

    private DialListener mDialListener;
    private ShowLongClickActionMenuListener mShowLongClickActionMenuListener;
    private SearchResult mSearchResult = SearchResult.EMPTY_RESULT;
    private ActionSheet mLongClickPopupDialog;
    private LayoutInflater mInflater;
    private Context mContext;
    private PinyinSearch mPinyinSearch;
    private String inputString;

    private static final int LIST_TYPE_COUNT = 5;
    private static final int LIST_ITEM_TYPE_SEPARATOR = 0;
    private static final int LIST_ITEM_TYPE_CONTACTS = 1;
    private static final int LIST_ITEM_TYPE_CALLOG = 2;
    private static final int LIST_ITEM_TYPE_YELLOWPAGE = 3;
    private static final int LIST_ITEM_TYPE_QUICK_CALL = 4;

    private CallLogDeletedListener mCallLogDeletedListener;
    public interface CallLogDeletedListener {
        void onCallLogDeleteFinish(final String phoneNumber);
    }

    public void setOnCallLogDeletedListener(CallLogDeletedListener listener) {
        mCallLogDeletedListener = listener;
    }

    public static class ViewHolder {
        public int type = MatchResult.TYPE_SEPARATOR;
        public String phoneNumber;

        public TextView name;
        public TextView labelAndNumber;
        public ImageView detailIcon;
    }

    public static class QuickCallSearchWrapper {
        public final String phoneNumber;
        public final int keyCode;

        public QuickCallSearchWrapper(String phoneNumber, int keyCode) {
            this.phoneNumber = phoneNumber;
            this.keyCode = keyCode;
        }
    }

    public static class ContactSearchKeyWrapper {
        public final String key;

        public ContactSearchKeyWrapper(String key) {
            this.key = key;
        }

        @Override
        public String toString() {
            return key;
        }
    }

    /**
     * add by tianyuan, 20150203
     * set speed listener to handle speed number click
     */
    public void setOnSpeedDialListener(OnSpeedDialListener l) {
        mSpeedDialListener = l;
    }

    public void setShowLongClickActionMenuListener(ShowLongClickActionMenuListener listener) {
        mShowLongClickActionMenuListener = listener;
    }

    public OnItemClickListener itemClickListener = new OnItemClickListener() {

        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            Log.d(TAG, "OnItemClickListener.onItemClick: clicked at position "+position);
            Object obj = null;
            Object viewTag = view.getTag();
            if (viewTag instanceof MatchContactsListAdapter.ViewHolder) {
                obj = ((MatchContactsListAdapter.ViewHolder)viewTag).detailIcon.getTag();
            }

            if (obj instanceof ContactSearchKeyWrapper) {
                // for name search result, the tag is a string like {raw_contact_id}T{phone_number}.
                String key = ((ContactSearchKeyWrapper)obj).key;
                String phoneNum = PersistWorker.getPhoneNumberFromKey(key);
                mDialListener.callNumberDirectly(phoneNum);
                UsageReporter.onClick(null, DialpadFragment.CLASSNAME, UsageReporter.DialpadPage.DP_MO_FROM_SEARCH);
            } else if (obj instanceof Intent) {
                Intent intent = (Intent)obj;
                String phoneNum = intent.getStringExtra(CallDetailActivity.EXTRA_MATCH_PHONE_NUMBER);
                if (phoneNum != null) {
                    mDialListener.callNumberDirectly(phoneNum);
                }
                UsageReporter.onClick(null, DialpadFragment.CLASSNAME, UsageReporter.DialpadPage.DP_MO_FROM_SEARCH);
            } else if (obj instanceof MatchResult) {
                String phoneNum = ((MatchResult)obj).phoneNumber;
                if (phoneNum != null) {
                    mDialListener.callNumberDirectly(phoneNum);
                }
                UsageReporter.onClick(null, DialpadFragment.CLASSNAME, UsageReporter.DialpadPage.DP_MO_FROM_SEARCH);
            } else if (obj instanceof QuickCallSearchWrapper) {
                QuickCallSearchWrapper wrapper = ((QuickCallSearchWrapper) obj);
                String phoneNumber = wrapper.phoneNumber;
                int keyCode = wrapper.keyCode;
                if (TextUtils.isEmpty(phoneNumber)) {
                    Intent intent = new Intent(mContext, QuickCallPickerActivity.class);
                    intent.putExtra(QuickCallSetting.EXTRAPOS, keyCode);
                    //#<!-- [[ YunOS BEGIN PB
                    //##module:()  ##author:xiuneng.wpf@alibaba-inc.com
                    //##BugID:(7936997)  ##date:2016-04-11 19:23 -->
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    //#<!-- YunOS END PB ]] -->
                    ((Activity)mContext).startActivityForResult(intent,
                            CallLogFragment.QUICK_CALL_ACTIVITY_REQUEST_CODE);
                } else {
                    if (mQuickCallSetting == null) {
                        mQuickCallSetting = QuickCallSetting.getQuickCallInstance(mContext);
                    }
                    int sim = mQuickCallSetting.getDefaultQuickDialSim(keyCode);
                    if (mSpeedDialListener != null) {
                        mSpeedDialListener.onSpeedDial(phoneNumber, sim);
                    }
                    UsageReporter.onClick(null, DialpadFragment.CLASSNAME, UsageReporter.DialpadPage.DP_MO_FROM_SEARCH);
                    //mDialListener.onListViewItemClicked(phoneNumber);
                }
            }
            return;
        }
    };

    public OnItemLongClickListener mItemLongClickListener = new OnItemLongClickListener() {

        @Override
        public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
            if (view.getTag() instanceof ViewHolder) {
                ViewHolder holder = (ViewHolder) view.getTag();
                if (holder.type == MatchResult.TYPE_QUICK_CALL
                        || holder.type == MatchResult.TYPE_SEPARATOR) {
                    return false;
                }
                String name = holder.name.getText().toString();
                String number = holder.phoneNumber;

                if (mShowLongClickActionMenuListener != null) {
                    mShowLongClickActionMenuListener.onShowMenu();
                }
                showLongClickDialog(view, name, number, holder.type == MatchResult.TYPE_CALLOG);
                return true;
            }
            return false;
        }
    };

    private void handleBlackList(String longClickNumber) {
        if (AliCallLogExtensionHelper.PLATFORM_YUNOS) {
            boolean result = false;
            if (YunOSFeatureHelper.isBlack(mContext, longClickNumber)) {
                result = YunOSFeatureHelper.removeBlack(mContext, longClickNumber);
                if (result) {
                    Toast.makeText(mContext, R.string.contact_detail_removeFromBlackListOK,
                            Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(mContext, R.string.contact_detail_removeFromBlackListFail,
                            Toast.LENGTH_SHORT).show();
                }
                UsageReporter.onClick(null, DialpadFragment.CLASSNAME, UsageReporter.DialpadPage.LONG_CLICK_SEARCH_RESULT_REMOVE_BLACK_LIST);
            } else {
                int type = 3;// type 3: block call and sms
                result = YunOSFeatureHelper.addBlack(mContext, longClickNumber, type);
                if (result) {
                    Toast.makeText(mContext, R.string.contact_detail_addToBlackListOK,
                            Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(mContext, R.string.contact_detail_addToBlackListFail,
                            Toast.LENGTH_SHORT).show();
                }
                UsageReporter.onClick(null, DialpadFragment.CLASSNAME, UsageReporter.DialpadPage.LONG_CLICK_SEARCH_RESULT_ADD_BLACK_LIST);
            }
        }
    }

    private class LongClickMenu {
        public static final int COMMON_CALL = 0;
        public static final int COPY_PHONE_NUMBER = 1;
        public static final int VIDEO_CALL = 2;
        public static final int DELETE_CALL_LOG = 4;
        public static final int EDIT_BEFORE_CALL = 5;
        public static final int HANDLE_BLACK_LIST = 6;
    };

    private void showLongClickDialog(View view, final String name, final String phoneNumber, boolean isCallLog) {

        class PopUpActionMenuListener implements ActionSheet.CommonButtonListener {
            private int[] mWhichToPositionTable;
            public PopUpActionMenuListener(int[] table) {
                mWhichToPositionTable = table;
            }

            @Override
            public void onDismiss(ActionSheet as) {
            }

            @Override
            public void onClick(int which) {
                int pos = mWhichToPositionTable[which];
                switch(pos) {
                case LongClickMenu.COMMON_CALL:
                    mDialListener.callNumberDirectly(phoneNumber);
                    UsageReporter.onClick(null, DialpadFragment.CLASSNAME, UsageReporter.DialpadPage.LONG_CLICK_SEARCH_RESULT_COMMON_CALL);
                    break;
                case LongClickMenu.VIDEO_CALL:
                    Intent videoCallIntent = CallUtil.getVideoCallIntent(mContext, phoneNumber, mContext.getClass().getCanonicalName());
                    mContext.startActivity(videoCallIntent);
                    UsageReporter.onClick(null, DialpadFragment.CLASSNAME, UsageReporter.DialpadPage.LONG_CLICK_SEARCH_RESULT_VIDEO_CALL);
                    break;
                case LongClickMenu.COPY_PHONE_NUMBER:
                    ClipboardUtils.copyText(mContext, null, phoneNumber, true);
                    UsageReporter.onClick(null, DialpadFragment.CLASSNAME, UsageReporter.DialpadPage.LONG_CLICK_SEARCH_RESULT_COPY_NUMBER);
                    break;
                case LongClickMenu.DELETE_CALL_LOG:
                    clearAllCallLog(phoneNumber);
                    UsageReporter.onClick(null, DialpadFragment.CLASSNAME, UsageReporter.DialpadPage.LONG_CLICK_SEARCH_RESULT_DELETE_CALL_LOG);
                    break;
                case LongClickMenu.EDIT_BEFORE_CALL:
                    if (mDialListener != null) {
                        mDialListener.editBeforeCall(phoneNumber);
                        UsageReporter.onClick(null, DialpadFragment.CLASSNAME, UsageReporter.DialpadPage.LONG_CLICK_SEARCH_RESULT_EDIT_BEFORE_CALL);
                    }
                    break;
                case LongClickMenu.HANDLE_BLACK_LIST:
                    handleBlackList(phoneNumber);
                    break;
                default:
                    Log.d(TAG, "Invalid button clicked!");
                }
            }
        }
        List<String> items = new ArrayList<String>(7);
        Resources res = mContext.getResources();
        List<Integer> tableItemIndex = new ArrayList<Integer>(7);

        // Common call menu.
        items.add(res.getString(R.string.call_other));
        tableItemIndex.add(LongClickMenu.COMMON_CALL);

        // video call menu
        // FIXME: in Spreadtrum, we might need to get volte attach state.
        boolean canMakeVideoCall = SimUtil.isVideoCallEnabled(mContext)
                && AliCallLogExtensionHelper.canPlaceVolteVideoCallByNumber(phoneNumber);
        if (canMakeVideoCall) {
            items.add(res.getString(R.string.make_video_call));
            tableItemIndex.add(LongClickMenu.VIDEO_CALL);
        }

        // copy number menu.
        items.add(res.getString(R.string.copy_phone_number));
        tableItemIndex.add(LongClickMenu.COPY_PHONE_NUMBER);

        //If is callLog item, add delete menu.
        if (isCallLog) {
            items.add(res.getString(R.string.menu_delete_call_log));
            tableItemIndex.add(LongClickMenu.DELETE_CALL_LOG);
        }

        // Edit before call.
        items.add(res.getString(R.string.edit_before_call));
        tableItemIndex.add(LongClickMenu.EDIT_BEFORE_CALL);

        // Black list menu.
        int stringId = -1;
        if (YunOSFeatureHelper.isBlack(mContext, phoneNumber)) {
            stringId = R.string.calllog_blacklist_remove;
        } else {
            stringId = R.string.calllog_blacklist_add;
        }
        items.add(res.getString(stringId));
        tableItemIndex.add(LongClickMenu.HANDLE_BLACK_LIST);

        int[] indexTable = new int [tableItemIndex.size()];
        for (int i = 0; i < tableItemIndex.size(); i++) {
            indexTable[i] = tableItemIndex.get(i);
        }
        mLongClickPopupDialog = new ActionSheet(mContext);
        // Per YaoWei's comments in bug http://k3.alibaba-inc.com/issue/6506061?versionId=1169325
        // we do NOT set title for action sheet any more.
        // mLongClickPopupDialog.setTitle(name);
        mLongClickPopupDialog.setCommonButtons(items, null, null,
                new PopUpActionMenuListener(indexTable));
        mLongClickPopupDialog.show(view);
    }

    private void clearAllCallLog(final String phoneNumber) {
        if (mCallLogDeletedListener != null) {
            mCallLogDeletedListener.onCallLogDeleteFinish(phoneNumber);
        }
    }

    public void clearPopupDialog() {
        if (mLongClickPopupDialog != null) {
            mLongClickPopupDialog.dismiss();
            mLongClickPopupDialog = null;
        }
    }

    public void setDialListener(DialListener listener) {
        mDialListener = listener;
    }

    public MatchContactsListAdapter(Context context) {
        super(context, null, R.layout.twelve_contacts_list_item_view, null, null);
        mContext = context;
        mPinyinSearch = PinyinSearch.getInstance(context);

        mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    public int getCount() {
        return mSearchResult.getResultCount();
    }

    public void setInputStringAndMatchResult(String inputString, SearchResult searchResult) {
        this.mSearchResult = searchResult;
        this.inputString = inputString;
    }

    @Override
    public int getViewTypeCount() {
        return LIST_TYPE_COUNT;
    }

    @Override
    public int getItemViewType(int position) {
        MatchResult mr = mSearchResult.get(position);
        int type = LIST_ITEM_TYPE_SEPARATOR;
        switch (mr.type) {
            case MatchResult.TYPE_CALLOG:
                type = LIST_ITEM_TYPE_CALLOG;
                break;
            case MatchResult.TYPE_CONTACTS:
                type = LIST_ITEM_TYPE_CONTACTS;
                break;
            case MatchResult.TYPE_YELLOWPAGE:
                type = LIST_ITEM_TYPE_YELLOWPAGE;
                break;
            case MatchResult.TYPE_QUICK_CALL:
                type = LIST_ITEM_TYPE_QUICK_CALL;
                break;
            default:
                break;
        }
        return type;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        MatchResult mr = mSearchResult.get(position);
        if (convertView == null) {
            if (mr.type == MatchResult.TYPE_SEPARATOR) {
                convertView = mInflater.inflate(R.layout.gd_separator_item_view, parent, false);
            } else if (mr.type == MatchResult.TYPE_QUICK_CALL) {
                convertView = mInflater.inflate(R.layout.dialer_search_quick_call_item, parent,
                        false);
                cacheViews(convertView);
            } else {
                convertView = mInflater.inflate(R.layout.dialer_search_item, parent, false);
                cacheViews(convertView);
            }
        }

        if (PinyinSearch.mHaveNewInput) {
            return convertView;
        }
        if (mr.type == MatchResult.TYPE_SEPARATOR) {
            return showSeparatorView(mr, convertView);
        }
        return showItemView(mr, convertView, position);
    }

    public SearchResult getSearchResult() {
        return mSearchResult;
    }

    public void updateQuickCallItem() {
        if (mSearchResult.getResultCount() >= 2) {
            // position 0 is quick call SEPARATOR,1 is quick call item;
            MatchResult mr = mSearchResult.get(1);
            if (mr != null && mr.type == MatchResult.TYPE_QUICK_CALL) {
                QuickCallSetting quickSetting = QuickCallSetting.getQuickCallInstance(mContext);
                Pair<String, String> nameAndNumber = quickSetting.getNameAndPhoneNumber(mContext,
                        mr.key.charAt(0));
                mr.name = nameAndNumber.first;
                mr.phoneNumber = nameAndNumber.second;
                notifyDataSetChanged();
            }
        }
    }

    private View showSeparatorView(MatchResult mr, View view) {
        ((SeparatorItemView) view).setText(mr.separatorNameResId);
        return view;
    }

    private View showQuickCallView(MatchResult mr, View view, int position) {
        ViewHolder viewHolder = (ViewHolder) view.getTag();
        int resId = 0;
        int keyCode = 0;
        if (mr.key.equals("1")) {
            resId = R.drawable.dial_keyborad_number_1_normal;
            keyCode = KeyEvent.KEYCODE_1;
        } else if (mr.key.equals("2")) {
            resId = R.drawable.dial_keyborad_number_2_normal;
            keyCode = KeyEvent.KEYCODE_2;
        } else if (mr.key.equals("3")) {
            resId = R.drawable.dial_keyborad_number_3_normal;
            keyCode = KeyEvent.KEYCODE_3;
        } else if (mr.key.equals("4")) {
            resId = R.drawable.dial_keyborad_number_4_normal;
            keyCode = KeyEvent.KEYCODE_4;
        } else if (mr.key.equals("5")) {
            resId = R.drawable.dial_keyborad_number_5_normal;
            keyCode = KeyEvent.KEYCODE_5;
        } else if (mr.key.equals("6")) {
            resId = R.drawable.dial_keyborad_number_6_normal;
            keyCode = KeyEvent.KEYCODE_6;
        } else if (mr.key.equals("7")) {
            resId = R.drawable.dial_keyborad_number_7_normal;
            keyCode = KeyEvent.KEYCODE_7;
        } else if (mr.key.equals("8")) {
            resId = R.drawable.dial_keyborad_number_8_normal;
            keyCode = KeyEvent.KEYCODE_8;
        } else if (mr.key.equals("9")) {
            resId = R.drawable.dial_keyborad_number_9_normal;
            keyCode = KeyEvent.KEYCODE_9;
        }

        if (TextUtils.isEmpty(mr.name) && TextUtils.isEmpty(mr.phoneNumber)) {
            viewHolder.name.setText(R.string.quick_call_add_contact);
            viewHolder.labelAndNumber.setVisibility(View.GONE);
        } else {
            if (TextUtils.isEmpty(mr.name)) {
                viewHolder.name.setText(mr.phoneNumber);
                viewHolder.labelAndNumber.setVisibility(View.GONE);
            } else {
                viewHolder.name.setText(mr.name);
                viewHolder.labelAndNumber.setText(mr.phoneNumber);
                viewHolder.labelAndNumber.setVisibility(View.VISIBLE);
            }
        }

        viewHolder.detailIcon.setImageResource(resId);
        viewHolder.detailIcon.setTag(new QuickCallSearchWrapper(mr.phoneNumber, keyCode));
        return view;
    }

    private View showItemView(MatchResult mr, View view, int position) {
        String phoneNumber = "";

        ViewHolder viewHolder = (ViewHolder) view.getTag();
        viewHolder.type = mr.type;
        viewHolder.phoneNumber = mr.phoneNumber;
        // 显示Item中的名字和号码
        TextView tvName = viewHolder.name;
        TextView tvNumber = viewHolder.labelAndNumber;
        ImageView detailButton = viewHolder.detailIcon;

        if (mr.type == MatchResult.TYPE_QUICK_CALL) {
            showQuickCallView(mr, view, position);
            return view;
        } else if (mr.type == MatchResult.TYPE_CONTACTS) {
            // Contacts found, get result from PinyinSearch
            ContactInMemory cim = mPinyinSearch.getSearchTable().get(mr.key);
            if (cim == null) {
                // We get MatchResult.TYPE_CONTACTS type, it means we match a
                // contact.
                // But the search table does not have the contact in it,
                // this means the contact is not put to the search table.
                // Only contact without name is not in the search table.
                tvName.setText("");
                phoneNumber = mr.phoneNumber;
            } else {
                tvName.setText(cim.contactName);
                if (TextUtils.isEmpty(cim.area)) {
                    phoneNumber = mr.phoneNumber;
                } else {
                    phoneNumber = new StringBuilder().append(mr.phoneNumber).append(' ')
                            .append(cim.area).toString();
                }
            }

            viewHolder.detailIcon.setTag(new ContactSearchKeyWrapper(mr.key));
            detailButton.setOnClickListener(this);
        } else if (mr.type == MatchResult.TYPE_CALLOG) {
            // No contacts ID found
            if (TextUtils.isEmpty(mr.name)) {
                // name is empty, the result must be a number match record.
                // but the call log item displays phone number in name location.
                mr.name = mr.phoneNumber;
                mr.matchedNamePart = mr.matchedNumberPart;
                mr.nameMatchStart = mr.numberMatchStart;
                mr.nameMatchLength = mr.numberMatchLength;
                mr.matchPart = MatchResult.MATCH_PART_NAME;
            } else {
                phoneNumber = mr.phoneNumber;
            }
            tvName.setText(mr.name);
            // NOTE:
            // The local variable: phoneNumber will be displayed in the second
            // line.
            // For call log item, it must be a strange number,
            // The first line (name line) is the phone number.
            // The second line is the location.
            if (!TextUtils.isEmpty(mr.mLocation)) {
                if (TextUtils.isEmpty(phoneNumber)) {
                    phoneNumber = mr.mLocation;
                } else {
                    phoneNumber = new StringBuilder().append(mr.phoneNumber).append(' ')
                            .append(mr.mLocation).toString();;
                }
            }

            Intent intent = new Intent(mContext, CallDetailActivity.class);
            intent.putExtra(CallDetailActivity.EXTRA_MATCH_PHONE_NUMBER, mr.phoneNumber);
            intent.putExtra(CallDetailActivity.EXTRA_NEED_UPDATE_CALLLOG, true);

            viewHolder.detailIcon.setTag(intent);
            detailButton.setOnClickListener(this);
        } else if (mr.type == MatchResult.TYPE_YELLOWPAGE) {
            tvName.setText(mr.name);
            phoneNumber = mr.phoneNumber;
            viewHolder.detailIcon.setTag(mr);
            detailButton.setOnClickListener(this);
        }

        // Because we might have highlight part in both name and number area.
        // so we check highlight for name and number separately, not in if-else
        // mode.
        CharSequence tvNameText = tvName.getText();
        int tvNameTextLength = tvNameText.length();
        int color = this.mContext.getResources().getColor(R.color.match_contacts_hilite_color);
        if (((mr.matchPart & MatchResult.MATCH_PART_NAME) == MatchResult.MATCH_PART_NAME)
                && (tvNameTextLength >= (mr.nameMatchStart + mr.nameMatchLength))) {
            SpannableStringBuilder spanName = new SpannableStringBuilder(tvNameText);
            spanName.setSpan(new ForegroundColorSpan(color), mr.nameMatchStart, mr.nameMatchStart
                    + mr.nameMatchLength, Spannable.SPAN_EXCLUSIVE_INCLUSIVE);
            tvName.setText(spanName);
        }
        if ((mr.matchPart & MatchResult.MATCH_PART_PHONE_NUMBER) == MatchResult.MATCH_PART_PHONE_NUMBER) {
            // in some cases, phoneNumber contains location info in the end.
            Spannable numbertoSpan = new SpannableString(phoneNumber);
            numbertoSpan.setSpan(new ForegroundColorSpan(color), mr.numberMatchStart,
                    mr.numberMatchStart + mr.numberMatchLength, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            tvNumber.setText(numbertoSpan);
        } else if ((mr.type != MatchResult.TYPE_CALLOG) && phoneNumber.contains(inputString)) {
            // currently, call log item only displays number in name widget,
            // so only consider non-calllog type to set phone number highlight.
            int start = phoneNumber.indexOf(inputString);
            Spannable numbertoSpan = new SpannableString(phoneNumber);
            numbertoSpan.setSpan(new ForegroundColorSpan(color), start,
                    start + inputString.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            tvNumber.setText(numbertoSpan);
        } else {
            tvNumber.setText(phoneNumber);
        }

        return view;
    }

    private void cacheViews(View view) {
        ViewHolder viewHolder = new ViewHolder();
        viewHolder.name = (TextView) view.findViewById(R.id.sc_name);
        viewHolder.labelAndNumber = (TextView) view.findViewById(R.id.sc_labelAndNumber);
        viewHolder.detailIcon = (ImageView) view.findViewById(R.id.sc_icon_detail);
        view.setTag(viewHolder);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.icon_detail:
            case R.id.icon_detail_container:
            case R.id.sc_icon_detail:
                Object obj = v.getTag();
                if (obj instanceof ContactSearchKeyWrapper) {
                    final String key = ((ContactSearchKeyWrapper) obj).key;
                    final String rawContactId = PersistWorker.getRawContactidFromKey(key);
                    long cid = Long.parseLong(rawContactId);

                    // According to current design, cid>0 means contacts,
                    // cid<0 means yellowPage.
                    if (cid >= 0) {
                        startContactDetailActivity(rawContactId);
                    } else {
                        startYellowPageContactDetailActivity(Math.abs(cid), 0, false);
                    }
                } else if (obj instanceof Intent) {
                    final Intent intent = (Intent) obj;
                    DebugLog.d("ZengHuan", "Intent is:" + intent.toString());
                    mContext.startActivity(intent);
                } else if (obj instanceof MatchResult) {
                    MatchResult mr = (MatchResult) obj;
                    if (mr.databaseID == 0) {
                        // the part before separator in the key is -1*databaseID
                        // for yellow page case,
                        // so we use a '-' before it is assigned to databaseID.
                        mr.databaseID = -Long.parseLong(PersistWorker
                                .getRawContactidFromKey(mr.key));
                    }
                    startYellowPageContactDetailActivity(mr.databaseID, 0, false);
                }
                break;
            default:
                break;
        }
    }

    private void startContactDetailActivity(String rawContactId) {
        new AsyncTask<String, Void, Uri>() {
            @Override
            protected Uri doInBackground(String... params) {
                final ContentResolver resolver = mContext.getContentResolver();
                final Uri rawContactUri = Uri.withAppendedPath(RawContacts.CONTENT_URI, Uri.encode(params[0]));
                final Uri contactLookupUri = RawContacts.getContactLookupUri(resolver, rawContactUri);
                return contactLookupUri;
            }

            @Override
            protected void onPostExecute(Uri result) {
                if (result != null) {
                    Intent intent = ContactsUtils.getViewContactIntent(mContext,
                            ContactDetailActivity.class, result);
                    mContext.startActivity(intent);
                } else {
                    // Cannot find the raw contact id in DB,
                    // this is probably because background synchronize job deleted the
                    // contact.
                    Toast.makeText(mContext, R.string.contact_changed_in_smart_search, Toast.LENGTH_SHORT)
                            .show();
                }
            }

        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, rawContactId);
    }

    private void startYellowPageContactDetailActivity(long ypcId, int provId, boolean isTemp) {
//        Intent intent = new Intent(mContext, YellowPageContactDetailActivity.class);
//        intent.putExtra(YellowPageContactDetailActivity.EXTRA_YP_CONTACT_ID, ypcId);
//        intent.putExtra(YellowPageContactDetailActivity.EXTRA_YP_PROVINCE_ID, provId);
//        intent.putExtra(YellowPageContactDetailActivity.EXTRA_YP_IS_TEMP, isTemp);
//        mContext.startActivity(intent);
    }

}
