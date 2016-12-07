package com.example.liugetdisplaymetrics;

import android.content.Context;
import android.util.DisplayMetrics;
import android.util.Log;

public class GetDisplayMetrics {
	private GetDisplayMetrics() {
	}

	public static void getDisplayMetrics(Context mContext) {
		DisplayMetrics metric=mContext.getResources().getDisplayMetrics();
		int width=metric.widthPixels;//屏幕宽度 
		int height=metric.heightPixels;//屏幕高度 
		float density=metric.density;//屏幕密度(0.75/1.0/1.5/2.0/3.0) 
		int densityDpi=metric.densityDpi;//屏幕密度Dpi(120/160/240/320/480)
		Log.e("屏幕信息", "宽"+width+"高"+height+"屏幕密度"+density+"dpi"+densityDpi);
	}

}
