package com.zendogames.zenlibdemo.assets;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.zendogames.zenlib.assets.Asset;
import com.zendogames.zenlib.assets.MapType;

/** The demo's Tiled maps, loaded with everything else at startup; registered in {@code Main.init()}. */
public enum GameMap implements MapType {
    DEMO("maps/warp-room.tmx");

    /** Class property defaults come from here; every demo map shares the one project. */
    private static final String PROJECT = "maps/zenlib-demo.tiled-project";

    private final Asset<TiledMap> asset = new Asset<>(this);
    private final String path;

    GameMap(String path) {
        this.path = path;
    }

    @Override public Asset<TiledMap> asset() { return asset; }
    @Override public FileHandle file()       { return Gdx.files.internal(path); }
    @Override public String projectFile()    { return PROJECT; }
}
