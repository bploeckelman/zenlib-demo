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

    // TODO: example only, should probably live elsewhere when provided by zenlib
    /** Position at the start of the last tick, for render interpolation. */
    public int prevX, prevY;

    public Velocity(float dx, float dy, Position start) {
        this.dx = dx;
        this.dy = dy;
        this.remX = 0;
        this.remY = 0;
        this.prevX = start.x;
        this.prevY = start.y;
    }

    private final Link link = new Link(TYPE);
    @Override public Link link() { return link; }
}
