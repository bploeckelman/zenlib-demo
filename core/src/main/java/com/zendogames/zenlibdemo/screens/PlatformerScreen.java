package com.zendogames.zenlibdemo.screens;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.zendogames.zenlib.Log;
import com.zendogames.zenlib.ZenGame;
import com.zendogames.zenlib.collision.ContactResponder;
import com.zendogames.zenlib.ecs.Entity;
import com.zendogames.zenlib.ecs.components.Tilemap;
import com.zendogames.zenlib.ecs.systems.*;
import com.zendogames.zenlib.render.CameraFollow;
import com.zendogames.zenlib.render.WorldView;
import com.zendogames.zenlib.tilemap.MapObjectSpawner;
import com.zendogames.zenlib.ui.DebugViews;
import com.zendogames.zenlibdemo.assets.GameMap;
import com.zendogames.zenlibdemo.ecs.GameContacts;
import com.zendogames.zenlibdemo.ecs.GameMask;
import com.zendogames.zenlibdemo.ecs.factories.EnemyFactory;
import com.zendogames.zenlibdemo.ecs.factories.Factory;
import com.zendogames.zenlibdemo.ecs.factories.MapObjFactory;

public class PlatformerScreen extends DemoScreen {

    private static final String TAG = PlatformerScreen.class.getSimpleName();

    static final int VIEW_WIDTH  = 320;
    static final int VIEW_HEIGHT = 180;
    static final GameMap MAP = GameMap.PLATFORMER;

    final EnemyFactory enemyFactory;
    final MapObjFactory mapObjectFactory;
    final ContactSystem contactSystem;
    final MapObjectSpawner spawner;
    final CameraFollow cameraFollow;
    final Vector2 mapCenter;

    Entity player;
    Tilemap tilemap;

    public PlatformerScreen(ZenGame game) {
        super(game, /*withCollision*/ true);
        this.enemyFactory = new EnemyFactory(world);
        this.mapObjectFactory = new MapObjFactory(factory);
        this.contactSystem = new ContactSystem(collision, createContactResponder());
        this.mapCenter = new Vector2();

        // Tiled class -> what it spawns; an unknown class throws listing these, so a typo in Tiled fails loudly
        this.spawner = new MapObjectSpawner()
                .on(Factory.MARKER,     (map, obj) -> {}) // point objects other objects reference; nothing to spawn
                .on(Factory.PLAYER,     (map, obj) -> player = mapObjectFactory.platformerPlayer(obj))
                .on(Factory.BOUNCER,    (map, obj) -> mapObjectFactory.bouncer(obj))
                .on(Factory.JUMPTHRU,   (map, obj) -> mapObjectFactory.jumpthru(obj))
                .on(Factory.SPIKE,      (map, obj) -> mapObjectFactory.spike(obj))
                .on(Factory.TELEPORTER, (map, obj) -> mapObjectFactory.teleporter(obj))
                .on(Factory.WALKER,     (map, obj) -> mapObjectFactory.walker(obj))
                .on(Factory.FLYER,      (map, obj) -> mapObjectFactory.flyer(obj))
                .on(Factory.JUMPER,     (map, obj) -> mapObjectFactory.jumper(obj))
                .on(Factory.LEAPER,     (map, obj) -> mapObjectFactory.leaper(obj))
                .on(Factory.SHOOTER,    (map, obj) -> mapObjectFactory.shooter(obj))
                .on(Factory.BALL,       (map, obj) -> mapObjectFactory.ball(obj));

        world.addSystem(new InputSystem(game.input));
        world.addSystem(new SensorSystem(collision));
        world.addSystem(new TimerSystem());
        world.addSystem(new BehaviorSystem());
        world.addSystem(new LifetimeSystem());
        world.addSystem(new MovementSystem(collision));
        world.addSystem(contactSystem);
        addCommonSystems();

        this.cameraFollow = new CameraFollow(worldCamera);
        this.cameraFollow.smoothing = 5f;
        worldCamera.position.set(VIEW_WIDTH / 2f, VIEW_HEIGHT / 2f, 0f);

        buildLevel();

        debugViews.add(DebugViews.tilemaps(world));
        debugViews.add(DebugViews.movers(world));
        debugViews.add(DebugViews.contacts(contactSystem));
        debugViews.add(DebugViews.behaviors(world));
        debugViews.add(DebugViews.sensors(world));
        debugViews.add(DebugViews.timers(world));
        actions .add("reload map",    Input.Keys.M, this::reloadMap)
                .add("rebuild level", Input.Keys.B, this::buildLevel)
                .add("pause",         Input.Keys.P, () -> {
                    var animSystem = world.getSystem(AnimationSystem.class);
                    animSystem.enabled = !animSystem.enabled;
                });
    }

    /** The rules for this screen's contacts; called from the constructor, so read no subclass fields. The examples' by default. */
    protected ContactResponder createContactResponder() {
        return new GameContacts(factory);
    }

    /** Clears the world and spawns the map again: its tile layers, then every object through {@link #spawner}. */
    protected void buildLevel() {
        world.clear();

        player = null;
        tilemap = new Tilemap(MAP.get());
        tilemap.spawn(world, GameMask.SOLID, batch, worldCamera);
        spawner.spawnAll(tilemap);

        mapCenter.set(tilemap.width() / 2f, tilemap.height() / 2f);
        cameraFollow.bounds = new Rectangle(0, 0, tilemap.width(), tilemap.height());
        if (player != null) cameraFollow.follow(player, /*snap*/ true);
        else                cameraFollow.follow(mapCenter);

        Log.info(TAG, "built level: %dx%d tiles, %d layers, %d objects, player %s",
                tilemap.cols, tilemap.rows, tilemap.layers.size, tilemap.objects.size,
                player == null ? "none" : player.toString());
    }

    /** Save in Tiled, press M: the file is read again and the level rebuilt from it. */
    private void reloadMap() {
        MAP.reload(assets);
        buildLevel();
    }

    @Override
    protected WorldView createWorldView() {
        var view = new WorldView(worldCamera, VIEW_WIDTH, VIEW_HEIGHT);
        view.clearColor.set(Color.DARK_GRAY);
        return view;
    }

    @Override
    protected void afterWorldUpdate(float dt) {
        // when the followed entity is gone, look at the map instead
        if (cameraFollow.target() instanceof CameraFollow.Target.Follow f && !f.entity().isAlive()) {
            cameraFollow.follow(mapCenter);
        }
        cameraFollow.update(dt);
    }
}
