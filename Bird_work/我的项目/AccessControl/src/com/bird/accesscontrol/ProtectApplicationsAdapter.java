/*
 * Author:Wang Lei
 */

package com.bird.accesscontrol;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.ActivityInfo;//lvhuaiyi add
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckedTextView;
import android.widget.ImageView;

import com.bird.widget.MyAdapter;

public class ProtectApplicationsAdapter extends MyAdapter {
	private Context mContext;
	private LayoutInflater mInflater;
	private PackageManager mPackageManager;
	private List<ResolveInfo> mApps;
	private ArrayList<Integer> mP = new ArrayList<Integer>();

	//[96933],filter all lauchers,chengting,@20130916
	private List<ResolveInfo> mLauchers;
	private List<ResolveInfo> getAlllauncher(Context mContext) {
		Intent intent = new Intent(Intent.ACTION_MAIN);
		intent.addCategory(Intent.CATEGORY_HOME);
		return mPackageManager.queryIntentActivities(intent,PackageManager.PERMISSION_GRANTED);
	}
	//[96933],filter all lauchers,chengting,@20130916
	
	private void loadApps()
	{
		int i, j;
		String packageName = null, prePackageName = null, className = null, preClassName = null, iLabel, jLabel;//lvhuaiyi add className = null preClassName = null
		Intent mainintent = new Intent(Intent.ACTION_MAIN);

		mainintent.addCategory(Intent.CATEGORY_LAUNCHER);
		mApps = mPackageManager.queryIntentActivities(mainintent, 0);
		
		mLauchers=getAlllauncher(mContext);//[96933],filter all lauchers,chengting,@20130916

		for (i=0; i<mApps.size(); i++)
		{
			packageName = mApps.get(i).activityInfo.applicationInfo.packageName;
			className = mApps.get(i).activityInfo.name;//lvhuaiyi add
			
			//[96933],filter all lauchers,chengting,@20130916,begin
			boolean isLaucher=false;
			
			for(int tt=0;tt<mLauchers.size();tt++){
				if(packageName.equals(mLauchers.get(tt).activityInfo.applicationInfo.packageName)){
					android.util.Log.d("cting","filter launcher : "+packageName);
					isLaucher=true;
					break;
				}
			}
			/*
			//lvhuaiyi add begin
			for(int tt=0;tt<mLauchers.size();tt++){
				if(className.equals(mLauchers.get(tt).activityInfo.name)){
					isLaucher=true;
					break;
				}
			}			
			//lvhuaiyi add end
			*/
			//[96933],filter all lauchers,chengting,@20130916,end
      /*lvhuayi remove
			if ((i==0 || !packageName.equals(prePackageName)) && !packageName.equals(Data.PACKAGE) 
					&& !isLaucher)//[96933],filter all lauchers,chengting,@20130916
			{
				iLabel = mApps.get(i).activityInfo.applicationInfo.loadLabel(mPackageManager).toString();

				for (j=0; j<mP.size(); j++)
				{
					jLabel = mApps.get(mP.get(j)).activityInfo.applicationInfo.loadLabel(mPackageManager).toString();

					if (Utils.sCollator.compare(iLabel, jLabel)<0)
					{
						mP.add(j, i);
						break;
					}
				}

				if (j==mP.size())
				{
					mP.add(i);
				}
			}
			
			prePackageName = packageName;*/
			//lvhuaiyi add begin
			if ((i==0 || !className.equals(preClassName)) && !className.contains(Data.PACKAGE)&& !isLaucher 
			&& !className.equals("com.nbbsw.theme.ThemeSwitchActivity"))
			{
				iLabel = mApps.get(i).activityInfo.loadLabel(mPackageManager).toString();

				for (j=0; j<mP.size(); j++)
				{
					jLabel = mApps.get(mP.get(j)).activityInfo.loadLabel(mPackageManager).toString();

					if (Utils.sCollator.compare(iLabel, jLabel)<0)
					{
						mP.add(j, i);
						break;
					}
				}

				if (j==mP.size())
				{
					mP.add(i);
				}
			}
			
			preClassName = className;			
			//lvhuaiyi add end
		}
	}

	public ProtectApplicationsAdapter(Context context)
	{
		mContext = context;
		mInflater = LayoutInflater.from(context);
		mPackageManager = context.getPackageManager();
		loadApps();
	}

	@Override
	public int getCount() {
		return mP.size();
	}

	@Override
	public Object getItem(int position) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public long getItemId(int position) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	protected void bindView(int position, View v, ViewGroup parent) {
		//lvhuaiyi remove ApplicationInfo applicationInfo = mApps.get(mP.get(position)).activityInfo.applicationInfo;
		ActivityInfo activityInfo = mApps.get(mP.get(position)).activityInfo;//lvhuaiyi add
		ImageView icon = (ImageView)v.findViewById(android.R.id.icon);
		//lvhuaiyi remove icon.setImageDrawable(applicationInfo.loadIcon(mPackageManager));
		icon.setImageDrawable(activityInfo.loadIcon(mPackageManager));//lvhuayi add
		CheckedTextView text1 = (CheckedTextView)v.findViewById(android.R.id.text1);
		//lvhuaiyi remove text1.setText(applicationInfo.loadLabel(mPackageManager));
		text1.setText(activityInfo.loadLabel(mPackageManager));//lvhuaiyi add
		text1.setChecked(getChecked(position));
	}

	@Override
	protected View newView(int position, ViewGroup parent) {
		View v = null;

		v = mInflater.inflate(R.layout.list_item, null);

		return v;
	}
	
	public boolean getChecked(int position)
	{
		ApplicationInfo applicationInfo = mApps.get(mP.get(position)).activityInfo.applicationInfo;
		return Data.isInAccessControl(mContext, applicationInfo.packageName);
	}
	
	public void setChecked(int position, View v, boolean checked)
	{
		CheckedTextView text1 = (CheckedTextView)v.findViewById(android.R.id.text1);
		String packageName = mApps.get(mP.get(position)).activityInfo.applicationInfo.packageName;
		
		if (checked)
		{
			Data.insertAccessControl(mContext, packageName);
		}
		else
		{
			Data.deleteAccessControl(mContext, packageName);
		}
		
		text1.setChecked(getChecked(position));
	}
	
	public void toggle(int position, View v)
	{
		setChecked(position, v, !getChecked(position));
	}
}