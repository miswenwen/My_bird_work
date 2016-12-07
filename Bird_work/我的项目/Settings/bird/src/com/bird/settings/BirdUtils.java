package com.bird.settings;

import android.content.ContentResolver;
import android.content.Context;
import android.provider.Settings;

import android.text.TextUtils;
import android.util.Log;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.io.InputStream;
import android.os.SystemProperties;
import com.android.settings.R;
// @{ BIRD_SYSTEM_SPACE, add by shenzhiwang, @20160707
import java.util.regex.Pattern;
import java.util.regex.Matcher;
// @}

public class BirdUtils {
	private static final String TAG = "BirdUtils";
	
	private static String fetch(String prop){
		String src=SystemProperties.get(prop);
		return TextUtils.isEmpty(src)?null:src.replace("__", " ");
	}
	//kernel version
	public static String getKernelVersion(){
    	return fetch("ro.bdmisc.kernel_version");
	}
	public static String getBaseBandVersion(){
		return fetch("ro.bdmisc.baseband_version");
	}
	public static String getBuildNumber(){
		return fetch("ro.bdmisc.build_number");
	}

// @{ BIRD_SYSTEM_SPACE, add by shenzhiwang, @20160707
	public static float getFloatById(Context context, int id) {
		String strSize = context.getString(id);
		Pattern p = Pattern.compile("[0-9\\.]+");
		Matcher m = p.matcher(strSize);
		if(m.find()) {
		    return Float.valueOf(m.group());
		} else {
		    return 0.0f;
		}
	}
// @}

//[103061], add start by shenzhiwang, 20140603
    public static String getCupNameCustom(Context context) {
        String cpuName = SystemProperties.get("ro.bird.cpu_model", 
            context.getString(R.string.zzz_bird_cpu_model_summary)).replace("__", " ");
        if(cpuName.contains("Quad")) {
            cpuName = cpuName.replace("Quad", context.getString(R.string.zzz_bird_cpu_model_Quad));
        } else if(cpuName.contains("Octuple")) {
            cpuName = cpuName.replace("Octuple", context.getString(R.string.zzz_bird_cpu_model_Octuple));
        } else if(cpuName.equals("TrueName")) {
            cpuName = getCpuName(context);
        }
        return cpuName;
    }

	private static String getCpuName(Context context) {
        String cpu_name = Settings.System.getString(context.getContentResolver(), "ro.bird.cpu_name");
        if(cpu_name == null) {
            try {
                FileReader fr = new FileReader("/proc/cpuinfo");
                BufferedReader br = new BufferedReader(fr);
                String text = br.readLine();
                String[] array = text.split(":");
                br.close();
                cpu_name = array[1];
            } catch (FileNotFoundException e) {
                Log.e(TAG, "FileNotFoundException:" + e);
            } catch (IOException e) {
                Log.e(TAG, "IOException:" + e);
            }
            Settings.System.putString(context.getContentResolver(), "ro.bird.cpu_name", cpu_name);
        }

        return cpu_name;
    }

    public static String getMaxCpuFreqCustom(Context context) {
        String cpuFreq = SystemProperties.get("ro.bird.max_cpu_freq", 
            context.getString(R.string.zzz_bird_cpu_frequency_summary));
        String [] cpuFreqs = cpuFreq.split("__");
        if(cpuFreqs.length>=2) {
            cpuFreq = cpuFreqs[1];
        }
        else {
            cpuFreq = getMaxCpuFreq(context);
        }
		String strGHz = context.getString(R.string.filter_GHz);
        cpuFreq = cpuFreq.replace("GHz", strGHz);
        return cpuFreq;
    }

    private static String getMaxCpuFreq(Context context) {
        String result = "";
        ProcessBuilder cmd;

        String max_cpu_freq = Settings.System.getString(context.getContentResolver(), "ro.bird.max_cpu_freq");
        if(max_cpu_freq == null) {
            try {
                String[] args = {"system/bin/cat", "sys/devices/system/cpu/cpu0/cpufreq/cpuinfo_max_freq"};
                cmd = new ProcessBuilder(args);
                Process process = cmd.start();
                InputStream in = process.getInputStream();
                byte[] re = new byte[24];
                while(in.read(re) != -1) {
                    result = result + new String(re);
                }
                in.close();
            } catch (IOException e) {
                return result = "N/A";
            }

            Integer freq = Integer.decode(result.trim());
            double freqGHz = freq/1000000.0;

			String strGHz = context.getString(R.string.filter_GHz);
            max_cpu_freq = String.valueOf(freqGHz).substring(0, 3) + strGHz;
            Settings.System.putString(context.getContentResolver(), "ro.bird.max_cpu_freq", max_cpu_freq);
        }
        return max_cpu_freq;
    }
	
	public static String getMaxCpuCoreCustom(Context context) {
		String cpuFreq = SystemProperties.get("ro.bird.max_cpu_freq", 
        		context.getString(R.string.zzz_bird_cpu_frequency_summary));
		String [] cpuFreqs = cpuFreq.split("__");
        cpuFreq = cpuFreqs[0];
        if(cpuFreq.contains("Quad core")) {
            cpuFreq = cpuFreq.replace("Quad core", context.getString(R.string.zzz_bird_cpu_model_Quad));
        } else if(cpuFreq.contains("Octuple core")) {
            cpuFreq = cpuFreq.replace("Octuple core", context.getString(R.string.zzz_bird_cpu_model_Octuple));
        }
        return cpuFreq;
	}
	
}
