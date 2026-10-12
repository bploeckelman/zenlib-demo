package com.zendogames.zenlibdemo.screens;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.zendogames.zenlib.Log;
import com.zendogames.zenlib.Time;
import com.zendogames.zenlib.ZenFlag;
import com.zendogames.zenlib.ZenGame;
import com.zendogames.zenlib.ecs.Entity;
import com.zendogames.zenlib.ecs.components.Behavior;
import com.zendogames.zenlib.ecs.components.Mover;
import com.zendogames.zenlib.ecs.components.Name;
import com.zendogames.zenlib.ecs.components.Position;
import com.zendogames.zenlib.ecs.movement.platformer.PlatformerMotion;
import com.zendogames.zenlib.ecs.movement.platformer.PlatformerTuning;
import com.zendogames.zenlib.ecs.systems.*;
import com.zendogames.zenlib.input.InputControls;
import com.zendogames.zenlib.render.WorldView;
import com.zendogames.zenlib.ui.DebugViews;
import com.zendogames.zenlibdemo.ecs.DemoContacts;
import com.zendogames.zenlibdemo.ecs.EnemyFactory;
import com.zendogames.zenlibdemo.ecs.HeroState;
import com.zendogames.zenlibdemo.ecs.Level;

/**
 * The movement slice on one 640x360 map: a platformer hero on WASD + Space (double tap to dash),
 * a top-down walker on the arrows in the walled pen at the top right, crates that fall and stack,
 * blobs that hop after the hero, and level pieces sized to the hero's tuning so the physics
 * contracts can be seen:
 * <pre>
 *   ledge:80     a block exactly jumpHeight (5 tiles = 80 px) tall: jump onto it from the floor
 *   ledge:81     one pixel taller, right beside it: the same jump does not clear it
 *   platform     one-way at 56: jump through from below, land on it, down + jump drops through
 *   pillar       tall and thin: wall slide (push into it while falling), wall jump (jump while on it),
 *                ledge grab (push into it near the top while falling; down lets go, jump climbs)
 *   pen          the walker's area, no gravity: 8-way, diagonals the same speed, 4-way facing
 *   blob         hops after the hero; touching it knocks the hero back with the HURT animation and
 *                i-frames for its length ({@code HeroState}); hurt / kill / revive drive the machine
 * </pre>
 * Everything else is a {@code DebugActions} button in the Debug window, keys in parentheses there:
 * crate at the pointer (also the unarmed click), a blob that hops toward the hero, a second
 * platformer hero on the arrows + Enter, toggle the hero's tuning between the preset and an
 * 8-tile jump, launch the hero in its facing, hit-stop, rebuild the level.
 */
public class MovementScreen extends DemoScreen {

    private static final String TAG = MovementScreen.class.getSimpleName();

    static final int VIEW_WIDTH  = 640;
    static final int VIEW_HEIGHT = 360;
    static final int MAP_WIDTH   = 640;
    static final int MAP_HEIGHT  = 360;
    static final int FLOOR       = WALL;   // top of the bottom wall

    static final InputControls HERO_1 = InputControls.wasd(Input.Keys.SPACE);
    static final InputControls HERO_2 = InputControls.arrows(Input.Keys.ENTER);

    final EnemyFactory enemies;
    final ContactSystem contactSystem;

    Entity hero;
    boolean highJump = false;

    public MovementScreen(ZenGame game) {
        super(game, /*withCollision*/ true);
        this.enemies = new EnemyFactory(world);
        this.contactSystem = new ContactSystem(collision, new DemoContacts());

        world.addSystem(new InputSystem(inputProcessor));
        world.addSystem(new TimerSystem());
        world.addSystem(new BehaviorSystem());
        world.addSystem(new MovementSystem(collision));
        world.addSystem(contactSystem);
        addCommonSystems();

        worldCamera.position.set(MAP_WIDTH / 2f, MAP_HEIGHT / 2f, 0f);
        buildLevel();

        debugViews.add(DebugViews.movers(world));
        debugViews.add(DebugViews.contacts(contactSystem));
        debugViews.add(DebugViews.behaviors(world));
        actions.defaultClick(this::crate)
                .arm("crate",                 Input.Keys.C, this::crate)
                .arm("blob",                  Input.Keys.O, (x, y) -> enemies.blob(x, y, this::heroX))
                .arm("hero 2 (arrows+enter)", Input.Keys.H, (x, y) -> factory.platformerHero(x, y, HERO_2, PlatformerTuning.player(UNIT)))
                .add("toggle 8-tile jump",    Input.Keys.T, this::toggleTuning)
                .add("launch hero",           Input.Keys.L, this::launchHero)
                .add("hurt hero",             Input.Keys.U, () -> requestHero(HeroState.HURT))
                .add("kill hero",             Input.Keys.K, () -> requestHero(HeroState.DEAD))
                .add("revive hero",           Input.Keys.V, this::reviveHero)
                .add("LOG_STATE",             Input.Keys.G, ZenFlag.LOG_STATE::toggle)
                .add("hit-stop 0.5s",         Input.Keys.P, () -> Time.pauseFor(0.5f))
                .add("rebuild level",         Input.Keys.R, () -> { world.clear(); buildLevel(); });
    }

    private void buildLevel() {
        Level.walled(factory, pixel, MAP_WIDTH, MAP_HEIGHT, WALL);

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

    private void crate(int x, int y) {
        factory.crate(pixel, x, y);
    }

    /** Where the blobs hop toward: the hero, or the map center once it is gone. */
    private int heroX() {
        return hero.isAlive() ? Position.get(hero).x : MAP_WIDTH / 2;
    }

    private void toggleTuning() {
        if (!hero.isAlive()) return;
        var motion = Mover.get(hero).motion(PlatformerMotion.class);
        highJump = !highJump;
        motion.tuning = highJump ? PlatformerTuning.player(UNIT).withJumpHeight(8) : PlatformerTuning.player(UNIT);
        Log.info(TAG, "%s jump height %.0f tiles (gravity %.0f px/s^2, impulse %.0f px/s)",
                Name.of(hero), motion.tuning.jumpHeight(), motion.tuning.gravity(), motion.tuning.jumpImpulse());
    }

    private void launchHero() {
        if (!hero.isAlive()) return;
        var mover = Mover.get(hero);
        var motion = mover.motion(PlatformerMotion.class);
        motion.launch(mover, motion.facing, /*dist*/ 6, /*height*/ 4, /*lock*/ 0.5f);
        Log.info(TAG, "%s launched %+d", Name.of(hero), motion.facing);
    }

    /** Asks the hero's state machine for {@code state} on its next tick; HURT and DEAD refuse what they refuse (LOG_STATE shows refusals). */
    private void requestHero(HeroState state) {
        if (!hero.isAlive()) return;
        Behavior.get(hero).request(state);
        Log.info(TAG, "%s requested %s (now %s)", Name.of(hero), state, Behavior.get(hero).current());
    }

    /** Change to NORMAL state again from anything, immediately: the setup/respawn paths. */
    private void reviveHero() {
        if (!hero.isAlive()) return;
        HeroState.revive(hero, HERO_1);
    }

    @Override
    protected WorldView createWorldView() {
        var view = new WorldView(worldCamera, VIEW_WIDTH, VIEW_HEIGHT);
        view.clearColor.set(Color.LIGHT_GRAY);
        return view;
    }
}
