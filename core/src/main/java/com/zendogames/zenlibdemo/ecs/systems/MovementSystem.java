package com.zendogames.zenlibdemo.ecs.systems;

import com.zendogames.zenlib.collision.CollisionWorld;
import com.zendogames.zenlib.ecs.Entity;
import com.zendogames.zenlib.ecs.Family;
import com.zendogames.zenlib.ecs.IteratingSystem;
import com.zendogames.zenlib.ecs.SystemOrder;
import com.zendogames.zenlib.ecs.components.Animator;
import com.zendogames.zenlib.ecs.components.Collider;
import com.zendogames.zenlib.ecs.components.Contacts;
import com.zendogames.zenlib.ecs.components.Position;
import com.zendogames.zenlibdemo.ecs.components.Velocity;

/**
 * Integrates {@link Velocity} onto {@link Position}. An entity with a {@link Collider} walks a
 * pixel at a time against the {@link CollisionWorld} and bounces off whatever blocks it,
 * reporting the block to its {@link Contacts}; one without bounces off the map edges.
 */
public class MovementSystem extends IteratingSystem {

    private final int mapWidth;
    private final int mapHeight;
    private final CollisionWorld collision;

    public MovementSystem(int mapWidth, int mapHeight) {
        this(mapWidth, mapHeight, null);
    }

    public MovementSystem(int mapWidth, int mapHeight, CollisionWorld collision) {
        super(Family.all(Position.TYPE, Velocity.TYPE), SystemOrder.PHYSICS);
        this.mapWidth  = mapWidth;
        this.mapHeight = mapHeight;
        this.collision = collision;
    }

    @Override
    protected void process(Entity e, float dt) {
        var pos = Position.get(e);
        var vel = Velocity.get(e);
        pos.savePrev(); // important: save previous position before moving, once per tick

        vel.remX += vel.dx * dt;
        vel.remY += vel.dy * dt;
        int moveX = (int) vel.remX;
        int moveY = (int) vel.remY;
        vel.remX -= moveX;
        vel.remY -= moveY;

        if (collision != null && Collider.has(e)) {
            walk(e, pos, vel, moveX, 0);
            walk(e, pos, vel, 0, moveY);
        } else {
            pos.x += moveX;
            pos.y += moveY;
            bounceOffEdges(e, pos, vel);
        }
    }

    /** One axis of the pixel walk: probe each pixel, stop and reverse on a block. Exactly one of {@code moveX, moveY} is non-zero. */
    private void walk(Entity e, Position pos, Velocity vel, int moveX, int moveY) {
        int dx = Integer.signum(moveX);
        int dy = Integer.signum(moveY);
        int steps = Math.abs(moveX + moveY);
        for (int i = 0; i < steps; i++) {
            // ONE_WAY counts as blocked: the demo's heroes never drop through
            if (collision.probe(e, dx, dy).isBlocked()) {
                if (Contacts.has(e)) Contacts.get(e).blocked(dx, dy, collision.lastContact());
                if (dx != 0) { vel.dx = -vel.dx; vel.remX = 0; }
                if (dy != 0) { vel.dy = -vel.dy; vel.remY = 0; }
                return;
            }
            pos.add(dx, dy);
        }
    }

    // @formatter:off
    private void bounceOffEdges(Entity e, Position pos, Velocity vel) {
        var animator = Animator.get(e);
        int w = (animator != null) ? (int) animator.size.x : 0;
        int h = (animator != null) ? (int) animator.size.y : 0;
        if (pos.x < 0)             { pos.x = 0;             vel.dx =  Math.abs(vel.dx); }
        if (pos.x > mapWidth  - w) { pos.x = mapWidth  - w; vel.dx = -Math.abs(vel.dx); }
        if (pos.y < 0)             { pos.y = 0;             vel.dy =  Math.abs(vel.dy); }
        if (pos.y > mapHeight - h) { pos.y = mapHeight - h; vel.dy = -Math.abs(vel.dy); }
    }
    // @formatter:on
}
