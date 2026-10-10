package com.zendogames.zenlibdemo.screens;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.zendogames.zenlib.Log;
import com.zendogames.zenlib.Time;
import com.zendogames.zenlib.ZenFlag;
import com.zendogames.zenlib.ZenGame;
import com.zendogames.zenlib.assets.ZenAtlas;
import com.zendogames.zenlib.collision.CollisionDraw;
import com.zendogames.zenlib.collision.CollisionWorld;
import com.zendogames.zenlib.ecs.Entity;
import com.zendogames.zenlib.ecs.World;
import com.zendogames.zenlib.ecs.components.Animator;
import com.zendogames.zenlib.ecs.components.Position;
import com.zendogames.zenlib.ecs.systems.AnimationSystem;
import com.zendogames.zenlib.ecs.systems.ContactSystem;
import com.zendogames.zenlib.ecs.systems.LifetimeSystem;
import com.zendogames.zenlib.ecs.systems.RenderSystem;
import com.zendogames.zenlib.input.ScreenInputProcessor;
import com.zendogames.zenlib.render.CameraFollow;
import com.zendogames.zenlib.render.WorldView;
import com.zendogames.zenlib.screens.ZenScreen;
import com.zendogames.zenlibdemo.Main;
import com.zendogames.zenlibdemo.ecs.DemoContacts;
import com.zendogames.zenlibdemo.ecs.Factory;
import com.zendogames.zenlibdemo.ecs.systems.MovementSystem;

/**
 * Heroes bouncing around a walled 640x360 pixel-art map through through a {@link CollisionWorld}: solid walls, a
 * one-way platform and a one-way wall, a ramp grid, a trigger zone, and hero-on-hero bumps reported through
 * {@link ContactSystem}.
 * <pre>
 *   click   spawn a colliding hero at the pointer
 *   K       toggle the spawned shape: capsule (default) or rect
 *   2       DEBUG_RENDER: collider outlines over everything (library number row)
 *   P       pause the contact log; X include BLOCKED exits in it
 *   F       follow the hero nearest the pointer; G back to the map center
 *   C       world.clear() then rebuild the level
 *   N       transition to TimerScreen
 *   T       transition to a fresh CollisionScreen; B back to FirstScreen
 *   9       one tick while FRAME_STEP (0) is on
 * </pre>
 */
public class CollisionScreen extends ZenScreen {

    private static final String TAG = CollisionScreen.class.getSimpleName();

    // NOTE: setting view size to be half map size allows camera follow to be exercised
    // TODO: this screen will be consolidated with others in the near future, so leaving for now
//    static final int VIEW_WIDTH  = 320;
//    static final int VIEW_HEIGHT = 180;
    static final int VIEW_WIDTH  = 640;
    static final int VIEW_HEIGHT = 360;
    static final int MAP_WIDTH   = 640;
    static final int MAP_HEIGHT  = 360;
    static final int WALL        = 8;

    final Main main;
    final World world;
    final Factory factory;
    final CollisionWorld collision;
    final ContactSystem contactSystem;
    final TextureRegion pixel;
    final CameraFollow cameraFollow;
    final Vector2 mapCenter;

    Factory.HeroShape spawnShape = Factory.HeroShape.CAPSULE;

    public CollisionScreen(ZenGame game) {
        super(game);
        this.main = (Main) game;
        this.world = new World();
        this.factory = new Factory(world);
        this.collision = new CollisionWorld(world);
        this.contactSystem = new ContactSystem(collision, new DemoContacts());
        this.pixel = new TextureRegion(game.pixel);

        world.addSystem(new LifetimeSystem());
        world.addSystem(new MovementSystem(MAP_WIDTH, MAP_HEIGHT, collision));
        world.addSystem(contactSystem);
        world.addSystem(new AnimationSystem());
        world.addSystem(new RenderSystem(batch));

        this.mapCenter = new Vector2(MAP_WIDTH / 2f, MAP_HEIGHT / 2f);
        this.cameraFollow = new CameraFollow(worldCamera);
        this.cameraFollow.bounds = new Rectangle(0, 0, MAP_WIDTH, MAP_HEIGHT);
        this.cameraFollow.follow(mapCenter);

        buildLevel();
        ZenAtlas.SPRITES.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
    }

    private void buildLevel() {
        factory.backdrop(pixel, MAP_WIDTH, MAP_HEIGHT);
        factory.wall(pixel, "wall:left",   0,                0,                 WALL,      MAP_HEIGHT);
        factory.wall(pixel, "wall:right",  MAP_WIDTH - WALL, 0,                 WALL,      MAP_HEIGHT);
        factory.wall(pixel, "wall:bottom", 0,                0,                 MAP_WIDTH, WALL);
        factory.wall(pixel, "wall:top",    0,                MAP_HEIGHT - WALL, MAP_WIDTH, WALL);
        factory.wall(pixel, "block",       300,              150, 40, 60);
        factory.platform(pixel, 100, 250, 120);
        factory.oneWayWall(pixel, 480, 60, 100);
        factory.trigger(pixel, "zone", 420, 220, 100, 80);
        factory.ramps(120, WALL);
        factory.collidingHero(MAP_WIDTH / 2, MAP_HEIGHT / 2, Factory.HeroShape.CAPSULE);
    }

    @Override
    protected WorldView createWorldView() {
        var view = new WorldView(worldCamera, VIEW_WIDTH, VIEW_HEIGHT);
        view.clearColor.set(Color.LIGHT_GRAY);
        return view;
    }

    @Override
    protected ScreenInputProcessor createInputProcessor() {
        return new ScreenInputProcessor(this) {
            @Override
            protected boolean onTouchDown(int screenX, int screenY, int pointer, int button) {
                factory.collidingHero((int) this.pointer.x, (int) this.pointer.y, spawnShape);
                return true;
            }

            @Override
            protected boolean onKeyDown(int keycode) {
                switch (keycode) {
                    case Input.Keys.K -> {
                        spawnShape = (spawnShape == Factory.HeroShape.CAPSULE) ? Factory.HeroShape.RECT : Factory.HeroShape.CAPSULE;
                        Log.info(TAG, "spawning %s hero", spawnShape);
                    }
                    case Input.Keys.P -> contactSystem.logPaused = !contactSystem.logPaused;
                    case Input.Keys.X -> contactSystem.logBlockedExits = !contactSystem.logBlockedExits;
                    case Input.Keys.F -> followNearest();
                    case Input.Keys.G -> cameraFollow.follow(mapCenter);
                    case Input.Keys.C -> {
                        world.clear();
                        buildLevel();
                    }
                    case Input.Keys.T -> game.setScreen(new CollisionScreen(game));
                    case Input.Keys.N -> game.setScreen(new TimerScreen(game));
                    case Input.Keys.B -> game.setScreen(new FirstScreen(game));
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

    private void followNearest() {
        var pointer = inputProcessor.pointer;
        var entities = collision.family().entities();
        var best = (Entity) null;
        var bestDist = Float.MAX_VALUE;
        for (int i = 0; i < entities.size; i++) {
            var e = entities.get(i);
            if (!Animator.has(e)) continue;
            var pos = Position.get(e);
            var dist = pointer.dst2(pos.x, pos.y);
            if (dist < bestDist) { best = e; bestDist = dist; }
        }
        if (best != null) {
            cameraFollow.follow(best, /*snap*/ false);
        }
    }

    @Override
    public void update(float dt) {
        world.update(dt);
        if (cameraFollow.target() instanceof CameraFollow.Target.Follow f && !f.entity().isAlive()) {
            cameraFollow.follow(mapCenter);
        }
        cameraFollow.update(dt); // before super: world view sets the projection from the camera
        super.update(dt);
    }

    @Override
    public void renderWorld(SpriteBatch batch) {
        batch.begin();
        world.render(Time.delta);
        if (ZenFlag.DEBUG_RENDER.on()) {
            CollisionDraw.all(shapes, collision, Color.MAGENTA, 1f);
        }
        batch.end();
    }

    @Override
    public void renderImGui() {
        main.debugUI.render();
        main.debugUI.ecs(world);
        main.debugUI.contacts(contactSystem);
    }

    @Override
    public void dispose() {
        world.dispose();
        super.dispose();
    }
}
