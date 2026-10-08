package com.zendogames.zenlibdemo.ecs;

import com.badlogic.gdx.math.MathUtils;
import com.zendogames.zenlib.Log;
import com.zendogames.zenlib.assets.ZenAnimHero;
import com.zendogames.zenlib.ecs.Entity;
import com.zendogames.zenlib.ecs.Family;
import com.zendogames.zenlib.ecs.World;
import com.zendogames.zenlibdemo.ecs.components.Animator;
import com.zendogames.zenlibdemo.ecs.components.Lifetime;
import com.zendogames.zenlibdemo.ecs.components.Position;
import com.zendogames.zenlibdemo.ecs.components.Velocity;

/** Spawns the demo's entities. Logs family joins so the per-add timing in setup code is visible. */
public class Factory {

    private static final String TAG = "Factory";

    private final World world;

    public Factory(World world) {
        this.world = world;

        // Outside a system each add() refreshes membership at once, so this fires on the
        // second of Position/Velocity, mid-construction. Inside a system (SpawnSystem)
        // it fires once, after the system returns, with the entity complete.
        var movers = world.family(Family.all(Position.TYPE, Velocity.TYPE));
        movers.onAdd(e -> Log.debug(TAG, "%s joined movers (animator=%b, updating=%b)", e, Animator.has(e), world.isUpdating()));
        movers.onRemove(e -> Log.debug(TAG, "%s left movers (alive=%b)", e, e.isAlive()));
    }

    public Entity hero(int x, int y) {
        var pos = new Position(x, y);
        var speed = MathUtils.random(20f, 60f);
        var angle = MathUtils.random(0f, MathUtils.PI2);
        var entity = world.createEntity()
                .add(pos)
                .add(new Velocity(speed * MathUtils.cos(angle), speed * MathUtils.sin(angle), pos))
                .add(new Animator(ZenAnimHero.RUN))
                .add(new Lifetime(MathUtils.random(5f, 15f)));
        Log.debug(TAG, "spawned %s at (%d, %d)", entity, x, y);
        return entity;
    }
}
