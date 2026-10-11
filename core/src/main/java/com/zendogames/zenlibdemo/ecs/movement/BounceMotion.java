package com.zendogames.zenlibdemo.ecs.movement;

import com.github.tommyettinger.gdcrux.PointI2;
import com.zendogames.zenlib.collision.shapes.CollisionShape;
import com.zendogames.zenlib.ecs.components.Mover;
import com.zendogames.zenlib.ecs.movement.Motion;
import com.zendogames.zenlib.ecs.movement.MoveIntent;
import com.zendogames.zenlib.ecs.movement.MoveWorld;

/**
 * The demo's bouncing heroes as a game-side {@link Motion}: constant speed, reversed on whatever
 * the sweep reports as a hit. The sweep zeroes the velocity on a block, so the speed to resume
 * with is remembered here. Movers without a collider get their edge bounces from {@code EdgeBounceSystem}.
 */
public class BounceMotion implements Motion {

    private final float speedX;
    private final float speedY;

    public BounceMotion(float speedX, float speedY) {
        this.speedX = speedX;
        this.speedY = speedY;
    }

    @Override
    public void control(Mover mover, MoveIntent intent, float dt) {
        // Velocity carries over; nothing to steer
    }

    @Override
    public void after(Mover mover, MoveIntent intent, PointI2 pos, CollisionShape shape, MoveWorld world) {
        if (mover.xHit != 0) mover.xVel = -mover.xHit * speedX;
        if (mover.yHit != 0) mover.yVel = -mover.yHit * speedY;
    }
}
