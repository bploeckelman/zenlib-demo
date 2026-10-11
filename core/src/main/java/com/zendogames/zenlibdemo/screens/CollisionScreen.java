package com.zendogames.zenlibdemo.screens;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.zendogames.zenlib.Log;
import com.zendogames.zenlib.ZenFlag;
import com.zendogames.zenlib.ZenGame;
import com.zendogames.zenlib.ecs.Entity;
import com.zendogames.zenlib.ecs.components.Animator;
import com.zendogames.zenlib.ecs.components.Lifetime;
import com.zendogames.zenlib.ecs.components.Name;
import com.zendogames.zenlib.ecs.components.Position;
import com.zendogames.zenlib.ecs.systems.ContactSystem;
import com.zendogames.zenlib.ecs.systems.LifetimeSystem;
import com.zendogames.zenlib.ecs.systems.MovementSystem;
import com.zendogames.zenlib.render.CameraFollow;
import com.zendogames.zenlib.render.WorldView;
import com.zendogames.zenlib.ui.DebugViews;
import com.zendogames.zenlibdemo.ecs.DemoContacts;
import com.zendogames.zenlibdemo.ecs.Factory;
import com.zendogames.zenlibdemo.ecs.Level;
import com.zendogames.zenlibdemo.ecs.systems.SpawnSystem;

/**
 * Heroes bouncing around a walled 640x360 map seen through a 320x180 view: solid blocks, a one-way
 * platform and a one-way wall, a ramp grid, a trigger zone, hero-on-hero bumps through
 * {@link ContactSystem}, and a {@link CameraFollow} clamped to the map. Everything is a
 * {@code DebugActions} button in the Debug window, keys in parentheses there:
 * <pre>
 *   hero           arm a spawn at the pointer (also the unarmed click); capsule/rect picks its shape
 *   auto-fill      SpawnSystem keeps 10 short-lived heroes alive, spawning from inside a system
 *   follow         the hero nearest the pointer, or the map center; a followed hero that despawns
 *                  hands the camera back to the center
 *   RENDER_INTERP  off, tracking and heroes stutter on a high refresh display
 *   contact log    pause it, or include BLOCKED exits
 *   rebuild        world.clear() then the level again: ids start at 0
 * </pre>
 */
public class CollisionScreen extends DemoScreen {

    private static final String TAG = CollisionScreen.class.getSimpleName();

    // The view is half the map so the camera follow is exercised
    static final int VIEW_WIDTH    = 320;
    static final int VIEW_HEIGHT   = 180;
    static final int MAP_WIDTH     = 640;
    static final int MAP_HEIGHT    = 360;
    static final int TARGET_HEROES = 10;

    final ContactSystem contactSystem;
    final SpawnSystem autoFill;
    final CameraFollow cameraFollow;
    final Vector2 mapCenter;

    Factory.HeroShape spawnShape = Factory.HeroShape.CAPSULE;

    public CollisionScreen(ZenGame game) {
        super(game, /*withCollision*/ true);
        this.contactSystem = new ContactSystem(collision, new DemoContacts());
        this.autoFill = new SpawnSystem(TARGET_HEROES, MAP_WIDTH, MAP_HEIGHT, this::temporaryHero);

        world.addSystem(autoFill);
        world.addSystem(new LifetimeSystem());
        world.addSystem(new MovementSystem(collision));
        world.addSystem(contactSystem);
        addCommonSystems();

        this.mapCenter = new Vector2(MAP_WIDTH / 2f, MAP_HEIGHT / 2f);
        this.cameraFollow = new CameraFollow(worldCamera);
        this.cameraFollow.bounds = new Rectangle(0, 0, MAP_WIDTH, MAP_HEIGHT);
        this.cameraFollow.follow(mapCenter);

        buildLevel();

        debugViews.add(DebugViews.contacts(contactSystem));
        debugViews.add(DebugViews.movers(world));
        actions.defaultClick(this::hero)
               .arm("hero",              Input.Keys.H, this::hero)
               .add("capsule/rect",      Input.Keys.K, this::toggleShape)
               .add("auto-fill 10",      Input.Keys.A, () -> autoFill.enabled = !autoFill.enabled)
               .add("follow nearest",    Input.Keys.F, this::followNearest)
               .add("follow center",     Input.Keys.G, () -> cameraFollow.follow(mapCenter))
               .add("RENDER_INTERP",     Input.Keys.I, ZenFlag.RENDER_INTERP::toggle)
               .add("pause contact log", Input.Keys.P, () -> contactSystem.logPaused = !contactSystem.logPaused)
               .add("log BLOCKED exits", Input.Keys.X, () -> contactSystem.logBlockedExits = !contactSystem.logBlockedExits)
               .add("rebuild level",     Input.Keys.R, () -> { world.clear(); buildLevel(); });
    }

    private void buildLevel() {
        Level.walled(factory, pixel, MAP_WIDTH, MAP_HEIGHT, WALL);
        factory.wall(pixel, "block", 300, 150, 40, 60);
        factory.platform(pixel, 100, 250, 120);
        factory.oneWayWall(pixel, 480, 60, 100);
        factory.trigger(pixel, "zone", 420, 220, 100, 80);
        factory.ramps(120, WALL);
        factory.collidingHero(MAP_WIDTH / 2, MAP_HEIGHT / 2, Factory.HeroShape.CAPSULE);
    }

    private void hero(int x, int y) {
        factory.collidingHero(x, y, spawnShape);
    }

    /** The auto-fill's hero: the same, despawning after a while so the fill keeps spawning. */
    private void temporaryHero(int x, int y) {
        factory.collidingHero(x, y, spawnShape).add(new Lifetime(MathUtils.random(5f, 15f)));
    }

    private void toggleShape() {
        spawnShape = (spawnShape == Factory.HeroShape.CAPSULE) ? Factory.HeroShape.RECT : Factory.HeroShape.CAPSULE;
        Log.info(TAG, "spawning %s heroes", spawnShape);
    }

    private void followNearest() {
        var pointer  = inputProcessor.pointer;
        var entities = collision.family().entities();
        var best     = (Entity) null;
        var bestDist = Float.MAX_VALUE;
        for (int i = 0; i < entities.size; i++) {
            var e = entities.get(i);
            if (!Animator.has(e)) continue;
            var pos  = Position.get(e);
            var dist = pointer.dst2(pos.x, pos.y);
            if (dist < bestDist) { best = e; bestDist = dist; }
        }
        if (best != null) {
            cameraFollow.follow(best, /*snap*/ false);
            Log.info(TAG, "following %s (%s)", Name.of(best), best);
        }
    }

    @Override
    protected WorldView createWorldView() {
        var view = new WorldView(worldCamera, VIEW_WIDTH, VIEW_HEIGHT);
        view.clearColor.set(Color.LIGHT_GRAY);
        return view;
    }

    @Override
    protected void afterWorldUpdate(float dt) {
        if (cameraFollow.target() instanceof CameraFollow.Target.Follow f && !f.entity().isAlive()) {
            cameraFollow.follow(mapCenter);
        }
        cameraFollow.update(dt);
    }
}
