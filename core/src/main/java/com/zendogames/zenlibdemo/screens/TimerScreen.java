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
import com.zendogames.zenlib.ui.DebugActions;
import com.zendogames.zenlib.ui.DebugView;
import com.zendogames.zenlib.ui.DebugViews;
import com.zendogames.zenlibdemo.Main;
import com.zendogames.zenlibdemo.ecs.EnemyFactory;
import com.zendogames.zenlibdemo.ecs.Factory;
import com.zendogames.zenlibdemo.ecs.systems.MovementSystem;

/**
 * Simple enemies built from {@code Timers} lambdas on a map the size of the view. The controls are
 * the {@link DebugActions} in the Debug window's "Screen" section (keys in parentheses there):
 * arm a spawn and click in the world, or press its key. A click with nothing armed spawns a spitter.
 * {@code 9} steps one tick while {@code FRAME_STEP} ({@code 0}) is on.
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
    final DebugActions actions;
    final DebugView ecsView;
    final DebugView timersView;

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

        this.ecsView = DebugViews.ecs(world);
        this.timersView = DebugViews.timers(world);
        this.actions = new DebugActions()
                .defaultClick(enemies::spitter)
                .arm("spitter", Input.Keys.S, enemies::spitter)
                .arm("blob",    Input.Keys.O, (x, y) -> enemies.blob(x, y, VIEW_WIDTH / 2))
                .arm("door",    Input.Keys.D, enemies::door)
                .add("kill enemies",  Input.Keys.K, () -> Log.info(TAG, "killed %d enemies", enemies.killEnemies()))
                .add("hit-stop 0.5s", Input.Keys.P, () -> Time.pauseFor(0.5f))
                .add("LOG_TIMERS",    Input.Keys.L, ZenFlag.LOG_TIMERS::toggle)
                .add("clear world",   Input.Keys.C, () -> {
                    world.clear();
                    factory.backdrop(pixel, VIEW_WIDTH, VIEW_HEIGHT);
                })
                .add("fresh TimerScreen", Input.Keys.T, () -> game.setScreen(new TimerScreen(game)))
                .add("EcsScreen",         Input.Keys.B, () -> game.setScreen(new EcsScreen(game)));
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
                return actions.click((int) this.pointer.x, (int) this.pointer.y);
            }

            @Override
            protected boolean onKeyDown(int keycode) {
                return actions.key(keycode);
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
        main.debugUI.render(actions, timersView, ecsView);
    }

    @Override
    public void dispose() {
        world.dispose();
        super.dispose();
    }
}
