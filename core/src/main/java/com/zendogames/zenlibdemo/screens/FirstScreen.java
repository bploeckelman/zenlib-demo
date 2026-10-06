package com.zendogames.zenlibdemo.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.ScreenUtils;
import com.zendogames.zenlib.ZenGame;
import com.zendogames.zenlib.screens.ZenScreen;
import com.zendogames.zenlibdemo.Main;

/** First screen of the application. Displayed after the application is created. */
public class FirstScreen extends ZenScreen {

    Main main;
    TextureRegion region;

    public FirstScreen(ZenGame game) {
        super(game);
        this.main = (Main) game;
        this.region = new TextureRegion(game.pixel);
    }

    @Override
    public void update(float delta) {
        if (!transitioning && Gdx.input.justTouched()) {
            game.setScreen(new SecondScreen(game));
        }
        super.update(delta);
    }

    @Override
    public void render(float delta) {
        var margin = 50;
        var w = Gdx.graphics.getWidth();
        var h = Gdx.graphics.getHeight();

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