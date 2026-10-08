package com.zendogames.zenlibdemo.ecs.systems;

import com.badlogic.gdx.math.MathUtils;
import com.zendogames.zenlib.ecs.EntitySystem;
import com.zendogames.zenlib.ecs.Family;
import com.zendogames.zenlib.ecs.SystemOrder;
import com.zendogames.zenlib.ecs.World;
import com.zendogames.zenlibdemo.ecs.Factory;
import com.zendogames.zenlibdemo.ecs.components.Animator;
import com.zendogames.zenlibdemo.ecs.components.Position;

public class SpawnSystem extends EntitySystem {

    private final Factory factory;
    private final int target;
    private final int worldWidth;
    private final int worldHeight;

    private Family heroes;

    public SpawnSystem(Factory factory, int target, int worldWidth, int worldHeight) {
        super(SystemOrder.DECIDE);
        this.factory = factory;
        this.target = target;
        this.worldWidth = worldWidth;
        this.worldHeight = worldHeight;
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
            factory.hero(MathUtils.random(0, worldWidth - heroSize), MathUtils.random(0, worldHeight - heroSize));
        }
    }

}
