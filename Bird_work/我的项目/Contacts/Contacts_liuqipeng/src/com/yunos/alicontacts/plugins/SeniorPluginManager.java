package com.yunos.alicontacts.plugins;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;

public class SeniorPluginManager {
    private static final List<SeniorPluginBean> seniors = new ArrayList<SeniorPluginBean>();
    private static SeniorPluginManager _instance = null;

//    static {
//        SeniorPluginBean plugin = new AlipayPlugin();
//        plugin.setPackageName(AlipayPlugin.alipayApp);
//
//        seniors.add(plugin);
//    }

    public static SeniorPluginManager getInstance() {
        if (_instance == null) {
            _instance = new SeniorPluginManager();
        }
        return _instance;
    }

    public boolean contains(String packageName) {
        for (SeniorPluginBean plugin : seniors) {
            if (plugin.getPackageName().equals(packageName))
                return true;
        }
        return false;
    }

    public void addSeniorPlugin(SeniorPluginBean plugin) {
        seniors.add(plugin);
    }

    public List<SeniorPluginBean> getSeniorPlugins(Context context) {
        return seniors;
    }
}
