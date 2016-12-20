package com.example.liuunitconvert;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.content.Intent;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.ViewCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
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
		StatusBarUtil.setStatusBar(this);
		mGridView = (GridView) findViewById(R.id.functionset_grid);
		CustomAdapter mAdapter = new CustomAdapter(FunctionSetActivity.this,
				R.layout.function_item, mList, mGridView);
		init();
		mGridView.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				Intent mIntent = null;
				switch (position) {
				case 0:
					mIntent = new Intent(FunctionSetActivity.this,
							UpperNumActivity.class);
					break;
				case 1:
					Toast.makeText(FunctionSetActivity.this,
							"Not privided now,sry!", 0).show();
					break;
				case 2:
					mIntent = new Intent(FunctionSetActivity.this,
							UnitConvertActivity.class);
					mIntent.putExtra("UnitType", UnitConvertUtil.LENGTH);
					break;
				case 3:
					mIntent = new Intent(FunctionSetActivity.this,
							UnitConvertActivity.class);
					mIntent.putExtra("UnitType", UnitConvertUtil.AREA);
					break;
				case 4:
					mIntent = new Intent(FunctionSetActivity.this,
							UnitConvertActivity.class);
					mIntent.putExtra("UnitType", UnitConvertUtil.VOLUME);
					break;
				case 5:
					mIntent = new Intent(FunctionSetActivity.this,
							UnitConvertActivity.class);
					mIntent.putExtra("UnitType", UnitConvertUtil.TEMPERATURE);
					break;
				case 6:
					mIntent = new Intent(FunctionSetActivity.this,
							UnitConvertActivity.class);
					mIntent.putExtra("UnitType", UnitConvertUtil.SPEED);
					break;
				case 7:
					mIntent = new Intent(FunctionSetActivity.this,
							UnitConvertActivity.class);
					mIntent.putExtra("UnitType", UnitConvertUtil.TIME);
					break;
				case 8:
					mIntent = new Intent(FunctionSetActivity.this,
							UnitConvertActivity.class);
					mIntent.putExtra("UnitType", UnitConvertUtil.MASS);
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

	private void init() {
		// TODO Auto-generated method stub
		String[] convertTexts = getResources().getStringArray(
				R.array.convert_texts);
		TypedArray typedArray = getResources().obtainTypedArray(
				R.array.convert_icons);
		for (int index = 0; index < typedArray.length(); index++) {
			int resId = typedArray.getResourceId(index, 0);
			FunctionItem mFunction = new FunctionItem(convertTexts[index],
					resId);
			mList.add(mFunction);
		}
		typedArray.recycle();
	}

	class CustomAdapter extends ArrayAdapter<FunctionItem> {
		private int resourceId;
		GridView mm;

		public CustomAdapter(Context context, int resource,
				List<FunctionItem> objects, GridView gridView) {
			super(context, resource, objects);
			resourceId = resource;
			mm = gridView;
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
			View mView = (View) view.findViewById(R.id.item_rel);
			ImageView mImageView = (ImageView) view
					.findViewById(R.id.function_iv);
			mTextView.setText(functionName);
			mImageView.setImageResource(pictureSrc);
			Log.e("gridviewheight", "" + mm.getHeight());
			LayoutParams mLayoutParams = mView.getLayoutParams();
			mLayoutParams.height = (int) ((mm.getHeight() - DensityUtil.dip2px(
					FunctionSetActivity.this, 4.0f)) / 3);
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
