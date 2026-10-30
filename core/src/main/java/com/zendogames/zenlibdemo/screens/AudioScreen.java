package com.zendogames.zenlibdemo.screens;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.zendogames.zenlib.Log;
import com.zendogames.zenlib.ZenFlag;
import com.zendogames.zenlib.ZenGame;
import com.zendogames.zenlib.audio.Audio;
import com.zendogames.zenlib.render.WorldView;
import com.zendogames.zenlib.ui.DebugViews;
import com.zendogames.zenlibdemo.assets.GameMusic;
import com.zendogames.zenlibdemo.assets.GameSound;

/**
 * The audio service on a bare screen: every rule has a key, the Audio section of the Debug window
 * shows what it did (buses, voices, refusals), and the window's own play buttons try any file.
 * <pre>
 *   J  jump         one-shot on SFX
 *   F  steps x3     SoundGroup: three plays in one frame, one gets through (0.1 s group cooldown); presses never repeat a step
 *   B  pop burst    ten POP plays in one frame: maxVoices 4 lets four through, six are refused VOICES
 *   L  hum          start / stop a looping voice;  K steps its pitch
 *   M / N / X       music MENU / LEVEL (1 s crossfade; N then M mid-fade swaps back) / stop (fade out)
 *   U  duck         music to 25% and back
 *   P  pause        Audio.pause() / resume(): the hum freezes, J is refused PAUSED, C still plays
 *   C  click        a UI bus sound
 *   G  LOG_AUDIO
 *   click           LAND.playAt(worldX): panned across the unzoomed view, quieter past its edge (zoom out and click far off)
 * </pre>
 */
public class AudioScreen extends DemoScreen {

    private static final String TAG = AudioScreen.class.getSimpleName();

    static final int VIEW_WIDTH  = 320;
    static final int VIEW_HEIGHT = 180;
    static final float[] HUM_PITCHES = { 1f, 1.25f, 1.5f, 0.75f };

    long hum = -1;
    int humPitch = 0;
    boolean ducked = false;

    public AudioScreen(ZenGame game) {
        super(game, /*withCollision*/ false);
        addCommonSystems();
        worldCamera.position.set(VIEW_WIDTH / 2f, VIEW_HEIGHT / 2f, 0f);
        debugViews.add(DebugViews.audio(assets));
        actions .defaultClick((x, y) -> GameSound.JUMPS.playAt(x))
                .add("jump",        Input.Keys.J, GameSound.JUMPS::play)
                .add("jumps x3",    Input.Keys.F, this::jumps)
//                .add("pop burst",   Input.Keys.B, this::burst)
//                .add("hum loop",    Input.Keys.L, this::toggleHum)
//                .add("hum pitch",   Input.Keys.K, this::stepHumPitch)
                .add("music menu",  Input.Keys.M, () -> GameMusic.MENU.play(1f))
                .add("music level", Input.Keys.N, () -> GameMusic.LEVEL.play(1f))
                .add("music stop",  Input.Keys.X, Audio::stopMusic)
//                .add("duck",        Input.Keys.U, this::toggleDuck)
                .add("pause",       Input.Keys.P, () -> { if (Audio.isPaused()) Audio.resume(); else Audio.pause(); })
                .add("click (UI)",  Input.Keys.C, GameSound.CLICKS::play)
                .add("LOG_AUDIO",   Input.Keys.G, ZenFlag.LOG_AUDIO::toggle);
    }

    /** Three group plays in one frame: the first picks and plays, the group's cooldown refuses the other two. */
    private void jumps() {
        var refusedBefore = Audio.refusals(Audio.Refusal.COOLDOWN);
        var played = 0;
        for (int i = 0; i < 3; i++) {
            if (GameSound.JUMPS.play(0.8f) != -1) played++;
        }
        Log.info(TAG, "jumps: %d played (%s), %d refused", played, GameSound.JUMPS.last(), Audio.refusals(Audio.Refusal.COOLDOWN) - refusedBefore);
    }

    /** Ten starts in one frame: the cap lets {@code maxVoices} through and refuses the rest. */
//    private void burst() {
//        var refusedBefore = Audio.refusals(Audio.Refusal.VOICES);
//        var played = 0;
//        for (int i = 0; i < 10; i++) {
//            if (GameSound.POP.play() != -1) played++;
//        }
//        Log.info(TAG, "pop burst: %d played, %d refused", played, Audio.refusals(Audio.Refusal.VOICES) - refusedBefore);
//    }

//    private void toggleHum() {
//        if (hum != -1 && Audio.isAlive(hum)) {
//            Audio.stop(hum);
//            hum = -1;
//        } else {
//            humPitch = 0;
//            hum = GameSound.HUM.loop(0.6f);
//        }
//    }

//    private void stepHumPitch() {
//        if (hum == -1) return;
//        humPitch = (humPitch + 1) % HUM_PITCHES.length;
//        Audio.pitch(hum, HUM_PITCHES[humPitch]);
//    }

//    private void toggleDuck() {
//        ducked = !ducked;
//        Audio.duck(ducked ? 0.25f : 1f, 0.3f);
//    }

    @Override
    public void update(float dt) {
        Audio.listener(worldCamera.position.x, VIEW_WIDTH / 2f); // the unzoomed view: zoomed out, clicks past its edge attenuate
        super.update(dt);
    }

    @Override
    protected WorldView createWorldView() {
        var view = new WorldView(worldCamera, VIEW_WIDTH, VIEW_HEIGHT);
        view.clearColor.set(Color.DARK_GRAY);
        return view;
    }
}
