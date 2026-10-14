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

/**
 * The {@code DECIDE} half of the watcher: steers its {@link Input} away from the heroes its
 * {@link Sensor} sees and hops what is low in its way. Reads this tick's readings ({@code SENSE}
 * ran just before) and writes the fields {@code InputSystem} writes for a human, so the mover
 * never knows who is steering. The steer is one {@link Sensor#repulsion} sum rather than the
 * nearest threat, so two heroes closing from both sides push it toward the farther one instead
 * of flipping it between them.
 * <p>
 * The hop reads two whiskers by index, 0 deg (ahead) and 60 deg (ahead-up), which the watcher's
 * {@code fan(12, 360, 0, ...)} places at 0 and 2; the sensor's {@code facing} keeps them pointing
 * the way it runs.
 */
public class FleeSystem extends IteratingSystem {

    /** A repulsion shorter than this is far enough: the watcher stands. With exponent 2 over a 10 tile range that is about 8 tiles. */
    private static final float FAR_ENOUGH = 0.05f;

    /** Terrain within this (pixels) on the ahead whisker is in the way. */
    private static final float BLOCKED_WITHIN = Factory.UNIT;

    /** The ahead-up whisker clear this far says the obstacle is low enough to hop (a crate, not a wall). */
    private static final float HOP_CLEARANCE = 3 * Factory.UNIT;

    private final PointF2 steer;

    public FleeSystem() {
        super(Family.all(Input.TYPE, Sensor.TYPE, Mover.TYPE), SystemOrder.DECIDE);
        this.steer = new PointF2();
    }

    @Override
    protected void process(Entity e, float dt) {
        var input  = Input.get(e);
        var sensor = Sensor.get(e);
        var mover  = Mover.get(e);

        if (!mover.is(PlatformerMotion.class)) return;
        var motion = mover.motion(PlatformerMotion.class);
        
        // Run from the heroes, the nearer one weighing more
        sensor.repulsion(sensor.threats, /*exponent*/ 2f, steer);
        input.moveX = (Math.abs(steer.x) < FAR_ENOUGH) ? 0 : (steer.x < 0) ? -1 : +1;
        if (input.moveX != 0) {
            sensor.facing = input.moveX; // "ahead" is the way it runs from the next cast on
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
