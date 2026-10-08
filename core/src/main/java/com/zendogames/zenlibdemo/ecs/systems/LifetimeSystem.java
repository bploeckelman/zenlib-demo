package com.zendogames.zenlibdemo.ecs.systems;

import com.zendogames.zenlib.ecs.Entity;
import com.zendogames.zenlib.ecs.Family;
import com.zendogames.zenlib.ecs.IteratingSystem;
import com.zendogames.zenlib.ecs.SystemOrder;
import com.zendogames.zenlibdemo.ecs.components.Lifetime;

public class LifetimeSystem extends IteratingSystem {

    public LifetimeSystem() {
        super(Family.all(Lifetime.TYPE), SystemOrder.TIMERS);
    }

    @Override
    protected void process(Entity e, float dt) {
        var lifetime = Lifetime.get(e);
        if (--lifetime.ticks <= 0) {
            world.despawn(e);
        }
    }
}
