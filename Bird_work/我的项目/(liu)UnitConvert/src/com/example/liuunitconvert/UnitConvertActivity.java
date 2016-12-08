package com.example.liuunitconvert;

import java.util.ArrayList;
import java.util.Locale;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.content.Intent;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.support.v4.view.ViewCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.TextView;

public class UnitConvertActivity extends Activity implements OnClickListener {
	private int nowUnitType;// 根据单位类型载入不同的文本
	private final int isFromPreUnitView = 0;
	private final int isFromGoalUnitView = 1;
	private Intent mIntent;
	private ArrayList<String> UnitSet = new ArrayList<String>();// 单位集合
	private ArrayList<String> UnitShortSet = new ArrayList<String>();// 单位的缩写集合
	private ArrayList<String> mPickerData = new ArrayList<String>();// 单位加上单位缩写组成的字符串
	private TextView mTitleView;
	private TextView mPreUnitView;
	private TextView mGoalUnitView;
	private TextView mPreUnitShortView;
	private TextView mGoalUnitShortView;
	private TextView mInputText;
	private TextView mResultText;
	private PickerView mUnitPickerView;

	private ImageButton mSwitchButton;
	// 默认文本单位,在UnitSet，UnitShortSet集合中的
	private final int PRE_LENGTH_DEF = 0;
	private final int GOAL_LENGTH_DEF = 3;
	private final int PRE_AREA_DEF = 0;
	private final int GOAL_AREA_DEF = 5;
	private final int PRE_VOLUME_DEF = 0;
	private final int GOAL_VOLUME_DEF = 2;
	private final int PRE_TEMPERATURE_DEF = 0;
	private final int GOAL_TEMPERATURE_DEF = 1;
	private final int PRE_SPEED_DEF = 2;
	private final int GOAL_SPEED_DEF = 4;
	private final int PRE_TIME_DEF = 4;
	private final int GOAL_TIME_DEF = 5;
	private final int PRE_MASS_DEF = 1;
	private final int GOAL_MASS_DEF = 2;
	private String inputNum = "1";
	// 默认PreUnit,AftUnit;
	private int mPreUnitIndex;
	private int mGoalUnitIndex;
	// 结果
	private String result;
	private Resources mResources;
	private boolean dotNeverClick = true;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.unit_convert);
		mIntent = getIntent();
		nowUnitType = mIntent.getIntExtra("UnitType", 0);
		mResources = getResources();
		setStatusBar();
		init();
	}

	private void setStatusBar() {
		// TODO Auto-generated method stub
		// 首先使 ChildView 不预留空间
		Window window = this.getWindow();
		ViewGroup mContentView = (ViewGroup) findViewById(Window.ID_ANDROID_CONTENT);
		View mChildView = mContentView.getChildAt(0);
		if (mChildView != null) {
			ViewCompat.setFitsSystemWindows(mChildView, false);
		}

		int statusBarHeight = getStatusBarHeight();
		// 需要设置这个 flag 才能设置状态栏
		window.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
		// 避免多次调用该方法时,多次移除了 View
		if (mChildView != null && mChildView.getLayoutParams() != null
				&& mChildView.getLayoutParams().height == statusBarHeight) {
			// 移除假的 View.
			mContentView.removeView(mChildView);
			mChildView = mContentView.getChildAt(0);
		}
		if (mChildView != null) {
			FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) mChildView
					.getLayoutParams();
			// 清除 ChildView 的 marginTop 属性
			if (lp != null && lp.topMargin >= statusBarHeight) {
				lp.topMargin -= statusBarHeight;
				mChildView.setLayoutParams(lp);
			}
		}
	}

	private void init() {
		// 初始化UnitSet，UnitShortSet，mPickerData单位集合
		initUnitSetAndUnitShortSet(nowUnitType);
		for (int i = 0; i < UnitSet.size(); i++) {
			mPickerData.add(UnitSet.get(i) + " " + UnitShortSet.get(i));
		}
		// 获取控件实例
		mTitleView = (TextView) findViewById(R.id.title_tv);
		mPreUnitView = (TextView) findViewById(R.id.pre_unit_tv);
		mGoalUnitView = (TextView) findViewById(R.id.goal_unit_tv);
		mPreUnitShortView = (TextView) findViewById(R.id.pre_unit_short_tv);
		mGoalUnitShortView = (TextView) findViewById(R.id.goal_unit_short_tv);
		mInputText = (TextView) findViewById(R.id.input_num_tv);
		mResultText = (TextView) findViewById(R.id.result_num_tv);
		mUnitPickerView = (PickerView) findViewById(R.id.unit_set_pick);
		mSwitchButton = (ImageButton) findViewById(R.id.switch_btn);
		// 所有TextView设置默认内容， 设置标题，设置默认数，默认pre单位和aft单位，并计算一次。
		mTitleView.setText(getTitleText(nowUnitType));
		if (Locale.getDefault().getLanguage().endsWith("zh")) {
			mPreUnitView.setText(getPreUnitTextDef(nowUnitType));
			mGoalUnitView.setText(getGoalUnitTextDef(nowUnitType));
			mPreUnitShortView.setText(getPreUnitShortTextDef(nowUnitType));
			mGoalUnitShortView.setText(getGoalUnitShortTextDef(nowUnitType));
		} else {
			mPreUnitShortView.setText(getPreUnitTextDef(nowUnitType));
			mGoalUnitShortView.setText(getGoalUnitTextDef(nowUnitType));
			mPreUnitView.setText(getPreUnitShortTextDef(nowUnitType));
			mGoalUnitView.setText(getGoalUnitShortTextDef(nowUnitType));
		}

		mInputText.setText(inputNum);
		setPreUnitIndexAndGoalUnitIndex(nowUnitType);
		result = UnitConvertUtil.computeConvertResult(inputNum, mPreUnitIndex,
				mGoalUnitIndex, nowUnitType);
		mResultText.setText(String.valueOf(result));
		// 初始化后把inputNum设置为空字符串
		inputNum = "";
		// mPreUnitView,mGoalUnitView 前后单位的选择设置监听以及默认值
		mPreUnitView.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				mPreUnitView.setTextColor(mResources
						.getColor(R.color.text_pressed));
				showPickerDialog(isFromPreUnitView);
			}
		});
		mGoalUnitView.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				mGoalUnitView.setTextColor(mResources
						.getColor(R.color.text_pressed));
				showPickerDialog(isFromGoalUnitView);
			}
		});
		mSwitchButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				finish();
			}
		});

		final TypedArray convertButtons = mResources
				.obtainTypedArray(R.array.unit_convert_buttons);
		for (int i = 0; i < convertButtons.length(); i++) {
			ImageButton mButton = (ImageButton) findViewById(convertButtons
					.getResourceId(i, 0));
			mButton.setOnClickListener(this);
		}
		convertButtons.recycle();
	}

	private void setPreUnitIndexAndGoalUnitIndex(int type) {
		// TODO Auto-generated method stub
		switch (type) {
		case UnitConvertUtil.Length:
			mPreUnitIndex = PRE_LENGTH_DEF;
			mGoalUnitIndex = GOAL_LENGTH_DEF;
			break;
		case UnitConvertUtil.Area:
			mPreUnitIndex = PRE_AREA_DEF;
			mGoalUnitIndex = GOAL_AREA_DEF;
			break;

		case UnitConvertUtil.Volume:
			mPreUnitIndex = PRE_VOLUME_DEF;
			mGoalUnitIndex = GOAL_VOLUME_DEF;
			break;

		case UnitConvertUtil.Temperature:
			mPreUnitIndex = PRE_TEMPERATURE_DEF;
			mGoalUnitIndex = GOAL_TEMPERATURE_DEF;
			break;

		case UnitConvertUtil.Speed:
			mPreUnitIndex = PRE_SPEED_DEF;
			mGoalUnitIndex = GOAL_SPEED_DEF;
			break;

		case UnitConvertUtil.Time:
			mPreUnitIndex = PRE_TIME_DEF;
			mGoalUnitIndex = GOAL_TIME_DEF;
			break;

		case UnitConvertUtil.Mass:
			mPreUnitIndex = PRE_MASS_DEF;
			mGoalUnitIndex = GOAL_MASS_DEF;
			break;

		default:
			break;
		}
	}

	private void initUnitSetAndUnitShortSet(int type) {
		// TODO Auto-generated method stub
		switch (type) {
		case UnitConvertUtil.Length:
			// UnitSet初始化
			UnitSet.add(getString(R.string.km_length));
			UnitSet.add(getString(R.string.m_length));
			UnitSet.add(getString(R.string.dm_length));
			UnitSet.add(getString(R.string.cm_length));
			UnitSet.add(getString(R.string.mm_length));
			UnitSet.add(getString(R.string.um_length));
			UnitSet.add(getString(R.string.nm_length));
			UnitSet.add(getString(R.string.pm_length));
			UnitSet.add(getString(R.string.nmi_length));
			UnitSet.add(getString(R.string.mi_length));
			UnitSet.add(getString(R.string.fur_length));
			UnitSet.add(getString(R.string.fm_length));
			UnitSet.add(getString(R.string.yd_length));
			UnitSet.add(getString(R.string.ft_length));
			UnitSet.add(getString(R.string.in_length));
			UnitSet.add(getString(R.string.gongli_length));
			UnitSet.add(getString(R.string.li_length));
			UnitSet.add(getString(R.string.zhang_length));
			UnitSet.add(getString(R.string.chi_length));
			UnitSet.add(getString(R.string.cun_length));
			UnitSet.add(getString(R.string.fen_length));
			UnitSet.add(getString(R.string.li_new_length));
			UnitSet.add(getString(R.string.hao_length));
			UnitSet.add(getString(R.string.pc_length));
			UnitSet.add(getString(R.string.ld_length));
			UnitSet.add(getString(R.string.tianwen_length));
			UnitSet.add(getString(R.string.ly_length));
			// UnitShortSet初始化
			UnitShortSet.add(getString(R.string.km_length_short));
			UnitShortSet.add(getString(R.string.m_length_short));
			UnitShortSet.add(getString(R.string.dm_length_short));
			UnitShortSet.add(getString(R.string.cm_length_short));
			UnitShortSet.add(getString(R.string.mm_length_short));
			UnitShortSet.add(getString(R.string.um_length_short));
			UnitShortSet.add(getString(R.string.nm_length_short));
			UnitShortSet.add(getString(R.string.pm_length_short));
			UnitShortSet.add(getString(R.string.nmi_length_short));
			UnitShortSet.add(getString(R.string.mi_length_short));
			UnitShortSet.add(getString(R.string.fur_length_short));
			UnitShortSet.add(getString(R.string.fm_length_short));
			UnitShortSet.add(getString(R.string.yd_length_short));
			UnitShortSet.add(getString(R.string.ft_length_short));
			UnitShortSet.add(getString(R.string.in_length_short));
			UnitShortSet.add(getString(R.string.gongli_length_short));
			UnitShortSet.add(getString(R.string.li_length_short));
			UnitShortSet.add(getString(R.string.zhang_length_short));
			UnitShortSet.add(getString(R.string.chi_length_short));
			UnitShortSet.add(getString(R.string.cun_length_short));
			UnitShortSet.add(getString(R.string.fen_length_short));
			UnitShortSet.add(getString(R.string.li_new_length_short));
			UnitShortSet.add(getString(R.string.hao_length_short));
			UnitShortSet.add(getString(R.string.pc_length_short));
			UnitShortSet.add(getString(R.string.ld_length_short));
			UnitShortSet.add(getString(R.string.tianwen_length_short));
			UnitShortSet.add(getString(R.string.ly_length_short));
			break;
		case UnitConvertUtil.Area:
			// UnitSet初始化
			UnitSet.add(getString(R.string.km2_area));
			UnitSet.add(getString(R.string.ha_area));
			UnitSet.add(getString(R.string.are_area));
			UnitSet.add(getString(R.string.m2_area));
			UnitSet.add(getString(R.string.dm2_area));
			UnitSet.add(getString(R.string.cm2_area));
			UnitSet.add(getString(R.string.mm2_area));
			UnitSet.add(getString(R.string.um2_area));
			UnitSet.add(getString(R.string.acre_area));
			UnitSet.add(getString(R.string.mile2_area));
			UnitSet.add(getString(R.string.yd2_area));
			UnitSet.add(getString(R.string.ft2_area));
			UnitSet.add(getString(R.string.in2_area));
			UnitSet.add(getString(R.string.rd2_area));
			UnitSet.add(getString(R.string.qing_area));
			UnitSet.add(getString(R.string.mu_area));
			UnitSet.add(getString(R.string.chi2_area));
			UnitSet.add(getString(R.string.cun2_area));
			UnitSet.add(getString(R.string.gongli2_area));
			// UnitShortSet初始化
			UnitShortSet.add(getString(R.string.km2_area_short));
			UnitShortSet.add(getString(R.string.ha_area_short));
			UnitShortSet.add(getString(R.string.are_area_short));
			UnitShortSet.add(getString(R.string.m2_area_short));
			UnitShortSet.add(getString(R.string.dm2_area_short));
			UnitShortSet.add(getString(R.string.cm2_area_short));
			UnitShortSet.add(getString(R.string.mm2_area_short));
			UnitShortSet.add(getString(R.string.um2_area_short));
			UnitShortSet.add(getString(R.string.acre_area_short));
			UnitShortSet.add(getString(R.string.mile2_area_short));
			UnitShortSet.add(getString(R.string.yd2_area_short));
			UnitShortSet.add(getString(R.string.ft2_area_short));
			UnitShortSet.add(getString(R.string.in2_area_short));
			UnitShortSet.add(getString(R.string.rd2_area_short));
			UnitShortSet.add(getString(R.string.qing_area_short));
			UnitShortSet.add(getString(R.string.mu_area_short));
			UnitShortSet.add(getString(R.string.chi2_area_short));
			UnitShortSet.add(getString(R.string.cun2_area_short));
			UnitShortSet.add(getString(R.string.gongli2_area_short));
			break;

		case UnitConvertUtil.Volume:
			// UnitSet初始化
			UnitSet.add(getString(R.string.m3_volume));
			UnitSet.add(getString(R.string.dm3_volume));
			UnitSet.add(getString(R.string.cm3_volume));
			UnitSet.add(getString(R.string.mm3_volume));
			UnitSet.add(getString(R.string.hl3_volume));
			UnitSet.add(getString(R.string.l_volume));
			UnitSet.add(getString(R.string.dl_volume));
			UnitSet.add(getString(R.string.cl_volume));
			UnitSet.add(getString(R.string.ml_volume));
			UnitSet.add(getString(R.string.ft3_volume));
			UnitSet.add(getString(R.string.ln3_volume));
			UnitSet.add(getString(R.string.yd3_volume));
			UnitSet.add(getString(R.string.af3_volume));
			// UnitShortSet初始化
			UnitShortSet.add(getString(R.string.m3_volume_short));
			UnitShortSet.add(getString(R.string.dm3_volume_short));
			UnitShortSet.add(getString(R.string.cm3_volume_short));
			UnitShortSet.add(getString(R.string.mm3_volume_short));
			UnitShortSet.add(getString(R.string.hl3_volume_short));
			UnitShortSet.add(getString(R.string.l_volume_short));
			UnitShortSet.add(getString(R.string.dl_volume_short));
			UnitShortSet.add(getString(R.string.cl_volume_short));
			UnitShortSet.add(getString(R.string.ml_volume_short));
			UnitShortSet.add(getString(R.string.ft3_volume_short));
			UnitShortSet.add(getString(R.string.ln3_volume_short));
			UnitShortSet.add(getString(R.string.yd3_volume_short));
			UnitShortSet.add(getString(R.string.af3_volume_short));
			break;

		case UnitConvertUtil.Temperature:
			// UnitSet初始化
			UnitSet.add(getString(R.string.c_temperature));
			UnitSet.add(getString(R.string.f_temperature));
			UnitSet.add(getString(R.string.k_temperature));
			UnitSet.add(getString(R.string.r_temperature));
			UnitSet.add(getString(R.string.re_temperature));
			// UnitShortSet初始化
			UnitShortSet.add(getString(R.string.c_temperature_short));
			UnitShortSet.add(getString(R.string.f_temperature_short));
			UnitShortSet.add(getString(R.string.k_temperature_short));
			UnitShortSet.add(getString(R.string.r_temperature_short));
			UnitShortSet.add(getString(R.string.re_temperature_short));
			break;

		case UnitConvertUtil.Speed:
			// UnitSet初始化
			UnitSet.add(getString(R.string.c_speed));
			UnitSet.add(getString(R.string.ma_speed));
			UnitSet.add(getString(R.string.m_s_speed));
			UnitSet.add(getString(R.string.km_h_speed));
			UnitSet.add(getString(R.string.km_s_speed));
			UnitSet.add(getString(R.string.kn_speed));
			UnitSet.add(getString(R.string.mph_speed));
			UnitSet.add(getString(R.string.fps_speed));
			UnitSet.add(getString(R.string.ips_speed));
			// UnitShortSet初始化
			UnitShortSet.add(getString(R.string.c_speed_short));
			UnitShortSet.add(getString(R.string.ma_speed_short));
			UnitShortSet.add(getString(R.string.m_s_speed_short));
			UnitShortSet.add(getString(R.string.km_h_speed_short));
			UnitShortSet.add(getString(R.string.km_s_speed_short));
			UnitShortSet.add(getString(R.string.kn_speed_short));
			UnitShortSet.add(getString(R.string.mph_speed_short));
			UnitShortSet.add(getString(R.string.fps_speed_short));
			UnitShortSet.add(getString(R.string.ips_speed_short));
			break;

		case UnitConvertUtil.Time:
			// UnitSet初始化
			UnitSet.add(getString(R.string.yr_time));
			UnitSet.add(getString(R.string.wk_time));
			UnitSet.add(getString(R.string.day_time));
			UnitSet.add(getString(R.string.h_time));
			UnitSet.add(getString(R.string.min_time));
			UnitSet.add(getString(R.string.s_time));
			UnitSet.add(getString(R.string.ms_time));
			UnitSet.add(getString(R.string.us_time));
			UnitSet.add(getString(R.string.ps_time));
			// UnitShortSet初始化
			UnitShortSet.add(getString(R.string.yr_time_short));
			UnitShortSet.add(getString(R.string.wk_time_short));
			UnitShortSet.add(getString(R.string.day_time_short));
			UnitShortSet.add(getString(R.string.h_time_short));
			UnitShortSet.add(getString(R.string.min_time_short));
			UnitShortSet.add(getString(R.string.s_time_short));
			UnitShortSet.add(getString(R.string.ms_time_short));
			UnitShortSet.add(getString(R.string.us_time_short));
			UnitShortSet.add(getString(R.string.ps_time_short));
			break;

		case UnitConvertUtil.Mass:
			// UnitSet初始化
			UnitSet.add(getString(R.string.t_mass));
			UnitSet.add(getString(R.string.kg_mass));
			UnitSet.add(getString(R.string.g_mass));
			UnitSet.add(getString(R.string.mg_mass));
			UnitSet.add(getString(R.string.ug_mass));
			UnitSet.add(getString(R.string.q_mass));
			UnitSet.add(getString(R.string.lb_mass));
			UnitSet.add(getString(R.string.oz_mass));
			UnitSet.add(getString(R.string.ct_mass));
			UnitSet.add(getString(R.string.gr_mass));
			UnitSet.add(getString(R.string.l_t_mass));
			UnitSet.add(getString(R.string.sh_t_mass));
			UnitSet.add(getString(R.string.cwt_uk_mass));
			UnitSet.add(getString(R.string.cwt_us_mass));
			UnitSet.add(getString(R.string.uk_st_mass));
			UnitSet.add(getString(R.string.dr_mass));
			UnitSet.add(getString(R.string.dan_mass));
			UnitSet.add(getString(R.string.jin_mass));
			UnitSet.add(getString(R.string.qian_mass));
			UnitSet.add(getString(R.string.liang_mass));
			// UnitShortSet初始化
			UnitShortSet.add(getString(R.string.t_mass_short));
			UnitShortSet.add(getString(R.string.kg_mass_short));
			UnitShortSet.add(getString(R.string.g_mass_short));
			UnitShortSet.add(getString(R.string.mg_mass_short));
			UnitShortSet.add(getString(R.string.ug_mass_short));
			UnitShortSet.add(getString(R.string.q_mass_short));
			UnitShortSet.add(getString(R.string.lb_mass_short));
			UnitShortSet.add(getString(R.string.oz_mass_short));
			UnitShortSet.add(getString(R.string.ct_mass_short));
			UnitShortSet.add(getString(R.string.gr_mass_short));
			UnitShortSet.add(getString(R.string.l_t_mass_short));
			UnitShortSet.add(getString(R.string.sh_t_mass_short));
			UnitShortSet.add(getString(R.string.cwt_uk_mass_short));
			UnitShortSet.add(getString(R.string.cwt_us_mass_short));
			UnitShortSet.add(getString(R.string.uk_st_mass_short));
			UnitShortSet.add(getString(R.string.dr_mass_short));
			UnitShortSet.add(getString(R.string.dan_mass_short));
			UnitShortSet.add(getString(R.string.jin_mass_short));
			UnitShortSet.add(getString(R.string.qian_mass_short));
			UnitShortSet.add(getString(R.string.liang_mass_short));
			break;

		default:
			break;
		}
	}

	private CharSequence getGoalUnitShortTextDef(int type) {
		// TODO Auto-generated method stub
		String mGoalUnitShortText = null;
		switch (type) {
		case UnitConvertUtil.Length:
			mGoalUnitShortText = UnitShortSet.get(GOAL_LENGTH_DEF);
			break;
		case UnitConvertUtil.Area:
			mGoalUnitShortText = UnitShortSet.get(GOAL_AREA_DEF);
			break;

		case UnitConvertUtil.Volume:
			mGoalUnitShortText = UnitShortSet.get(GOAL_VOLUME_DEF);
			break;

		case UnitConvertUtil.Temperature:
			mGoalUnitShortText = UnitShortSet.get(GOAL_TEMPERATURE_DEF);
			break;

		case UnitConvertUtil.Speed:
			mGoalUnitShortText = UnitShortSet.get(GOAL_SPEED_DEF);
			break;

		case UnitConvertUtil.Time:
			mGoalUnitShortText = UnitShortSet.get(GOAL_TIME_DEF);
			break;

		case UnitConvertUtil.Mass:
			mGoalUnitShortText = UnitShortSet.get(GOAL_MASS_DEF);
			break;

		default:
			break;
		}
		return mGoalUnitShortText;
	}

	private CharSequence getPreUnitShortTextDef(int type) {
		// TODO Auto-generated method stub
		String mPreUnitShort = null;
		switch (type) {
		case UnitConvertUtil.Length:
			mPreUnitShort = UnitShortSet.get(PRE_LENGTH_DEF);
			break;
		case UnitConvertUtil.Area:
			mPreUnitShort = UnitShortSet.get(PRE_AREA_DEF);
			break;

		case UnitConvertUtil.Volume:
			mPreUnitShort = UnitShortSet.get(PRE_VOLUME_DEF);
			break;

		case UnitConvertUtil.Temperature:
			mPreUnitShort = UnitShortSet.get(PRE_TEMPERATURE_DEF);
			break;

		case UnitConvertUtil.Speed:
			mPreUnitShort = UnitShortSet.get(PRE_SPEED_DEF);
			break;

		case UnitConvertUtil.Time:
			mPreUnitShort = UnitShortSet.get(PRE_TIME_DEF);
			break;

		case UnitConvertUtil.Mass:
			mPreUnitShort = UnitShortSet.get(PRE_MASS_DEF);
			break;

		default:
			break;
		}
		return mPreUnitShort;
	}

	private CharSequence getGoalUnitTextDef(int type) {
		// TODO Auto-generated method stub
		String mGoalUnit = null;
		switch (type) {
		case UnitConvertUtil.Length:
			mGoalUnit = UnitSet.get(GOAL_LENGTH_DEF);
			break;
		case UnitConvertUtil.Area:
			mGoalUnit = UnitSet.get(GOAL_AREA_DEF);
			break;

		case UnitConvertUtil.Volume:
			mGoalUnit = UnitSet.get(GOAL_VOLUME_DEF);
			break;

		case UnitConvertUtil.Temperature:
			mGoalUnit = UnitSet.get(GOAL_TEMPERATURE_DEF);
			break;

		case UnitConvertUtil.Speed:
			mGoalUnit = UnitSet.get(GOAL_SPEED_DEF);
			break;

		case UnitConvertUtil.Time:
			mGoalUnit = UnitSet.get(GOAL_TIME_DEF);
			break;

		case UnitConvertUtil.Mass:
			mGoalUnit = UnitSet.get(GOAL_MASS_DEF);
			break;

		default:
			break;
		}
		return mGoalUnit;
	}

	private CharSequence getPreUnitTextDef(int type) {
		// TODO Auto-generated method stub
		String mPreUnit = null;
		switch (type) {
		case UnitConvertUtil.Length:
			mPreUnit = UnitSet.get(PRE_LENGTH_DEF);
			break;
		case UnitConvertUtil.Area:
			mPreUnit = UnitSet.get(PRE_AREA_DEF);
			break;

		case UnitConvertUtil.Volume:
			mPreUnit = UnitSet.get(PRE_VOLUME_DEF);
			break;

		case UnitConvertUtil.Temperature:
			mPreUnit = UnitSet.get(PRE_TEMPERATURE_DEF);
			break;

		case UnitConvertUtil.Speed:
			mPreUnit = UnitSet.get(PRE_SPEED_DEF);
			break;

		case UnitConvertUtil.Time:
			mPreUnit = UnitSet.get(PRE_TIME_DEF);
			break;

		case UnitConvertUtil.Mass:
			mPreUnit = UnitSet.get(PRE_MASS_DEF);
			break;

		default:
			break;
		}
		return mPreUnit;
	}

	private CharSequence getTitleText(int type) {
		// TODO Auto-generated method stub
		String mTitleText = null;
		switch (type) {
		case UnitConvertUtil.Length:
			mTitleText = getString(R.string.length_title);
			break;
		case UnitConvertUtil.Area:
			mTitleText = getString(R.string.area_title);
			break;

		case UnitConvertUtil.Volume:
			mTitleText = getString(R.string.volume_title);
			break;

		case UnitConvertUtil.Temperature:
			mTitleText = getString(R.string.temperature_title);
			break;

		case UnitConvertUtil.Speed:
			mTitleText = getString(R.string.speed_title);
			break;

		case UnitConvertUtil.Time:
			mTitleText = getString(R.string.time_title);
			break;

		case UnitConvertUtil.Mass:
			mTitleText = getString(R.string.mass_title);
			break;

		default:
			break;
		}
		return mTitleText;
	}

	private void showPickerDialog(final int preOrGoal) {
		AlertDialog.Builder dialog = new AlertDialog.Builder(this);
		// 自定义dialog的title，居中。不用setTitle。　因为setTitle默认文字靠左，不能居中。
		TextView title = new TextView(this);
		title.setText(getString(R.string.choose_unit));
		title.setTextSize(24);
		title.setHeight(200);
		title.setGravity(android.view.Gravity.CENTER);
		dialog.setCustomTitle(title);
		LayoutInflater mInflater = LayoutInflater.from(this);
		View mView = mInflater.inflate(R.layout.pick_data_view, null);
		mUnitPickerView = (PickerView) mView.findViewById(R.id.unit_set_pick);
		mUnitPickerView.setData(mPickerData);
		// 设置当前选中位置与mPreUnitView一致
		switch (preOrGoal) {
		case isFromPreUnitView:
			mUnitPickerView.setChecked(mPreUnitIndex);
			break;
		case isFromGoalUnitView:
			mUnitPickerView.setChecked(mGoalUnitIndex);
			break;
		default:
			break;
		}
		dialog.setView(mView);
		dialog.setNegativeButton(getString(R.string.pick_cancel),
				new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						// TODO Auto-generated method stub
					}
				});
		dialog.setPositiveButton(getString(R.string.pick_commit),
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						// TODO Auto-generated method stub
						switch (preOrGoal) {
						case isFromPreUnitView:
							mPreUnitIndex = mUnitPickerView
									.getCurrentPosition();
							if (Locale.getDefault().getLanguage()
									.endsWith("zh")) {
								mPreUnitView.setText(UnitSet.get(mPreUnitIndex));
								mPreUnitShortView.setText(UnitShortSet
										.get(mPreUnitIndex));
							} else {
								mPreUnitShortView.setText(UnitSet
										.get(mPreUnitIndex));
								mPreUnitView.setText(UnitShortSet
										.get(mPreUnitIndex));
							}
							// mPreUnitView.setTextColor(getResources().getColor(
							// R.color.text_def));
							break;
						case isFromGoalUnitView:
							mGoalUnitIndex = mUnitPickerView
									.getCurrentPosition();
							if (Locale.getDefault().getLanguage()
									.endsWith("zh")) {
								mGoalUnitView.setText(UnitSet
										.get(mGoalUnitIndex));
								mGoalUnitShortView.setText(UnitShortSet
										.get(mGoalUnitIndex));
							} else {
								mGoalUnitShortView.setText(UnitSet
										.get(mGoalUnitIndex));
								mGoalUnitView.setText(UnitShortSet
										.get(mGoalUnitIndex));
							}
							// mGoalUnitView.setTextColor(getResources().getColor(
							// R.color.text_def));
							break;
						default:
							break;
						}
						// 进行一次计算
						if (inputNum.equals("")) {
							inputNum = "1";
						}
						result = UnitConvertUtil.computeConvertResult(inputNum,
								mPreUnitIndex, mGoalUnitIndex, nowUnitType);
						mResultText.setText(String.valueOf(result));
					}
				});
		dialog.setOnDismissListener(new OnDismissListener() {

			@Override
			public void onDismiss(DialogInterface dialog) {
				// TODO Auto-generated method stub
				switch (preOrGoal) {
				case isFromPreUnitView:
					mPreUnitView.setTextColor(mResources
							.getColor(R.color.text_def));
					break;
				case isFromGoalUnitView:
					mGoalUnitView.setTextColor(mResources
							.getColor(R.color.text_def));
					break;
				default:
					break;
				}
			}
		});
		dialog.show();
	}

	@Override
	public void onClick(View v) {
		// TODO Auto-generated method stub
		if (inputNum.equals("0")) {
			inputNum = "";
		}
		switch (v.getId()) {
		case R.id.digit_0:
			inputNum = inputNum + "0";
			break;
		case R.id.digit_1:
			inputNum = inputNum + "1";
			break;
		case R.id.digit_2:
			inputNum = inputNum + "2";
			break;
		case R.id.digit_3:
			inputNum = inputNum + "3";
			break;
		case R.id.digit_4:
			inputNum = inputNum + "4";
			break;
		case R.id.digit_5:
			inputNum = inputNum + "5";
			break;
		case R.id.digit_6:
			inputNum = inputNum + "6";
			break;
		case R.id.digit_7:
			inputNum = inputNum + "7";
			break;
		case R.id.digit_8:
			inputNum = inputNum + "8";
			break;

		case R.id.digit_9:
			inputNum = inputNum + "9";
			break;
		case R.id.digit_dot:
			if (inputNum.equals("")) {
				inputNum = "0.";
				dotNeverClick = false;
			} else if (dotNeverClick) {
				inputNum = inputNum + ".";
				dotNeverClick = false;
			}
			break;
		case R.id.func_clear:
			inputNum = "0";
			dotNeverClick = true;
			break;
		case R.id.func_del:
			if (inputNum.length() > 0 && !inputNum.equals("0")) {
				if (inputNum.charAt(inputNum.length() - 1) == '.') {
					dotNeverClick = true;
				}
				inputNum = inputNum.substring(0, inputNum.length() - 1);
				if (inputNum.equals("")) {
					inputNum = "0";
				}
			} else {
				inputNum = "0";
			}
			break;
		default:
			break;
		}
		mInputText.setText(inputNum);
		try {
			result = UnitConvertUtil.computeConvertResult(inputNum,
					mPreUnitIndex, mGoalUnitIndex, nowUnitType);
			mResultText.setText(String.valueOf(result));
		} catch (Exception e) {
			// TODO: handle exception
			mResultText.setText("Error");
		}

	}

	public int getStatusBarHeight() {
		int result = 0;
		int resourceId = getResources().getIdentifier("status_bar_height",
				"dimen", "android");
		if (resourceId > 0) {
			result = getResources().getDimensionPixelSize(resourceId);
		}
		return result;
	}
}
