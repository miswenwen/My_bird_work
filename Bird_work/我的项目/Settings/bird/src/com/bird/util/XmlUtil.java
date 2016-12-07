package com.bird.util;

import static org.xmlpull.v1.XmlPullParser.END_DOCUMENT;
import static org.xmlpull.v1.XmlPullParser.END_TAG;
import static org.xmlpull.v1.XmlPullParser.START_TAG;

import java.io.File;
import java.io.FileInputStream;
import java.util.HashMap;

import org.xmlpull.v1.XmlPullParser;

import android.content.Context;
import android.util.Log;
import android.util.Xml;

public class XmlUtil {
	public static final String TAG = "XmlUtil";
	public static final String PATH_CUSTOM_FILENAME_CONFIG = "/system/custom/custom_filename_config.xml";
	// static String PATH_CUSTOM_FILENAME_CONFIG = "/mnt/sdcard/custom_filename_config.xml";
	private static final String TAG_FILENAME_MAP = "filename-map";
	private static final String TAG_FILENAME = "filename";
	private static final String ATTR_NAME = "name";
	public static HashMap<String, String> mFileNameMap = new HashMap<String, String>();

	public static HashMap<String, String> parseConfigFile() {
		
		File configFile = new File(PATH_CUSTOM_FILENAME_CONFIG);
		mFileNameMap.clear();
		if (configFile != null && !configFile.exists()) {
			Log.d(TAG, "custom_filename_config.xml file not exit");
			return null;
		}
		FileInputStream infile = null;
		try {
			infile = new FileInputStream(configFile);
			final XmlPullParser parser = Xml.newPullParser();
			parser.setInput(infile, null);

			int type;
			String tag;
			while ((type = parser.next()) != END_DOCUMENT) {
				tag = parser.getName();
				if (type == START_TAG) {
					if (TAG_FILENAME_MAP.equals(tag)) {
						while ((type = parser.next()) != END_DOCUMENT) {
							tag = parser.getName();
							if (TAG_FILENAME.equals(tag)) {
								String name = parser.getAttributeValue(null,
										ATTR_NAME);
								String chineseName = parser.nextText();
								mFileNameMap.put(name, chineseName);
								Log.d(TAG, "name:" + name + ",chineseName:"
										+ chineseName);
							} else if (TAG_FILENAME_MAP.equals(tag)
									&& type == END_TAG) {
								break;
							}
						}

					}
				}
			}
		} catch (Exception e) {
			Log.d(TAG, "custom_filename_config.xml file load failed");
			e.printStackTrace();
		} finally {

		}
		return mFileNameMap;
	}
}
