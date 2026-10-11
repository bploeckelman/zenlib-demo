package com.zendogames.zenlibdemo.screens;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.zendogames.zenlib.Log;
import com.zendogames.zenlib.Time;
import com.zendogames.zenlib.ZenFlag;
import com.zendogames.zenlib.ZenGame;
import com.zendogames.zenlib.assets.ZenAtlas;
import com.zendogames.zenlib.collision.CollisionDraw;
import com.zendogames.zenlib.collision.CollisionWorld;
import com.zendogames.zenlib.ecs.Entity;
import com.zendogames.zenlib.ecs.World;
import com.zendogames.zenlib.ecs.components.Mover;
import com.zendogames.zenlib.ecs.components.Name;
import com.zendogames.zenlib.ecs.movement.platformer.PlatformerMotion;
import com.zendogames.zenlib.ecs.movement.platformer.PlatformerTuning;
import com.zendogames.zenlib.ecs.systems.*;
import com.zendogames.zenlib.input.InputControls;
import com.zendogames.zenlib.input.ScreenInputProcessor;
import com.zendogames.zenlib.render.WorldView;
import com.zendogames.zenlib.screens.ZenScreen;
import com.zendogames.zenlib.ui.DebugActions;
import com.zendogames.zenlib.ui.DebugView;
import com.zendogames.zenlib.ui.DebugViews;
import com.zendogames.zenlibdemo.Main;
import com.zendogames.zenlibdemo.ecs.DemoContacts;
import com.zendogames.zenlibdemo.ecs.Factory;
import com.zendogames.zenlibdemo.ecs.systems.HeroAnimSystem;

/**
 * The movement slice on one 640x360 map: a platformer hero on WASD + Space (double tap to dash),
 * a top-down walker on the arrows in the walled pen at the top right, crates that fall and stack,
 * and level pieces sized to the hero's tuning so the physics contracts can be seen:
 * <pre>
 *   ledge:80     a block exactly jumpHeight (5 tiles = 80 px) tall: jump onto it from the floor
 *   wall:81      one pixel taller, right beside it: the same jump does not clear it
 *   platform     one-way at 56: jump through from below, land on it, down + jump drops through
 *   pillar       tall and thin: wall slide (push into it while falling), wall jump (jump while on it),
 *                ledge grab (push into it near the top while falling; down lets go, jump climbs)
 *   pen          the walker's area, no gravity: 8-way, diagonals the same speed, 4-way facing
 * </pre>
 * Everything else is a {@link DebugActions} button in the Debug window, keys in parentheses there:
 * {@code C} crate at the pointer (also the unarmed click), {@code H} a second platformer hero on the
 * arrows + Enter, {@code T} toggles the hero's tuning between the preset and an 8-tile jump,
 * {@code L} launches the hero in its facing, {@code P} hit-stop, {@code R} rebuilds the level,
 * {@code B} back to CollisionScreen. {@code 2} draws colliders, {@code 0} / {@code 9} step ticks.
 */
public class MovementScreen extends ZenScreen {

    private static final String TAG = MovementScreen.class.getSimpleName();

    static final int VIEW_WIDTH  = 640;
    static final int VIEW_HEIGHT = 360;
    static final int MAP_WIDTH   = 640;
    static final int MAP_HEIGHT  = 360;
    static final int WALL        = 8;
    static final int FLOOR       = WALL;   // top of the bottom wall
    static final int UNIT        = Factory.UNIT;

    static final InputControls HERO_1 = InputControls.wasd(Input.Keys.SPACE);
    static final InputControls HERO_2 = InputControls.arrows(Input.Keys.ENTER);

    final Main main;
    final World world;
    final Factory factory;
    final CollisionWorld collision;
    final ContactSystem contactSystem;
    final TextureRegion pixel;
    final DebugActions actions;
    final DebugView ecsView;
    final DebugView moversView;
    final DebugView contactsView;

    Entity hero;
    boolean highJump = false;

    public MovementScreen(ZenGame game) {
        super(game);
        this.main = (Main) game;
        this.world = new World();
        this.factory = new Factory(world);
        this.collision = new CollisionWorld(world);
        this.contactSystem = new ContactSystem(collision, new DemoContacts());
        this.pixel = new TextureRegion(game.pixel);

        world.addSystem(new InputSystem(inputProcessor));
        world.addSystem(new MovementSystem(collision));
        world.addSystem(contactSystem);
        world.addSystem(new HeroAnimSystem());
        world.addSystem(new AnimationSystem());
        world.addSystem(new RenderSystem(batch));

        worldCamera.position.set(MAP_WIDTH / 2f, MAP_HEIGHT / 2f, 0f);
        buildLevel();
        ZenAtlas.SPRITES.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);

        this.ecsView      = DebugViews.ecs(world);
        this.moversView   = DebugViews.movers(world);
        this.contactsView = DebugViews.contacts(contactSystem);
        this.actions = new DebugActions()
                .defaultClick((x, y) -> factory.crate(pixel, x, y))
                .arm("crate",                 Input.Keys.C, (x, y) -> factory.crate(pixel, x, y))
                .arm("hero 2 (arrows+enter)", Input.Keys.H, (x, y) -> factory.platformerHero(x, y, HERO_2, PlatformerTuning.player(UNIT)))
                .add("toggle 8-tile jump",    Input.Keys.T, this::toggleTuning)
                .add("launch hero",           Input.Keys.L, this::launchHero)
                .add("hit-stop 0.5s",         Input.Keys.P, () -> Time.pauseFor(0.5f))
                .add("rebuild level",         Input.Keys.R, () -> { world.clear(); buildLevel(); })
                .add("CollisionScreen",       Input.Keys.B, () -> game.setScreen(new CollisionScreen(game)));
    }

    private void buildLevel() {
        factory.backdrop(pixel, MAP_WIDTH, MAP_HEIGHT);
        factory.wall(pixel, "wall:left",   0,                0,                 WALL, MAP_HEIGHT);
        factory.wall(pixel, "wall:right",  MAP_WIDTH - WALL, 0,                 WALL, MAP_HEIGHT);
        factory.wall(pixel, "wall:bottom", 0,                0,                 MAP_WIDTH, WALL);
        factory.wall(pixel, "wall:top",    0,                MAP_HEIGHT - WALL, MAP_WIDTH, WALL);

        // Jump contract: 5 tiles is reachable, one more pixel is not
        factory.wall(pixel, "ledge:80", 200, FLOOR, 48, 5 * UNIT);
        factory.wall(pixel, "ledge:81", 248, FLOOR, 48, 5 * UNIT + 1);

        // One-way platform, under the jump height so it can be reached from the floor
        factory.platform(pixel, 60, FLOOR + 3 * UNIT, 120);

        // A pillar for wall slide, wall jump, and ledge grab
        factory.wall(pixel, "pillar", 400, FLOOR, 24, 10 * UNIT);

        // The walker's pen: two walls, the map walls close it
        factory.wall(pixel, "pen:bottom", 480, 200, MAP_WIDTH - 480, WALL);
        factory.wall(pixel, "pen:left",   480, 200, WALL, MAP_HEIGHT - 200);
        factory.wall(pixel, "pen:block",  540, 260, UNIT, UNIT);
        factory.walker(552, 300, HERO_2);

        hero = factory.platformerHero(92, FLOOR, HERO_1, PlatformerTuning.player(UNIT));
        highJump = false;
    }

    private void toggleTuning() {
        if (!hero.isAlive()) return;
        var motion = Mover.get(hero).motion(PlatformerMotion.class);
        motion.tuning = highJump ? PlatformerTuning.player(UNIT).withJumpHeight(8) : PlatformerTuning.player(UNIT);
        Log.info(TAG, "%s jump height %.0f tiles (gravity %.0f px/s^2, impulse %.0f px/s",
                Name.of(hero), motion.tuning.jumpHeight(), motion.tuning.gravity(), motion.tuning.jumpImpulse());
    }

    private void launchHero() {
        if (!hero.isAlive()) return;
        var mover = Mover.get(hero);
        var motion = mover.motion(PlatformerMotion.class);
        motion.launch(mover, motion.facing, /*dist*/ 6, /*height*/ 4, /*lock*/ 0.5f);
        Log.info(TAG, "%s launched %+d", Name.of(hero), motion.facing);
    }

    @Override
    protected WorldView createWorldView() {
        var view = new WorldView(worldCamera, VIEW_WIDTH, VIEW_HEIGHT);
        view.clearColor.set(Color.LIGHT_GRAY);
        return view;
    }

    @Override
    protected ScreenInputProcessor createInputProcessor() {
        return new ScreenInputProcessor(this) {
            @Override
            protected boolean onTouchDown(int screenX, int screenY, int pointer, int button) {
                return actions.click((int) this.pointer.x, (int) this.pointer.y);
            }

            @Override
            protected boolean onKeyDown(int keycode) {
                return actions.key(keycode);
            }

            @Override
            protected boolean debugKey(int keycode) {
                if (keycode == Input.Keys.NUM_9) {
                    world.step.requestStep();
                    return true;
                }
                return super.debugKey(keycode);
            }
        };
    }

    @Override
    public void update(float dt) {
        world.update(dt);
        super.update(dt);
    }

    @Override
    public void renderWorld(SpriteBatch batch) {
        batch.begin();
        world.render(Time.delta);
        if (ZenFlag.DEBUG_RENDER.on()) {
            CollisionDraw.all(shapes, collision, Color.MAGENTA, 1f);
        }
        batch.end();
    }

    @Override
    public void renderImGui() {
        main.debugUI.render(actions, moversView, contactsView, ecsView);
    }

    @Override
    public void dispose() {
        world.dispose();
        super.dispose();
    }
}
