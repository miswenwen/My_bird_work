package com.yunos.alicontacts.weibo;

public interface WeiboFinals {
    /**
     * The scheme of weibo client.
     */
    public static final String CLIENT_SCHEME = "sinaweibo";
    /**
     * The weibo uri of weibo client. Use to launch weibo client to view weibo
     * profile.
     */
    public static final String CLIENT_VIEW_PROFILE_URI_1 = "sinaweibo://userinfo?nick=%1$s";
    public static final String CLIENT_VIEW_PROFILE_URI_2 = "sinaweibo://userinfo?uid=%1$s&nick=%2$s";
    /**
     * The weibo URL to browser weibo profile.
     */
    public static final String BROWSER_PROFILE_URL_ID = "http://m.weibo.cn/u/%s";
    public static final String BROWSER_PROFILE_URL_NAME = "http://m.weibo.cn/n/%s";
    public static final String BROWSER_PROFILE_URL_DEFAULT = "http://m.weibo.cn";
    /**
     * The extra key to keep weibo contact profile uid
     */
    public static final String EXTRA_KEY_WEIBO_PROFILE_UID = "extra_key_weibo_profile_uid";
    /**
     * The extra key to keep weibo contact profile name
     */
    public static final String EXTRA_KEY_WEIBO_PROFILE_NAME = "extra_key_weibo_profile_name";

    public static final String PLUGIN_PACKAGE_NAME = "com.yunos.alicontacts.weibo";
    public static final String PLUGIN_ACTIVITY_NAME = "com.yunos.alicontacts.weibo.WeiboRedirectActivity";
    public static final String PREFERENCES_NAME = "com_weibo_sdk_android";
    public static final String PLUGIN_WEIBO_LOGINED = "weibo_login";
    public static final String PLUGIN_WEIBO_SHOW_PHOTO = "is_show_photo";

    public static final String EXTRA_KEY_ALERT_DIALOG = "extra_key_alert_dialog";
}
