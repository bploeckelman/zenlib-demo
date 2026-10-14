package com.zendogames.zenlibdemo.screens;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.maps.MapObject;
import com.badlogic.gdx.maps.objects.PolylineMapObject;
import com.badlogic.gdx.maps.tiled.objects.TiledMapTileMapObject;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.GdxRuntimeException;
import com.zendogames.zenlib.Log;
import com.zendogames.zenlib.ZenGame;
import com.zendogames.zenlib.collision.CollisionMask;
import com.zendogames.zenlib.ecs.Entity;
import com.zendogames.zenlib.ecs.components.*;
import com.zendogames.zenlib.ecs.movement.platformer.PlatformerTuning;
import com.zendogames.zenlib.ecs.systems.*;
import com.zendogames.zenlib.input.InputControls;
import com.zendogames.zenlib.render.CameraFollow;
import com.zendogames.zenlib.render.RenderLayer;
import com.zendogames.zenlib.render.WorldView;
import com.zendogames.zenlib.tilemap.MapObjectSpawner;
import com.zendogames.zenlib.tilemap.TiledProps;
import com.zendogames.zenlib.ui.DebugViews;
import com.zendogames.zenlibdemo.assets.GameMap;
import com.zendogames.zenlibdemo.ecs.DemoContacts;
import com.zendogames.zenlibdemo.ecs.EnemyFactory;
import com.zendogames.zenlibdemo.ecs.GameMask;
import com.zendogames.zenlibdemo.ecs.components.Trigger;
import com.zendogames.zenlibdemo.ecs.systems.FleeSystem;

/**
 * The library's Tiled support on the reference project's warp room ({@code assets/maps/warp-room.tmx},
 * 60x30 tiles) seen through a 320x180 view that follows player 1 inside the map bounds:
 * <pre>
 *   tile layers   background / middle (world) / collision (hidden) / foreground, drawn in RenderLayer order
 *                 with the heroes between middle and foreground; the collision layer builds the grid
 *   background    the room backdrop, a tile object scaled to the room, behind the background tiles
 *   player        player-1 on WASD + Space (the camera's target), player-2 on the arrows + Enter
 *   launcher      every 'period' seconds throws a projectile at ('xVel', 'yVel') under gravity;
 *                 it pops on any wall and hurts a hero it touches
 *   jumpthru      a horizontal polyline: a one-way platform
 *   trigger       a zone with an 'effect': log (the two loggers), bounce ('height' tiles, the bouncer),
 *                 teleport (to the 'target' marker after 'dwell' seconds, the two teleporters)
 *   marker        a named Position-only entity, what teleports point at
 *   watcher       (not in the map, spawned by hand) an enemy that runs from the heroes its {@code Sensor}
 *                 sees and hops crates in its way; {@code FleeSystem} steers it
 * </pre>
 * Buttons in the Debug window, keys in parentheses there: crate at the pointer (also the unarmed click),
 * a watcher or the ramps grid at the pointer, a sensor on player 2 (named rays on a body you steer, press
 * again to remove), toggle the collision layer's tiles over the world (compare with {@code 2}'s grid
 * outlines), reload the map from disk after saving in Tiled, rebuild from the loaded map. {@code 2} also
 * draws every sensor's whiskers; the Sensors view lists their readings, the Tilemaps view every layer
 * with its own visibility toggle.
 */
public class TilemapScreen extends DemoScreen {

    private static final String TAG = TilemapScreen.class.getSimpleName();

    static final int VIEW_WIDTH  = 320;
    static final int VIEW_HEIGHT = 180;

    static final InputControls HERO_1 = InputControls.wasd(Input.Keys.SPACE);
    static final InputControls HERO_2 = InputControls.arrows(Input.Keys.ENTER);

    /**
     * Half the library preset's speed with the preset's jump: a full jump covers 4 tiles instead of 7.5, a
     * standing jump with a 0.1 s nudge lands one tile over instead of 2.4, and crossing the 320 px view takes
     * 3.3 s instead of 1.7. The dash and wall-jump kicks are halved to match.
     */
    static final PlatformerTuning HERO_TUNING = new PlatformerTuning(
            UNIT,
            6, 0.1f, 0.08f,         // ground: speed (tiles/s), accel time, stop time
            6, 0.2f, 0.2f, 22,      // air: speed, accel time, stop time, max fall speed
            5, 0.38f,               // jump: apex height (tiles), time to peak
            2.5f, 2f,               // gravity multipliers: jump cut, fall
            0.1f, 0.12f,            // jump helpers: buffer time, coyote time
            new PlatformerTuning.Abilities(
                    6, 10, 0.2f,                                         // wall: slide speed, jump speed x, steer-lock time
                    PlatformerTuning.Abilities.LedgeGrab.PUSH, 0.25f, 0.1f, // ledge: grab rule, reach, regrab time
                    20, 0.2f, 0.4f));                                    // dash: speed, time, cooldown

    final EnemyFactory enemies;
    final ContactSystem contactSystem;
    final CameraFollow cameraFollow;
    final MapObjectSpawner spawner;
    final Vector2 mapCenter = new Vector2();

    Tilemap tilemap;
    Entity hero;
    Entity hero2;

    public TilemapScreen(ZenGame game) {
        super(game, /*withCollision*/ true);
        this.enemies = new EnemyFactory(world);
        this.contactSystem = new ContactSystem(collision, new DemoContacts());

        world.addSystem(new InputSystem(inputProcessor));
        world.addSystem(new SensorSystem(collision));
        world.addSystem(new FleeSystem());
        world.addSystem(new TimerSystem());
        world.addSystem(new BehaviorSystem());
        world.addSystem(new LifetimeSystem());
        world.addSystem(new MovementSystem(collision));
        world.addSystem(contactSystem);
        addCommonSystems();

        this.cameraFollow = new CameraFollow(worldCamera);
        this.spawner = new MapObjectSpawner()
                .on("background", this::background)
                .on("player",     this::player)
                .on("launcher",   this::launcher)
                .on("crate",      (map, obj) -> factory.crate(pixel, TiledProps.x(obj), TiledProps.y(obj)))
                .on("jumpthru",   this::jumpthru)
                .on("trigger",    this::trigger)
                .on("marker",     (map, obj) -> world.createEntity().add(new Name("marker:" + obj.getName())).add(new Position(TiledProps.x(obj), TiledProps.y(obj))))
                .otherwise((map, obj) -> Log.warn(TAG, "no spawn for map object %s, skipped", TiledProps.describe(obj)));

        buildLevel();

        debugViews.add(DebugViews.tilemaps(world));
        debugViews.add(DebugViews.movers(world));
        debugViews.add(DebugViews.contacts(contactSystem));
        debugViews.add(DebugViews.behaviors(world));
        debugViews.add(DebugViews.sensors(world));
        debugViews.add(DebugViews.timers(world));
        actions.defaultClick(this::crate)
                .arm("crate",                  Input.Keys.C, this::crate)
                .arm("watcher",                Input.Keys.T, enemies::watcher)
                .arm("ramps",                  Input.Keys.P, factory::ramps)
                .add("sensor on player 2",     Input.Keys.H, this::sensorOnPlayer2)
                .add("toggle collision layer", Input.Keys.L, this::toggleCollisionLayer)
                .add("reload map from disk",   Input.Keys.M, this::reloadMap)
                .add("rebuild level",          Input.Keys.R, () -> { world.clear(); buildLevel(); });
    }

    /** The map entity and its layers, then the objects; a new {@code Tilemap} each time since one instance is one map entity. */
    private void buildLevel() {
        hero = null;
        hero2 = null;
        tilemap = new Tilemap(GameMap.DEMO.get());
        tilemap.spawn(world, GameMask.SOLID, batch, worldCamera);
        spawner.spawnAll(tilemap);

        mapCenter.set(tilemap.width() / 2f, tilemap.height() / 2f);
        cameraFollow.bounds = new Rectangle(0, 0, tilemap.width(), tilemap.height());
        if (hero != null) cameraFollow.follow(hero, /*snap*/ true);
        else              cameraFollow.follow(mapCenter);

        Log.info(TAG, "built level: %dx%d tiles, %d layers, %d objects, hero %s",
                tilemap.cols, tilemap.rows, tilemap.layers.size, tilemap.objects.size, hero == null ? "none" : hero.toString());
    }

    // ------------------------------------------------------------------------
    // Spawns for the map's object classes
    // ------------------------------------------------------------------------

    /** The backdrop: a tile object (one big tile) stretched to the object's size, behind the background tile layer. */
    private void background(Tilemap map, MapObject object) {
        if (!(object instanceof TiledMapTileMapObject tile)) {
            throw new GdxRuntimeException("background %s must be a tile object".formatted(TiledProps.describe(object)));
        }
        var image = new Image(tile.getTextureRegion(), TiledProps.width(object), TiledProps.height(object));
        image.layer = RenderLayer.BACKGROUND;
        image.order = -1; // the background tile layer sits at its walk index, 1
        world.createEntity()
                .add(new Name("background"))
                .add(new Position(TiledProps.x(object), TiledProps.y(object)))
                .add(image);
    }

    /** {@code number} picks the controls; player 1 is the camera's target. The default in the project file is "1". */
    private void player(Tilemap map, MapObject object) {
        var number = TiledProps.get(object, "number", "1", String.class);
        var entity = switch (number) {
            case "1" -> hero  = factory.platformerHero(TiledProps.x(object), TiledProps.y(object), HERO_1, HERO_TUNING);
            case "2" -> hero2 = factory.platformerHero(TiledProps.x(object), TiledProps.y(object), HERO_2, HERO_TUNING);
            default  -> throw new GdxRuntimeException("player %s has number '%s', expected 1 or 2".formatted(TiledProps.describe(object), number));
        };
        Name.get(entity).name = object.getName();
    }

    private void launcher(Tilemap map, MapObject object) {
        enemies.launcher(TiledProps.x(object), TiledProps.y(object),
                TiledProps.require(object, "period", Float.class),
                TiledProps.require(object, "xVel", Integer.class),
                TiledProps.require(object, "yVel", Integer.class));
    }

    /** A polyline's length is the platform width; a rectangle's width does too, so either object kind works. */
    private void jumpthru(Tilemap map, MapObject object) {
        int width = (object instanceof PolylineMapObject polyline)
                ? MathUtils.round(polyline.getPolyline().getLength())
                : TiledProps.width(object);
        if (width <= 0) {
            Log.warn(TAG, "jumpthru %s has no width, skipped", TiledProps.describe(object));
            return;
        }
        factory.platform(pixel, TiledProps.x(object), TiledProps.y(object), width);
    }

    /** The zone from the factory plus its {@link Trigger} effect from the object's class properties. */
    private void trigger(Tilemap map, MapObject object) {
        var effect = switch (TiledProps.get(object, "effect", "log", String.class)) {
            case "log"    -> new Trigger.Log();
            case "bounce" -> new Trigger.Bounce(TiledProps.require(object, "height", Float.class));
            case "teleport" -> {
                var target = TiledProps.require(object, "target", MapObject.class);
                yield new Trigger.Teleport(TiledProps.x(target), TiledProps.y(target), TiledProps.get(object, "dwell", 0f, Float.class));
            }
            default -> throw new GdxRuntimeException("trigger %s has an unknown effect".formatted(TiledProps.describe(object)));
        };
        factory.trigger(pixel, object.getName(), TiledProps.x(object), TiledProps.y(object), TiledProps.width(object), TiledProps.height(object))
                .add(new Trigger(effect));
    }

    // ------------------------------------------------------------------------
    // Actions
    // ------------------------------------------------------------------------

    private void crate(int x, int y) {
        factory.crate(pixel, x, y);
    }

    private void sensorOnPlayer2() {
        if (hero2 == null || !hero2.isAlive()) {
            Log.warn(TAG, "no player 2 to sense with");
            return;
        }
        if (Sensor.has(hero2)) {
            hero2.remove(Sensor.TYPE);
            return;
        }
        hero2.add(Sensor.rays(6 * UNIT, CollisionMask.of(GameMask.SOLID), CollisionMask.of(GameMask.PLAYER, GameMask.ENEMY), 0, -45, -90, 180));
    }

    private void toggleCollisionLayer() {
        for (int i = 0; i < tilemap.layerEntities.size; i++) {
            var tileLayer = TileLayer.get(tilemap.layerEntities.get(i));
            if (tileLayer.collision) {
                tileLayer.visible = !tileLayer.visible;
            }
        }
    }

    /** Save in Tiled, press M: the file is read again and the level rebuilt from it. */
    private void reloadMap() {
        GameMap.DEMO.reload(assets);
        world.clear();
        buildLevel();
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
