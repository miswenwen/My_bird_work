/*
 * Copyright (C) 2015 The Android Open Source Project
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


import java.util.List;
import android.app.Fragment;
import android.app.FragmentManager;
import android.support.v13.app.FragmentPagerAdapter;

public class FragAdapter extends FragmentPagerAdapter {  
	  
    private List<Fragment> mFragments; 
      
    public FragAdapter(FragmentManager fm,List<Fragment> fragments) {  
        super(fm);  
        // TODO Auto-generated constructor stub  
        mFragments=fragments;  
    }  
  
    @Override  
    public Fragment getItem(int arg0) {  
        // TODO Auto-generated method stub  
        return mFragments.get(arg0);  
    }  
  
    @Override  
    public int getCount() {  
        // TODO Auto-generated method stub  
        return mFragments.size();  
    }  
  
}  

 

