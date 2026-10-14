package com.zendogames.zenlibdemo.ecs.systems;

import com.github.tommyettinger.gdcrux.PointF2;
import com.zendogames.zenlib.ecs.Entity;
import com.zendogames.zenlib.ecs.Family;
import com.zendogames.zenlib.ecs.IteratingSystem;
import com.zendogames.zenlib.ecs.SystemOrder;
import com.zendogames.zenlib.ecs.components.Input;
import com.zendogames.zenlib.ecs.components.Mover;
import com.zendogames.zenlib.ecs.components.Sensor;
import com.zendogames.zenlib.ecs.movement.platformer.PlatformerMotion;
import com.zendogames.zenlibdemo.ecs.Factory;
import com.zendogames.zenlibdemo.ecs.components.Flee;

/**
 * The {@code DECIDE} half of the watcher: steers its {@link Input} away from the heroes its
 * {@link Sensor}'s sight rays see and hops what is low in its way. Reads this tick's readings
 * ({@code SENSE} ran just before) and writes the fields {@code InputSystem} writes for a human, so
 * the mover never knows who is steering. The steer is one {@link Sensor#repulsion(float, PointF2)}
 * sum over the threats in view rather than the nearest one, so two heroes closing from both sides
 * push it toward the farther one instead of flipping it between them.
 * <p>
 * Flight starts when that sum is longer than {@link #START} and ends when it is shorter than
 * {@link #STOP}: the gap keeps the watcher from stuttering at the one distance where a single
 * threshold would flip every tick. The same idea decides whether it can run at all: with a hero on
 * each side the sideways part of the sum cancels at the balance point, and a watcher that flipped
 * on the sign there would jitter in place, so below {@link #CORNERED} it stands (facing kept) and
 * only runs again above {@link #UNCORNERED}. Losing sight (a hero steps behind a crate) keeps it
 * running the same way for {@link #MEMORY} seconds before it stands; those are the {@link Flee} fields.
 * <p>
 * The hop reads two whiskers by index, 0 deg (ahead) and 60 deg (ahead-up), which the watcher's
 * {@code fan(12, 360, 0, ...)} places at 0 and 2; the sensor's {@code facing} keeps them pointing
 * the way it runs. The whiskers are for terrain only now: a fan misses a body between two rays and
 * flips at every crossing, a sight ray finds it wherever it is.
 */
public class FleeSystem extends IteratingSystem {

    /** Start running when the seen threats press harder than this: with exponent 2 over a 10 tile range, a hero nearer than 7 tiles. */
    static final float START = 0.09f;

    /** Stop once they press less than this: a hero farther than about 8.5 tiles. */
    static final float STOP = 0.025f;

    /** Stand when the sideways part of the steer is less than this fraction of the whole: the pulls cancel, or the threat is straight above. */
    static final float CORNERED = 0.15f;

    /** Run again once the sideways part is more than this fraction of the whole. */
    static final float UNCORNERED = 0.3f;

    /** Seconds to keep running the last way after every threat drops out of view. */
    static final float MEMORY = 0.25f;

    /** Terrain within this (pixels) on the ahead whisker is in the way. */
    private static final float BLOCKED_WITHIN = Factory.UNIT;

    /** The ahead-up whisker clear this far says the obstacle is low enough to hop (a crate, not a wall). */
    private static final float HOP_CLEARANCE = 3 * Factory.UNIT;

    private final PointF2 steer;

    public FleeSystem() {
        super(Family.all(Input.TYPE, Sensor.TYPE, Mover.TYPE, Flee.TYPE), SystemOrder.DECIDE);
        this.steer = new PointF2();
    }

    @Override
    protected void process(Entity e, float dt) {
        var input  = Input.get(e);
        var sensor = Sensor.get(e);
        var mover  = Mover.get(e);
        var flee   = Flee.get(e);

        if (!mover.is(PlatformerMotion.class)) return;
        var motion = mover.motion(PlatformerMotion.class);

        // Run from the heroes in view, the nearer one weighing more; a hysteresis band decides whether to run at all
        sensor.repulsion(/*exponent*/ 2f, steer);
        var pressure = (float) Math.sqrt(steer.x * steer.x + steer.y * steer.y);
        var seen = sensor.seenCount() > 0;
        flee.sinceSeen = seen ? 0f : flee.sinceSeen + dt;
        if (seen) {
            if      (!flee.fleeing && pressure > START) flee.fleeing = true;
            else if ( flee.fleeing && pressure < STOP)  flee.fleeing = false;
        } else if (flee.sinceSeen > MEMORY) {
            flee.fleeing = false;
        }

        // Cornered when the pulls cancel sideways: a second band, so the balance point is a place to stand, not to flip at
        if (flee.fleeing && seen) {
            var sideways = Math.abs(steer.x) / pressure;
            if      (!flee.cornered && sideways < CORNERED)   flee.cornered = true;
            else if ( flee.cornered && sideways > UNCORNERED) flee.cornered = false;
        } else if (!flee.fleeing) {
            flee.cornered = false;
        }

        // Which way: away from what is seen, the last way while sight is lost, nowhere when cornered
        if (flee.fleeing && !flee.cornered) {
            var moveX = seen ? ((steer.x < 0f) ? -1 : +1) : flee.lastMoveX;
            input.moveX = moveX;
            flee.lastMoveX = moveX;
            if (moveX != 0) sensor.facing = moveX; // "ahead" is the way it runs from the next cast on
        } else {
            input.moveX = 0;
            flee.lastMoveX = 0;
        }

        // Something low in the way: press the jump, hold it through the rise, let go at the apex
        var ahead   = sensor.whiskers[0];
        var aheadUp = sensor.whiskers[2];
        var blocked = ahead.hits(sensor.terrain)   && ahead.distance   <= BLOCKED_WITHIN;
        var tall    = aheadUp.hits(sensor.terrain) && aheadUp.distance <= HOP_CLEARANCE;
        var hop     = (input.moveX != 0) && motion.grounded && blocked && !tall;
        input.setActionHeld(hop || motion.isJumping(mover));
    }
}
