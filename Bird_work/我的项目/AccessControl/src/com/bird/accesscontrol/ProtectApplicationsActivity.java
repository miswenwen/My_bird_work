/*
 * Author:Wang Lei
 */

package com.bird.accesscontrol;

import com.bird.widget.MyListActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ListView;

public class ProtectApplicationsActivity extends MyListActivity {
	private ProtectApplicationsAdapter mAdapter;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		if(Pref.PASSWORD_NUMBER_TYPE&&!android.os.SystemProperties.getBoolean("ro.bdui.black_ui_style",false)) setTheme(com.android.internal.R.style.Theme_Holo_Light);//lvhuaiyi add
		super.onCreate(savedInstanceState);

		mAdapter = new ProtectApplicationsAdapter(this);
		setListAdapter(mAdapter);
        if(!Pref.PASSWORD_NUMBER_TYPE)//lvhuaiyi add
		startActivityForResult(new Intent(this, ConfirmLockPattern.class).putExtra(Utils.EXTRA_PREF, Pref.PROTECT_APPLICATIONGS), Utils.REQUEST_CODE_ASK);
        else//lvhuaiyi add
   		startActivityForResult(new Intent(this, ConfirmPassword.class).putExtra(Utils.EXTRA_PREF, Pref.SELECR_PROTECT_APPLICATION), Utils.REQUEST_CONFIRM_PASSWORD); //lvhuaiyi add       	
	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		mAdapter.toggle(position, v);
	}

	protected void onActivityResult(int resquestcode, int resultcode, Intent data)
	{
		if(resquestcode==Utils.REQUEST_CODE_ASK && resultcode==RESULT_OK && data.getStringExtra(Utils.EXTRA_PREF).equals(Pref.PROTECT_APPLICATIONGS))
		{
			finish();
		}
		//lvhuaiyi add begin
		else if(resquestcode==Utils.REQUEST_CONFIRM_PASSWORD && resultcode==RESULT_OK && data.getStringExtra(Utils.EXTRA_PREF).equals(Pref.SELECR_PROTECT_APPLICATION))
		{
			finish();
		}
		//lvhuaiyi add end
	}

	private void SaveIniData()
	{
		stopService(new Intent(this, AccessControlService.class));

		if(Pref.getEnableAccessControl(this) || Pref.getEnableAccessControlPassword(this))//lvhuaiyi add Pref.getEnableAccessControlPassword(this)
		{	    
			startService(new Intent(this, AccessControlService.class)); 
		}
	}
	
	protected void onPause() {
		super.onPause();
		
		SaveIniData();
	}
}