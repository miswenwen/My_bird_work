/*
 * Copyright (C) 2006 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.settings;
import com.android.settings.dashboard.DashboardSummary;
import com.android.settings.dashboard.DashboardSummaryCommon;
import java.util.ArrayList;
import java.util.List;
import android.app.FragmentManager;
import android.app.FragmentTransaction;

import android.app.Fragment;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.ImageView;
import android.widget.TextView;
import android.view.View.OnClickListener;
import java.lang.reflect.Field;

public class TwoPageFragment extends Fragment {
	private TextView mSwitchLayoutTextView;
	private ViewPager mViewPager;
 	private ImageView cursor;
	private int bmpw = 0; 
	private int offset = 0;
	private int currIndex = 0;
    private View view;
	private TextView comSettingsBtn;
	private TextView allSettingsBtn;
	private SettingsActivity mSettingsActivity;
	private	Fragment f;
	private	Fragment ff;
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		mSettingsActivity=(SettingsActivity)getActivity();
		view = inflater.inflate(R.layout.two_page_frag, container, false);
		mSwitchLayoutTextView=(TextView)view.findViewById(R.id.switch_layout);
		mSwitchLayoutTextView.setOnClickListener(new OnClickListener() {		
			@Override
			public void onClick(View v) {
			FragmentManager frg_mng = getFragmentManager();
			FragmentTransaction ft_a = frg_mng.beginTransaction();
			FragmentTransaction ft_b = frg_mng.beginTransaction();
			ft_a.remove(f).commitAllowingStateLoss();
			ft_b.remove(ff).commitAllowingStateLoss();
			mSettingsActivity.switchLayout();
			}
		});
		mViewPager = (ViewPager) view.findViewById(R.id.viewpager);
		List<Fragment> fragments = new ArrayList<Fragment>();

		f = Fragment.instantiate((SettingsActivity) getActivity(),
				DashboardSummary.class.getName(), savedInstanceState);
		//Fragment ff = Fragment.instantiate((SettingsActivity) getActivity(),
		//		DashboardSummaryCommon.class.getName(), savedInstanceState);
		ff = new FirstFrag();
		fragments.add(ff);
		fragments.add(f);
		FragAdapter adapter = new FragAdapter(getFragmentManager(), fragments);
		initCursorPos();
		mViewPager.setAdapter(adapter);
		mViewPager.setOnPageChangeListener(new MyPageChangeListener());
		comSettingsBtn=(TextView)view.findViewById(R.id.common_settings);
		allSettingsBtn=(TextView)view.findViewById(R.id.all_settings);
		comSettingsBtn.setOnClickListener(new View.OnClickListener() {  
			@Override  
			public void onClick(View v) {
			mViewPager.setCurrentItem(0);  
			}  
		}); 
		allSettingsBtn.setOnClickListener(new View.OnClickListener() {  
			@Override  
			public void onClick(View v) {
			mViewPager.setCurrentItem(1);  
			}  
		}); 
		return view;
	}
	public void initCursorPos() {
		cursor = (ImageView) view.findViewById(R.id.cursor);
		bmpw = BitmapFactory.decodeResource(getResources(), R.drawable.a)
				.getWidth();

		DisplayMetrics dm = new DisplayMetrics();
		((SettingsActivity) getActivity()).getWindowManager().getDefaultDisplay().getMetrics(dm);
		int screenW = dm.widthPixels;
		offset = (screenW / 2 - bmpw) / 2;
		Matrix matrix = new Matrix();
		matrix.postTranslate(offset, 0);
		cursor.setImageMatrix(matrix);
	}
	public class MyPageChangeListener implements OnPageChangeListener {

		int one = offset * 2 + bmpw;
		int two = one * 2;

		@Override
		public void onPageSelected(int arg0) {
			Animation animation = null;
			switch (arg0) {
			case 0:
				if (currIndex == 1) {
					animation = new TranslateAnimation(one, 0, 0, 0);
				}
				break;
			case 1:
				if (currIndex == 0) {
					animation = new TranslateAnimation(offset, one, 0, 0);
				}
				break;
			}
			currIndex = arg0;
			animation.setFillAfter(true);
			animation.setDuration(300);
			cursor.startAnimation(animation);
		}

		@Override
		public void onPageScrolled(int arg0, float arg1, int arg2) {
		}

		@Override
		public void onPageScrollStateChanged(int arg0) {
		}
	}
	public void onDestroyView() {
		// TODO Auto-generated method stub
		super.onDestroyView();
		/*FragmentManager frg_mng = getFragmentManager();
		FragmentTransaction ft_a = frg_mng.beginTransaction();
		FragmentTransaction ft_b = frg_mng.beginTransaction();
		ft_a.remove(f).commitAllowingStateLoss();
		ft_b.remove(ff).commitAllowingStateLoss();*/
	}
/*
	@Override
	public void onDestroyView() {
	// TODO Auto-generated method stub
	super.onDestroyView();
	try {
		   Field childFragmentManager = Fragment.class.getDeclaredField("mChildFragmentManager");
		   childFragmentManager.setAccessible(true);
		   childFragmentManager.set(this, null);


		} catch (NoSuchFieldException e) {
		   throw new RuntimeException(e);
		} catch (IllegalAccessException e) {
		   throw new RuntimeException(e);
		}
}*/
}
