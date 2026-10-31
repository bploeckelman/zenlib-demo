package com.zendogames.zenlibdemo.assets;

import com.badlogic.gdx.audio.Sound;
import com.zendogames.zenlib.assets.Asset;
import com.zendogames.zenlib.assets.SoundType;
import com.zendogames.zenlib.audio.AudioBus;
import com.zendogames.zenlib.audio.SoundGroup;
import com.zendogames.zenlib.audio.ZenBus;

/**
 * The demo's sound effects, registered in {@code Main.init()}. Each constant's body is where a
 * sound's rules go: varying pitch, capped number of voices, playing on the UI bus (so it is heard
 * while the game is paused. The static constants are groups of sounds with associated rules.
 */
// @formatter:off
public enum GameSound implements SoundType {
      COIN        ("sounds/coin.ogg")
    , EXPLOSION_1 ("sounds/explosion1.ogg")
    , EXPLOSION_2 ("sounds/explosion2.ogg")
    , EXPLOSION_3 ("sounds/explosion3.ogg")
    , EXPLOSION_4 ("sounds/explosion4.ogg")
    , EXPLOSION_5 ("sounds/explosion5.ogg")
    , HIT_1       ("sounds/hit1.ogg")
    , HIT_2       ("sounds/hit2.ogg")
    , HIT_3       ("sounds/hit3.ogg")
    , HIT_4       ("sounds/hit4.ogg")
    , HIT_5       ("sounds/hit5.ogg")
    , JUMP        ("sounds/jump.ogg")
    , SLIME       ("sounds/slime.ogg")
    , PLUCK_1     ("sounds/pluck1.ogg") { @Override public AudioBus bus() { return ZenBus.UI; } }
    , PLUCK_2     ("sounds/pluck2.ogg") { @Override public AudioBus bus() { return ZenBus.UI; } }
    , TONE_THREE  ("sounds/tone-three.ogg") { @Override public AudioBus bus() { return ZenBus.UI; } }
    , TONE_TWO    ("sounds/tone-two.ogg") { @Override public AudioBus bus() { return ZenBus.UI; } }
//    , LAND  ("audio/sounds/land.ogg")  { @Override public float pitchVariance() { return 0.1f; } }
//    , POP   ("audio/sounds/pop.ogg")   { @Override public int maxVoices() { return 4; } @Override public float duration() { return 0.3f; } }
//    , HUM   ("audio/sounds/hum.ogg")   // a loop
    ;

    public static final SoundGroup EXPLOSIONS = SoundGroup.of(EXPLOSION_1, EXPLOSION_2, EXPLOSION_3, EXPLOSION_4, EXPLOSION_5).noRepeat().cooldown(0.1f);
    public static final SoundGroup HITS = SoundGroup.of(HIT_1, HIT_2, HIT_3, HIT_4, HIT_5).noRepeat().cooldown(0.1f);
    public static final SoundGroup PLUCKS = SoundGroup.of(PLUCK_1, PLUCK_2).noRepeat().cooldown(0.1f);

    private final Asset<Sound> asset = new Asset<>(this);
    private final String file;

    GameSound(String file) {
        this.file = file;
    }

    @Override public Asset<Sound> asset() { return asset; }
    @Override public String file()         { return file; }
}
// @formatter:on
