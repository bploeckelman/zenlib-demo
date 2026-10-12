package com.zendogames.zenlibdemo.ecs;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.zendogames.zenlib.Log;
import com.zendogames.zenlib.assets.ZenAnimHero;
import com.zendogames.zenlib.collision.shapes.CollisionAASeg;
import com.zendogames.zenlib.collision.shapes.CollisionGrid;
import com.zendogames.zenlib.ecs.Entity;
import com.zendogames.zenlib.ecs.Family;
import com.zendogames.zenlib.ecs.World;
import com.zendogames.zenlib.ecs.components.*;
import com.zendogames.zenlib.ecs.movement.platformer.PlatformerMode;
import com.zendogames.zenlib.ecs.movement.platformer.PlatformerMotion;
import com.zendogames.zenlib.ecs.movement.platformer.PlatformerTuning;
import com.zendogames.zenlib.ecs.movement.topdown.TopDownMotion;
import com.zendogames.zenlib.ecs.movement.topdown.TopDownTuning;
import com.zendogames.zenlib.input.InputControls;
import com.zendogames.zenlib.render.RenderLayer;
import com.zendogames.zenlibdemo.ecs.components.Health;
import com.zendogames.zenlibdemo.ecs.movement.BounceMotion;

/** Spawns the demo's entities. Logs family joins so the per-add timing in setup code is visible. */
public class Factory {

    private static final String TAG = Factory.class.getSimpleName();

    /** The demo's tile size: the unit every tuning is authored in. */
    public static final int UNIT = 16;

    /** Hero frames are 16x16 with the origin at the bottom-left; the body is the middle 8px. */
    public enum HeroShape { CAPSULE, RECT }

    private final World world;

    public Factory(World world) {
        this.world = world;

        // Outside a system each add() refreshes membership at once, so this fires on the
        // second of Position/Mover, mid-construction. Inside a system (SpawnSystem)
        // it fires once, after the system returns, with the entity complete.
        var movers = world.family(Family.all(Position.TYPE, Mover.TYPE));
        movers.onAdd(e -> Log.debug(TAG, "%s joined movers (animator=%b, updating=%b)", e, Animator.has(e), world.isUpdating()));
        movers.onRemove(e -> Log.debug(TAG, "%s left movers (alive=%b)", e, e.isAlive()));
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

    // ------------------------------------------------------------------------
    // Movers for MovementScreen
    // ------------------------------------------------------------------------

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
                        .blockedBy(GameMask.SOLID)
                        .detects(GameMask.ENEMY));
        Log.debug(TAG, "spawned platformer %s at (%d, %d)", entity, x, y);
        return entity;
    }

    /** A keyboard-driven top-down walker: 8-way, no gravity, an 8x8 foot box. */
    public Entity walker(int x, int y, InputControls controls) {
        var animator = new Animator(ZenAnimHero.IDLE, 8, 0);
        animator.tint.set(Color.SKY);

        var entity = world.createEntity()
                .add(new Name("walker"))
                .add(new Position(x, y))
                .add(new Input(controls))
                .add(new Behavior(HeroState.NORMAL))
                .add(new Mover(new TopDownMotion(TopDownTuning.walker(UNIT))))
                .add(new Contacts())
                .add(animator)
                .add(Collider.rect(GameMask.PLAYER, -4, 0, 8, 16).blockedBy(GameMask.SOLID));

        Log.debug(TAG, "spawned walker %s at (%d, %d)", entity, x, y);
        return entity;
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

    // ------------------------------------------------------------------------
    // Level pieces for CollisionScreen and MovementScreen
    // ------------------------------------------------------------------------

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
