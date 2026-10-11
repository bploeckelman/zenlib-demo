package com.zendogames.zenlibdemo.ecs.systems;

import com.zendogames.zenlib.ecs.Entity;
import com.zendogames.zenlib.ecs.Family;
import com.zendogames.zenlib.ecs.IteratingSystem;
import com.zendogames.zenlib.ecs.SystemOrder;
import com.zendogames.zenlib.ecs.components.Animator;
import com.zendogames.zenlib.ecs.components.Collider;
import com.zendogames.zenlib.ecs.components.Mover;
import com.zendogames.zenlib.ecs.components.Position;

/**
 * Keeps collider-less movers inside the map; right after {@code MovementSystem}, a mover past an
 * edge is clamped to it and its velocity on that axis is reversed. Movers with a {@link Collider}
 * are left to the walls.
 */
public class EdgeBounceSystem extends IteratingSystem {

    private final int mapWidth;
    private final int mapHeight;

    public EdgeBounceSystem(int mapWidth, int mapHeight) {
        super(Family.all(Position.TYPE, Mover.TYPE).exclude(Collider.TYPE), SystemOrder.PHYSICS + 1);
        this.mapWidth = mapWidth;
        this.mapHeight = mapHeight;
    }

    // @formatter:off
    @Override
    protected void process(Entity e, float dt) {
        var position = Position.get(e);
        var animator = Animator.get(e);
        var mover    = Mover.get(e);
        int w = (animator != null) ? (int) animator.size.x : 0;
        int h = (animator != null) ? (int) animator.size.y : 0;
        if (position.x < 0)             { position.x = 0;             mover.xVel = +Math.abs(mover.xVel); }
        if (position.x > mapWidth  - w) { position.x = mapWidth  - w; mover.xVel = -Math.abs(mover.xVel); }
        if (position.y < 0)             { position.y = 0;             mover.yVel = +Math.abs(mover.yVel); }
        if (position.y > mapHeight - h) { position.y = mapHeight - h; mover.yVel = -Math.abs(mover.yVel); }
    }
    // @formatter:on
}
