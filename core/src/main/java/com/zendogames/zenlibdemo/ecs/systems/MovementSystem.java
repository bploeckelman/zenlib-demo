package com.zendogames.zenlibdemo.ecs.systems;

import com.zendogames.zenlib.ecs.Entity;
import com.zendogames.zenlib.ecs.Family;
import com.zendogames.zenlib.ecs.IteratingSystem;
import com.zendogames.zenlib.ecs.SystemOrder;
import com.zendogames.zenlib.ecs.components.Animator;
import com.zendogames.zenlib.ecs.components.Position;
import com.zendogames.zenlibdemo.ecs.components.Velocity;

/** Integrates {@link Velocity} onto {@link Position} and bounces off the world edges. */
public class MovementSystem extends IteratingSystem {

    private final int mapWidth;
    private final int mapHeight;

    public MovementSystem(int mapWidth, int mapHeight) {
        super(Family.all(Position.TYPE, Velocity.TYPE), SystemOrder.PHYSICS);
        this.mapWidth  = mapWidth;
        this.mapHeight = mapHeight;
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
        // TODO: will eventually pixel walk, checking colliders along the way and preventing movement that would overlap
        pos.x += moveX;
        pos.y += moveY;

        var animator = Animator.get(e);
        int w = (animator != null) ? (int) animator.size.x : 0;
        int h = (animator != null) ? (int) animator.size.y : 0;
        if (pos.x < 0)             { pos.x = 0;             vel.dx =  Math.abs(vel.dx); }
        if (pos.x > mapWidth  - w) { pos.x = mapWidth  - w; vel.dx = -Math.abs(vel.dx); }
        if (pos.y < 0)             { pos.y = 0;             vel.dy =  Math.abs(vel.dy); }
        if (pos.y > mapHeight - h) { pos.y = mapHeight - h; vel.dy = -Math.abs(vel.dy); }
    }
}
