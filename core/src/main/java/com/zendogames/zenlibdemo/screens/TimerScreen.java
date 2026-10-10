package com.zendogames.zenlibdemo.screens;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.zendogames.zenlib.Log;
import com.zendogames.zenlib.Time;
import com.zendogames.zenlib.ZenFlag;
import com.zendogames.zenlib.ZenGame;
import com.zendogames.zenlib.ecs.World;
import com.zendogames.zenlib.ecs.systems.AnimationSystem;
import com.zendogames.zenlib.ecs.systems.LifetimeSystem;
import com.zendogames.zenlib.ecs.systems.RenderSystem;
import com.zendogames.zenlib.ecs.systems.TimerSystem;
import com.zendogames.zenlib.input.ScreenInputProcessor;
import com.zendogames.zenlib.render.WorldView;
import com.zendogames.zenlib.screens.ZenScreen;
import com.zendogames.zenlibdemo.Main;
import com.zendogames.zenlibdemo.ecs.EnemyFactory;
import com.zendogames.zenlibdemo.ecs.Factory;
import com.zendogames.zenlibdemo.ecs.systems.MovementSystem;

/**
 * Simple enemies built from {@code Timers} lambdas, one key each, on a map the size of the view.
 * <pre>
 *   click   spawn a spitter at the pointer (shoots left every 3s)
 *   O       spawn a blob at the pointer, hopping toward the map center
 *   D       spawn a door at the pointer: opens (pops, despawns) once no enemy is left
 *   K       kill every enemy; any doors open a quarter second later
 *   P       hit-stop 0.5s: every timer freezes with the simulation
 *   L       toggle LOG_TIMERS
 *   C       world.clear(): no timer fires afterwards, Timers.onRemove cancelled them
 *   T       transition to a fresh TimerScreen
 *   B       back to EcsScreen
 *   9       one tick while FRAME_STEP (0) is on: timers advance one tick
 * </pre>
 */
public class TimerScreen extends ZenScreen {

    private static final String TAG = TimerScreen.class.getSimpleName();

    static final int VIEW_WIDTH  = 320;
    static final int VIEW_HEIGHT = 180;

    final Main main;
    final World world;
    final Factory factory;
    final EnemyFactory enemies;
    final TextureRegion pixel;

    public TimerScreen(ZenGame game) {
        super(game);
        this.main = (Main) game;
        this.pixel = new TextureRegion(game.pixel);
        this.world = new World();
        this.factory = new Factory(world);
        this.enemies = new EnemyFactory(world);

        world.addSystem(new TimerSystem());
        world.addSystem(new LifetimeSystem());
        world.addSystem(new MovementSystem(VIEW_WIDTH, VIEW_HEIGHT));
        world.addSystem(new AnimationSystem());
        world.addSystem(new RenderSystem(batch));

        worldCamera.position.set(VIEW_WIDTH / 2f, VIEW_HEIGHT / 2f, 0);
        factory.backdrop(pixel, VIEW_WIDTH, VIEW_HEIGHT);
        enemies.spitter(VIEW_WIDTH - 40, 40);
        enemies.door(20, 40);
    }

    @Override
    protected WorldView createWorldView() {
        var view = new WorldView(worldCamera, VIEW_WIDTH, VIEW_HEIGHT);
        view.clearColor.set(Color.DARK_GRAY);
        return view;
    }

    @Override
    protected ScreenInputProcessor createInputProcessor() {
        return new ScreenInputProcessor(this) {
            @Override
            protected boolean onTouchDown(int screenX, int screenY, int pointer, int button) {
                enemies.spitter((int) this.pointer.x, (int) this.pointer.y);
                return true;
            }

            @Override
            protected boolean onKeyDown(int keycode) {
                switch (keycode) {
                    case Input.Keys.O -> enemies.blob((int) pointer.x, (int) pointer.y, VIEW_WIDTH / 2);
                    case Input.Keys.D -> enemies.door((int) pointer.x, (int) pointer.y);
                    case Input.Keys.K -> Log.info(TAG, "killed %d enemies", enemies.killEnemies());
                    case Input.Keys.P -> Time.pauseFor(0.5f);
                    case Input.Keys.L -> ZenFlag.LOG_TIMERS.toggle();
                    case Input.Keys.C -> {
                        world.clear();
                        factory.backdrop(pixel, VIEW_WIDTH, VIEW_HEIGHT);
                    }
                    case Input.Keys.T -> game.setScreen(new TimerScreen(game));
                    case Input.Keys.B -> game.setScreen(new EcsScreen(game));
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

    @Override
    public void update(float dt) {
        world.update(dt);
        super.update(dt);
    }

    @Override
    public void renderWorld(SpriteBatch batch) {
        batch.begin();
        world.render(Time.delta);
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
