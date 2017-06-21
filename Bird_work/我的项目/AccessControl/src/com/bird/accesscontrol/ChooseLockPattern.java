/*
 * Author:Wang Lei
 */

package com.bird.accesscontrol;

import java.util.Arrays;

import com.bird.ninekeylock.NineKeyLockListener;
import com.bird.ninekeylock.NineKeyLockView;
import com.bird.widget.MyActivity;

import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

public class ChooseLockPattern extends MyActivity implements NineKeyLockListener, OnClickListener {
	private TextView headerText;
	private NineKeyLockView lockPattern;
	private Button footerLeftButton;
	private Button footerRightButton;
	private byte[] password = null;
	
	private void cancel()
	{
		setResult(RESULT_OK, getIntent());
		finish();
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.choose_lock_pattern);
		
		headerText = (TextView)findViewById(R.id.headerText);
		lockPattern = (NineKeyLockView)findViewById(R.id.lockPattern);
		lockPattern.setListener(this);
		footerLeftButton = (Button)findViewById(R.id.footerLeftButton);
		footerLeftButton.setOnClickListener(this);
		footerLeftButton.setTag(android.R.string.cancel);
		footerRightButton = (Button)findViewById(R.id.footerRightButton);
		footerRightButton.setOnClickListener(this);
		footerRightButton.setEnabled(false);
		footerRightButton.setTag(R.string.lockpattern_continue_button_text);
	}

	@Override
	public void beforeInput(NineKeyLockView nineKeyLockView) {
		headerText.setText(R.string.lockpattern_recording_inprogress);
		footerLeftButton.setEnabled(false);
		footerRightButton.setEnabled(false);
	}

	@Override
	public void afterInput(NineKeyLockView nineKeyLockView) {
		boolean isChecked = false;
		byte[] input = nineKeyLockView.getPassword();

		if (password == null)
		{
			isChecked = nineKeyLockView.getPatternSize()>=Utils.MIN_LOCK_PATTERN_SIZE;
		}
		else
		{
			isChecked = Arrays.equals(password, input);
		}

		nineKeyLockView.checkPassed(isChecked);
	}

	@Override
	public void beforeCheck(NineKeyLockView nineKeyLockView, boolean passed) {
		if (password == null)
		{
			if (passed)
			{
				headerText.setText(R.string.lockpattern_pattern_entered_header);
				footerRightButton.setEnabled(true);
			}
			else
			{
				headerText.setText(getString(R.string.lockpattern_recording_incorrect_too_short, Utils.MIN_LOCK_PATTERN_SIZE));
			}
			
			footerLeftButton.setText(R.string.lockpattern_retry_button_text);
			footerLeftButton.setTag(R.string.lockpattern_retry_button_text);
			footerLeftButton.setEnabled(true);
		}
		else
		{
			if (passed)
			{
				headerText.setText(R.string.lockpattern_pattern_confirmed_header);
				footerRightButton.setEnabled(true);
			}
			else
			{
				headerText.setText(R.string.lockpattern_need_to_unlock_wrong);
			}
			
			footerLeftButton.setEnabled(true);
		}
	}

	@Override
	public void afterCheck(NineKeyLockView nineKeyLockView, boolean passed) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onClick(View v) {
		int tag;

		switch (v.getId())
		{
			case R.id.footerLeftButton:
				tag = (Integer)v.getTag();
				
				switch (tag)
				{
					case android.R.string.cancel:
						cancel();
						break;
						
					case R.string.lockpattern_retry_button_text:
						headerText.setText(R.string.lockpassword_choose_your_pattern_sub_title);
						footerLeftButton.setText(android.R.string.cancel);
						footerLeftButton.setTag(android.R.string.cancel);
						footerRightButton.setEnabled(false);
						lockPattern.reset();
						break;

					default:
						break;
				}
				break;

			case R.id.footerRightButton:
				tag = (Integer)v.getTag();
				
				switch (tag)
				{
					case R.string.lockpattern_continue_button_text:
						headerText.setText(R.string.lockpattern_need_to_confirm);
						footerLeftButton.setText(android.R.string.cancel);
						footerLeftButton.setTag(android.R.string.cancel);
						footerRightButton.setText(android.R.string.ok);
						footerRightButton.setTag(android.R.string.ok);
						footerRightButton.setEnabled(false);
						password = lockPattern.getPassword();
						lockPattern.reset();
						break;
						
					case android.R.string.ok:
						Pref.setPassword(this, password);
						finish();
						break;

					default:
						break;
				}
				break;

			default:
				break;
		}
	}

	public void onBackPressed()
	{
		cancel();
	}

	public boolean onKeyDown(int keyCode, KeyEvent event)
	{
		boolean result = false;

		if (keyCode == KeyEvent.KEYCODE_BACK)
		{
			cancel();
			result = true;
		}

		if (!result)
		{
			result = super.onKeyDown(keyCode, event);
		}

		return result;
	}

	@Override
	public void onAddHit(NineKeyLockView nineKeyLockView) {
		// TODO Auto-generated method stub
		
	}
}