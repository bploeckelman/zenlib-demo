package com.zendogames.zenlibdemo.ecs.systems;

import com.zendogames.zenlib.assets.ZenAnimHero;
import com.zendogames.zenlib.ecs.Entity;
import com.zendogames.zenlib.ecs.Family;
import com.zendogames.zenlib.ecs.IteratingSystem;
import com.zendogames.zenlib.ecs.SystemOrder;
import com.zendogames.zenlib.ecs.components.Animator;
import com.zendogames.zenlib.ecs.components.Lifetime;
import com.zendogames.zenlibdemo.ecs.Tags;
import com.zendogames.zenlibdemo.ecs.components.Velocity;

/**
 * A freshly hurt entity stops, plays the hurt animation and gets a short lifetime.
 * Exercises tag families and component remove/replace from inside a system;
 * the animation is the once-only guard.
 */
public class HurtSystem extends IteratingSystem {

    public HurtSystem() {
        super(Family.all(Tags.HURT, Animator.TYPE), SystemOrder.CONTROL);
    }

    @Override
    protected void process(Entity e, float dt) {
        var animator = Animator.get(e);
        if (animator.isNot(ZenAnimHero.HURT)) {
            animator.play(ZenAnimHero.HURT);
            animator.scale.set(1.4f, 0.6f);
            e.remove(Velocity.TYPE);
            // NOTE: represents duration for 'hurt' anim and tag, not hero's ephemeral lifetime
            // TODO: consider a built-in Timers component where multiple timers can be added by name, tick independently, and auto-remove
            e.add(new Lifetime(1f)); // replaces any existing lifetime
        }
    }
}
