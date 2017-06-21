/* 
 * Author:Wang Lei
 */

package com.bird.accesscontrol;

import com.bird.widget.MyActivity;

import android.os.Bundle;

public class AccessControlActivity extends MyActivity {

	@Override
  protected void onCreate(Bundle savedInstanceState) {
  	if(Pref.PASSWORD_NUMBER_TYPE&&!android.os.SystemProperties.getBoolean("ro.bdui.black_ui_style",false)) setTheme(com.android.internal.R.style.Theme_Holo_Light);//lvhuaiyi add
		super.onCreate(savedInstanceState);

		// Display the fragment as the main content.
		getFragmentManager().beginTransaction().replace(android.R.id.content,
				new AccessControlFragment()).commit();
  }	
}
