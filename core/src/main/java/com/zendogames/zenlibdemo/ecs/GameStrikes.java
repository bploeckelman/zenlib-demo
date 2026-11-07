package com.zendogames.zenlibdemo.ecs;

import com.zendogames.zenlib.ecs.combat.Strike;
import com.zendogames.zenlib.ecs.components.Behavior;
import com.zendogames.zenlib.ecs.components.Health;
import com.zendogames.zenlib.ecs.components.Mover;
import com.zendogames.zenlib.ecs.movement.platformer.PlatformerMotion;
import com.zendogames.zenlibdemo.ecs.state.HeroState;
import com.zendogames.zenlibdemo.events.CombatEvent;

/**
 * The demo's rule book for strikes. A hero (an entity whose {@code Behavior} runs {@link HeroState})
 * is turned to face the attacker and asked for HURT, which takes its hit point and throws it back,
 * the same path {@code DemoContacts.hurt} takes for touching an enemy. Anything else with
 * {@code Health} loses the attack's damage, is thrown away from the attacker by the attack's
 * knockback when it has a platformer mover, and despawns at zero. The flash is the target's own
 * {@code onStruck}, not a rule, so it lives where the target is spawned. Every strike is published as a
 * {@link CombatEvent} after the rule has run, so the sounds and any shake live with whoever listens.
 */
public class GameStrikes implements Strike.Responder {

    @Override
    public void strike(Strike strike) {
        apply(strike);
        CombatEvent.struck(strike);
    }

    private void apply(Strike strike) {
        var target = strike.target();
        var behavior = Behavior.has(target) ? Behavior.get(target) : null;
        if (behavior != null && behavior.current() instanceof HeroState) {
            if (behavior.isNot(HeroState.NORMAL) && behavior.isNot(HeroState.ATTACK)) return;
            Mover.get(target).motion(PlatformerMotion.class).facing = -strike.away(); // HURT launches away from facing
            behavior.request(HeroState.HURT);
            return;
        }

        if (!Health.has(target)) return;
        var left = Health.get(target).damage(strike.attack().damage());
        if (left == 0) {
            target.world().despawn(target);
            CombatEvent.died(target, strike);
            return;
        }
        var mover = Mover.has(target) ? Mover.get(target) : null;
        if (mover != null && mover.motion instanceof PlatformerMotion motion) {
            var attack = strike.attack();
            motion.launch(mover, strike.away(), attack.knockback(), attack.height(), /*lock*/ 0.5f);
        }
    }
}
