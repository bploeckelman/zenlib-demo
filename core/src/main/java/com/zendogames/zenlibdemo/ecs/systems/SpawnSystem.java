package com.zendogames.zenlibdemo.ecs.systems;

import com.badlogic.gdx.math.MathUtils;
import com.zendogames.zenlib.ecs.EntitySystem;
import com.zendogames.zenlib.ecs.Family;
import com.zendogames.zenlib.ecs.SystemOrder;
import com.zendogames.zenlib.ecs.World;
import com.zendogames.zenlib.ecs.components.Animator;
import com.zendogames.zenlib.ecs.components.Position;
import com.zendogames.zenlibdemo.ecs.Factory;

public class SpawnSystem extends EntitySystem {

    private final Factory factory;
    private final int target;
    private final int mapWidth;
    private final int mapHeight;

    private Family heroes;

    public SpawnSystem(Factory factory, int target, int mapWidth, int mapHeight) {
        super(SystemOrder.DECIDE);
        this.factory   = factory;
        this.target    = target;
        this.mapWidth  = mapWidth;
        this.mapHeight = mapHeight;
    }

    @Override
    public void addedToWorld(World world) {
        heroes = world.family(Family.all(Position.TYPE, Animator.TYPE));
    }

    @Override
    public void update(float dt) {
        int heroSize = 16;

        int missing = target - heroes.size();
        for (int i = 0; i < missing; i++) {
            var x = MathUtils.random(0, mapWidth  - heroSize);
            var y = MathUtils.random(0, mapHeight - heroSize);
            factory.hero(x, y);
        }
    }
}
