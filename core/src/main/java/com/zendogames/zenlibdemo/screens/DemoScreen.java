package com.zendogames.zenlibdemo.screens;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.zendogames.zenlib.Time;
import com.zendogames.zenlib.ZenFlag;
import com.zendogames.zenlib.ZenGame;
import com.zendogames.zenlib.assets.ZenAtlas;
import com.zendogames.zenlib.collision.CollisionDraw;
import com.zendogames.zenlib.collision.CollisionWorld;
import com.zendogames.zenlib.ecs.World;
import com.zendogames.zenlib.ecs.systems.AnimationSystem;
import com.zendogames.zenlib.ecs.systems.RenderSystem;
import com.zendogames.zenlib.input.ScreenInputProcessor;
import com.zendogames.zenlib.screens.ZenScreen;
import com.zendogames.zenlib.ui.DebugActions;
import com.zendogames.zenlib.ui.DebugViews;
import com.zendogames.zenlibdemo.ecs.Factory;

/**
 * What every demo screen with a World needs, once: the world and factory, an optional
 * CollisionWorld, the pixel region, the frame-step key, the DebugActions and the views the
 * screen hands to the library DebugUI. Subclasses add systems and build the level in their
 * constructor and add their own actions; the switcher view is added by Main for every screen.
 * <p>
 * Keys shared by every screen: {@code 9} steps one tick while {@code FRAME_STEP} ({@code 0}) is on,
 * {@code 2} draws collider outlines when the screen has a CollisionWorld, {@code 5} hides imgui.
 */
public abstract class DemoScreen extends ZenScreen {

    static final int UNIT = Factory.UNIT;
    static final int WALL = 8;

    final World world;
    final Factory factory;
    final CollisionWorld collision; // null when withCollision is false
    final TextureRegion pixel;
    final DebugActions actions;

    protected DemoScreen(ZenGame game, boolean withCollision) {
        super(game);
        this.pixel = new TextureRegion(game.pixel);
        this.world = new World();
        this.factory = new Factory(world);
        this.collision = withCollision ? new CollisionWorld(world) : null;
        this.actions = new DebugActions();
        this.debugViews.add(actions);                // 'Screen' section first, after the switcher Main pins
        this.debugViews.add(DebugViews.ecs(world));
        ZenAtlas.SPRITES.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
    }

    /** The world's systems after the game-specific ones every subclass adds; call at the end of the constructor. */
    protected void addCommonSystems() {
        world.addSystem(new AnimationSystem());
        world.addSystem(new RenderSystem(batch));
    }

    @Override
    protected ScreenInputProcessor createInputProcessor() {
        return new ScreenInputProcessor(this) {
            @Override protected boolean onTouchDown(int sx, int sy, int pointer, int button) {
                return actions.click((int) this.pointer.x, (int) this.pointer.y);
            }
            @Override protected boolean onKeyDown(int keycode) {
                return actions.key(keycode);
            }
            @Override protected boolean debugKey(int keycode) {
                if (keycode == Input.Keys.NUM_9) {
                    world.step.requestStep();
                    return true;
                }
                return super.debugKey(keycode);
            }
        };
    }

    @Override public void update(float dt) {
        world.update(dt);
        afterWorldUpdate(dt);
        super.update(dt);
    }

    /** Between the world's tick and the camera update in {@code ZenScreen.update}: move the camera here. */
    protected void afterWorldUpdate(float dt) {}

    @Override
    public void renderWorld(SpriteBatch batch) {
        batch.begin();
        world.render(Time.delta);
        if (collision != null && ZenFlag.DEBUG_RENDER.on()) {
            CollisionDraw.all(shapes, collision, Color.MAGENTA, 1f, world.alpha());
        }
        batch.end();
    }

    @Override public void dispose() {
        world.dispose();
        super.dispose();
    }
}
