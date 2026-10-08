package com.zendogames.zenlibdemo.ecs.components;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.zendogames.zenlib.assets.AnimType;
import com.zendogames.zenlib.ecs.Component;
import com.zendogames.zenlib.ecs.ComponentType;
import com.zendogames.zenlib.ecs.Entity;

/** The animation an entity is playing and how far into it. Time is float seconds: presentation state. */
public class Animator implements Component {

    public static final ComponentType<Animator> TYPE = ComponentType.register(Animator.class);
    public static Animator get(Entity e) { return TYPE.get(e); }
    public static boolean  has(Entity e) { return TYPE.has(e); }

    public AnimType anim;
    public float time;

    public Animator(AnimType anim) {
        this.anim = anim;
        this.time = 0f;
    }

    public void play(AnimType next) {
        if (anim == next) return;
        anim = next;
        time = 0f;
    }

    public TextureRegion frame() {
        return anim.get().getKeyFrame(time);
    }

    private final Link link = new Link(TYPE);
    @Override public Link link() { return link; }
}
