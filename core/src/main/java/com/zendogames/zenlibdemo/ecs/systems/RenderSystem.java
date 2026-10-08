package com.zendogames.zenlibdemo.ecs.systems;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.zendogames.zenlib.ZenFlag;
import com.zendogames.zenlib.ecs.Entity;
import com.zendogames.zenlib.ecs.Family;
import com.zendogames.zenlib.ecs.IteratingSystem;
import com.zendogames.zenlib.ecs.SystemOrder;
import com.zendogames.zenlibdemo.ecs.components.Animator;
import com.zendogames.zenlibdemo.ecs.components.Position;
import com.zendogames.zenlibdemo.ecs.components.Velocity;

/**
 * Render-rate system: {@code runsOnTick()} is false, so {@code World.render} drives it once per
 * frame from the screen's {@code renderWorld}, between {@code batch.begin()} and {@code end()}.
 * Interpolates between the last two tick positions with {@code world.alpha()}.
 */
public class RenderSystem extends IteratingSystem {

    private final SpriteBatch batch;

    public RenderSystem(SpriteBatch batch) {
        super(Family.all(Position.TYPE, Animator.TYPE), SystemOrder.MANUAL);
        this.batch = batch;
    }

    @Override
    public boolean runsOnTick() {
        return false;
    }

    @Override
    protected void process(Entity e, float dt) {
        var pos = Position.get(e);
        float x = pos.x;
        float y = pos.y;

        var vel = Velocity.get(e);
        if (vel != null && ZenFlag.RENDER_INTERP.on()) {
            float alpha = world.alpha();
            x = MathUtils.round(MathUtils.lerp(vel.prevX, pos.x, alpha));
            y = MathUtils.round(MathUtils.lerp(vel.prevY, pos.y, alpha));
        }

        var animator = Animator.get(e);
        batch.draw(animator.frame(), x, y);
    }
}
