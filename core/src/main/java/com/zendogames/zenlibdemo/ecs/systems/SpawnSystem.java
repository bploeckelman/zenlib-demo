package com.zendogames.zenlibdemo.ecs.systems;

import com.badlogic.gdx.math.MathUtils;
import com.zendogames.zenlib.ecs.EntitySystem;
import com.zendogames.zenlib.ecs.Family;
import com.zendogames.zenlib.ecs.SystemOrder;
import com.zendogames.zenlib.ecs.World;
import com.zendogames.zenlib.ecs.components.Animator;
import com.zendogames.zenlib.ecs.components.Mover;
import com.zendogames.zenlib.ui.DebugActions;

/**
 * Keeps the count of animated movers at {@code target} by spawning at random map positions, so
 * spawning from inside a system (membership applied after the system returns) stays exercised.
 * Starts disabled; the screen toggles {@code enabled} from an action.
 */
public class SpawnSystem extends EntitySystem {

    private final int target;
    private final int mapWidth;
    private final int mapHeight;
    private final DebugActions.Spawn spawn;

    private Family movers;

    public SpawnSystem(int target, int mapWidth, int mapHeight, DebugActions.Spawn spawn) {
        super(SystemOrder.DECIDE);
        this.target    = target;
        this.mapWidth  = mapWidth;
        this.mapHeight = mapHeight;
        this.spawn     = spawn;
        this.enabled   = false;
    }

    @Override
    public void addedToWorld(World world) {
        movers = world.family(Family.all(Mover.TYPE, Animator.TYPE));
    }

    @Override
    public void update(float dt) {
        int margin = 16;
        int missing = target - movers.size();
        for (int i = 0; i < missing; i++) {
            var x = MathUtils.random(margin, mapWidth  - margin);
            var y = MathUtils.random(margin, mapHeight - margin);
            spawn.at(x, y);
        }
    }
}
