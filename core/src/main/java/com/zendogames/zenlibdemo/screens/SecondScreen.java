package com.zendogames.zenlibdemo.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.ScreenUtils;
import com.zendogames.zenlib.FramePool;
import com.zendogames.zenlib.Log;
import com.zendogames.zenlib.ZenFlag;
import com.zendogames.zenlib.ZenGame;
import com.zendogames.zenlib.input.ScreenInputProcessor;
import com.zendogames.zenlib.screens.ZenScreen;
import com.zendogames.zenlib.sim.SimulationStep;
import com.zendogames.zenlibdemo.Main;

public class SecondScreen extends ZenScreen {

    Main main;
    TextureRegion region;
    SimulationStep sim;

    static final int poolPeak = 1000;

    public SecondScreen(ZenGame game) {
        super(game);
        this.main = (Main) game;
        this.region = new TextureRegion(game.pixel);
        this.sim = new SimulationStep();
    }

    @Override
    protected ScreenInputProcessor createInputProcessor() {
        return new ScreenInputProcessor(this) {
            @Override
            protected boolean onTouchDown(int screenX, int screenY, int pointer, int button) {
                game.setScreen(new FirstScreen(game));
                return true;
            }

            /** Supplements built-in debug keys, allows use of game/screen specific code and data. */
            @Override
            protected boolean debugKey(int keycode) {
                if (keycode == Input.Keys.NUM_9) {
                    sim.requestStep();
                    return true;
                }
                return super.debugKey(keycode);
            }
        };
    }

    private final int ticksUntilLog = 1000;
    private int ticks = ticksUntilLog;
    private void stepSimulation(float dt) {
        if (ZenFlag.FRAME_STEP.on()) Log.info("Sim", "stepped 1 frame (manual)");
        if (ticks <= 0) {
            ticks = ticksUntilLog;
            Log.info("Sim", "stepped %d frames", ticksUntilLog);
            return;
        }
        ticks--;
    }

    @Override
    public void update(float delta) {
        var numTemps = MathUtils.random(1, poolPeak);
        for (int i = 0; i < numTemps; i++) {
            FramePool.circle(0, 0, 0);
        }

        sim.update(delta, this::stepSimulation);

        super.update(delta);
    }

    @Override
    public void render(float delta) {
        var margin = 50;
        var w = Gdx.graphics.getWidth();
        var h = Gdx.graphics.getHeight();

        ScreenUtils.clear(Color.LIGHT_GRAY);
        batch.begin();
        batch.setColor(Color.CYAN);
        batch.draw(region, margin, margin, w - 2*margin, h - 2*margin);
        batch.setColor(Color.WHITE);
        batch.end();
    }

    @Override
    public void renderImGui() {
        main.debugUI.render();
    }
}
