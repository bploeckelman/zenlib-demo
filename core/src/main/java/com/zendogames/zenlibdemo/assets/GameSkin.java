package com.zendogames.zenlibdemo.assets;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.zendogames.zenlib.assets.Asset;
import com.zendogames.zenlib.assets.SkinType;

public enum GameSkin implements SkinType {
    DEFAULT("ui/uiskin.json")
    ;

    private final Asset<Skin> asset;
    private final String path;

    GameSkin(String path) {
        this.asset = new Asset<>(this);
        this.path = path;
    }

    @Override public Asset<Skin> asset() { return asset; }
    @Override public FileHandle file() { return Gdx.files.classpath(path); }
}
