package com.yunos.alicontacts.plugins;

import com.yunos.alicontacts.R;

public class PluginBean {
	private String packageName;
	private String detailAction;
	private String settingAction;
	private int iconId;
	private boolean isFullScreen;
	private boolean isLayout;

	public PluginBean() {
		packageName = new String();
		detailAction = new String();
		settingAction = new String();
		isFullScreen = false;
		isLayout = false;
		iconId = R.drawable.btn_star_on_normal_holo_light;
	}

	public String getPackageName() {
		return packageName;
	}

	public void setPackageName(String packageName) {
		this.packageName = packageName;
	}

	public String getDetailAction() {
		return detailAction;
	}

	public void setDetailAction(String action) {
		this.detailAction = action;
	}

	public String getSettingAction() {
		return settingAction;
	}

	public void setSettingAction(String action) {
		this.settingAction = action;
	}

	public int getIconId() {
		return iconId;
	}

	public void setIconId(int iconId) {
		this.iconId = iconId;
	}

	public boolean isFullScreen() {
		return isFullScreen;
	}

	public void setFullScreen(boolean isFullScreen) {
		this.isFullScreen = isFullScreen;
	}
	
	public boolean isLayout() {
		return isLayout;
	}

	public void setLayout(boolean isLayout) {
		this.isLayout = isLayout;
	}

}
