package com.zendogames.zenlibdemo.screens;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.zendogames.zenlib.Log;
import com.zendogames.zenlib.Time;
import com.zendogames.zenlib.ZenGame;
import com.zendogames.zenlib.assets.ZenAtlas;
import com.zendogames.zenlib.ecs.World;
import com.zendogames.zenlib.input.ScreenInputProcessor;
import com.zendogames.zenlib.render.WorldView;
import com.zendogames.zenlib.screens.ZenScreen;
import com.zendogames.zenlibdemo.Main;
import com.zendogames.zenlibdemo.ecs.Factory;
import com.zendogames.zenlibdemo.ecs.Tags;
import com.zendogames.zenlibdemo.ecs.components.Position;
import com.zendogames.zenlibdemo.ecs.systems.*;

/**
 * Heroes bouncing around a 320x180 pixel-art world, driven by a {@link World}.
 * <pre>
 *   click   spawn a hero at the pointer (through worldView.unproject)
 *   H       tag every entity HURT: HurtSystem stops it, plays HURT, despawns after 1 s
 *   C       world.clear(): SpawnSystem refills next tick, ids start at 0 again
 *   T       transition to a fresh EcsScreen: two worlds alive and updating at once
 *   B       back to FirstScreen
 *   R       toggle RenderSystem.enabled
 *   9       one tick while FRAME_STEP (0) is on
 *   6       RENDER_FBO, 5 imgui, as everywhere
 * </pre>
 */
public class EcsScreen extends ZenScreen {

    private static final String TAG = "EcsScreen";

    static final int WORLD_WIDTH   = 320;
    static final int WORLD_HEIGHT  = 180;
    static final int TARGET_HEROES = 20;

    final Main main;
    final World world;
    final Factory factory;
    final TextureRegion region;

    public EcsScreen(ZenGame game) {
        super(game);
        this.main = (Main) game;
        this.region = new TextureRegion(game.pixel);
        this.world = new World();
        this.factory = new Factory(world);

        world.addSystem(new SpawnSystem(factory, TARGET_HEROES, WORLD_WIDTH, WORLD_HEIGHT));
        world.addSystem(new HurtSystem());
        world.addSystem(new LifetimeSystem());
        world.addSystem(new MovementSystem(WORLD_WIDTH, WORLD_HEIGHT));
        world.addSystem(new AnimationSystem());
        world.addSystem(new RenderSystem(batch));

        // One spawn in setup code, so the Factory's family hook logs the mid-construction join
        factory.hero(WORLD_WIDTH / 2, WORLD_HEIGHT / 2);

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
                factory.hero((int) this.pointer.x, (int) this.pointer.y);
                return true;
            }

            @Override
            protected boolean onKeyDown(int keycode) {
                switch (keycode) {
                    case Input.Keys.H -> hurtEveryone();
                    case Input.Keys.C -> world.clear();
                    case Input.Keys.T -> game.setScreen(new EcsScreen(game));
                    case Input.Keys.B -> game.setScreen(new FirstScreen(game));
                    case Input.Keys.R -> {
                        var render = world.getSystem(RenderSystem.class);
                        render.enabled = !render.enabled;
                    }
                    default -> { return false; }
                }
                return true;
            }

            @Override
            protected boolean debugKey(int keycode) {
                if (keycode == Input.Keys.NUM_9) {
                    world.step.requestStep();
                    return true;
                }
                return super.debugKey(keycode);
            }
        };
    }

    private void hurtEveryone() {
        int numHurt = 0;
        var entities = world.entities();
        for (int i = 0; i < entities.size; i++) {
            var e = entities.get(i);
            if (Position.has(e)) {
                e.tag(Tags.HURT);
                numHurt++;
            }
        }
        Log.info(TAG, "hurt %d entities", numHurt);
    }

    @Override
    public void update(float dt) {
        world.update(dt);
        super.update(dt);
    }

    @Override
    public void renderWorld(SpriteBatch batch) {
        batch.begin();
        {
            batch.setColor(Color.SLATE);
            batch.draw(region, 0, 0, WORLD_WIDTH, WORLD_HEIGHT);
            batch.setColor(Color.WHITE);

            world.render(Time.delta);
        }
        batch.end();
    }

    @Override
    public void renderImGui() {
        main.debugUI.render();
        main.debugUI.ecs(world);
    }

    @Override
    public void dispose() {
        world.dispose();
        super.dispose();
    }
}
