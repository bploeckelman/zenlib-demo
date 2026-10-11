package com.zendogames.zenlibdemo.ecs;

import com.badlogic.gdx.graphics.g2d.TextureRegion;

/** The level pieces the screens share, so the walled map is built in one place. */
public final class Level {

    private Level() {}

    /** Backdrop plus four SOLID walls of thickness {@code wall} around a {@code w x h} map. The floor's top is {@code wall}. */
    public static void walled(Factory factory, TextureRegion pixel, int w, int h, int wall) {
        factory.backdrop(pixel, w, h);
        factory.wall(pixel, "wall:left",   0,        0,        wall, h);
        factory.wall(pixel, "wall:right",  w - wall, 0,        wall, h);
        factory.wall(pixel, "wall:bottom", 0,        0,        w, wall);
        factory.wall(pixel, "wall:top",    0,        h - wall, w, wall);
    }
}
