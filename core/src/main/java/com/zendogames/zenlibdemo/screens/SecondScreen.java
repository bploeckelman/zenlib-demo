package com.zendogames.zenlibdemo.screens;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.github.tommyettinger.gdcrux.PointI2;
import com.zendogames.zenlib.Log;
import com.zendogames.zenlib.ZenFlag;
import com.zendogames.zenlib.ZenGame;
import com.zendogames.zenlib.assets.AnimType;
import com.zendogames.zenlib.assets.ZenAnimHero;
import com.zendogames.zenlib.assets.ZenAtlas;
import com.zendogames.zenlib.input.ScreenInputProcessor;
import com.zendogames.zenlib.render.WorldView;
import com.zendogames.zenlib.screens.ZenScreen;
import com.zendogames.zenlib.sim.SimulationStep;
import com.zendogames.zenlibdemo.Main;

public class SecondScreen extends ZenScreen {

    static final int WORLD_WIDTH  = 320;
    static final int WORLD_HEIGHT = 180;

    Main main;
    TextureRegion region;

    SimulationStep sim;
    PointI2 pos;
    float timer;
    int animIndex;
    AnimType anim;

    final float duration = 3f;
    final ZenAnimHero[] anims = ZenAnimHero.values();

    public SecondScreen(ZenGame game) {
        super(game);
        this.main = (Main) game;
        this.region = new TextureRegion(game.pixel);
        this.sim = new SimulationStep();
        this.pos = new PointI2(WORLD_WIDTH / 2, WORLD_HEIGHT / 2);
        this.timer = duration;
        this.animIndex = 0;
        this.anim = anims[animIndex];

        ZenAtlas.SPRITES.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
    }

    @Override
    protected WorldView createWorldView() {
        var view = new WorldView(worldCamera, WORLD_WIDTH, WORLD_HEIGHT);
        view.centerOnResize = true;
        view.clearColor.set(Color.LIGHT_GRAY);
        return view;
    }

    @Override
    protected ScreenInputProcessor createInputProcessor() {
        return new ScreenInputProcessor(this) {
            @Override
            protected boolean onTouchDown(int screenX, int screenY, int pointer, int button) {
                game.setScreen(new EcsScreen(game));
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

    @Override
    public void resize(int width, int height) {
        super.resize(width, height);
        pos.set(worldCamera.viewportWidth / 2, worldCamera.viewportHeight / 2);
    }

    private void stepSimulation(float dt) {
        if (ZenFlag.FRAME_STEP.on()) {
            Log.info("Sim", "stepped 1 frame (manual)");
        }

        timer -= dt;
        if (timer <= 0) {
            timer = duration;
            animIndex = (animIndex + 1) % anims.length;
            anim = anims[animIndex];
            Log.info("Sim", "changed animation to %s", anim.name());
        }
    }

    @Override
    public void update(float delta) {
        sim.update(delta, this::stepSimulation);
        super.update(delta);
    }

    @Override
    public void renderWorld(SpriteBatch batch) {
        var margin = 50 / 4;
        var w = worldView.worldWidth();
        var h = worldView.worldHeight();

        batch.begin();
        {
            batch.setColor(Color.CYAN);
            batch.draw(region, margin, margin, w - 2 * margin, h - 2 * margin);
            batch.setColor(Color.WHITE);

            batch.draw(anim.get().getKeyFrame(timer), pos.x, pos.y);
        }
        batch.end();
    }

    @Override
    public void renderImGui() {
        main.debugUI.render();
    }
}
