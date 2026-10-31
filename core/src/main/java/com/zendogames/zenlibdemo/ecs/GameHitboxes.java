package com.zendogames.zenlibdemo.ecs;

import com.zendogames.zenlib.assets.ZenAnimHero;
import com.zendogames.zenlib.ecs.combat.Hitbox;
import com.zendogames.zenlib.ecs.combat.HitboxSet;

import static com.zendogames.zenlib.ecs.combat.Hitbox.rect;

/**
 * The demo's hitbox tables, in pixels from the feet center of the 16x16 hero frames (the
 * {@code Animator} origin is {@code 8, 0}). The body is the drawn figure, 10 wide and 13 tall; the
 * swing reaches 7 px past the center on the first two ATTACK frames, so the slash is out from 0.05 s
 * to 0.2 s of the 0.4 s clip.
 */
public final class GameHitboxes {

    /** One hit point (the hero's HURT state takes exactly one whatever the damage), thrown 1.5 tiles away and 1 up. */
    public static final Hitbox.Attack SLASH = Hitbox.attack(1, 1.5f, 1f);

    public static final HitboxSet HERO = new HitboxSet()
            .base(Hitbox.hurt("body", rect(-5, 0, 10, 13)))
            .window(ZenAnimHero.ATTACK, 0.05f, 0.2f, Hitbox.hit("slash", rect(2, 3, 9, 6), SLASH));

    /** A punching bag: the hero sheet's body and nothing that hits. */
    public static final HitboxSet BLOB = new HitboxSet()
            .base(Hitbox.hurt("body", rect(-5, 0, 10, 13)));

    private GameHitboxes() {}
}
