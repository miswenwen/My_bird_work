package com.example.liuunitconvert;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.ViewCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

public class FunctionSetActivity extends FragmentActivity {
	private List<FunctionItem> mList = new ArrayList<FunctionItem>();
	private GridView mGridView;

	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.functionset_choose);
		Window window = this.getWindow();  
		ViewGroup mContentView = (ViewGroup) findViewById(Window.ID_ANDROID_CONTENT);  
		  
		//首先使 ChildView 不预留空间  
		View mChildView = mContentView.getChildAt(0);  
		if (mChildView != null) {  
		    ViewCompat.setFitsSystemWindows(mChildView, false);  
		}  
		  
		int statusBarHeight = getStatusBarHeight();  
		//需要设置这个 flag 才能设置状态栏  
		window.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);  
		//避免多次调用该方法时,多次移除了 View  
		if (mChildView != null && mChildView.getLayoutParams() != null && mChildView.getLayoutParams().height == statusBarHeight) {  
		    //移除假的 View.  
		    mContentView.removeView(mChildView);  
		    mChildView = mContentView.getChildAt(0);  
		}  
		if (mChildView != null) {  
		    FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) mChildView.getLayoutParams();  
		    //清除 ChildView 的 marginTop 属性  
		    if (lp != null && lp.topMargin >= statusBarHeight) {  
		        lp.topMargin -= statusBarHeight;  
		        mChildView.setLayoutParams(lp);  
		    }  
		}  
		mGridView = (GridView) findViewById(R.id.functionset_grid);
		CustomAdapter mAdapter = new CustomAdapter(FunctionSetActivity.this,
				R.layout.function_item, mList);
		init();
		mGridView.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				Intent mIntent = null;
				switch (position) {
				case 0:
					Toast.makeText(FunctionSetActivity.this,
							"Not privided now,sry!", 0).show();
					break;
				case 1:
					Toast.makeText(FunctionSetActivity.this,
							"Not privided now,sry!", 0).show();
					break;
				case 2:
					mIntent = new Intent(FunctionSetActivity.this,
							UnitConvertActivity.class);
					mIntent.putExtra("UnitType", UnitConvertUtil.Length);
					break;
				case 3:
					mIntent = new Intent(FunctionSetActivity.this,
							UnitConvertActivity.class);
					mIntent.putExtra("UnitType", UnitConvertUtil.Area);
					break;
				case 4:
					mIntent = new Intent(FunctionSetActivity.this,
							UnitConvertActivity.class);
					mIntent.putExtra("UnitType", UnitConvertUtil.Volume);
					break;
				case 5:
					mIntent = new Intent(FunctionSetActivity.this,
							UnitConvertActivity.class);
					mIntent.putExtra("UnitType", UnitConvertUtil.Temperature);
					break;
				case 6:
					mIntent = new Intent(FunctionSetActivity.this,
							UnitConvertActivity.class);
					mIntent.putExtra("UnitType", UnitConvertUtil.Speed);
					break;
				case 7:
					mIntent = new Intent(FunctionSetActivity.this,
							UnitConvertActivity.class);
					mIntent.putExtra("UnitType", UnitConvertUtil.Time);
					break;
				case 8:
					mIntent = new Intent(FunctionSetActivity.this,
							UnitConvertActivity.class);
					mIntent.putExtra("UnitType", UnitConvertUtil.Mass);
					break;

				default:
					break;
				}
				if (mIntent != null) {
					startActivity(mIntent);
				}
			}
		});
		mGridView.setAdapter(mAdapter);
	}

	public int getStatusBarHeight() {
	    int result = 0;
	    int resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
	    if (resourceId > 0) {
	        result = getResources().getDimensionPixelSize(resourceId);
	    }
	    return result;
	}

	private void init() {
		// TODO Auto-generated method stub
		FunctionItem mFunction1 = new FunctionItem(
				getString(R.string.capital_text), R.drawable.capital_convert);
		mList.add(mFunction1);
		FunctionItem mFunction2 = new FunctionItem(
				getString(R.string.science_text), R.drawable.science_convert);
		mList.add(mFunction2);
		FunctionItem mFunction3 = new FunctionItem(
				getString(R.string.length_text), R.drawable.length_convert);
		mList.add(mFunction3);
		FunctionItem mFunction4 = new FunctionItem(
				getString(R.string.area_text), R.drawable.area_convert);
		mList.add(mFunction4);
		FunctionItem mFunction5 = new FunctionItem(
				getString(R.string.volume_text), R.drawable.volume_convert);
		mList.add(mFunction5);
		FunctionItem mFunction6 = new FunctionItem(
				getString(R.string.temperature_text),
				R.drawable.temperature_convert);
		mList.add(mFunction6);
		FunctionItem mFunction7 = new FunctionItem(
				getString(R.string.speed_text), R.drawable.speed_convert);
		mList.add(mFunction7);
		FunctionItem mFunction8 = new FunctionItem(
				getString(R.string.time_text), R.drawable.time_convert);
		mList.add(mFunction8);
		FunctionItem mFunction9 = new FunctionItem(
				getString(R.string.mass_text), R.drawable.mass_convert);
		mList.add(mFunction9);
	}

	class CustomAdapter extends ArrayAdapter<FunctionItem> {
		private int resourceId;

		public CustomAdapter(Context context, int resource,
				List<FunctionItem> objects) {
			super(context, resource, objects);
			resourceId = resource;
			// TODO Auto-generated constructor stub
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			// TODO Auto-generated method stub
			String functionName = getItem(position).getFunctionName();
			int pictureSrc = getItem(position).getPictureSrc();
			LayoutInflater mInflater = LayoutInflater.from(getContext());
			View view = mInflater.inflate(resourceId, null);
			TextView mTextView = (TextView) view.findViewById(R.id.function_tv);
			ImageView mImageView = (ImageView) view
					.findViewById(R.id.function_iv);
			mTextView.setText(functionName);
			mImageView.setImageResource(pictureSrc);
			return view;
		}
	}

	class FunctionItem {
		private String functionName;
		private int pictureSrc;

		public FunctionItem(String functionName, int pictureSrc) {
			this.functionName = functionName;
			this.pictureSrc = pictureSrc;
		}

		public String getFunctionName() {
			return functionName;
		}

		public int getPictureSrc() {
			return pictureSrc;
		}
	}

}
