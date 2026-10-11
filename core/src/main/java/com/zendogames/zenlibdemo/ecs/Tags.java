package com.zendogames.zenlibdemo.ecs;

import com.zendogames.zenlib.ecs.Tag;

/** Data-less markers; families match on them like {@code Component}s. */
public final class Tags {

    public static final Tag ENEMY = Tag.register("Enemy");

    private Tags() {}
}
