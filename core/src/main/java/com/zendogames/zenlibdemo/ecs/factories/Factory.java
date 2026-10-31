package com.zendogames.zenlibdemo.ecs.factories;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.github.tommyettinger.gand.utils.Direction;
import com.zendogames.zenlib.Log;
import com.zendogames.zenlib.assets.ZenAnimHero;
import com.zendogames.zenlib.collision.CollisionMask;
import com.zendogames.zenlib.collision.shapes.CollisionAASeg;
import com.zendogames.zenlib.collision.shapes.CollisionGrid;
import com.zendogames.zenlib.ecs.Entity;
import com.zendogames.zenlib.ecs.Family;
import com.zendogames.zenlib.ecs.World;
import com.zendogames.zenlib.ecs.components.*;
import com.zendogames.zenlib.ecs.movement.Motion;
import com.zendogames.zenlib.ecs.movement.platformer.PlatformerMode;
import com.zendogames.zenlib.ecs.movement.platformer.PlatformerMotion;
import com.zendogames.zenlib.ecs.movement.platformer.PlatformerTuning;
import com.zendogames.zenlib.ecs.movement.topdown.TopDownMotion;
import com.zendogames.zenlib.ecs.movement.topdown.TopDownTuning;
import com.zendogames.zenlib.input.InputControls;
import com.zendogames.zenlib.render.RenderLayer;
import com.zendogames.zenlibdemo.ecs.GameMask;
import com.zendogames.zenlibdemo.ecs.GameTags;
import com.zendogames.zenlibdemo.ecs.components.Trigger;
import com.zendogames.zenlibdemo.ecs.movement.BounceMotion;
import com.zendogames.zenlibdemo.ecs.movement.FlightMotion;
import com.zendogames.zenlibdemo.ecs.state.EnemyState;
import com.zendogames.zenlibdemo.ecs.state.HeroState;
import com.zendogames.zenlibdemo.ecs.state.PlayerState;

/** Spawns the demo's entities. Logs family joins so the per-add timing in setup code is visible. */
public class Factory {

    private static final String TAG = Factory.class.getSimpleName();

    /** The demo's tile size: the unit every tuning is authored in. */
    public static final int UNIT = 16;

    public static final String MARKER     = "marker"; // point objects that other objects reference (teleporter / leaper target); spawns nothing
    public static final String PLAYER     = "player";
    public static final String DUMMY      = "dummy";
    public static final String BOUNCER    = "bouncer";
    public static final String JUMPTHRU   = "jumpthru";
    public static final String SPIKE      = "spike";
    public static final String TELEPORTER = "teleporter";
    public static final String WALKER     = "walker";
    public static final String FLYER      = "flyer";
    public static final String JUMPER     = "jumper";
    public static final String LEAPER     = "leaper";
    public static final String SHOOTER    = "shooter";
    public static final String BALL       = "ball";
    public static final String PATROL     = "patrol";       // simple enemy for overhead example

    /** The top-down enemies' movement: half the player's speed, a slow start, stops very quickly. */
    static final TopDownTuning PATROL_TUNING = new TopDownTuning(UNIT, 3, 0.3f, 0.05f);

    /**
     * The grounded enemies' movement: a slow walk that stops almost at once, barely steerable in
     * the air, the player's jump numbers so gravity matches, no jump helpers and no abilities.
     */
    static final PlatformerTuning ENEMY_TUNING = new PlatformerTuning(
            UNIT,
            3, 0.3f, 0.05f,         // ground: speed (tiles/s), accel time, stop time
            1, 0.5f, 0.3f, 22,      // air: speed, accel time, stop time, max fall speed
            5, 0.38f,               // jump: apex height (tiles), time to peak; only its gravity is used
            2.5f, 2f,               // gravity multipliers: jump cut, fall
            0f, 0f,                 // jump helpers: none, nothing presses jump
            PlatformerTuning.Abilities.NONE);

    /** Seconds a bullet flies before it expires on its own. */
    public static final float BULLET_LIFETIME = 3f;

    /** Hero frames are 16x16 with the origin at the bottom-left; the body is the middle 8px. */
    public enum HeroShape { CAPSULE, RECT }

    private final World world;
    private final TextureRegion pixel;

    /** Every live player, for the enemies' AI to look for. */
    private final Family players;

    public Factory(World world, TextureRegion pixel) {
        this.world = world;
        this.pixel = pixel;
        this.players = world.family(Family.all(GameTags.PLAYER, Position.TYPE));

        // Outside a system each add() refreshes membership at once, so this fires on the
        // second of Position/Mover, mid-construction. Inside a system (SpawnSystem)
        // it fires once, after the system returns, with the entity complete.
        var movers = world.family(Family.all(Position.TYPE, Mover.TYPE));
        movers.onAdd(e -> Log.debug(TAG, "%s joined movers (animator=%b, updating=%b)", e, Animator.has(e), world.isUpdating()));
        movers.onRemove(e -> Log.debug(TAG, "%s left movers (alive=%b)", e, e.isAlive()));
    }

    public Entity platformerPlayer(int x, int y) {
        var ledgeGrab = PlatformerTuning.Abilities.LedgeGrab.PUSH;
        var tuning = new PlatformerTuning(
                UNIT,                   // unit -> pixels, base unit that tuning params are authored in (often tile size)
                7, 0.1f, 0.1f,          // ground: speed (tiles/s), accel time, stop time
                8, 0.2f, 0.2f, 22,      // air: speed, accel time, stop time, max fall speed
                5, 0.38f,               // jump: apex height (tiles), time to peak
                2.5f, 2f,               // gravity multipliers: jump cut, fall
                0.1f, 0.12f,            // jump helpers: buffer time, coyote time
                new PlatformerTuning.Abilities(
                        6, 10, 0.2f,            // wall: slide speed, jump speed x, steer-lock time
                        ledgeGrab, 0.25f, 0.1f, // ledge: grab rule, reach, regrab time
                        20, 0.2f, 0.4f));       // dash: speed, time, cooldown
        var inputControls = InputControls.wasd(com.badlogic.gdx.Input.Keys.SPACE);
        return world.createEntity()
                .add(new Name("player"))
                .add(new Position(x, y))
                .add(new Health(3))
                .add(new Contacts())
                .add(new Animator(ZenAnimHero.IDLE, 8, 0))
                .add(new Behavior(PlayerState.NORMAL))
                .add(new Mover(new PlatformerMotion(tuning)))
                .add(new Input(inputControls))
                .add(Collider.capsule(GameMask.PLAYER, -4, 0, 8, 16)
                        .blockedBy(GameMask.SOLID)
                        .detects(GameMask.ENEMY, GameMask.TRIGGER, GameMask.HAZARD));
    }

    public Entity overheadPlayer(int x, int y) {
        var controls = InputControls.wasd(com.badlogic.gdx.Input.Keys.SPACE);
        var tuning = new TopDownTuning(
                UNIT,    // authoring unit -> pixels
                6,       // speed (tiles/sec, same for every direction)
                0.08f,   // accel time
                0.08f);  // stop time

        return world.createEntity()
                .add(new Name("player"))
                .add(new Position(x, y))
                .add(new Health(3))
                .add(new Contacts())
                .add(new Input(controls))
                .add(new Animator(ZenAnimHero.IDLE, 8, 0))
                .add(new Behavior(PlayerState.NORMAL))
                .add(new Mover(new TopDownMotion(tuning)))
                .add(Collider.rect(GameMask.PLAYER, -4, 0, 8, 8) // feet: sprite's top half may overlap a wall behind it
                        .blockedBy(GameMask.SOLID)
                        .detects(GameMask.ENEMY, GameMask.TRIGGER, GameMask.HAZARD));
    }

    public Entity bouncer(TextureRegion region, int x, int y, float heightTiles) {
        return trigger("bouncer", region, x, y, new Trigger.Bounce(heightTiles));
    }

    public Entity jumpthru(int x, int y, int w) {
        var image = new Image(pixel, w, 1);
        image.tint.set(Color.SKY);
        return world.createEntity()
                .add(new Name("platform"))
                .add(new Position(x, y))
                .add(Collider.platform(GameMask.SOLID, w))
                .add(image);
    }

    public Entity spike(TextureRegion region, int x, int y, Direction dir) {
        var image = new Image(region, UNIT, UNIT);
        if (region == pixel) image.tint.set(Color.RED);
        // TODO: dir picks the sprite orientation once spikes have art; the hazard box is the full tile for now
        return world.createEntity()
                .add(new Name("spike"))
                .add(new Position(x, y))
                .add(Collider.rect(GameMask.HAZARD, 0, 0, UNIT, UNIT))
                .add(image);
    }

    public Entity trigger(String name, TextureRegion region, int x, int y, Trigger.Effect effect) {
        var image = new Image(region, UNIT, UNIT);
        return world.createEntity()
                .add(new Name("trigger:" + name))
                .add(new Position(x, y))
                .add(new Trigger(effect))
                .add(Collider.rect(GameMask.TRIGGER, 0, 0, UNIT, UNIT))
                .add(image);
    }

    public Entity pop(int x, int y) {
        var animator = new Animator(ZenAnimHero.EFFECT_LAND);
        var duration = animator.animation.getAnimationDuration();
        animator.origin.set(8, 0);
        return world.createEntity()
                .add(new Name("pop"))
                .add(new Position(x, y))
                .add(new Lifetime(duration))
                .add(animator);
    }

    /** A 12x12 crate that falls under the hero's gravity, rests on anything SOLID and is SOLID itself, so heroes stand on it and crates stack. */
    public Entity crate(TextureRegion pixel, int x, int y) {
        var image = new Image(pixel, 12, 12);
        image.tint.set(Color.BROWN);

        var entity = world.createEntity()
                .add(new Name("crate"))
                .add(new Position(x, y))
                .add(image)
                .add(new Mover(new PlatformerMotion(PlatformerTuning.ballistic(UNIT), PlatformerMode.BALLISTIC)))
                .add(Collider.rect(GameMask.SOLID, 0, 0, 12, 12).blockedBy(GameMask.SOLID));

        Log.debug(TAG, "spawned %s at (%d, %d)", entity, x, y);
        return entity;
    }

    // ------------------------------------------------------------------------
    // Enemies: ENEMY (or HAZARD) colliders the player detects; GameContacts does the hurting
    // ------------------------------------------------------------------------

    public Entity walker(TextureRegion region, int x, int y, Direction dir) {
        var input = new Input();
        input.moveX = (dir == Direction.LEFT) ? -1 : 1;

        var whisker = new Sensor.Whisker(-90, UNIT / 2f + 1, 0);
        var sensor  = new Sensor(UNIT / 2f + 4, CollisionMask.of(GameMask.SOLID), 0, whisker);
        sensor.facing = input.moveX;

        return world.createEntity()
                .tag(GameTags.ENEMY)
                .add(new Name("walker"))
                .add(new Position(x, y))
                .add(new Image(region, UNIT, UNIT))
                .add(new Behavior(EnemyState.WALK))
                .add(new Mover(new PlatformerMotion(ENEMY_TUNING)))
                .add(Collider.rect(GameMask.ENEMY, 0, 0, UNIT, UNIT).blockedBy(GameMask.SOLID))
                .add(input)
                .add(sensor);
    }

    public Entity flyer(TextureRegion region, int x, int y, Direction dir, float speed, float range, float amplitude, float period) {
        var motion = new FlightMotion(x, dir.deltaX, speed * UNIT, range * UNIT, amplitude * UNIT, period);
        return world.createEntity()
                .tag(GameTags.ENEMY)
                .add(new Name("flyer"))
                .add(new Position(x, y))
                .add(new Image(region, UNIT, UNIT))
                .add(new Mover(motion))
                .add(Collider.rect(GameMask.ENEMY, 0, 0, UNIT, UNIT));
    }

    public Entity jumper(TextureRegion region, int x, int y, Direction dir, float period, float dist, float height) {
        var motion = new PlatformerMotion(ENEMY_TUNING);
        motion.facing = (dir == Direction.LEFT) ? -1 : 1;

        var mover = new Mover(motion);
        mover.onHitX = (ent, mov, hitDir) -> mov.motion(PlatformerMotion.class).facing = -hitDir;

        var timers = new Timers();
        var entity = world.createEntity()
                .tag(GameTags.ENEMY)
                .add(new Name("jumper"))
                .add(new Position(x, y))
                .add(new Image(region, UNIT, UNIT))
                .add(Collider.rect(GameMask.ENEMY, 0, 0, UNIT, UNIT).blockedBy(GameMask.SOLID))
                .add(mover)
                .add(timers);

        timers.every("hop", period, hop -> {
            var mov = Mover.get(hop.entity());
            var mot = mov.motion(PlatformerMotion.class);
            if (!mot.grounded) return;
            mot.launch(mov, mot.facing, dist, height, /*lock*/ 1f);
        });
        return entity;
    }

    public Entity leaper(TextureRegion region, int x, int y, int targetX, int targetY, float period, float height) {
        var timers = new Timers();
        var entity = world.createEntity()
                .tag(GameTags.ENEMY)
                .add(new Name("leaper"))
                .add(new Position(x, y))
                .add(new Image(region, UNIT, UNIT))
                .add(new Mover(new PlatformerMotion(ENEMY_TUNING)))
                .add(Collider.rect(GameMask.ENEMY, 0, 0, UNIT, UNIT).blockedBy(GameMask.SOLID))
                .add(timers);

        timers.every("leap", period, leap -> {
            var pos = Position.get(leap.entity());
            var mov = Mover.get(leap.entity());
            var mot = mov.motion(PlatformerMotion.class);
            if (!mot.grounded) return;
            int dCurrent = Math.abs(pos.x - x)       + Math.abs(pos.y - y);
            int dTarget  = Math.abs(pos.x - targetX) + Math.abs(pos.y - targetY);
            var nearSpawn = dCurrent <= dTarget;
            int destX = nearSpawn ? targetX : x;
            int destY = nearSpawn ? targetY : y;
            leap(mov, mot, destX - pos.x, destY - pos.y, height);
        });
        return entity;
    }

    static void leap(Mover mover, PlatformerMotion motion, int dx, int dy, float apex) {
        var tuning = motion.tuning;
        var h = Math.max(dy, 0) / tuning.unit() + apex;
        var v0 = tuning.impulseForHeight(h);
        var g = tuning.gravity();
        var t = (-v0 - (float) Math.sqrt(v0 * v0 + 2f * g * dy)) / g;
        var hang = tuning.hangTime(h);
        var dir = (dx < 0) ? -1 : 1;
        var dist = Math.abs(dx) * hang / (t * tuning.unit());
        motion.launch(mover, dir, dist, h, /*lock*/ (t + 0.1f) / hang);
    }

    public Entity shooter(TextureRegion region, int x, int y, Direction dir, float interval, float speed, boolean gravity) {
        var timers = new Timers();

        var entity = world.createEntity()
                .tag(GameTags.ENEMY)
                .add(new Name("shooter"))
                .add(new Position(x, y))
                .add(new Image(region, UNIT, UNIT))
                .add(Collider.rect(GameMask.ENEMY, 0, 0, UNIT, UNIT))
                .add(timers);

        timers.every("shoot", interval, shoot -> {
            var pos = Position.get(shoot.entity());
            bullet(pos.x + UNIT / 2 + dir.deltaX * UNIT,
                    pos.y + UNIT / 2 + dir.deltaY * UNIT,
                    dir.deltaX * speed * UNIT,
                    dir.deltaY * speed * UNIT,
                    gravity);
        });
        return entity;
    }

    public Entity bullet(int x, int y, float xVel, float yVel, boolean gravity) {
        var image = new Image(pixel, 4, 4);
        image.origin.set(2, 2);
        image.tint.set(Color.YELLOW);

        var motion = gravity ? new PlatformerMotion(PlatformerTuning.ballistic(UNIT), PlatformerMode.BALLISTIC) : Motion.NONE;
        var mover  = new Mover(motion, xVel, yVel);
        var pop    = (Mover.HitCallback) (ent, mov, hitDir) -> {
            var pos = Position.get(ent);
            pop(pos.x - (int) image.origin.x, pos.y - (int) image.origin.y);
            world.despawn(ent);
        };
        mover.onHitX = pop;
        mover.onHitY = pop;

        return world.createEntity()
                .tag(GameTags.BULLET)
                .add(new Name("bullet"))
                .add(new Position(x, y))
                .add(new Lifetime(BULLET_LIFETIME))
                .add(Collider.circ(GameMask.HAZARD, 0, 0, 2).blockedBy(GameMask.SOLID))
                .add(image)
                .add(mover);
    }

    public Entity ball(TextureRegion region, int x, int y, Direction dir, float speed, float height) {
        var image = new Image(region, UNIT, UNIT);
        image.origin.set(UNIT / 2f, UNIT / 2f);
        var tuning = PlatformerTuning.ballistic(UNIT);
        var mover  = new Mover(new PlatformerMotion(tuning, PlatformerMode.BALLISTIC), dir.deltaX * speed * UNIT, 0);
        mover.onHitX = (ent, mov, hitDir) -> mov.xVel = -hitDir * speed * UNIT;
        mover.onHitY = (ent, mov, hitDir) -> { if (hitDir < 0) mov.yVel = tuning.impulseForHeight(height); };

        return world.createEntity()
                .tag(GameTags.BULLET)
                .add(new Name("ball"))
                .add(new Position(x, y))
                .add(Collider.circ(GameMask.ENEMY, UNIT / 2f, UNIT / 2f, UNIT / 2f).blockedBy(GameMask.SOLID))
                .add(image)
                .add(mover);
    }

    public Entity patrol(TextureRegion region, int x, int y, Direction dir) {
        var input = new Input();
        input.moveX = dir.deltaX;
        input.moveY = dir.deltaY;

        return world.createEntity()
                .tag(GameTags.ENEMY)
                .add(new Name("patrol"))
                .add(new Position(x, y))
                .add(new Image(region, UNIT, UNIT))
                .add(new Behavior(EnemyState.PATROL))
                .add(new Mover(new TopDownMotion(PATROL_TUNING)))
                .add(Collider.rect(GameMask.ENEMY, 0, 0, UNIT, UNIT).blockedBy(GameMask.SOLID))
                .add(input);
    }

    /** A keyboard-driven platformer hero: {@code controls} steer it, the action key jumps, a double tap dashes. */
    public Entity platformerHero(int x, int y, InputControls controls, PlatformerTuning tuning) {
        var entity = world.createEntity()
                .add(new Name("hero"))
                .add(new Position(x, y))
                .add(new Input(controls))
                .add(new Health(3))
                .add(new Contacts())
                .add(new Animator(ZenAnimHero.IDLE, 8, 0))
                .add(new Mover(new PlatformerMotion(tuning)))
                .add(new Behavior(HeroState.NORMAL))
                .add(Collider.rect(GameMask.PLAYER, -4, 0, 8, 16)
                        .blockedBy(GameMask.SOLID, GameMask.PLAYER)
                        .detects(GameMask.TRIGGER, GameMask.ENEMY));
        Log.debug(TAG, "spawned platformer %s at (%d, %d)", entity, x, y);
        return entity;
    }

    public Entity hero(int x, int y) {
        var pos   = new Position(x, y);
        var speed = MathUtils.random(20f, 60f);
        var angle = MathUtils.random(0f, MathUtils.PI2);
        var velX  = speed * MathUtils.cos(angle);
        var velY  = speed * MathUtils.sin(angle);
        var entity = world.createEntity()
                .add(pos)
                .add(new Name("hero"))
                .add(new Health(3))
                .add(new Mover(new BounceMotion(velX, velY), velX, velY))
                .add(new Animator(ZenAnimHero.RUN, 8, 0))
                .add(new Lifetime(MathUtils.random(5f, 15f)));
        Log.debug(TAG, "spawned %s at (%d, %d)", entity, x, y);
        return entity;
    }

    /** A {@link #hero} that collides: blocked by SOLID and other heroes, detecting triggers and heroes, with contact memory. No lifetime, so bumps can be watched. */
    public Entity collidingHero(int x, int y, HeroShape shape) {
        var hero = hero(x, y);
        hero.remove(Lifetime.TYPE);
        var collider = switch (shape) {
            case CAPSULE -> Collider.capsule(GameMask.PLAYER, -4, 0, 8, 16);
            case RECT    -> Collider.rect(GameMask.PLAYER, -4, 0, 8, 16);
        };
        hero.add(collider
                .blockedBy(GameMask.SOLID, GameMask.PLAYER)
                .detects(GameMask.TRIGGER, GameMask.PLAYER));
        hero.add(new Contacts());
        return hero;
    }

    /** A tinted {@code Image} filling the map behind everything, to show {@code RenderLayer} sorting. */
    public Entity backdrop(TextureRegion pixel, int width, int height) {
        var image = new Image(pixel, width, height);
        image.tint.set(Color.ORANGE);
        image.layer = RenderLayer.BACKGROUND;
        var entity = world.createEntity()
                .add(new Name("backdrop"))
                .add(new Position(0, 0))
                .add(image);
        Log.debug(TAG, "spawned %s at (0, 0, %d, %d)", entity, width, height);
        return entity;
    }

    public Entity wall(TextureRegion pixel, String name, int x, int y, int w, int h) {
        var image = new Image(pixel, w, h);
        image.tint.set(Color.SALMON);
        return world.createEntity()
                .add(new Name(name))
                .add(new Position(x, y))
                .add(image)
                .add(Collider.rect(GameMask.SOLID, 0, 0, w, h));
    }

    /** A one-way platform: heroes falling onto it stop, heroes rising through it pass. Drawn one pixel tall. */
    public Entity platform(TextureRegion pixel, int x, int y, int w) {
        var image = new Image(pixel, w, 1);
        image.tint.set(Color.NAVY);
        return world.createEntity()
                .add(new Name("platform"))
                .add(new Position(x, y))
                .add(image)
                .add(Collider.platform(GameMask.SOLID, w));
    }

    /** A vertical one-way wall supporting its right side: heroes moving left are stopped, heroes moving right pass through. Drawn one pixel wide. */
    public Entity oneWayWall(TextureRegion pixel, int x, int y, int h) {
        var image = new Image(pixel, 1, h);
        image.tint.set(Color.NAVY);
        return world.createEntity()
                .add(new Name("one-way wall"))
                .add(new Position(x, y))
                .add(image)
                .add(Collider.segment(GameMask.SOLID, CollisionAASeg.Axis.VERTICAL, 0, 0, h, CollisionAASeg.Facing.POSITIVE));
    }

    /** A zone heroes detect but are not blocked by. Drawn translucent. */
    public Entity trigger(TextureRegion pixel, String name, int x, int y, int w, int h) {
        var image = new Image(pixel, w, h);
        image.tint.set(0.2f, 0.9f, 0.9f, 0.35f);
        return world.createEntity()
                .add(new Name("trigger:" + name))
                .add(new Position(x, y))
                .add(image)
                .add(Collider.rect(GameMask.TRIGGER, 0, 0, w, h));
    }

    public Entity ramps(int x, int y) {
        var collider = Collider.grid(GameMask.SOLID, 16, 4, 3);
        var grid = collider.shape(CollisionGrid.class);
        for (int cx = 0; cx < 4; cx++) grid.set(cx, 0, CollisionGrid.CellShape.FULL);
        grid.set(0, 1, CollisionGrid.CellShape.RAMP_NE); // climbs to the right
        grid.set(1, 1, CollisionGrid.CellShape.FULL);
        grid.set(2, 1, CollisionGrid.CellShape.FULL);
        grid.set(3, 1, CollisionGrid.CellShape.RAMP_NW); // climbs to the left
        grid.set(1, 2, CollisionGrid.CellShape.RAMP_SW);
        grid.set(2, 2, CollisionGrid.CellShape.RAMP_SE);
        return world.createEntity()
                .add(new Name("ramps"))
                .add(new Position(x, y))
                .add(collider);
    }
}
