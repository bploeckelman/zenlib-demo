package com.zendogames.zenlibdemo;

import com.zendogames.zenlib.ZenConfig;

public class Config {
    public static final String windowTitle     = "Zenlib Demo";
    public static final String preferencesName = "zenlib-demo-prefs";

    public static final int windowWidth  = 1280;
    public static final int windowHeight =  720;

    public static final boolean useVsync = true;

    public static ZenConfig zenConfig() {
         return new ZenConfig(preferencesName, windowWidth, windowHeight);
    }
}
