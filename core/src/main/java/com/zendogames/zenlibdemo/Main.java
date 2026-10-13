package com.zendogames.zenlibdemo;

import com.zendogames.zenlib.ZenConfig;
import com.zendogames.zenlib.ZenFlag;
import com.zendogames.zenlib.ZenGame;
import com.zendogames.zenlib.imgui.ImGuiGdx;
import com.zendogames.zenlib.screens.ZenScreen;
import com.zendogames.zenlib.ui.ScreenSwitcher;
import com.zendogames.zenlibdemo.assets.GameMap;
import com.zendogames.zenlibdemo.screens.*;

/** {@link com.badlogic.gdx.ApplicationListener} implementation shared by all platforms. */
public class Main extends ZenGame {

    public static Main game;

    public Main() {
        Main.game = this;
    }

    @Override
    protected void init() {
        //ZenFlag.LOG_DEBUG.enable();
        //ZenFlag.LOG_INPUT.enable();
        ZenFlag.DEBUG_FRAME_POOL.enable();
        assets.register(GameMap.values());
        debugUI.always(new ScreenSwitcher(this)
                .add("Sprites",   SpriteScreen.class,    SpriteScreen::new)
                .add("Collision", CollisionScreen.class, CollisionScreen::new)
                .add("Movement",  MovementScreen.class,  MovementScreen::new)
                .add("Enemy",     EnemyScreen.class,     EnemyScreen::new)
                .add("Tilemap",   TilemapScreen.class,   TilemapScreen::new));
    }

    @Override public ZenConfig config() {
        return Config.zenConfig();
    }

    @Override
    protected ZenScreen createFirstScreen() {
        return new SpriteScreen(this);
    }

    @Override
    protected void update() {
        super.update();
        if (ImGuiGdx.isInitialized() && ZenFlag.SHOW_IMGUI.off()) {
            ZenFlag.SHOW_IMGUI.enable();
        }
    }
}