package com.zendogames.zenlibdemo;

import com.zendogames.zenlib.ZenConfig;
import com.zendogames.zenlib.ZenFlag;
import com.zendogames.zenlib.ZenGame;
import com.zendogames.zenlib.imgui.ImGuiGdx;
import com.zendogames.zenlib.screens.ZenScreen;
import com.zendogames.zenlibdemo.screens.FirstScreen;
import com.zendogames.zenlibdemo.ui.DebugUI;

/** {@link com.badlogic.gdx.ApplicationListener} implementation shared by all platforms. */
public class Main extends ZenGame {

    public static Main game;

    public final DebugUI debugUI;

    public Main() {
        Main.game = this;
        this.debugUI = new DebugUI();
    }

    @Override
    protected void init() {
        ZenFlag.LOG_DEBUG.enable();
        ZenFlag.LOG_INPUT.enable();
        ZenFlag.DEBUG_FRAME_POOL.enable();
    }

    @Override public ZenConfig config() {
        return Config.zenConfig();
    }

    @Override
    protected ZenScreen createFirstScreen() {
        return new FirstScreen(this);
    }

    @Override
    protected void update() {
        super.update();
        if (ImGuiGdx.isInitialized() && ZenFlag.SHOW_IMGUI.off()) {
            ZenFlag.SHOW_IMGUI.enable();
        }
    }
}