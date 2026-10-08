package com.zendogames.zenlibdemo.ecs.systems;

import com.zendogames.zenlib.ecs.Entity;
import com.zendogames.zenlib.ecs.Family;
import com.zendogames.zenlib.ecs.IteratingSystem;
import com.zendogames.zenlib.ecs.SystemOrder;
import com.zendogames.zenlibdemo.ecs.components.Animator;

/** Advances every animation by the tick, so animations freeze with the sim under {@code ZenFlag.FRAME_STEP}. */
public class AnimationSystem extends IteratingSystem {

    public AnimationSystem() {
        super(Family.all(Animator.TYPE), SystemOrder.PRESENT);
    }

    @Override
    protected void process(Entity e, float dt) {
        var animator = Animator.get(e);
        animator.time += dt;
    }
}
