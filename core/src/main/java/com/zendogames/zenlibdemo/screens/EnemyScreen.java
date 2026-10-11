package com.zendogames.zenlibdemo.screens;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.zendogames.zenlib.Log;
import com.zendogames.zenlib.Time;
import com.zendogames.zenlib.ZenFlag;
import com.zendogames.zenlib.ZenGame;
import com.zendogames.zenlib.ecs.systems.LifetimeSystem;
import com.zendogames.zenlib.ecs.systems.MovementSystem;
import com.zendogames.zenlib.ecs.systems.TimerSystem;
import com.zendogames.zenlib.render.WorldView;
import com.zendogames.zenlib.ui.DebugViews;
import com.zendogames.zenlibdemo.ecs.EnemyFactory;
import com.zendogames.zenlibdemo.ecs.Level;
import com.zendogames.zenlibdemo.ecs.systems.HeroAnimSystem;

/**
 * Simple enemies built from {@code Timers} lambdas on a walled map the size of the view, under
 * the hero's gravity: a spitter that fires bullets (they stop at the walls and expire), a blob
 * that hops toward the center with {@code PlatformerMotion.launch}, faster as it gets angrier,
 * and a door that opens when no enemy is left. The controls are the {@code DebugActions} in the
 * Debug window's "Screen" section (keys in parentheses there): arm a spawn and click in the world,
 * or press its key. A click with nothing armed spawns a spitter.
 */
public class EnemyScreen extends DemoScreen {

    private static final String TAG = EnemyScreen.class.getSimpleName();

    static final int VIEW_WIDTH  = 320;
    static final int VIEW_HEIGHT = 180;
    static final int FLOOR       = WALL;

    final EnemyFactory enemies;

    public EnemyScreen(ZenGame game) {
        super(game, /*withCollision*/ true);
        this.enemies = new EnemyFactory(world);

        world.addSystem(new TimerSystem());
        world.addSystem(new LifetimeSystem());
        world.addSystem(new MovementSystem(collision));
        world.addSystem(new HeroAnimSystem());
        addCommonSystems();

        worldCamera.position.set(VIEW_WIDTH / 2f, VIEW_HEIGHT / 2f, 0f);
        buildLevel();

        debugViews.add(DebugViews.timers(world));
        debugViews.add(DebugViews.movers(world));
        actions.defaultClick(enemies::spitter)
               .arm("spitter",       Input.Keys.S, enemies::spitter)
               .arm("blob",          Input.Keys.O, (x, y) -> enemies.blob(x, y, () -> VIEW_WIDTH / 2))
               .arm("door",          Input.Keys.D, enemies::door)
               .add("kill enemies",  Input.Keys.K, () -> Log.info(TAG, "killed %d enemies", enemies.killEnemies()))
               .add("hit-stop 0.5s", Input.Keys.P, () -> Time.pauseFor(0.5f))
               .add("LOG_TIMERS",    Input.Keys.L, ZenFlag.LOG_TIMERS::toggle)
               .add("rebuild level", Input.Keys.R, () -> { world.clear(); buildLevel(); });
    }

    private void buildLevel() {
        Level.walled(factory, pixel, VIEW_WIDTH, VIEW_HEIGHT, WALL);
        enemies.spitter(VIEW_WIDTH - 40, FLOOR);
        enemies.blob(VIEW_WIDTH / 2 - 60, FLOOR, () -> VIEW_WIDTH / 2);
        enemies.door(20, FLOOR);
    }

    @Override
    protected WorldView createWorldView() {
        var view = new WorldView(worldCamera, VIEW_WIDTH, VIEW_HEIGHT);
        view.clearColor.set(Color.DARK_GRAY);
        return view;
    }
}
