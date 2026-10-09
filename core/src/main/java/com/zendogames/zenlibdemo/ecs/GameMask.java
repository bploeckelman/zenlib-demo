package com.zendogames.zenlibdemo.ecs;

import com.zendogames.zenlib.collision.CollisionMask;

/** The demo's collider categories. One-way platforms are a shape, not a mask: they are plain {@link #SOLID} segments. */
public enum GameMask implements CollisionMask {
      SOLID
    , PLAYER
    , TRIGGER
    ;

      @Override
      public int bit() {
          return 1 << ordinal();
      }
}
