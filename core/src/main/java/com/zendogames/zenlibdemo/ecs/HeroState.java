package com.zendogames.zenlibdemo.ecs;

import com.zendogames.zenlib.assets.ZenAnimHero;
import com.zendogames.zenlib.ecs.Entity;
import com.zendogames.zenlib.ecs.components.*;
import com.zendogames.zenlib.ecs.movement.platformer.PlatformerMode;
import com.zendogames.zenlib.ecs.movement.platformer.PlatformerMotion;
import com.zendogames.zenlib.ecs.movement.topdown.TopDownMotion;
import com.zendogames.zenlib.input.InputControls;
import com.zendogames.zenlib.state.State;
import com.zendogames.zenlib.state.StateMachine;
import com.zendogames.zenlibdemo.ecs.components.Health;

import static com.zendogames.zenlib.assets.ZenAnimHero.*;
import static com.zendogames.zenlib.assets.ZenAnimHero.IDLE;
import static com.zendogames.zenlib.assets.ZenAnimHero.JUMP;
import static com.zendogames.zenlib.assets.ZenAnimHero.RUN;

/**
 * The platformer hero's behavior layer over its movement. {@link #NORMAL} performs the standard
 * animation updates; {@link #HURT} and {@link #DEAD} own it, so that system skips a hero that
 * is not NORMAL. Knockback is a {@code PlatformerMotion.launch} away from {@code facing}: whoever
 * requests HURT faces the hero at the attacker first ({@code DemoContacts}), which keeps the
 * state free of per-hit data. HURT refuses everything but DEAD (invulnerability frames for the
 * length of the clip); DEAD refuses everything and only {@link #revive} leaves it.
 */
public enum HeroState implements State<Entity> {
    /** Player control. */
    NORMAL {
        @Override
        public State<Entity> update(StateMachine<Entity> m, Entity e, float dt) {
            var anim  = Animator.get(e);
            var mover = Mover.get(e);
            if (mover.motion instanceof PlatformerMotion p) {
                platformer(anim, mover, p);
                if (Sensor.has(e)) {
                    Sensor.get(e).facing = p.facing; // the whiskers face the sprite's way from the next tick's SENSE on
                }
            } else if (mover.motion instanceof TopDownMotion t) {
                topDown(anim, mover, t);
            }
            return this;
        }

        private void platformer(Animator anim, Mover mover, PlatformerMotion motion) {
            anim.facing = motion.facing;

            if (motion.grounded && !motion.jumped) {
                var running = Math.abs(mover.xVel) > 1f;
                anim.play(running ? RUN : IDLE);
            }

            if (motion.dropping) { anim.play(FALL); }
            if (motion.jumped)   { anim.play(JUMP); anim.scale.set(0.8f, 1.2f); }
            if (motion.landed)   { anim.play(IDLE); anim.scale.set(1.2f, 0.8f); }

            if (motion.mode == PlatformerMode.LEDGE) { anim.play(IDLE); }
            if (motion.mode == PlatformerMode.DASH)  { anim.play(RUN); anim.scale.set(1.3f, 0.7f); }
        }

        private void topDown(Animator anim, Mover mover, TopDownMotion motion) {
            if (motion.faceX != 0) {
                anim.facing = motion.faceX; // one sprite sheet: left/right flips, up/down keep the last horizontal facing
            }
            anim.play(mover.isMoving() ? RUN : IDLE);
        }
    },
    /** Thrown back from a hit, steering locked for the arc, HURT played once; NORMAL when the animation is completed. */
    HURT {
        @Override
        public void enter(StateMachine<Entity> m, Entity e) {
            var mover  = Mover.get(e);
            var motion = mover.motion(PlatformerMotion.class);
            motion.launch(mover, -motion.facing, /*dist*/ 1.5f, /*height*/ 1f, /*lock*/ 1f);

            var anim = Animator.get(e);
            anim.start(ZenAnimHero.HURT);
            anim.scale.set(1.3f, 0.7f);

            Health.get(e).current--;
        }

        @Override
        public State<Entity> update(StateMachine<Entity> m, Entity e, float dt) {
            return Animator.get(e).isComplete()
                    ? (Health.get(e).isDead() ? DEAD : NORMAL)
                    : this;
        }

        @Override
        public boolean interruptible(StateMachine<Entity> m, Entity e, State<Entity> by) {
            return by == DEAD;
        }
    },
    /** Stopped, (controls cleared; the {@code Input} component stays so a revive only restores them), DEATH played; never leaves on its own. */
    DEAD {
        @Override
        public void enter(StateMachine<Entity> m, Entity e) {
            Mover.get(e).stop();
            Input.get(e).reset();
            Animator.get(e).start(ZenAnimHero.DEATH);
        }

        @Override
        public boolean interruptible(StateMachine<Entity> m, Entity e, State<Entity> by) {
            return false;
        }
    };

    /** Stay; HURT overrides. */
    @Override
    public State<Entity> update(StateMachine<Entity> machine, Entity e, float dt) {
        return this;
    }

    /** The respawn path: controls restored (DEAD cleared them) and {@code force} to NORMAL from whatever the state is. */
    public static void revive(Entity e, InputControls controls) {
        Health.get(e).restore();
        Input.get(e).controls = controls;
        Behavior.get(e).machine.force(e, HeroState.NORMAL);
    }
}
