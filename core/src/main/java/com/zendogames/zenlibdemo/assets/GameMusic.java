package com.zendogames.zenlibdemo.assets;

import com.badlogic.gdx.audio.Music;
import com.zendogames.zenlib.assets.Asset;
import com.zendogames.zenlib.assets.MusicType;

/** The demo's tracks, registered in {@code Main.init()}; both loop on the MUSIC bus. */
public enum GameMusic implements MusicType {
      MENU  ("music/infinite-descent.ogg")
    , LEVEL ("music/sad-descent.ogg")
    ;

    private final Asset<Music> asset = new Asset<>(this);
    private final String file;

    GameMusic(String file) {
        this.file = file;
    }

    @Override public Asset<Music> asset() { return asset; }
    @Override public String file()         { return file; }
}
