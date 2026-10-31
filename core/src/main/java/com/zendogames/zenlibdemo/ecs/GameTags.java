package com.zendogames.zenlibdemo.ecs;

import com.zendogames.zenlib.ecs.Tag;

/** Data-less markers; families match on them like {@code Component}s. */
public final class GameTags {

    public static final Tag PLAYER = Tag.register("Player");
    public static final Tag ENEMY  = Tag.register("Enemy");
    public static final Tag BULLET = Tag.register("Bullet");

    private GameTags() {}
}
