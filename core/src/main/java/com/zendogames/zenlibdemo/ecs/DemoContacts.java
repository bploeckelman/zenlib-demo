package com.zendogames.zenlibdemo.ecs;

import com.badlogic.gdx.graphics.Color;
import com.zendogames.zenlib.Log;
import com.zendogames.zenlib.collision.Contact;
import com.zendogames.zenlib.collision.ContactResponder;
import com.zendogames.zenlib.ecs.components.Animator;
import com.zendogames.zenlib.ecs.components.Collider;
import com.zendogames.zenlib.ecs.components.Name;

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
}
