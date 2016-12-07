/*YUNOS BEGIN*/
//modules(Contacts): [bug 127610 ]for customize engineer mode !
//date 2014-6-13 author : xiongchao.lxc@alibaba-inc.com

package com.yunos.alicontacts.util;

import java.util.ArrayList;
import java.util.List;

import org.xmlpull.v1.XmlPullParser;

import android.util.Log;
import android.util.Xml;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import org.xmlpull.v1.XmlPullParserException;
import com.google.common.io.Closeables;

public class ActionPullParser {
    public static final String CUSTOMER = "customer";
    public static final String COMMANDID = "commandId";
    public static final String PACKAGENAME = "packageName";
    public static final String CLASSNAME = "className";
    public static final String ACTIONNAME = "actionName";
    public static final String SECRETCODE = "secretCode";
    public static final String EXTRANAME = "extraName";
    public static final String EXTRAVALUE = "extraValue";
    public static final String ENGINEERING_ORDER_FILE_PATH = "/system/etc/custom/engineer.xml";

    private static final String TAG = "ActionPullParser";
    private List<Customer> list;

    public List<Customer> getUserList(String xmlFileName) {
        FileInputStream fis = null;
        Customer user = null;
        try {
            File f = new File(xmlFileName);
            if (!f.exists()) {
                Log.i(TAG, "the file:" + xmlFileName + " not exit");
                return null;
            }
            fis = new FileInputStream(f);
            XmlPullParser parser = Xml.newPullParser();
            parser.setInput(fis, "UTF-8");
            int eventType = parser.getEventType();

            if (xmlFileName == ENGINEERING_ORDER_FILE_PATH) {
                while (eventType != XmlPullParser.END_DOCUMENT) {
                    switch (eventType) {
                    case XmlPullParser.START_DOCUMENT:
                        list = new ArrayList<Customer>();
                        break;
                    case XmlPullParser.START_TAG:
                        String name = parser.getName();
                        if (CUSTOMER.equals(name)) {
                            user = new Customer();
                        } else if (user != null) {
                            if (COMMANDID.equals(name)) {
                                user.setcommandId(parser.nextText());
                            } else if (PACKAGENAME.equals(name)) {
                                user.setPackageName(parser.nextText());
                            } else if (CLASSNAME.equals(name)) {
                                user.setClassName(parser.nextText());
                            } else if (ACTIONNAME.equals(name)) {
                                user.setActionName(parser.nextText());
                            } else if (SECRETCODE.equals(name)) {
                                user.setSecretCodeName(parser.nextText());
                            } else if (EXTRANAME.equals(name)) {
                                user.setExtraName(parser.nextText());
                            } else if(EXTRAVALUE.equals(name)){
                                user.setExtraValue(parser.nextText());
                            }
                        }
                        break;
                    case XmlPullParser.END_TAG:
                        if (CUSTOMER.equals(parser.getName())) {
                            list.add(user);
                        }
                        break;
                    }
                    eventType = parser.next();
                }

            } else {
                while (eventType != XmlPullParser.END_DOCUMENT) {
                    switch (eventType) {
                    case XmlPullParser.START_DOCUMENT:
                        list = new ArrayList<Customer>();
                        break;
                    case XmlPullParser.START_TAG:
                        String name = parser.getName();
                        if ("item".equals(name)) {
                            user = new Customer();
                            user.setItem(parser.nextText());
                            list.add(user);
                        }
                        break;
                    case XmlPullParser.END_TAG:
                        if ("item".equals(parser.getName())) {
                        }
                        break;
                    }
                    eventType = parser.next();
                }
            }
        } catch (FileNotFoundException e) {
            Log.e(TAG, "getUserList FileNotFoundException", e);
        } catch (IOException e) {
            Log.e(TAG, "getUserList IOException", e);
        } catch (XmlPullParserException e) {
            Log.e(TAG, "getUserList XmlPullParserException", e);
        } finally {
            Closeables.closeQuietly(fis);
        }

        return list;
    }

    public class Customer {
        private String commandId;
        private String packageName;
        private String className;
        private String actionName;
        private String secretCode;
        private String extraName;
        private String extraValue;
        private String item;

        public String getcommandId() {
            return commandId;
        }

        public void setcommandId(String commandId) {
            this.commandId = commandId.trim();
        }

        public String getPackageName() {
            return packageName;
        }

        public void setPackageName(String packageName) {
            this.packageName = packageName.trim();
        }

        public String getClassName() {
            return className;
        }

        public void setClassName(String className) {
            this.className = className.trim();
        }

        public String getActionName() {
            return actionName;
        }

        public void setActionName(String actionName) {
            this.actionName = actionName.trim();
        }

        public String getSecretCodeName() {
            return secretCode;
        }

        public void setSecretCodeName(String secretCode) {
            this.secretCode = secretCode.trim();
        }

        public String getExtraName() {
            return extraName;
        }

        public void setExtraName(String extraName) {
            this.extraName = extraName.trim();
        }

        public String getExtraValue() {
            return extraValue;
        }

        public void setExtraValue(String extraValue){
            this.extraValue = extraValue.trim();
        }

        public String getItem() {
            return item;
        }

        public void setItem(String item) {
            this.item = item.trim();
        }

    }
}

//Useage : put /system/etc/custom/engineer.xml to the device . Customers edit it by theirselves.
//For example:

//<?xml version="1.0" encoding="UTF-8"?>
//<list>
//	<customer>
//		<commandId>*#33333#</commandId>
//		<secretCode>android_secret_code://99999 </secretCode>
//	</customer>
//</list>

/*YUNOS END*/

