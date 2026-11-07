package com.zendogames.zenlibdemo.events;

import com.zendogames.zenlib.ecs.Entity;
import com.zendogames.zenlib.ecs.combat.Strike;
import com.zendogames.zenlib.events.Signal;

/**
 * What {@code GameStrikes} publishes for the feedback that is not a rule: a hit sound, a death
 * sound, a screen shake, a score. Subscribers take the one signal and switch on the sealed
 * subtypes, so the compiler keeps every listener complete when a kind is added.
 */
public sealed interface CombatEvent {

    Signal<CombatEvent> SIGNAL = new Signal<>("CombatEvent");

    /** A hit landed; {@code strike.target()} may be despawning by now (then a {@link Died} follows). */
    record Struck(Strike strike) implements CombatEvent {}

    /** {@code entity}'s health ran out from {@code strike}. */
    record Died(Entity entity, Strike strike) implements CombatEvent {}

    static void struck(Strike strike)         { SIGNAL.dispatch(new Struck(strike)); }
    static void died(Entity e, Strike strike) { SIGNAL.dispatch(new Died(e, strike)); }
}
