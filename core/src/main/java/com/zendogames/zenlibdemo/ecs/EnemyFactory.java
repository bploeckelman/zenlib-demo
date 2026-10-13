package com.zendogames.zenlibdemo.ecs;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.MathUtils;
import com.zendogames.zenlib.Log;
import com.zendogames.zenlib.assets.ZenAnimHero;
import com.zendogames.zenlib.ecs.*;
import com.zendogames.zenlib.ecs.components.*;
import com.zendogames.zenlib.ecs.movement.Motion;
import com.zendogames.zenlib.ecs.movement.platformer.PlatformerMode;
import com.zendogames.zenlib.ecs.movement.platformer.PlatformerMotion;
import com.zendogames.zenlib.ecs.movement.platformer.PlatformerTuning;
import com.zendogames.zenlib.render.RenderLayer;

import java.util.function.IntSupplier;

/**
 * Simple enemies from <a href="https://github.com/NoelFB/tiny_link">Tiny Link</a>
 * rebuilt from library components and {@link Timers} lambdas, one per method,
 * with hero art standing in for sprites. Nothing here has a component class or
 * a system of its own; what each one needs that a closure can't hold (eg. the
 * blob's hop count) is a component. Movement is a {@link Mover} with
 * {@link Motion#NONE}: the timers write the velocity, the sweep integrates it.
 * The screen has no gravity or ground, so the blob "hops" by moving for a moment and stopping.
 */
public class EnemyFactory {

    private static final String TAG = EnemyFactory.class.getSimpleName();

    private final World world;
    private final Family enemies;

    public EnemyFactory(World world) {
        this.world = world;
        this.enemies = world.family(Family.all(Tags.ENEMY));
    }

    public boolean anyEnemies() {
        Log.info(TAG, "enemies: %d", enemies.size());
        return !enemies.entities().isEmpty();
    }

    /** A burst that despawns when its animation ends. {@link Lifetime} is the one-shot "despawn after" and is still the right tool. */
    public Entity pop(int x, int y) {
        var anim = new Animator(ZenAnimHero.EFFECT_LAND);
        anim.tint.set(Color.YELLOW);
        var duration = anim.animation.getAnimationDuration();
        return world.createEntity()
                .add(new Name("pop"))
                .add(new Position(x, y))
                .add(new Lifetime(duration))
                .add(anim);
    }

    /** Every 3s: fire a bullet, play ATTACK, and a nested 0.4s timer returns to IDLE. */
    public Entity spitter(int x, int y) {
        var timers = new Timers();

        var entity = world.createEntity()
                .tag(Tags.ENEMY)
                .add(new Name("spitter"))
                .add(new Position(x, y))
                .add(new Animator(ZenAnimHero.IDLE))
                .add(timers);

        timers.every("shoot", 3f, shoot -> {
            var e = shoot.entity();
            var pos  = Position.get(e);
            var anim = Animator.get(e);

            anim.play(ZenAnimHero.ATTACK);
            bullet(pos.x - 8, pos.y + 8, -1);

            shoot.owner().after("idle", 0.4f,
                    idle -> Animator.get(idle.entity()).play(ZenAnimHero.IDLE));
        });
        return entity;
    }

    /**
     * Flies for 3s, flickering for the last 0.5s: a one-shot that starts a repeating timer,
     * and {@link Lifetime} for the despawn. Bounces off the map edges via the demo's movement.
     */
    public Entity bullet(int x, int y, int direction) {
        var timers   = new Timers();
        var animator = new Animator(ZenAnimHero.EFFECT_ATTACK);
        animator.scale.set(0.5f, 0.5f);
        var mover = new Mover(Motion.NONE, direction * 40f, 0f);
        mover.onHitX = (ent, mov, dir) -> {
            var pos = Position.get(ent);
            pop(pos.x, pos.y);
            world.despawn(ent);
        };

        var entity = world.createEntity()
                .tag(Tags.ENEMY)
                .add(new Name("bullet"))
                .add(new Position(x, y))
                .add(new Lifetime(3f))
                .add(Collider.circ(GameMask.ENEMY, 0, 0, 4).blockedBy(GameMask.SOLID))
                .add(mover)
                .add(animator)
                .add(timers);

        timers.after("fade", 2.5f, fade -> fade.owner()
                .every("blink", 0.05f, blink -> {
                    var tint = Animator.get(blink.entity()).tint;
                    tint.a = (tint.a < 1f) ? 1f : 0.2f;
                }));
        return entity;
    }

    /**
     * Every {@code period} seconds throws a {@link #projectile} at {@code (xVel, yVel)}; the map's
     * {@code launcher} class. A timer and no component, like the spitter.
     */
    public Entity launcher(int x, int y, float period, float xVel, float yVel) {
        var timers = new Timers();
        var anim = new Animator(ZenAnimHero.IDLE);
        anim.tint.set(Color.CORAL);

        var entity = world.createEntity()
                .tag(Tags.ENEMY)
                .add(new Name("launcher"))
                .add(new Position(x, y))
                .add(anim)
                .add(timers);

        timers.every("throw", period, _throw -> {
            var pos = Position.get(_throw.entity());
            projectile(pos.x + 8, pos.y + 8, xVel, yVel);
        });

        return entity;
    }

    /**
     * A thrown thing under the crate's gravity: flies an arc from its velocity, pops on any wall or
     * floor, hurts a hero it touches (the hero detects {@code ENEMY}), and despawns after 5s regardless.
     */
    public Entity projectile(int x, int y, float xVel, float yVel) {
        var animator = new Animator(ZenAnimHero.EFFECT_ATTACK);
        animator.scale.set(0.5f, 0.5f);
        animator.layer = RenderLayer.FOREGROUND;

        var tuning = PlatformerTuning.ballistic(Factory.UNIT);
        var motion = new PlatformerMotion(tuning, PlatformerMode.BALLISTIC);
        var mover = new Mover(motion, xVel, yVel);
        var onHitPop = (Mover.HitCallback) (ent, mov, dir) -> {
            var pos = Position.get(ent);
            pop(pos.x, pos.y);
            world.despawn(ent);
        };
        mover.onHitX = onHitPop;
        mover.onHitY = onHitPop;

        return world.createEntity()
                .tag(Tags.ENEMY)
                .add(new Name("projectile"))
                .add(new Position(x, y))
                .add(new Lifetime(5f))
                .add(Collider.circ(GameMask.ENEMY, 0, 0, 4).blockedBy(GameMask.SOLID))
                .add(mover)
                .add(animator);
    }

    public static class Hops implements Component {
        public static final ComponentType<Hops> TYPE = ComponentType.register(Hops.class);
        public static Hops get(Entity e) { return TYPE.get(e); }

        public int count  = 0;
        public int facing = 1;
    }

    /**
     * Every 2s hop toward {@code targetX}: JUMP, move for 0.3s, then a one-shot lands it
     * (IDLE, stop). The hop timer is a one-shot that re-arms itself with a delay that
     * shortens as it gets angrier, an example of the "repeat with a varying interval" idiom.
     */
    public Entity blob(int x, int y, IntSupplier targetX) {
        var timers = new Timers();
        var tuning = PlatformerTuning.player(Factory.UNIT)
                .withAbilities(PlatformerTuning.Abilities.NONE);

        var entity = world.createEntity()
                .tag(Tags.ENEMY)
                .add(new Name("blob"))
                .add(new Position(x, y))
                .add(new Mover(new PlatformerMotion(tuning)))
                .add(Collider.rect(GameMask.ENEMY, -4, 0, 8, 16).blockedBy(GameMask.SOLID))
                .add(new Animator(ZenAnimHero.IDLE, 8, 0))
                .add(new Hops())
                .add(timers);

        timers.after("hop", 2f, hop -> {
            var e = hop.entity();
            var hops  = Hops.get(e);
            var anim  = Animator.get(e);
            var pos   = Position.get(e);
            var mover = Mover.get(e);

            // Still in the air (a crate landed on it, a long fall): try again shortly, don't count it
            var motion = mover.motion(PlatformerMotion.class);
            if (!motion.grounded) {
                hop.restart(0.1f);
                return;
            }

            hops.count++;
            hops.facing = Integer.signum(targetX.getAsInt() - pos.x);
            if (hops.facing == 0) {
                hops.facing = 1;
            }

            // launch() leaves facing alone; HeroAnimSystem flips the sprite from it and plays IDLE on landing
            motion.facing = hops.facing;
            motion.launch(mover, hops.facing, /*dist*/ 2f, /*height*/ 1.5f, /*lock*/ 1f);
            anim.play(ZenAnimHero.JUMP);

            var nextHopSecs = Math.max(0.5f, 2f - 0.25f * hops.count);
            hop.restart(nextHopSecs);
        });

        return entity;
    }

    /**
     * Polls every 0.25fs: when no enemy is left, pop and open (despawn).
     * The door is a timer and a family query, no component.
     */
    public Entity door(int x, int y) {
        var timers = new Timers();
        var anim = new Animator(ZenAnimHero.IDLE);
        anim.tint.set(Color.SLATE);

        var entity = world.createEntity()
                .add(new Name("door"))
                .add(new Position(x, y))
                .add(anim)
                .add(timers);

        timers.every("poll", 0.25f, poll -> {
            if (anyEnemies()) return;
            var e = poll.entity();
            var name = Name.get(e);
            var pos = Position.get(e);
            pop(pos.x, pos.y + 8);
            Log.info(TAG, "%s:%s opened", e, name);
            world.despawn(e);
        });

        return entity;
    }

    /** Despawns everything tagged ENEMY, so the door reacts. */
    public int killEnemies() {
        var list = enemies.entities();
        int numKilled = list.size;
        // *** outside a system despawns are immediate, so walk backwards ***
        for (int i = list.size - 1; i >= 0; i--) {
            var e = list.get(i);
            var pos = Position.get(e);
            pop(pos.x + MathUtils.random(-4, 4), pos.y + 8);
            world.despawn(e);
        }
        return numKilled;
    }
}
