package com.example.liuunitconvert;

import android.app.Activity;
import android.support.v4.view.ViewCompat;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;

public class StatusBarUtil{
	public static void setStatusBar(Activity mActivity) {
		Window window = mActivity.getWindow();
		ViewGroup mContentView = (ViewGroup) mActivity.findViewById(Window.ID_ANDROID_CONTENT);
		View mChildView = mContentView.getChildAt(0);
		if (mChildView != null) {
			ViewCompat.setFitsSystemWindows(mChildView, false);
		}

		int statusBarHeight = getStatusBarHeight(mActivity);
		window.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
		if (mChildView != null && mChildView.getLayoutParams() != null
				&& mChildView.getLayoutParams().height == statusBarHeight) {
			mContentView.removeView(mChildView);
			mChildView = mContentView.getChildAt(0);
		}
		if (mChildView != null) {
			FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) mChildView
					.getLayoutParams();
			if (lp != null && lp.topMargin >= statusBarHeight) {
				lp.topMargin -= statusBarHeight;
				mChildView.setLayoutParams(lp);
			}
		}
	}
	public static int getStatusBarHeight(Activity mActivity) {
		int result = 0;
		int resourceId = mActivity.getResources().getIdentifier("status_bar_height",
				"dimen", "android");
		if (resourceId > 0) {
			result = mActivity.getResources().getDimensionPixelSize(resourceId);
		}
		return result;

	}
}
