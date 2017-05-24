package com.bird.music;

import android.os.SystemProperties;
public final class BirdFeatureOption {
	public static final boolean BIRD_MUSIC_SHARE = SystemProperties.getBoolean("ro.bdfun.music_share",false);
	
}
