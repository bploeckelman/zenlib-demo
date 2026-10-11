package com.zendogames.zenlibdemo.ecs.systems;

import com.zendogames.zenlib.assets.ZenAnimHero;
import com.zendogames.zenlib.ecs.Entity;
import com.zendogames.zenlib.ecs.Family;
import com.zendogames.zenlib.ecs.IteratingSystem;
import com.zendogames.zenlib.ecs.SystemOrder;
import com.zendogames.zenlib.ecs.components.Animator;
import com.zendogames.zenlib.ecs.components.Mover;
import com.zendogames.zenlib.ecs.movement.platformer.PlatformerMode;
import com.zendogames.zenlib.ecs.movement.platformer.PlatformerMotion;
import com.zendogames.zenlib.ecs.movement.platformer.PlatformerTuning;
import com.zendogames.zenlib.ecs.movement.topdown.TopDownMotion;

import static com.zendogames.zenlib.assets.ZenAnimHero.*;

/**
 * Maps movement facts to hero animations, the game's half of movement: the library knows
 * {@code grounded} and {@code landed}, only the game knows which clip that is. Platformer movers
 * read {@link PlatformerMotion}, top-down movers {@link TopDownMotion}; anything else is left alone.
 */
public class HeroAnimSystem extends IteratingSystem {

    public HeroAnimSystem() {
        super(Family.all(Mover.TYPE, Animator.TYPE), SystemOrder.PRESENT);
    }

    @Override
    protected void process(Entity e, float dt) {
        var anim  = Animator.get(e);
        var mover = Mover.get(e);
        if      (mover.motion instanceof PlatformerMotion p) platformer(anim, mover, p);
        else if (mover.motion instanceof TopDownMotion t)    topDown(anim, mover, t);
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
}
