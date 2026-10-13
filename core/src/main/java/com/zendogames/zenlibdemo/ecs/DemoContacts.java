package com.zendogames.zenlibdemo.ecs;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.IntSet;
import com.zendogames.zenlib.Log;
import com.zendogames.zenlib.collision.Contact;
import com.zendogames.zenlib.collision.ContactResponder;
import com.zendogames.zenlib.ecs.Entity;
import com.zendogames.zenlib.ecs.components.*;
import com.zendogames.zenlib.ecs.movement.platformer.PlatformerMotion;
import com.zendogames.zenlib.ecs.movement.topdown.TopDownMotion;
import com.zendogames.zenlibdemo.ecs.components.Trigger;

/**
 * The demo's half of contacts: heroes tint while inside a trigger and take its {@link Trigger}
 * effect, go HURT on an enemy, and bounce off each other: a hero landing on another's head
 * bounces up and stuns it, two heroes meeting sideways are thrown apart with the closing
 * speed partly swapped (the reference project's {@code GameContacts}).
 */
public class DemoContacts implements ContactResponder {

    private static final String TAG = DemoContacts.class.getSimpleName();

    /** Side bump, distances and height in tiles: thrown {@code minDist} plus up to {@code maxDist - minDist} by the closing speed; {@code transfer} 0..1 is how much of each hero's speed ends up in the other. */
    private record BumpTuning(float minDist, float maxDist, float height, float transfer, float lock) {}

    /** Head bounce in tiles; a jump pressed within {@code jumpWindow} seconds of the bounce becomes a normal jump. */
    private record HeadBounceTuning(float height, float jumpWindow) {}

    private static final BumpTuning       BUMP        = new BumpTuning(2f, 7f, 1.5f, 0.8f, 1f);
    private static final HeadBounceTuning HEAD_BOUNCE = new HeadBounceTuning(3f, 0.15f);

    /** Hero pairs already bumped this tick: a BLOCKED contact is reported by whichever hero walked into the other, which can be both. */
    private final IntSet bumpedPairs = new IntSet();
    private long bumpedTick = -1;

    @Override
    public void respond(Contact contact) {
        var mover = Collider.get(contact.mover()).mask();
        if (mover != GameMask.PLAYER) return;
        if (!contact.other().isAlive()) return; // an EXIT can name a despawned entity

        // mask() is the CollisionMask interface, so compare rather than switch on the enum
        var other = Collider.get(contact.other()).mask();
        if      (other == GameMask.TRIGGER) trigger(contact);
        else if (other == GameMask.PLAYER)  heroHero(contact);
        else if (other == GameMask.ENEMY)   hurt(contact);
    }

    // ------------------------------------------------------------------------
    // Triggers
    // ------------------------------------------------------------------------

    private void trigger(Contact contact) {
        var hero     = contact.mover();
        var animator = Animator.get(hero);
        var trigger  = Trigger.get(contact.other());
        switch (contact.phase()) {
            case ENTER -> {
                animator.tint.set(Color.MAGENTA);
                if (trigger != null) {
                    enter(hero, trigger.effect());
                }
            }
            case EXIT -> {
                animator.tint.set(Color.WHITE);
                if (trigger != null && trigger.effect() instanceof Trigger.Teleport && Timers.has(hero)) {
                    Timers.get(hero).cancel("teleport");
                }
            }
            case STAY -> { return; }
        }
        Log.info(TAG, "%s %s %s", Name.of(hero), contact.phase(), Name.of(contact.other()));
    }

    /** The map's trigger effects: a bounce is a straight-up launch, a teleport waits out its dwell on the hero's own timer. */
    private void enter(Entity hero, Trigger.Effect effect) {
        switch (effect) {
            case Trigger.Log    log    -> {}
            case Trigger.Bounce bounce -> {
                var mover = Mover.get(hero);
                if (mover.is(PlatformerMotion.class)) {
                    bounceUp(mover, mover.motion(PlatformerMotion.class), bounce.height(), 0f);
                }
            }
            case Trigger.Teleport teleport -> {
                var timers = Timers.get(hero);
                if (timers == null) {
                    timers = new Timers();
                    hero.add(timers);
                }
                timers.after("teleport", teleport.dwell(), tele -> {
                    var pos = Position.get(tele.entity());
                    pos.teleport(teleport.x(), teleport.y());
                    Log.info(TAG, "%s teleported to (%d, %d)", Name.of(tele.entity()), teleport.x(), teleport.y());
                });
            }
        }
    }

    // ------------------------------------------------------------------------
    // Hero vs hero
    // ------------------------------------------------------------------------

    /**
     * A BLOCKED contact on the hero that walked into the other: downward means it landed on the
     * other's head, sideways means a bump. Both heroes squash either way. Heroes without a
     * platformer motion (the Collision screen's bouncers) only squash.
     */
    private void heroHero(Contact contact) {
        if (contact.kind() != Contact.Kind.BLOCKED || !contact.isEnter()) return;
        var a = contact.mover();
        var b = contact.other();
        Animator.get(a).scale.set(1.3f, 0.7f);
        Animator.get(b).scale.set(1.3f, 0.7f);
        Log.debug(TAG, "%s bumped %s (%+d,%+d)", Name.of(a), Name.of(b), contact.dx(), contact.dy());

        var aMover = Mover.get(a);
        var bMover = Mover.get(b);
        if (aMover == null || bMover == null || !aMover.is(PlatformerMotion.class) || !bMover.is(PlatformerMotion.class)) return;
        if (!firstThisTick(a, b)) return;

        var aMotion = aMover.motion(PlatformerMotion.class);
        var bMotion = bMover.motion(PlatformerMotion.class);
        if (contact.dy() < 0) {
            headBounce(aMover, aMotion, bMover);
        } else if (contact.dx() != 0) {
            var aPos = Position.get(a);
            var bPos = Position.get(b);
            var aCol = Collider.get(a).shape();
            var bCol = Collider.get(b).shape();
            var aCenterX = (aCol.left(aPos) + aCol.right(aPos)) * 0.5f;
            var bCenterX = (bCol.left(bPos) + bCol.right(bPos)) * 0.5f;
            int sign = (aCenterX > bCenterX) ? +1 : -1; // a tie throws the mover left
            sideBump(aMover, aMotion, bMover, bMotion, sign);
        }
    }

    /** One bump per pair per tick, whichever side reports first. */
    private boolean firstThisTick(Entity a, Entity b) {
        var tick = a.world().step.tick;
        if (tick != bumpedTick) {
            bumpedTick = tick;
            bumpedPairs.clear();
        }
        int key = (Math.min(a.id, b.id) << 16) | Math.max(a.id, b.id);
        return bumpedPairs.add(key);
    }

    private void headBounce(Mover bouncer, PlatformerMotion bouncerMotion, Mover bouncee) {
        bounceUp(bouncer, bouncerMotion, HEAD_BOUNCE.height(), HEAD_BOUNCE.jumpWindow());
        bouncee.xVel = 0; // stunned, for now
    }

    /** Straight up {@code height} tiles with control kept; {@code coyote} reopens the jump gate so a press within {@code jumpWindow} is a normal jump. */
    private void bounceUp(Mover mover, PlatformerMotion motion, float height, float jumpWindow) {
        motion.launch(mover, motion.facing, /*dist*/ 0f, height, /*lock*/ 0f);
        motion.coyote = jumpWindow;
    }

    /** Thrown apart along {@code sign} (a's side): each hero flies {@code BUMP.minDist} plus a share of the pair's closing speed. */
    private void sideBump(Mover aMover, PlatformerMotion aMotion, Mover bMover, PlatformerMotion bMotion, int sign) {
        // Each hero's closing speed toward the other, as 0..1 of ground max speed
        var ref = aMotion.tuning.maxSpeed(/*grounded*/ true);
        var aIn = MathUtils.clamp(-sign * aMover.xVel / ref, 0f, 1f);
        var bIn = MathUtils.clamp(+sign * bMover.xVel / ref, 0f, 1f);

        // transfer = 1 is a full energy swap, 0 means each keeps its own
        var k = BUMP.transfer();
        var aOut = k * bIn + (1 - k) * aIn;
        var bOut = k * aIn + (1 - k) * bIn;

        var span = BUMP.maxDist() - BUMP.minDist();
        aMotion.launch(aMover, +sign, BUMP.minDist() + span * aOut, BUMP.height(), BUMP.lock());
        bMotion.launch(bMover, -sign, BUMP.minDist() + span * bOut, BUMP.height(), BUMP.lock());
    }

    // ------------------------------------------------------------------------
    // Enemies
    // ------------------------------------------------------------------------

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
