package com.zendogames.zenlibdemo.ecs.components;

import com.github.tommyettinger.gdcrux.PointI2;
import com.zendogames.zenlib.ecs.Component;
import com.zendogames.zenlib.ecs.ComponentType;
import com.zendogames.zenlib.ecs.Entity;

/** Integer world position of entity. */
public class Position extends PointI2 implements Component {

    public static final ComponentType<Position> TYPE = ComponentType.register(Position.class);
    public static Position get(Entity e) { return TYPE.get(e); }
    public static boolean  has(Entity e) { return TYPE.has(e); }

    public Position(int x, int y) {
        super(x, y);
    }

    private final Link link = new Link(TYPE);
    @Override public Link link() { return link; }
}
