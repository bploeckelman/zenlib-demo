package com.zendogames.zenlibdemo.ecs.components;

import com.zendogames.zenlib.ecs.Component;
import com.zendogames.zenlib.ecs.ComponentType;
import com.zendogames.zenlib.ecs.Entity;

/** Pixels per second, integrated onto an integer {@link Position} with sub-pixel remainders. */
public class Velocity implements Component {

    public static final ComponentType<Velocity> TYPE = ComponentType.register(Velocity.class);
    public static Velocity get(Entity e) { return TYPE.get(e); }
    public static boolean  has(Entity e) { return TYPE.has(e); }

    public float dx, dy;

    /** Fraction of a pixel not yet applied. */
    public float remX;
    public float remY;

    public Velocity(float dx, float dy) {
        this.dx = dx;
        this.dy = dy;
        this.remX = 0;
        this.remY = 0;
    }
}
