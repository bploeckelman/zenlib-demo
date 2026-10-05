package com.zendogames.zenlibdemo;

import com.zendogames.zenlib.ZenConfig;
import com.zendogames.zenlib.ZenGame;
import com.zendogames.zenlib.screens.ZenScreen;

/** {@link com.badlogic.gdx.ApplicationListener} implementation shared by all platforms. */
public class Main extends ZenGame {

    public static Main game;

    public Main() {
        game = this;
    }

    @Override public ZenConfig config() {
        return Config.zenConfig();
    }

    @Override
    protected ZenScreen createFirstScreen() {
        return new FirstScreen(this);
    }
}