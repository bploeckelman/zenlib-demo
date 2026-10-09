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
import com.zendogames.zenlib.render.RenderLayer;
import com.zendogames.zenlibdemo.ecs.components.Velocity;

/** Spawns the demo's entities. Logs family joins so the per-add timing in setup code is visible. */
public class Factory {

    private static final String TAG = Factory.class.getSimpleName();

    /** Hero frames are 16x16 with the origin at the bottom-left; the body is the middle 8px. */
    public enum HeroShape { CAPSULE, RECT }

    private final World world;

    public Factory(World world) {
        this.world = world;

        // Outside a system each add() refreshes membership at once, so this fires on the
        // second of Position/Velocity, mid-construction. Inside a system (SpawnSystem)
        // it fires once, after the system returns, with the entity complete.
        var movers = world.family(Family.all(Position.TYPE, Velocity.TYPE));
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
                .add(new Velocity(velX, velY))
                .add(new Animator(ZenAnimHero.RUN))
                .add(new Lifetime(MathUtils.random(5f, 15f)));
        Log.debug(TAG, "spawned %s at (%d, %d)", entity, x, y);
        return entity;
    }

    /** A {@link #hero} that collides: blocked by SOLID and other heroes, detecting triggers and heroes, with contact memory. No lifetime, so bumps can be watched. */
    public Entity collidingHero(int x, int y, HeroShape shape) {
        var hero = hero(x, y);
        hero.remove(Lifetime.TYPE);
        var collider = switch (shape) {
            case CAPSULE -> Collider.capsule(GameMask.PLAYER, 4, 0, 8, 16);
            case RECT    -> Collider.rect(GameMask.PLAYER, 4, 0, 8, 16);
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

    // ------------------------------------------------------------------------
    // Level pieces for CollisionScreen
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
