package com.zendogames.zenlibdemo.ecs.components;

import com.badlogic.gdx.math.MathUtils;
import com.zendogames.zenlib.ecs.Component;
import com.zendogames.zenlib.ecs.ComponentType;
import com.zendogames.zenlib.ecs.Entity;
import com.zendogames.zenlib.sim.SimulationStep;

/** Ticks until despawn. Seconds authored, ticks stored. */
public class Lifetime implements Component {

    public static final ComponentType<Lifetime> TYPE = ComponentType.register(Lifetime.class);
    public static Lifetime get(Entity e) { return TYPE.get(e); }
    public static boolean  has(Entity e) { return TYPE.has(e); }

    public int ticks;

    public Lifetime(float seconds) {
        // TODO: get from SimulationStep.ticks(seconds) once available
        this.ticks = MathUtils.round(seconds / SimulationStep.FIXED_DT);
    }

    private final Link link = new Link(TYPE);
    @Override public Link link() { return link; }
}
