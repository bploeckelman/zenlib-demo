package com.zendogames.zenlibdemo.screens;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Array;
import com.zendogames.zenlib.FramePool;
import com.zendogames.zenlib.Log;
import com.zendogames.zenlib.ZenGame;
import com.zendogames.zenlib.assets.ZenAnimHero;
import com.zendogames.zenlib.assets.ZenAtlas;
import com.zendogames.zenlib.ecs.Entity;
import com.zendogames.zenlib.ecs.components.Animator;
import com.zendogames.zenlib.ecs.components.Name;
import com.zendogames.zenlib.ecs.components.Position;
import com.zendogames.zenlib.ecs.systems.AnimationSystem;
import com.zendogames.zenlib.render.WorldView;

/**
 * Every library animation on a grid, each an entity with an {@link Animator}, named under it in
 * window space. The first screen: no collision, nothing moves, so it starts instantly and is the
 * place new atlases (icons, patches, aseprite files) get a row as they arrive.
 * <pre>
 *   filter   toggle the sprite atlas between Nearest (pixel art) and Linear
 *   scale    cycle the sprite scale 1x, 2x, 4x
 *   pause    toggle AnimationSystem.enabled
 * </pre>
 */
public class SpriteScreen extends DemoScreen {

    private static final String TAG = SpriteScreen.class.getSimpleName();

    static final int VIEW_WIDTH  = 320;
    static final int VIEW_HEIGHT = 180;
    static final int COLUMNS     = 5;
    static final int FRAME       = 16;   // the hero frames are 16x16

    final Array<Entity> cells = new Array<>();

    TextureFilter filter = TextureFilter.Nearest;
    int scale = 2;

    public SpriteScreen(ZenGame game) {
        super(game, /*withCollision*/ false);
        addCommonSystems();

        worldCamera.position.set(VIEW_WIDTH / 2f, VIEW_HEIGHT / 2f, 0f);
        layoutGrid();

        actions.add("filter", Input.Keys.F, this::toggleFilter)
               .add("scale",  Input.Keys.S, this::cycleScale)
               .add("pause",  Input.Keys.P, () -> {
                   var anims = world.getSystem(AnimationSystem.class);
                   anims.enabled = !anims.enabled;
               });
    }

    /** Grid pitch in world units: a frame and a half at the current scale. */
    private int pitch() {
        return FRAME * scale * 3 / 2;
    }

    /** One entity per {@link ZenAnimHero}, rows top to bottom, each frame standing on the lower third of its cell. */
    private void layoutGrid() {
        world.clear();
        cells.clear();

        var anims = ZenAnimHero.values();
        var rows  = (anims.length + COLUMNS - 1) / COLUMNS;
        var pitch = pitch();
        var left  = (VIEW_WIDTH  - COLUMNS * pitch) / 2;
        var top   = (VIEW_HEIGHT + rows    * pitch) / 2;

        for (int i = 0; i < anims.length; i++) {
            var anim = anims[i];
            var col  = i % COLUMNS;
            var row  = i / COLUMNS;
            var x    = left + col * pitch + pitch / 2;
            var y    = top  - (row + 1) * pitch + pitch / 3;

            // Origin at the frame's bottom center, like the hero, so the scale grows about the feet
            var animator = new Animator(anim, FRAME / 2f, 0);
            animator.defaultScale.set(scale, scale);
            animator.scale.set(scale, scale);

            var entity = world.createEntity()
                    .add(new Name(anim.name().toLowerCase()))
                    .add(new Position(x, y))
                    .add(animator);
            cells.add(entity);
        }
        Log.debug(TAG, "laid out %d animations at %dx", anims.length, scale);
    }

    private void toggleFilter() {
        filter = (filter == TextureFilter.Nearest) ? TextureFilter.Linear : TextureFilter.Nearest;
        ZenAtlas.SPRITES.setFilter(filter, filter);
        Log.info(TAG, "atlas filter %s", filter);
    }

    private void cycleScale() {
        scale = (scale >= 4) ? 1 : scale * 2;
        layoutGrid();
    }

    @Override
    protected WorldView createWorldView() {
        var view = new WorldView(worldCamera, VIEW_WIDTH, VIEW_HEIGHT);
        view.clearColor.set(Color.DARK_GRAY);
        return view;
    }

    /** Names in window pixels under each cell, so the font stays legible at any world scale. */
    @Override
    protected void renderUI() {
        super.renderUI();

        var font   = game.defaultFont;
        var layout = game.layout;
        var below  = pitch() / 4f;
        batch.setProjectionMatrix(windowCamera.combined);
        batch.begin();
        for (int i = 0; i < cells.size; i++) {
            var e   = cells.get(i);
            var pos = Position.get(e);
            var at  = worldView.project(pos.x, pos.y - below, FramePool.vec2(0, 0));
            layout.setText(font, Name.of(e), Color.WHITE, 0, Align.center, false);
            font.draw(batch, layout, at.x, at.y);
        }
        batch.end();
    }
}
