package com.example.liuapidemos.packagemanagersec;

import java.util.List;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import com.example.liuapidemos.R;

public class PackagerManagerSecAdapter extends ArrayAdapter<PackagerManagerSecAppInfo>{
	private int resourceId;
	public PackagerManagerSecAdapter(Context context, int textViewResourceId,
	List<PackagerManagerSecAppInfo> objects) {
	super(context, textViewResourceId, objects);
	resourceId = textViewResourceId;
	}
	public View getView(int position, View convertView, ViewGroup parent) {
		PackagerManagerSecAppInfo mAppInfo = getItem(position);
		View view = LayoutInflater.from(getContext()).inflate(resourceId, null);
		ImageView appIcon = (ImageView) view.findViewById(R.id.imageView1);
		TextView appLabel = (TextView) view.findViewById(R.id.textView1);
		TextView pkgName = (TextView) view.findViewById(R.id.textView2);
		TextView className = (TextView) view.findViewById(R.id.textView3);
		TextView sourceDir = (TextView) view.findViewById(R.id.textView4);
		appIcon.setImageDrawable(mAppInfo.getAppIcon());
		appLabel.setText(mAppInfo.getAppLabel());
		pkgName.setText(mAppInfo.getPkgName());
		className.setText(mAppInfo.getClassName());
		sourceDir.setText(mAppInfo.getSourceDir());
		return view;
		}


}
