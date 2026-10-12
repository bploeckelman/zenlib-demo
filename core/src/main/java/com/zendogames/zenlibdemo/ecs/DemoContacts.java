package com.zendogames.zenlibdemo.ecs;

import com.badlogic.gdx.graphics.Color;
import com.zendogames.zenlib.Log;
import com.zendogames.zenlib.collision.Contact;
import com.zendogames.zenlib.collision.ContactResponder;
import com.zendogames.zenlib.ecs.components.*;
import com.zendogames.zenlib.ecs.movement.platformer.PlatformerMotion;
import com.zendogames.zenlib.ecs.movement.topdown.TopDownMotion;

/** The demo's half of contacts: heroes tint while inside a trigger and squash when they bump each other. */
public class DemoContacts implements ContactResponder {

    private static final String TAG = DemoContacts.class.getSimpleName();

    @Override
    public void respond(Contact contact) {
        var mover = Collider.get(contact.mover()).mask();
        if (mover != GameMask.PLAYER) return;
        if (!contact.other().isAlive()) return; // an EXIT can name a despawned entity

        // mask() is the CollisionMask interface, so compare rather than switch on the enum
        var other = Collider.get(contact.other()).mask();
        if      (other == GameMask.TRIGGER) trigger(contact);
        else if (other == GameMask.PLAYER)  bump(contact);
        else if (other == GameMask.ENEMY)   hurt(contact);
    }

    private void trigger(Contact contact) {
        var animator = Animator.get(contact.mover());
        switch (contact.phase()) {
            case ENTER -> animator.tint.set(Color.MAGENTA);
            case EXIT  -> animator.tint.set(Color.WHITE);
            case STAY  -> { return; }
        }
        Log.info(TAG, "%s %s %s", Name.of(contact.mover()), contact.phase(), Name.of(contact.other()));
    }

    private void bump(Contact contact) {
        // Both heroes report the same bump, each squashes itself, so no dedupe needed
        if (contact.kind() != Contact.Kind.BLOCKED || !contact.isEnter()) return;
        var animator = Animator.get(contact.mover());
        animator.scale.set(1.3f, 0.7f);
        Log.debug(TAG, "%s bumped %s (%+d,%+d)", Name.of(contact.mover()), Name.of(contact.other()), contact.dx(), contact.dy());
    }

    /**
     * Touching an enemy: face it, then ask for a transition to the HURT state; the state does the
     * knockback away from {@code facing}. Only a NORMAL state hero is asked, so a hero flying through
     * a second enemy while hurt neither turns nor logs a refusal (remove the guard to watch HURT refuse
     * on LOG_STATE).
     */
    private void hurt(Contact contact) {
        if (contact.kind() != Contact.Kind.OVERLAP || !contact.isEnter()) {
            return;
        }

        var hero = contact.mover();
        var behavior = Behavior.get(hero);
        if (behavior == null || behavior.isNot(HeroState.NORMAL)) {
            return;
        }

        var mover = Mover.get(hero);
        if (mover.is(TopDownMotion.class)) {
            return;
        }

        var heroMotion = Mover.get(hero).motion(PlatformerMotion.class);
        var heroPos    = Position.get(hero);
        var otherPos   = Position.get(contact.other());
        var toward     = Integer.signum(otherPos.x - heroPos.x);
        if (toward != 0) {
            heroMotion.facing = toward;
        }
        Behavior.get(hero).request(HeroState.HURT);
        Log.info(TAG, "%s hurt by %s", Name.of(hero), Name.of(contact.other()));
    }
}
