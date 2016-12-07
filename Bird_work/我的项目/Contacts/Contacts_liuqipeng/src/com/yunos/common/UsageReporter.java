
package com.yunos.common;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;
import android.util.Log;

import com.aliyun.ams.ta.StatConfig;
import com.aliyun.ams.ta.TA;
import com.aliyun.ams.ta.Tracker;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

public class UsageReporter {
    private static final String TAG = "UsageReporter";
    private static final boolean isEnabled = true;
    private static final boolean DBG = true;

    public static final String APP_KEY = "21736485";

    private static final String GROUP_3_TAG = "Contacts G3:";

    /**
     * *************************************************************************
     * ** 拨号盘页
     * *************************************************************************
     */
    public final class DialpadPage {
        /** 通话记录页 */
        public static final String FILTER_CALL_LOG = "Click_filter_call_log_button"; // 点击筛选通话记录
        public static final String FILTER_CALL_LOG_MISSED = "Click_filter_call_log_button_filter_missed_item"; // 点击筛选通话记录-未接来电
        public static final String FILTER_CALL_LOG_INCOMING = "Click_filter_call_log_button_filter_incoming_item"; // 点击筛选通话记录-已接来电
        public static final String FILTER_CALL_LOG_OUTGOING = "Click_filter_call_log_button_filter_outgoing_item"; // 点击筛选通话记录-已拨电话
        public static final String FILTER_CALL_LOG_ALL = "Click_filter_call_log_button_filter_all_item"; // 点击筛选通话记录-全部
        public static final String BACKUP_CALL_LOG = "Click_backup_call_log_button"; // 点击备份通话记录
        public static final String SETTING_FROM_DIALPAD = "Click_settings_button_from_dialpad"; // 点击拨号盘中的设置
        public static final String DP_ENTER_CALLLOG_DELETE = "View_calllog_delete_UI_from_dialpad_by_clicking_footer_button"; // 点击拨号盘上的删除按钮
        public static final String DP_ENTER_CONFERENCE_CALL = "Make_Multi_Call"; // 点击拨号盘上的多方通话

        public static final String CLOUD_CALL = "Page_DialpadFragment_Click_Cloud_Phone_Main"; // 点击云通话

        public static final String DP_ADD_CONTACT_FROM_SEARCH = "Input_some_digits_and_add_new_contact_by_clicking_the_add_button"; // 拨号盘输入数字 点击添加按钮新建联系人
        public static final String DP_ADD_EXISTING_CONTACT_FROM_SEARCH = "Input_some_digits_and_add_to_existing_contact_by_clicking_the_add_to_existing_button"; // 拨号盘输入数字 点击存为现有联系人
        public static final String DP_SMS_FROM_SEARCH = "Input_some_digits_and_send_SMS_by_clicking_the_send_button";// 拨号盘输入数字 点击发送按钮发送短信

        public static final String DP_ENTER_DETAIL = "View_calllog_detail_from_dialpad_by_clicking_button_on_the_right_of_each_item"; // 点击列表项的右侧按钮查看通话记录详情
        public static final String DP_MO_FROM_CALLLOG = "Make_call_by_clicking_calllog_item";// 点击通话记录列表向发起呼叫
        public static final String DP_MO_FROM_SEARCH = "Input_some_digits_and_make call_by_clicking_on_the_searched_item";// 拨号盘输入数字点击检索结果中的某项发起呼叫

        /** 选择通话记录页 */
        public static final String DP_MULTI_SELECT_CALLLOG_SELECT_ALL = "Choose_calllog_click_select_all_button"; // 选择通话记录页点击全选
        public static final String DP_DELETE_CALLLOG_FROM_SELECT = "Delete_some_calllog_by_clicking_the_footer_button_int_the_calllog_multiselect_page";// 选择通话记录页点击删除

        /** 通话记录详情页 */
        public static final String DP_VIDEO_CALL_FROM_DETAIL = "Click_Video_Call_Button_to_Make_Call"; // 从通话详情界面发起视频通话
        public static final String DP_MO_FROM_DETAIL = "Make_call_in_the_calllog_detail_page"; // 从通话详情界面发起呼叫
        public static final String DP_SMS_FROM_DETAIL = "Send_SMS_in_the_calllog_detail_page"; // 从通话记录详情界面发送短信
        public static final String DP_ADD_CONTACT_FROM_DETAIL = "Add_new_contact_by_clicking_the_footer_button_in_the_calllog_detail_page"; // 通话记录详情界面点击保存联系人按钮
        public static final String DP_ADD_EXISTING_CONTACT_FROM_DETAIL = "Add_to_existing_contact_by_clicking_the_footer_button_in_the_calllog_detail_page"; // 通话记录详情界面点击存为现有联系人
        public static final String DP_BLACK_ADD_FROM_DETAIL = "Add_to_blacklist_by_clicking_the_footer_button_in_the_calllog_detail_page"; // 加入黑名单
        public static final String DP_BLACK_DELETE_FROM_DETAIL = "Remove_from_blacklist_by_clicking_the_footer_button_in_the_calllog_detail_page"; // 从黑名单中移除
        public static final String DP_DELETE_CALLLOG_FROM_DETAIL = "Delete_calllog_by_clicking_the_footer_button_in_the_calllog_detail_page"; // 通话记录详情界面点击删除按钮
        public static final String CALL_RECORD = "Click_call_record_button";

        /** 通话录音页 */
        public static final String CALL_RECORD_LISTEN = "Click_call_record_listen_button"; // 从通话录音页点击收听通话录音
        public static final String CALL_RECORD_DELETE = "Click_call_record_delete_button"; // 从通话录音页点击收听删除录音

        /** 拨号 */
        public static final String DP_MO_FROM_SIMCARD = "Make_call_by_clicking_the_footer_button"; // 输入号码点击拨号盘上的呼叫按钮 单卡或无卡呼叫
        public static final String DP_MO_FROM_SIMCARD1 = "Make_call_through_sim1_by_clicking_the_footer_button"; // 从sim1发起呼叫
        public static final String DP_MO_FROM_SIMCARD2 = "Make_call_through_sim2_by_clicking_the_footer_button"; // 从sim2发起呼叫

        /** 设置 */
        public static final String TURN_ON_AUTO_MARK_TAG = "Click_Mark_Strange_Phone_Number_On";// "打开陌生人来电自动弹标记";
        public static final String TURN_OFF_AUTO_MARK_TAG = "Click_Mark_Strange_Phone_Number_Off";// "关闭陌生人来电自动弹标记";
        public static final String DUAL_CARD_SETTING = "Click_Multi-sims_Setting";//"进入双卡设置"；

        /** 标记 */
        public static final String CLICK_MARK_STRANGE_CALL = "Click_Mark_Strange_Call";// "给陌生号打标记";
        public static final String CLICK_MARK_STRANGE_CALL_MARK_AGAIN = "Click_Mark_Strange_Call_Mark_Again";// "重新给陌生号打标记";
        public static final String CLICK_MARK_STRANGE_CALL_CANCLE_MARK = "Click_Mark_Strange_Call_Cancle_Mark";// "撤销标记";

        public static final String MARK_AS_HARASSING_CALL = "Mark_as_Harassing_call"; // 标记-骚扰电话
        public static final String MARK_AS_FRAUD_CALL = "Mark_as_Fraud_call"; // 标记-疑似诈骗
        public static final String MARK_AS_AD_PROMOTION_CALL = "Mark_as_AD_Promotion_call"; // 标记-广告推销
        public static final String MARK_AS_HOUSE_AGENT_CALL = "Mark_as_House_Agent_call"; // 标记-房产中介
        public static final String MARK_AS_EXPRESS_CALL = "Mark_as_Express_call"; // 标记-快递服务
        public static final String MARK_AS_CUSTOMED_CALL = "Mark_as_Customed_call"; // 标记-自定义

        public static final String DP_LC_MAKE_CALL = "Click_Make_Call_by_LongPress"; // 拨号界面长按通话记录点击打电话
        public static final String DP_LC_MAKE_VIDEO_CALL = "LongPress_Make_Video_Call"; // 拨号界面长按通话记录点击发起视频通话
        public static final String DP_LC_TO_COPY_NUMBER = "Click_Copy_Phone_Number"; // 拨号界面长按号码复制号码
        //public static final String DP_LC_SEND_SMS = "Click_Send_Sms_by_LongPress"; // 拨号界面长按通话记录点击发短信
        public static final String DP_LC_EDIT_BEFORE_CALL = "Click_Edit_Number_before_MakeCall_by_LongPress"; // 拨号界面长按通话记录点击拨号前编辑号码
        public static final String DP_LC_DELETE_CALLLOG = "Click_Delete_Call_Log_by_LongPress"; // 拨号界面长按通话记录点击删除通话记录
        public static final String DP_LC_ADD_BLACKLIST = "Click_Add_into_Blacklist_by_LongPress"; // 拨号界面长按通话记录点击加入黑名单
        public static final String DP_LC_MARK_NUM = "Click_Mark_the_Number_by_LongPress"; // 拨号界面长按通话记录点击标记号码

        public static final String Click_MENU_Harassing_Phone_Calls = "Page_DialpadFragment_Click_View_Harassing_Phone_Calls"; // 拨号界面菜单电话拦截

        /** 搜索结果长按菜单 */
        public static final String LONG_CLICK_SEARCH_RESULT_COMMON_CALL = "Input_some_digits_and_LongPress_the_searched_item_to_make_call"; //长按搜索结果拨号
        public static final String LONG_CLICK_SEARCH_RESULT_VIDEO_CALL = "Input_some_digits_and_LongPress_the_searched_item_to_video_call"; //长按搜索结果视频通话
        public static final String LONG_CLICK_SEARCH_RESULT_COPY_NUMBER = "Input_some_digits_and_LongPress_the_searched_item_to_copy_number"; //长按搜索结果复制号码
        public static final String LONG_CLICK_SEARCH_RESULT_DELETE_CALL_LOG = "Input_some_digits_and_LongPress_the_searched_item_to_Delete_Call_Log"; //长按搜索结果删除通话记录
        public static final String LONG_CLICK_SEARCH_RESULT_EDIT_BEFORE_CALL = "Input_some_digits_and_LongPress_the_searched_item_to_Edit_Numbers"; //长按搜索结果拨号前编辑
        public static final String LONG_CLICK_SEARCH_RESULT_ADD_BLACK_LIST = "Input_some_digits_and_LongPress_the_searched_item_to_Add_to_Blacklist"; //长按搜索结果加入黑名单
        public static final String LONG_CLICK_SEARCH_RESULT_REMOVE_BLACK_LIST = "Input_some_digits_and_LongPress_the_searched_item_to_Remove_from_Blacklist"; //长按搜索结果移除黑名单
    }

    /**
     * *************************************************************************
     * ** 联系人列表页
     * *************************************************************************
     */
    public final class ContactsListPage {
        /** Click: search */
        public static final String SEARCH_CL = "Click_on_the_search_bar";// "点击搜索栏";
        public static final String SEARCH_CL_VIEW_SEARCH_RESULT = "Click_the_search_result_to_view_contacts_detail";// "搜索结果进入联系人详情";

        /** Click: fish eye */
        public static final String FISH_EYE_1ST = GROUP_3_TAG + "fish eye 1st clicked:"; // 在联系人列表点击一级鱼眼
        public static final String FISH_EYE_2ND = GROUP_3_TAG + "fish eye 2nd clicked:"; // 在联系人列表点击二级鱼眼
        public static final String FISH_EYE_3RD = GROUP_3_TAG + "fish eye 3rd clicked:"; // 在联系人列表点击三级鱼眼
        public static final String FISH_EYE_4TH = GROUP_3_TAG + "fish eye 4th clicked:"; // 在联系人列表点击四级鱼眼

        /** Click: view detail */
        public static final String VIEW_CLICK_CL = "Click_to_view_the_details_in_the_contact_list";// "在联系人列表单击进入详情";
        public static final String VIEW_CLICK_FAVORITE_CL = "Click_to_view_the_favorite_details_in_the_contact_list"; // "在联系人列表单击进入Favorite详情";
        public static final String VIEW_CLICK_GROUP_CL = "Click_to_view_the_details_in_the_group_list"; // 在群组中的联系人列表中点击进入联系人详情

        /** Click: footer menu */
        public static final String FOOT_DEL_CL = "Click_footer_delete_button_in_the_contact_list";// "在联系人列表点击删除";
        public static final String FOOT_SET_CL = "Click_footer_settings_button_in_the_contact_list";// "在联系人列表点击设置";
        public static final String FOOT_SYNC_CL = "Click_footer_sync_button_in_the_contact_list";// "在联系人列表点击同步联系人";
        public static final String FOOT_ACCOUNT_FILTER_CL = "Click_Contacts_Group";// "在联系人列表点击要显示的联系人";
        public static final String ACTION_LIST_NEW_CONTACT = "Click_new_in_action_list";// "在联系人列表的菜单中新建联系人";
        public static final String CONTACTS_LIST_NEW_CONTACT = "Click_new_in_the_contact_list";// "在联系人列表新建联系人";
        public static final String EMPTY_LIST_NEW_CONTACT = "Click_new_in_blank";// "在联系人列表新建联系人";

        /** Click: reserved */
        public static final String ADD_FAVORITE = "Add_favorite_contacts_by_click_add_button";// "常用联系人点击“+”";
        public static final String CALL_FAVORITE = "In_the_favorite_contacts_bar_left_slide_to_call";// "在常用联系人栏左滑发起打电话";
        public static final String SMS_FAVORITE = "In_the_favorite_contacts_bar_right_slide_to_sms";// "在常用联系人栏右滑发起短信";
        public static final String CALL_CL = "In_the_contact_list_left_slide_to_call";// "在联系人列表左滑起打电话";
        public static final String SMS_CL = "In_the_contact_list_left_slide_to_sms";// "在联系人列表右滑起短信";

        public static final String EMPTY_IMPORT_FROM_SIM = GROUP_3_TAG + "import_from_sim"; // 空联系人列表中点击从SIM卡导入

        /** 选择联系人页 */
        public static final String MULTI_SELECT_PEOPLE_SELECT_ALL = "Click_select_all_button_in_multi_select_people_page";// "在选择联系人页点击全选";
        public static final String MULTI_SELECT_PEOPLE_DELETE = "Click_delete_button_in_multi_select_people_page"; // "在选择联系人页点击删除";

        public static final String CL_LC_VIEW_CONTACT = "Click_View_Contact_Details_by_LongPress"; // 联系人列表长按联系人点击查看联系人详情
        public static final String CL_LC_EDIT_CONTACT = "Click_Edit_the_Contact_by_LongPress"; // 联系人列表长按联系人点击编辑联系人
        public static final String CL_LC_DELETE_CONTACT = "Click_Delete_the_Contact_by_LongPress"; // 联系人列表长按联系人点击删除联系人
        public static final String CL_LC_ADD_FAVORITE = "Click_Add_the_Contact_to_Favorites_by_LongPress"; // 联系人列表长按联系人点击添加常用联系人
        public static final String CL_LC_CANCEL_FAVORITE = "Click_Cancle_the_Contact_from_Favorites_by_LongPress"; // 联系人列表长按联系人点击移除常用联系人
        public static final String CL_LC_SEND_DESKTOP = "Click_Send_the_Contact_to_Desktop_by_LongPress"; // 联系人列表长按联系人点击发送到桌面快捷方式
        public static final String CL_LC_DIAL_SHORTCUT = "DIRECT_DIAL_ADDNAME"; // 从桌面小工具添加联系人快速拨号widget进入电话选择列表，用户选择某一号码
    }

    /**
     * *************************************************************************
     * ** 群组列表页
     * *************************************************************************
     */
    public final class GroupListPage {
        /** 群组列表页 */
        public static final String ENTER_GROUP = "Enter_one_group";// 进入某一群组列表
        public static final String CREATE_GROUP = "Create_new_group";// 点击新建群组
        public static final String DELETE_GROUP = "Delete_group";// 点击删除群组
        public static final String UNFOLD_CITY_GROUP_LIST = "Unfold_City_Group";// 点击展开城市分组列表
        public static final String FOLD_CITY_GROUP_LIST = "Fold_City_Group"; // 点击收缩城市分组列表
        public static final String ENTER_ONE_CITY_GROUP = "Enter_One_City"; // 进入某一城市分组联系人列表

        /** 群组联系人列表页 */
        public static final String GROUP_CONTACTS_VIEW_CONTACT_DETAIL = "Group_click_to_view_contact_detail";// 群组联系人列表点击进入联系人详情
        public static final String GROUP_CONTACTS_NEW_CONTACT = "Group_click_to_create_new_contact";// 群组联系人列表点击新建联系人
        public static final String GROUP_CONTACTS_SMS_GROUP = "Send_group_message";// 点击群发短信
        public static final String GROUP_CONTACTS_ADD_GROUP_MEM = "Add_group_member";// 点击添加群组成员
        public static final String GROUP_CONTACTS_RE_GROUP_MEM = "Remove_group_member";// 点击移除群组成员
        public static final String GROUP_CONTACTS_GROUP_EDIT = "Group_rename";// 点击重命名该群组
        public static final String GROUP_CONTACTS_DELETE_GROUP = "Group_click_to_delete_group";// 点击删除该群组

    }

    /**
     * *************************************************************************
     * ** 黄页
     * *************************************************************************
     */
    public final class YellowPage {
        /** 黄页首页 */
        public static final String DP_YP_SEARCH_VIEW_CLICKED = "Yellow_page_search_view_clicked"; // 进入号码库搜索页面
        public static final String DP_YP_CITY_CLICKED = "Yellow_page_city_button_clicked"; // 点击地区进入
        public static final String DP_YELLOW_PAGE_TYPE_CLICKED = "Yellow_page_type_clicked"; // 进入电话号码库

        /** 黄页联系列表页 */
        public static final String DP_YP_GROUP_NATIONA = "Nationwide(全国类别)"; // 全国类别
        public static final String DP_YP_GROUP_ITEM_CLICKED = "Yellow_page_group_item_clicked"; // 点击号码库某一个类别项
        public static final String DP_YELLOW_CONTACT_LIST_ITEM_CLICKED = "Yellow_page_group_item_contact_list_item_clicked"; // 从号码库某一个类别项中点击某一号码

        /** 黄页详情页 */
        public static final String DP_OUTCALL_FROME_YP = "Out_going_call_from_yellow_page"; // 从黄页详情页号码库发起呼叫
        public static final String DP_SEND_MSG_FROME_YP = "Send_message_from_yellow_page"; // 从黄页详情页发信息
        public static final String DP_VIEW_ADDRESS_FROME_YP = "View_address_from_yellow_page"; // 从黄页详情页查看地址
        public static final String DP_VIEW_WEB_FROME_YP = "View_web_from_yellow_page"; // 从黄页详情页查看网址
        public static final String DP_VIEW_WEIBO_FROME_YP = "View_weibo_from_yellow_page"; // 从黄页详情页查看微博
        public static final String DP_VIEW_DETAIL_FROME_YP = "View_more_detail_from_yellow_page"; // 从黄页详情页查看详情
        public static final String DP_VIEW_MAP_FROME_YP = "View_map_from_yellow_page"; // 从黄页详情页查看详情
        public static final String DP_ERROR_CORRECTION_FROME_YP = "Click_error_correction_from_yellow_page"; // 从黄页详情页点击纠错

        /** 地区列表页 */
        public static final String DP_YP_DOWNLOAD_PROVINCE = "Yellow_page_download_province"; // 省份下载

    }

    /**
     * *************************************************************************
     * ** 联系人编辑页
     * *************************************************************************
     */
    public final class ContactsEditPage {
        /** 联系人编辑页 */
        public static final String EDITOR_ACTIVITY_ENTRY = GROUP_3_TAG + "entry_editor"; // 进入新建/编辑联系人界面
        public static final String EDITOR_PHOTO_INPUT = GROUP_3_TAG + "editor_screen:_photo_input"; // 编辑联系人头像
        public static final String EDITOR_NAME_INPUT = GROUP_3_TAG + "editor_screen:_name_input"; // 编辑联系人姓名
        public static final String EDITOR_CALL_INPUT = GROUP_3_TAG + "editor_screen:_call_input"; // 编辑联系人号码
        public static final String EDITOR_EMAIL_INPUT = GROUP_3_TAG + "editor_screen:_email_input"; // 编辑联系人邮箱
        public static final String EDITOR_GROUP_BUTTON_CLICKED = GROUP_3_TAG + "detail_screen,_group_choosing_button_clicked"; // 编辑联系人群组
        public static final String EDITOR_RINGTONE_BUTTON_CLICKED = GROUP_3_TAG + "detail_screen,_ringtone_button_clicked"; // 编辑联系人铃声
        public static final String EDITOR_ORGANIZATION_INPUT = GROUP_3_TAG + "editor_screen:_organization_input"; // 编辑联系人工作信息
        public static final String EDITOR_IM_INPUT = GROUP_3_TAG + "editor_screen:_im_input"; // 编辑联系人即时聊天工具
        public static final String EDITOR_ADDRESS_INPUT = GROUP_3_TAG + "editor_screen:_address_input"; // 编辑联系人地址
        public static final String EDITOR_NOTE_INPUT = GROUP_3_TAG + "editor_screen:_note_input"; // 编辑联系人备注
        public static final String EDITOR_WEB_INPUT = GROUP_3_TAG + "editor_screen:_web_input"; // 编辑联系人网站
        public static final String EDITOR_BIRTHDAY_INPUT = GROUP_3_TAG + "editor_screen:_birthday_input"; // 编辑联系人生日
        public static final String EDITOR_PINYIN_INPUT = GROUP_3_TAG + "editor_screen:_pinyin_input"; // 编辑联系人姓名拼音
        public static final String EDITOR_NICKNAME_INPUT = GROUP_3_TAG + "editor_screen:_nickname_input"; // 编辑联系人昵称
        public static final String EDITOR_SNS_INPUT = GROUP_3_TAG + "editor_screen:_sns_input"; // 编辑联系人网络社区

        /** 联系人编辑页： 删除某一编辑项 */
        public static final String DELETE_PHOTO_INPUT = GROUP_3_TAG + "editor_screen:_photo_delete"; // 删除联系人头像
        public static final String DELETE_NAME_INPUT = GROUP_3_TAG + "editor_screen:_name_delete"; // 删除联系人姓名
        public static final String DELETE_CALL_INPUT = GROUP_3_TAG + "editor_screen:_call_delete"; // 删除联系人号码
        public static final String DELETE_EMAIL_INPUT = GROUP_3_TAG + "editor_screen:_email_delete"; // 删除联系人邮箱
        public static final String DELETE_ORGANIZATION_INPUT = GROUP_3_TAG + "editor_screen:_organization_delete"; // 删除联系人工作信息
        public static final String DELETE_IM_INPUT = GROUP_3_TAG + "editor_screen:_im_delete"; // 删除联系人即时聊天工具
        public static final String DELETE_ADDRESS_INPUT = GROUP_3_TAG + "editor_screen:_address_delete"; // 删除联系人地址
        public static final String DELETE_NOTE_INPUT = GROUP_3_TAG + "editor_screen:_note_delete"; // 删除联系人备注
        public static final String DELETE_WEB_INPUT = GROUP_3_TAG + "editor_screen:_web_delete"; // 删除联系人网站
        public static final String DELETE_BIRTHDAY_INPUT = GROUP_3_TAG + "editor_screen:_birthday_delete"; // 删除联系人生日
        public static final String DELETE_PINYIN_INPUT = GROUP_3_TAG + "editor_screen:_pinyin_delete"; // 删除联系人姓名拼音
        public static final String DELETE_NICKNAME_INPUT = GROUP_3_TAG + "editor_screen:_nickname_delete"; // 删除联系人昵称
        public static final String DELETE_SNS_INPUT = GROUP_3_TAG + "editor_screen:_sns_delete"; // 删除联系人网络社区

    }

    /**
     * *************************************************************************
     * ** 联系人设置页
     * *************************************************************************
     */
    public final class ContactsSettingsPage {
        /** 我的名片 */
        public static final String SETTING_ENTER_CLICK_MY_VCARD = GROUP_3_TAG + "enter_my_personal_profile_info"; // 联系人设置中点击我的名片

        /** 微博插件 */
        public static final String SETTING_WEIBO_AUTH = GROUP_3_TAG + "weibo_authorization"; // 联系人设置中点击微博插件

        /** 云同步 */
        public static final String SETTING_SYNC = GROUP_3_TAG + "sync"; // 联系人设置中点击同步联系人
        public static final String SETTING_BACKUP_CALLLOG = "Sync_calllog_by_clicking_the_footer_button"; // 联系人设置中点击备份通话记录

        /** 联系人管理 */
        public static final String SETTING_ENTER_IMPORT_CONTACTS = GROUP_3_TAG + "enter_import_from_contacts"; // 联系人设置中点击导入联系人
        public static final String SETTING_ENTER_EXPORT_CONTACTS = GROUP_3_TAG + "enter_export_to_contacts"; // 联系人设置中点击导出联系人

        public static final String SETTING_ENTER_IMPORT_FROM_SIM = GROUP_3_TAG + "enter_import_from_sim_card"; // 联系人设置中点击导入联系人-从卡导入
        public static final String SETTING_ENTER_IMPORT_FROM_SIM1 = GROUP_3_TAG + "enter_import_from_sim_card1"; // 联系人设置中点击导入联系人-从卡1导入
        public static final String SETTING_ENTER_IMPORT_FROM_SIM2 = GROUP_3_TAG + "enter_import_from_sim_card2"; // 联系人设置中点击导入联系人-从卡2导入
        public static final String SETTING_ENTER_EXPORT_TO_SIM = GROUP_3_TAG + "enter_export_to_sim_card"; // 联系人设置中点击导出联系人-从卡导出
        public static final String SETTING_ENTER_EXPORT_TO_SIM1 = GROUP_3_TAG + "enter_export_to_sim_card1"; // 联系人设置中点击导出联系人-从卡1导出
        public static final String SETTING_ENTER_EXPORT_TO_SIM2 = GROUP_3_TAG + "enter_export_to_sim_card2"; // 联系人设置中点击导出联系人-从卡2导出

        /** 联系人管理-SIM卡/存储卡导入导出 */
        public static final String SIM_CARD1 = "sim_card1"; //卡1
        public static final String SIM_CARD2 = "sim_card2"; //卡2
        public static final String SDCARD = "sdcard"; //存储卡
        public static final String SETTING_START_IMPORT = "click_start_importing_"; //开始导入
        public static final String SETTING_START_EXPORT = "click_start_exporting_"; //开始导出

        /** 联系人管理-单卡导入导出或空联系人界面导入导出 */
        public static final String SETTING_IMPORT_FROM_VCARD = GROUP_3_TAG + "import_from_vcard"; // 联系人设置中点击从VCard导入
        public static final String SETTING_EXPORT_TO_VCARD = GROUP_3_TAG + "export_to_vcard"; // 联系人设置中点击导出到VCard

        /** 联系人显示 */
        public static final String TURN_ON_DISPLAY_FAVORITE = "Turn_on_display_favorite_contacts";// "打开显示常用联系人";
        public static final String TURN_OFF_DISPLAY_FAVORITE = "Turn_off_display_favorite_contacts";// "关闭显示常用联系人";
        public static final String TURN_ON_RECOMMEND_FAVORITE = "Turn_on_recommend_favorite_contacts";// "打开推荐常用联系人";
        public static final String TURN_OFF_RECOMMEND_FAVORITE = "Turn_off_recomment_favorite_contacts";// "关闭推荐常用联系人";
        public static final String TURN_ON_SHOW_HEAD_ICON = "Turn_on_show_head_icon";// "打开显示头像";
        public static final String TURN_OFF_SHOW_HEAD_ICON = "Turn_off_show_head_icon";// "关闭显示头像";

        /* 鱼眼横向/纵向 */
        public static final String TURN_ON_VERTICAL_FISH_EYE = "Turn_on_vertical_fish_eye";// "打开纵向鱼眼";
        public static final String TURN_ON_HORIZON_FISH_EYE = "Turn_on_horizon_fish_eye";// "打开横向鱼眼";

        /*快速拨号*/
        public static final String QUICK_CALL_LONG_PRESS = "Click_Make_Quickly_Call_by_LongPress_Dialpad_Number";
        public static final String QUICK_CALL_SETTING = "Click_Set_Quickly_Call";
        public static final String QUICK_CALL_ADD = "Click_Add_Quickly_Call_Contacts";
        public static final String QUICK_CALL_MODIFY = "Click_Modify_a_Quickly_Call_Contacts";
        public static final String QUICK_CALL_DELETE = "Click_Delete_a_Quickly_Call_Contacts";

        /** 公共号码库 */
        public static final String SETTING_ENTER_YELLOW_PAGE_NUMBER_DB = "enter_yellow_page_number_database";// "下载号码库离线数据";

        /** 安全 */
        public static final String SETTING_ENTER_BLACK_LIST = GROUP_3_TAG + "enter_black_list";
        public static final String SETTING_ENTER_ORGANIZE_CONTACTS = GROUP_3_TAG + "enter_organize_contacts"; // 联系人设置中点击整理联系人
        public static final String SETTING_ENTER_BARCODE_SCREEN = GROUP_3_TAG + "enter_barcode_screen"; // 联系人设置中点击二维码

    }

    /**
     * *************************************************************************
     * ** 拨号盘设置页
     * *************************************************************************
     */
    public final class PhoneSettingsPage {
        /** 常规设置 */
        public static final String PHONE_SETTINGS_CLICK_GENERAL_SETTINGS = "Click_General_settings";
        /** 通话设置 */
        public static final String PHONE_SETTINGS_CLICK_CALL_SETTINGS = "Click_Call_settings";

    }

    /**
     * *************************************************************************
     * ** 联系人详情页
     * *************************************************************************
     */
    public final class ContactsDetailPage {
        // contacts details
        public static final String DETAL_STARRED_CLICKED = GROUP_3_TAG + "detail_screen,_starred_icon_clicked"; // 联系人详情界面点击添加收藏为常用联系人
        public static final String DETAL_CANCEL_STARRED_CLICKED = GROUP_3_TAG + "detail_screen,_cancel_starred_icon_clicked"; // 联系人详情界面点击取消收藏为常用联系人
        public static final String DETAL_EDIT_BUTTON_CLICKED = GROUP_3_TAG + "detail__screen,_editor_button_clicked"; // 联系人详情界面点击编辑
        public static final String DETAL_DELETE_BUTTON_CONFIRMED = GROUP_3_TAG + "detail__screen,_deleted_button_clicked"; // 联系人详情界面点击删除
        public static final String DETAL_SMS_SHARE_BUTTON_CLICKED = GROUP_3_TAG + "detail__screen,_sms_shared_button_clicked"; // 联系人详情界面点击通过短信分享
        public static final String DETAL_SHARE_BUTTON_CLICKED = GROUP_3_TAG + "detail__screen,_other_shared_button_clicked"; //  联系人详情界面点击其他分享
        public static final String DETAL_PRIVATE_BUTTON_CLICKED = GROUP_3_TAG + "detail__screen,_privated_button_clicked"; // 联系人详情界面点击加为隐私联系人
        public static final String DETAL_ALIPAY_TRANSFER_CLICKED = GROUP_3_TAG + "detail__screen,_alipay_transfer_button_clicked"; // 联系人详情界面点击给Ta转账
        public static final String DETAL_ALIPAY_RECHARGE_CLICKED = GROUP_3_TAG + "detail__screen,_alipay_recharge_button_clicked"; // 联系人详情界面点击给Ta充话费
        public static final String DETAL_VIDEO_CALL_ICON_CLICKED = "Click_Video_Call_Button_to_Make_Call"; // 联系人详情界面点击拨打视频电话
        public static final String DETAL_CALL_ICON_CLICKED = GROUP_3_TAG + "detail__screen,_call_button_clicked"; // 联系人详情界面点击拨打电话
        public static final String DETAL_MESSAGE_ICON_CLICKED = GROUP_3_TAG + "detail__screen,_message_button_clicked"; // 联系人详情界面点击发信息
        public static final String DETAL_BARCODE_CLICKED = GROUP_3_TAG + "detail_screen,_barcode_icon_clicked"; // 联系人详情界面点击二维码扫描
        public static final String DETAL_DELETE_CALLLOG = GROUP_3_TAG + "detail_screen,_delete_calllog_clicked"; // 联系人详情界面点击清除通话记录
        public static final String DETAL_LC_SEND_DESKTOP = GROUP_3_TAG + "detail__screen,_send_the_contact_to_desktop"; // 联系人详情界面点击添加快捷方式到桌面

        public static final String DETAL_LC_COPY_NUMBER = GROUP_3_TAG + "detail_screen,long_Press_to_Copy_Phone_Number"; // 联系人详情界面长按号码复制号码
        public static final String DETAL_LC_MAKE_CALL = GROUP_3_TAG + "detail_screen,long_Press_to_Make_Call"; // 联系人详情界面长按号码呼叫
        public static final String DETAL_LC_SET_DEFAULT_NUMBER = GROUP_3_TAG + "detail_screen,long_Press_to_Set_Default_Phone_Number"; // 联系人详情界面长按号码设置默认值
        public static final String DETAL_LC_CLEAR_DEFAULT_NUMBER = GROUP_3_TAG + "detail_screen,long_Press_to_Cancle_Default_Phone_Number"; // 联系人详情界面长按号码清除默认值
        public static final String DETAL_PHONE_ICON_CLICKED = GROUP_3_TAG + "detail__screen,_call_icon_button_clicked";

        public static final String DETAL_SINA_WEIBO_CLICKED = GROUP_3_TAG + "detail_screen,weibo"; // 联系人详情界面点击新浪微博
        public static final String DETAL_THIRD_PARTY_IM_CLICKED = GROUP_3_TAG + "detail_screen,third_party_IM"; // 联系人详情界面点击其他三方帐号下信息
    }

    public final class CallDetailPage {
        public static final String CALL_DETAIL_LC_TO_COPY = "long_Press_to_Copy_Phone_Number"; // 通话记录详情界面长按号码复制号码
        public static final String CALL_DETAL_LC_MAKE_CALL = "long_Press_to_Make_Call"; // 通话记录详情界面长按号码呼叫
    }

    public final static class DuplicateRemove {
        /* Report the amount of similar contacts group */
        public static final String MERGE_CONTACTS = "Merge_Contacts"; /* custom event id */
        public static final String SIMILAR_CONTACT_NUMBER = "Similar_Contact_Number"; /* parameter name */

        /* click of merge contact button */
        public static final String MERGE_SIMILAR_CONTACTS = "Merge_Similar_Contacts";
    }

    // 帐号/群组过滤、选择
    public final static class AccountsSupport {
        public static final String ACCOUNT_GROUP_FILTER_PAGE_NAME = "ContactsGroup";
        public static final String ACCOUNT_SELECTION_PAGE_NAME = "AccountFilterManager";

        public static final long REPORT_INTERVAL = 1000L * 60 * 60 *24 * 7; // 1 week
        public static final String EVENT_ID_LOCAL_ACCOUNT_CHECKED = "Local_Contacts";
        public static final String EVENT_ID_SIM_ACCOUNT_CHECKED = "Sim_Contacts";
        public static final String EVENT_ID_OTHER_ACCOUNTS_CHECKED = "Other_Contacts";
    }

    public final static class GadgetPage {
        public static final String GADGET_PHONE_ENTER_DIALPAD = "GADGET_PHONE_ENTER_DIALPAD";
        public static final String GADGET_PHONE_MAKE_CALL = "GADGET_PHONE_CALLBACK";
    }

    /**
     * Call it when the application first time starting.
     *
     * @param context
     */
    public static void sendAppFirstStart(Context context) {
        log("[sendAppFirstStart] App First Started");
    }

    @Deprecated
    public static void sendUsageInfo(Context context, int type,
            Class<? extends Activity> activityClass, String value, String... args) {
        if (isEnabled) {
            if (args != null) {
                int i = 1;
                Map<String, String> lProperties = new HashMap<String, String>();
                for (String arg : args) {
                    lProperties.put("canshu" + i, arg);
                    i++;
                }
                TA.getInstance().getDefaultTracker()
                        .ctrlClicked(activityClass, value, lProperties);
                log("[sendUsageInfo] event:" + value);
            }
        }
    }

    public static void init(Context context) {
        if (isEnabled) {
            //StatConfig.getInstance().turnOnDebug();
            StatConfig.getInstance()
                    .setContext(context.getApplicationContext());
            StatConfig.getInstance().setChannel("aliyunos");
            Tracker lTracker = TA.getInstance().getTracker(APP_KEY);
            lTracker.setAppKey(APP_KEY);
            TA.getInstance().setDefaultTracker(lTracker);
        }
    }

    public static void onCreate(Activity activity, String pageName) {

    }

    public static void onClick(Class<? extends Activity> activityClass, String widgetName) {
        if (isEnabled) {
            String pageName = activityClass.getSimpleName();
            TA.getInstance().getDefaultTracker()
                    .commitEvent(pageName, 2101, widgetName, null, null, null);
        }
    }

    public static void onClick(Activity activity, String pageName, String widgetName) {
        onClick(activity, pageName, widgetName, null);
    }

    public static void onClick(Activity activity, String pageName, String widgetName, Map<String, String> pParams) {
        if (isEnabled) {
            if (pageName == null) {
                if (activity == null) {
                    return;
                }
                pageName = activity.getClass().getSimpleName();
            }
            TA.getInstance().getDefaultTracker()
                    .commitEvent(pageName, 2101, widgetName, null, null, pParams);
        }
    }

    /**
     * send custom event without relationship with any UI or widget
     */
    public static void commitEvent(String eventId, Map<String, String> pParams) {
        if (isEnabled) {
            if (!TextUtils.isEmpty(eventId)) {
                TA.getInstance().getDefaultTracker().commitEvent(eventId, pParams);
                log("[commitEvent] eventId:" + eventId);
            }
        }
    }

    public static void onResume(Activity activity, String pageName) {
        if (isEnabled) {
            if (pageName == null) {
                if (activity == null) {
                    return;
                }
                pageName = activity.getClass().getSimpleName();
            }
            TA.getInstance().getDefaultTracker().pageEnter(pageName);
            log("[onResume]" + pageName);
        }
    }

    public static void onPause(Activity activity, String pageName) {
        if (isEnabled) {
            if (pageName == null) {
                if (activity == null) {
                    return;
                }
                pageName = activity.getClass().getSimpleName();
            }
            TA.getInstance().getDefaultTracker().pageLeave(pageName);
            log("[onPause]" + pageName);
        }
    }

    private static final String PERIOD_REPORT_SERIALIZE_PREFIX = "period_report_";
    private static final String PERIOD_REPORT_SERIALIZE_FILE_ENCODE = "UTF-8";

    public static void serializeForPeriodReport(Context context, String name, HashMap<String, String> params) {
        String prefsName = getPrefsNameForPeriodReport(name);
        SharedPreferences prefs = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.clear();
        for (String key : params.keySet()) {
            editor.putString(key, params.get(key));
        }
        editor.apply();
    }

    public static HashMap<String, String> deserializeForPeriodReport(Context context, String name) {
        HashMap<String, String> result = null;
        String prefsName = getPrefsNameForPeriodReport(name);
        SharedPreferences prefs = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE);
        Map<String, ?> prefsMap = prefs.getAll();
        if ((prefsMap == null) || prefsMap.isEmpty()) {
            return result;
        }
        result = new HashMap<String, String>(prefsMap.size());
        for (String key : prefsMap.keySet()) {
            Object value = prefsMap.get(key);
            if (value != null) {
                result.put(key, value.toString());
            }
        }
        return result;
    }

    /**
     * In case some report string contains invalid chars for file name,
     * we encode the special chars with URLEncoder rule.
     * @param name
     * @return
     */
    private static String getPrefsNameForPeriodReport(String name) {
        String prefsName = "";
        try {
            prefsName = URLEncoder.encode(PERIOD_REPORT_SERIALIZE_PREFIX + name,
                    PERIOD_REPORT_SERIALIZE_FILE_ENCODE);
        } catch (UnsupportedEncodingException e) {
            // won't happen, UTF-8 shall always be supported.
            Log.e(TAG, "getPrefsName: got exception.", e);
        }
        // In above rule, '*' is not encoded, and ' ' is changed to '+',
        // use %xx to encode them.
        prefsName = prefsName.replace("*", "%2A").replace("+", "%20");
        return prefsName;
    }

    private static void log(String value) {
        if (DBG) Log.d(TAG, value);
    }

}
