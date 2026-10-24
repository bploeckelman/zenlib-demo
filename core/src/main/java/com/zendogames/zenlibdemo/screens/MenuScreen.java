package com.zendogames.zenlibdemo.screens;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.zendogames.zenlib.Log;
import com.zendogames.zenlib.ZenGame;
import com.zendogames.zenlib.input.ButtonBinding;
import com.zendogames.zenlib.input.KeySender;
import com.zendogames.zenlib.input.Pad;
import com.zendogames.zenlib.input.StickBinding;
import com.zendogames.zenlib.render.WorldView;
import com.zendogames.zenlib.ui.DebugViews;
import com.zendogames.zenlibdemo.assets.GameSkin;

/**
 * A keyboard-and-pad menu: a vertical list of buttons with a cursor, no mouse needed. The stage is
 * written for the keyboard (arrows, Enter, Escape, as scene2d delivers them) and a {@code KeySender}
 * adds the pad through the menu's own bindings, so the d-pad and stick repeat like OS key repeat and
 * A/B press and back. The menu owns its keys: the listener consumes them, so they never reach the
 * screen's processor or {@code game.input}. Mouse clicks still work through scene2d.
 */
public class MenuScreen extends DemoScreen {

    private static final String TAG = MenuScreen.class.getSimpleName();

    static final int VIEW_WIDTH  = 640;
    static final int VIEW_HEIGHT = 360;

    static final Color[] BACKGROUNDS = { Color.DARK_GRAY, Color.NAVY, Color.FOREST, Color.FIREBRICK };

    // The menu's bindings: pad only, since the keyboard reaches the stage directly. Adding keys here
    // would deliver them twice (once raw, once through KeySender) unless the listener consumed them first.
    final StickBinding nav      = new StickBinding().dpad(0).leftStick(0, 0.5f);
    final ButtonBinding confirm = new ButtonBinding().pad(0, Pad.A);
    final ButtonBinding back    = new ButtonBinding().pad(0, Pad.B);

    final KeySender keySender;
    TextButton[] buttons;
    int cursor = 0;
    int background = 0;

    public MenuScreen(ZenGame game) {
        super(game, /*withCollision*/ false);

        keySender = new KeySender(uiStage)
                .stick(nav, Input.Keys.LEFT, Input.Keys.RIGHT, Input.Keys.UP, Input.Keys.DOWN)
                .button(confirm, Input.Keys.ENTER)
                .button(back, Input.Keys.ESCAPE);

        initializeUI();
        debugViews.add(DebugViews.input(game.input));
    }

    @Override
    protected WorldView createWorldView() {
        var view = new WorldView(worldCamera, VIEW_WIDTH, VIEW_HEIGHT);
        view.clearColor.set(BACKGROUNDS[0]);
        return view;
    }

    @Override
    public void initializeUI() {
        var skin = GameSkin.DEFAULT.get();

        // A copy of the skin's style with a checked look for the cursor, so other screens' buttons are untouched
        var style = new TextButton.TextButtonStyle(skin.get(TextButton.TextButtonStyle.class));
        style.checked = new TextureRegionDrawable(pixel).tint(Color.FOREST);

        buttons = new TextButton[] {
                new TextButton("Cycle background", style),
                new TextButton("Repeat faster",    style),
                new TextButton("To Movement",      style),
        };

        var table = new Table(skin);
        table.setFillParent(true);
        table.defaults().width(200).pad(4);
        for (int i = 0; i < buttons.length; i++) {
            var index = i;
            buttons[i].setProgrammaticChangeEvents(false);
            buttons[i].addListener(new ChangeListener() {   // a mouse click: move the cursor there and activate
                @Override public void changed(ChangeEvent event, Actor actor) {
                    select(index);
                    activate(index);
                }
            });
            table.add(buttons[i]).row();
        }
        uiStage.addActor(table);
        select(0);

        uiStage.addListener(new InputListener() {
            @Override
            public boolean keyDown(InputEvent event, int keycode) {
                switch (keycode) {
                    case Input.Keys.UP     -> select(cursor - 1);
                    case Input.Keys.DOWN   -> select(cursor + 1);
                    case Input.Keys.ENTER  -> activate(cursor);
                    case Input.Keys.ESCAPE -> game.setScreen(new MovementScreen(game));
                    default -> { return false; }
                }
                return true; // the menu owns these keys
            }
        });
    }

    /** Moves the cursor, wrapping; the checked button is the cursor. */
    private void select(int index) {
        cursor = (index + buttons.length) % buttons.length;
        for (int i = 0; i < buttons.length; i++) {
            buttons[i].setChecked(i == cursor);
        }
    }

    private void activate(int index) {
        Log.info(TAG, "'%s' activated", buttons[index].getText());
        switch (index) {
            case 0 -> {
                background = (background + 1) % BACKGROUNDS.length;
                worldView.clearColor.set(BACKGROUNDS[background]);
            }
            case 1 -> {
                keySender.repeatInterval = keySender.repeatInterval > 0.05f ? 0.05f : 0.1f;
                buttons[1].setText(keySender.repeatInterval > 0.05f ? "Repeat faster" : "Repeat slower");
            }
            case 2 -> game.setScreen(new MovementScreen(game));
            default -> {}
        }
    }

    @Override
    public void alwaysUpdate(float dt) {
        super.alwaysUpdate(dt);
        keySender.update(game.input, dt);
    }

    @Override
    public void hide() {
        super.hide();        // zeroes game.input
        keySender.clear();   // and forgets what it had sent, so nothing is released into a dead stage
    }
}
