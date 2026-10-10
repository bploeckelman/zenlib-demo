package com.zendogames.zenlibdemo.ecs;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.MathUtils;
import com.zendogames.zenlib.Log;
import com.zendogames.zenlib.assets.ZenAnimHero;
import com.zendogames.zenlib.ecs.*;
import com.zendogames.zenlib.ecs.components.*;
import com.zendogames.zenlibdemo.ecs.components.Velocity;

/**
 * Simple enemies from <a href="https://github.com/NoelFB/tiny_link">Tiny Link</a>
 * rebuilt from library components and {@link Timers} lambdas, one per method,
 * with hero art standing in for sprites. Nothing here has a component class or
 * a system of its own; what each one needs that a closure can';t hold (eg. the
 * blob's hop count) is a component. Movement is the demo's {@link Velocity} until
 * the library's movement slice lands, so there is no gravity or ground: the blob
 * "hops" by moving for a moment and stopping.
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

        var entity = world.createEntity()
                .tag(Tags.ENEMY)
                .add(new Name("bullet"))
                .add(new Position(x, y))
                .add(new Velocity(direction * 40f, 0f))
                .add(new Lifetime(3f))
                .add(animator)
                .add(timers);

        timers.after("fade", 2.5f, fade -> fade.owner()
                .every("blink", 0.05f, blink -> {
                    var tint = Animator.get(blink.entity()).tint;
                    tint.a = (tint.a < 1f) ? 1f : 0.2f;
                }));
        return entity;
    }

    /** An ad-hoc component containing the hop count that the blob's timers share. */
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
    public Entity blob(int x, int y, int targetX) {
        var timers = new Timers();

        var entity = world.createEntity()
                .tag(Tags.ENEMY)
                .add(new Name("blob"))
                .add(new Position(x, y))
                .add(new Velocity(0f, 0f))
                .add(new Animator(ZenAnimHero.IDLE))
                .add(new Hops())
                .add(timers);

        timers.after("hop", 2f, hop -> {
            var e = hop.entity();
            var hops = Hops.get(e);
            var anim = Animator.get(e);
            var pos  = Position.get(e);
            var vel  = Velocity.get(e);

            hops.count++;
            hops.facing = Integer.signum(targetX - pos.x);
            if (hops.facing == 0) {
                hops.facing = 1;
            }
            anim.facing = hops.facing;
            anim.play(ZenAnimHero.JUMP);
            vel.dx = hops.facing * 40f;
            vel.dy = 60f;

            hop.owner().after("land", 0.3f, land -> {
                var v = Velocity.get(land.entity());
                v.dx = 0f;
                v.dy = 0f;
                var a = Animator.get(land.entity());
                a.play(ZenAnimHero.IDLE);
            });

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
