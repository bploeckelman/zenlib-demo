package com.zendogames.zenlibdemo.screens;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.zendogames.zenlib.Log;
import com.zendogames.zenlib.Time;
import com.zendogames.zenlib.ZenFlag;
import com.zendogames.zenlib.ZenGame;
import com.zendogames.zenlib.assets.ZenAtlas;
import com.zendogames.zenlib.ecs.World;
import com.zendogames.zenlib.ecs.components.Animator;
import com.zendogames.zenlib.ecs.components.Name;
import com.zendogames.zenlib.ecs.components.Position;
import com.zendogames.zenlib.ecs.systems.AnimationSystem;
import com.zendogames.zenlib.ecs.systems.LifetimeSystem;
import com.zendogames.zenlib.ecs.systems.RenderSystem;
import com.zendogames.zenlib.input.ScreenInputProcessor;
import com.zendogames.zenlib.render.CameraFollow;
import com.zendogames.zenlib.render.WorldView;
import com.zendogames.zenlib.screens.ZenScreen;
import com.zendogames.zenlibdemo.Main;
import com.zendogames.zenlibdemo.ecs.Factory;
import com.zendogames.zenlibdemo.ecs.Tags;
import com.zendogames.zenlibdemo.ecs.systems.HurtSystem;
import com.zendogames.zenlibdemo.ecs.systems.MovementSystem;
import com.zendogames.zenlibdemo.ecs.systems.SpawnSystem;

/**
 * Heroes bouncing around a 640x360 pixel-art map seen through a 320x180 view, driven by a {@link World}
 * with the library's render, animation and lifetime systems and a {@link CameraFollow}.
 * <pre>
 *   click   spawn a hero at the pointer (through worldView.unproject)
 *   F       follow the hero nearest the pointer; again to follow the next; the camera clamps to the map
 *   G       follow the map center (snaps)
 *   I       toggle RENDER_INTERP: tracking and heroes stutter without it on a high refresh display
 *   H       tag every entity HURT: HurtSystem stops it, squashes + plays HURT, despawns after 1 s
 *   C       world.clear() then respawn the backdrop; heroes refill next tick, ids start at 0 again
 *   T       transition to a fresh EcsScreen: two worlds alive and updating at once
 *   N       on to CollisionScreen
 *   B       back to FirstScreen
 *   R       toggle RenderSystem.enabled
 *   9       one tick while FRAME_STEP (0) is on
 *   6       RENDER_FBO, 5 imgui, as everywhere
 * </pre>
 */
public class EcsScreen extends ZenScreen {

    private static final String TAG = EcsScreen.class.getSimpleName();

    static final int VIEW_WIDTH    = 320;
    static final int VIEW_HEIGHT   = 180;
    static final int MAP_WIDTH     = 640;
    static final int MAP_HEIGHT    = 360;
    static final int TARGET_HEROES = 10;

    final Main main;
    final World world;
    final Factory factory;
    final TextureRegion pixel;
    final CameraFollow cameraFollow;
    final Vector2 mapCenter;

    public EcsScreen(ZenGame game) {
        super(game);
        this.main = (Main) game;
        this.pixel = new TextureRegion(game.pixel);
        this.world = new World();
        this.factory = new Factory(world);

        world.addSystem(new SpawnSystem(factory, TARGET_HEROES, MAP_WIDTH, MAP_HEIGHT));
        world.addSystem(new HurtSystem());
        world.addSystem(new LifetimeSystem());
        world.addSystem(new MovementSystem(MAP_WIDTH, MAP_HEIGHT));
        world.addSystem(new AnimationSystem());
        world.addSystem(new RenderSystem(batch));

        this.mapCenter = new Vector2(MAP_WIDTH / 2f, MAP_HEIGHT / 2f);
        this.cameraFollow = new CameraFollow(worldCamera);
        this.cameraFollow.bounds = new Rectangle(0, 0, MAP_WIDTH, MAP_HEIGHT);
        this.cameraFollow.follow(mapCenter);

        factory.backdrop(pixel, MAP_WIDTH, MAP_HEIGHT);
        // One spawn in setup code, so the Factory's family hook logs the mid-construction join
        factory.hero(MAP_WIDTH / 2, MAP_HEIGHT / 2);

        ZenAtlas.SPRITES.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
    }

    @Override
    protected WorldView createWorldView() {
        var view = new WorldView(worldCamera, VIEW_WIDTH, VIEW_HEIGHT);
        view.clearColor.set(Color.LIGHT_GRAY);
        return view;
    }

    @Override
    protected ScreenInputProcessor createInputProcessor() {
        return new ScreenInputProcessor(this) {
            @Override
            protected boolean onTouchDown(int screenX, int screenY, int pointer, int button) {
                factory.hero((int) this.pointer.x, (int) this.pointer.y);
                return true;
            }

            @Override
            protected boolean onKeyDown(int keycode) {
                switch (keycode) {
                    case Input.Keys.F -> followNextHero();
                    case Input.Keys.G -> cameraFollow.follow(mapCenter);
                    case Input.Keys.I -> ZenFlag.RENDER_INTERP.toggle();
                    case Input.Keys.H -> hurtEveryone();
                    case Input.Keys.C -> {
                        world.clear();
                        factory.backdrop(pixel, MAP_WIDTH, MAP_HEIGHT);
                    }
                    case Input.Keys.T -> game.setScreen(new EcsScreen(game));
                    case Input.Keys.N -> game.setScreen(new CollisionScreen(game));
                    case Input.Keys.B -> game.setScreen(new FirstScreen(game));
                    case Input.Keys.R -> {
                        var render = world.getSystem(RenderSystem.class);
                        render.enabled = !render.enabled;
                    }
                    default -> { return false; }
                }
                return true;
            }

            @Override
            protected boolean debugKey(int keycode) {
                if (keycode == Input.Keys.NUM_9) {
                    world.step.requestStep();
                    return true;
                }
                return super.debugKey(keycode);
            }
        };
    }

    /** Follows the first hero after the one currently followed (or the first one), wrapping around. */
    private void followNextHero() {
        var followed = (cameraFollow.target() instanceof CameraFollow.Target.Follow f) ? f.entity() : null;
        var entities = world.entities();
        int start = (followed != null) ? entities.indexOf(followed, true) + 1 : 0;
        for (int i = 0; i < entities.size; i++) {
            int index = (start + i) % entities.size;
            var e = entities.get(index);
            if (Animator.has(e)) {
                cameraFollow.follow(e, /*snap*/ false);
                Log.info(TAG, "following %s (%s)", Name.of(e), e);
                return;
            }
        }
    }

    private void hurtEveryone() {
        int numHurt = 0;
        var entities = world.entities();
        for (int i = 0; i < entities.size; i++) {
            var e = entities.get(i);
            if (Position.has(e)) {
                e.tag(Tags.HURT);
                numHurt++;
            }
        }
        Log.info(TAG, "hurt %d entities", numHurt);
    }

    @Override
    public void update(float dt) {
        world.update(dt);
        if (cameraFollow.target() instanceof CameraFollow.Target.Follow f && !f.entity().isAlive()) {
            followNextHero(); // the followed hero despawned: pick another rather than hold its last spot
        }
        cameraFollow.update(dt); // before super: world view sets the projection from the camera
        super.update(dt);
    }

    @Override
    public void renderWorld(SpriteBatch batch) {
        batch.begin();
        world.render(Time.delta);
        batch.end();
    }

    @Override
    public void renderImGui() {
        main.debugUI.render();
        main.debugUI.ecs(world);
    }

    @Override
    public void dispose() {
        world.dispose();
        super.dispose();
    }
}
