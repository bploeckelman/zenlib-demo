package com.zendogames.zenlibdemo.ecs.components;

import com.zendogames.zenlib.ecs.Component;
import com.zendogames.zenlib.ecs.ComponentType;
import com.zendogames.zenlib.ecs.Entity;

/**
 * What the {@code FleeSystem} remembers between ticks for one watcher: whether it is running, whether
 * it is cornered, how long since it last saw a threat, and which way it last ran. Captured state goes
 * in a component, not in the system, so two watchers never share it and the debug views can show it.
 */
public class Flee implements Component {

    public static final ComponentType<Flee> TYPE = ComponentType.register(Flee.class);
    public static Flee    get(Entity e) { return TYPE.get(e); }
    public static boolean has(Entity e) { return TYPE.has(e); }

    /** True while running: set when the threats in view press harder than the start threshold, cleared below the stop one or after sight is lost for good. */
    public boolean fleeing;

    /** True while fleeing with nowhere to run: the threats in view pull both ways (or straight down from above), so it stands instead of flipping. */
    public boolean cornered;

    /** Seconds since any threat was in view; 0 while one is. */
    public float sinceSeen;

    /** The way it last ran (-1, +1), kept through a short loss of sight; 0 when standing. */
    public int lastMoveX;

    @Override
    public String toString() {
        return "Flee{%s, sinceSeen %.2f, lastMoveX %+d}"
                .formatted(!fleeing ? "standing" : cornered ? "cornered" : "fleeing", sinceSeen, lastMoveX);
    }
}
