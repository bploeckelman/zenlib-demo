package com.zendogames.zenlibdemo.screens;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.zendogames.zenlib.Time;
import com.zendogames.zenlib.ZenFlag;
import com.zendogames.zenlib.ZenGame;
import com.zendogames.zenlib.ecs.Entity;
import com.zendogames.zenlib.ecs.combat.HitboxDraw;
import com.zendogames.zenlib.ecs.components.Animator;
import com.zendogames.zenlib.ecs.components.Behavior;
import com.zendogames.zenlib.ecs.components.Health;
import com.zendogames.zenlib.ecs.components.Hitboxes;
import com.zendogames.zenlib.ecs.components.Mover;
import com.zendogames.zenlib.ecs.components.Name;
import com.zendogames.zenlib.ecs.components.Position;
import com.zendogames.zenlib.ecs.components.Timers;
import com.zendogames.zenlib.ecs.movement.platformer.PlatformerMotion;
import com.zendogames.zenlib.ecs.movement.platformer.PlatformerTuning;
import com.zendogames.zenlib.ecs.systems.*;
import com.zendogames.zenlib.input.InputControls;
import com.zendogames.zenlib.render.WorldView;
import com.zendogames.zenlib.ui.DebugViews;
import com.zendogames.zenlibdemo.ecs.*;
import com.zendogames.zenlibdemo.ecs.factories.EnemyFactory;
import com.zendogames.zenlibdemo.ecs.state.HeroState;

/**
 * The combat slice: a hero on WASD + Space and a rival on the arrows + Enter, both the platformer
 * hero with {@code Hitboxes} from {@link GameHitboxes#HERO} on opposing teams, so each one's slash
 * can only hit the other. Blobs (the unarmed click) are hurt-only punching bags with 3 health that
 * flash when struck. {@link GameStrikes} is the rule book; the hero's own {@code onStrike} is a
 * short hit-stop. {@code 2} draws the hitboxes over the collider outlines, {@code 9} with
 * {@code FRAME_STEP} walks a swing tick by tick.
 * <p>
 * The space is the library's {@code FLAT}: boxes overlap where they are drawn, so the one-way
 * platform is there to show that a slash under a fighter standing on it misses.
 */
public class CombatScreen extends DemoScreen {

    static final int VIEW_WIDTH  = 320;
    static final int VIEW_HEIGHT = 180;
    static final int FLOOR       = WALL;
    static final int PLATFORM_X  = 120;
    static final int PLATFORM_Y  = FLOOR + 2 * UNIT;
    static final int PLATFORM_W  = 80;

    /** The attacker's own reaction to landing a hit: freeze the world for a few frames. */
    static final float HIT_STOP = 0.08f;

    static final InputControls HERO_1 = InputControls.wasd(Input.Keys.SPACE).gamepad(0);
    static final InputControls HERO_2 = InputControls.arrows(Input.Keys.ENTER);

    final EnemyFactory enemies;
    final HitSystem hitSystem;

    Entity hero;
    Entity rival;

    public CombatScreen(ZenGame game) {
        super(game, /*withCollision*/ true);
        this.enemies = new EnemyFactory(world);
        this.hitSystem = new HitSystem(new GameStrikes());

        world.addSystem(new InputSystem(game.input));
        world.addSystem(new TimerSystem());
        world.addSystem(new BehaviorSystem());
        world.addSystem(new MovementSystem(collision));
        world.addSystem(new ContactSystem(collision, new GameContacts()));
        world.addSystem(hitSystem);
        addCommonSystems();

        worldCamera.position.set(VIEW_WIDTH / 2f, VIEW_HEIGHT / 2f, 0f);
        buildLevel();

        debugViews.add(DebugViews.hitboxes(hitSystem));
        debugViews.add(DebugViews.behaviors(world));
        actions.defaultClick(this::blob)
                .arm("blob",                Input.Keys.B, this::blob)
                .add("attack",              Input.Keys.F, () -> request(hero, HeroState.ATTACK))
                .add("rival attack",        Input.Keys.SHIFT_RIGHT, () -> request(rival, HeroState.ATTACK))
                .add("trade",               Input.Keys.T, this::trade)
                .add("rival to platform",   Input.Keys.Y, () -> moveTo(rival, PLATFORM_X + PLATFORM_W / 2, PLATFORM_Y))
                .add("immune hero",         Input.Keys.I, this::toggleImmune)
                .add("revive both",         Input.Keys.V, this::revive)
                .add("LOG_COMBAT",          Input.Keys.G, ZenFlag.LOG_COMBAT::toggle)
                .add("rebuild level",       Input.Keys.R, () -> { world.clear(); buildLevel(); });
    }

    private void buildLevel() {
        Level.walled(factory, pixel, VIEW_WIDTH, VIEW_HEIGHT, WALL);
        factory.platform(pixel, PLATFORM_X, PLATFORM_Y, PLATFORM_W);

        hero  = fighter(factory.platformerHero(80,  FLOOR, HERO_1, PlatformerTuning.player(UNIT)), GameMask.PLAYER, GameMask.ENEMY);
        rival = fighter(factory.platformerHero(240, FLOOR, HERO_2, PlatformerTuning.player(UNIT)), GameMask.ENEMY,  GameMask.PLAYER);
        Name.get(rival).name = "rival";
        Animator.get(rival).tint.set(Color.SALMON);
        Hitboxes.get(hero).onStrike = (e, strike) -> Time.pauseFor(HIT_STOP);
    }

    /** A hero on {@code team} whose slash hits {@code targets}; the collider masks stay as the factory made them. */
    private Entity fighter(Entity e, GameMask team, GameMask targets) {
        return e.add(new Hitboxes(GameHitboxes.HERO, team, targets));
    }

    /** A hopping blob that takes three slashes, flashing red for each. */
    private void blob(int x, int y) {
        var blob = enemies.blob(x, y, this::heroX)
                .add(new Health(3))
                .add(new Hitboxes(GameHitboxes.BLOB, GameMask.ENEMY));
        Hitboxes.get(blob).onStruck = (e, strike) -> {
            Animator.get(e).tint.set(Color.RED);
            Timers.get(e).after("flash", 0.15f, t -> Animator.get(t.entity()).tint.set(Color.WHITE));
        };
    }

    private int heroX() {
        return hero.isAlive() ? Position.get(hero).x : VIEW_WIDTH / 2;
    }

    private static void request(Entity e, HeroState state) {
        if (e.isAlive()) Behavior.get(e).request(state);
    }

    /** Both swing on the same tick: the slashes land together and both strikes are reported. */
    private void trade() {
        if (!hero.isAlive() || !rival.isAlive()) return;
        var x = (Position.get(hero).x + Position.get(rival).x) / 2;
        moveTo(hero,  x - 6, FLOOR);
        moveTo(rival, x + 6, FLOOR);
        face(hero,  +1);
        face(rival, -1);
        request(hero,  HeroState.ATTACK);
        request(rival, HeroState.ATTACK);
    }

    private static void moveTo(Entity e, int x, int y) {
        if (e.isAlive()) Position.get(e).teleport(x, y);
    }

    /** The motion's facing and the sprite's: ATTACK does not copy one to the other, NORMAL does. */
    private static void face(Entity e, int dir) {
        Mover.get(e).motion(PlatformerMotion.class).facing = dir;
        Animator.get(e).facing = dir;
    }

    private void toggleImmune() {
        if (!hero.isAlive()) return;
        var boxes = Hitboxes.get(hero);
        boxes.vulnerable = !boxes.vulnerable;
    }

    private void revive() {
        if (hero.isAlive())  HeroState.revive(hero, HERO_1);
        if (rival.isAlive()) HeroState.revive(rival, HERO_2);
    }

    @Override
    public void renderWorld(Batch batch) {
        super.renderWorld(batch);
        if (ZenFlag.DEBUG_RENDER.on()) {
            batch.begin();
            HitboxDraw.all(shapes, hitSystem, world.alpha());
            batch.end();
        }
    }

    @Override
    protected WorldView createWorldView() {
        var view = new WorldView(worldCamera, VIEW_WIDTH, VIEW_HEIGHT);
        view.clearColor.set(Color.LIGHT_GRAY);
        return view;
    }
}
