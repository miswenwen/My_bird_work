package com.example.liuapidemos.packagemanagersec;

import android.graphics.drawable.Drawable;

public class PackagerManagerSecAppInfo {
	private String appLabel;    //appLabel 
    private Drawable appIcon ;  //appIcon  
    private String pkgName ;    //packageName  
    private String className ;    //className
    private String sourceDir;//apk sourceDirection
public PackagerManagerSecAppInfo( String appLabel,Drawable appIcon,String pkgName,String className,String sourceDir){
	this.appLabel=appLabel;
	this.appIcon=appIcon;
	this.pkgName=pkgName;
	this.className=className;
	this.sourceDir=sourceDir;
}
public String getAppLabel(){
	return appLabel;
}
public Drawable getAppIcon(){
	return appIcon;
}
public String getPkgName(){
	return pkgName;
}
public String getClassName(){
	return className;
}
public String getSourceDir(){
	return sourceDir;
}
	}


