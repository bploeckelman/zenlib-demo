package com.zendogames.zenlibdemo.screens;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.ScreenUtils;
import com.zendogames.zenlib.FramePool;
import com.zendogames.zenlib.ZenGame;
import com.zendogames.zenlib.input.ScreenInputProcessor;
import com.zendogames.zenlib.render.WorldView;
import com.zendogames.zenlib.screens.ZenScreen;
import com.zendogames.zenlibdemo.Main;

/** First screen of the application. Displayed after the application is created. */
public class FirstScreen extends ZenScreen {

    Main main;
    TextureRegion region;

    static final int poolPeak = 1000;

    public FirstScreen(ZenGame game) {
        super(game);
        this.main = (Main) game;
        this.region = new TextureRegion(game.pixel);
    }

    @Override
    protected WorldView createWorldView() {
        var view = super.createWorldView();
        view.centerOnResize = true;
        view.clearColor.set(Color.LIGHT_GRAY);
        return view;
    }

    @Override
    protected ScreenInputProcessor createInputProcessor() {
        return new ScreenInputProcessor(this) {
            @Override
            protected boolean onTouchDown(int screenX, int screenY, int pointer, int button) {
                game.setScreen(new SecondScreen(game));
                return true;
            }
        };
    }

    @Override
    public void update(float delta) {
        var numTemps = MathUtils.random(1, poolPeak);
        for (int i = 0; i < numTemps; i++) {
            FramePool.vec2(0, 0);
        }

        super.update(delta);
    }

    @Override
    public void render(float delta) {
        var margin = 50;
        var w = worldView.worldWidth();
        var h = worldView.worldHeight();

        ScreenUtils.clear(Color.LIGHT_GRAY);
        batch.begin();
        batch.setColor(Color.FOREST);
        batch.draw(region, margin, margin, w - 2*margin, h - 2*margin);
        batch.setColor(Color.WHITE);
        batch.end();
    }

    @Override
    public void renderImGui() {
        main.debugUI.render();
    }
}