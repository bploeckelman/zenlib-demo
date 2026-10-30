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
      JUMP_1 ("sounds/jump1.ogg")
    , JUMP_2 ("sounds/jump2.ogg")
    , JUMP_3 ("sounds/jump3.ogg")
    , JUMP_4 ("sounds/jump4.ogg")
    , JUMP_5 ("sounds/jump5.ogg")
//    , LAND  ("audio/sounds/land.ogg")  { @Override public float pitchVariance() { return 0.1f; } }
//    , POP   ("audio/sounds/pop.ogg")   { @Override public int maxVoices() { return 4; } @Override public float duration() { return 0.3f; } }
//    , HUM   ("audio/sounds/hum.ogg")   // a loop
    , CLICK_1        ("sounds/click_001.ogg")        { @Override public AudioBus bus() { return ZenBus.UI; } }
    , CLICK_2        ("sounds/click_002.ogg")        { @Override public AudioBus bus() { return ZenBus.UI; } }
    , CLICK_3        ("sounds/click_003.ogg")        { @Override public AudioBus bus() { return ZenBus.UI; } }
    , CLOSE_1        ("sounds/close_001.ogg")        { @Override public AudioBus bus() { return ZenBus.UI; } }
    , CLOSE_2        ("sounds/close_002.ogg")        { @Override public AudioBus bus() { return ZenBus.UI; } }
    , CLOSE_3        ("sounds/close_003.ogg")        { @Override public AudioBus bus() { return ZenBus.UI; } }
    , CONFIRMATION_1 ("sounds/confirmation_001.ogg") { @Override public AudioBus bus() { return ZenBus.UI; } }
    , CONFIRMATION_2 ("sounds/confirmation_002.ogg") { @Override public AudioBus bus() { return ZenBus.UI; } }
    , CONFIRMATION_3 ("sounds/confirmation_003.ogg") { @Override public AudioBus bus() { return ZenBus.UI; } }
    ;

    public static final SoundGroup JUMPS         = SoundGroup.of(JUMP_1, JUMP_2, JUMP_3, JUMP_4, JUMP_5).noRepeat().cooldown(0.1f);
    public static final SoundGroup CLICKS        = SoundGroup.of(CLICK_1, CLICK_2, CLICK_3).noRepeat().cooldown(0.1f);
    public static final SoundGroup CLOSES        = SoundGroup.of(CLOSE_1, CLOSE_2, CLOSE_3).noRepeat().cooldown(0.1f);
    public static final SoundGroup CONFIRMATIONS = SoundGroup.of(CONFIRMATION_1, CONFIRMATION_2, CONFIRMATION_3).noRepeat().cooldown(0.1f);

    private final Asset<Sound> asset = new Asset<>(this);
    private final String file;

    GameSound(String file) {
        this.file = file;
    }

    @Override public Asset<Sound> asset() { return asset; }
    @Override public String file()         { return file; }
}
// @formatter:on
