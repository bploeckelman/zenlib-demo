package com.zendogames.zenlibdemo.ecs.factories;

import com.badlogic.gdx.maps.MapObject;
import com.badlogic.gdx.maps.objects.PolylineMapObject;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.GdxRuntimeException;
import com.github.tommyettinger.gand.utils.Direction;
import com.zendogames.zenlib.ecs.Entity;
import com.zendogames.zenlib.tilemap.TiledProps;
import com.zendogames.zenlibdemo.ecs.GameTiledProps;
import com.zendogames.zenlibdemo.ecs.components.Trigger;

public class MapObjFactory {

    private final Factory factory;

    public MapObjFactory(Factory factory) {
        this.factory = factory;
    }

    public Entity platformerPlayer(MapObject object) {
        int x = TiledProps.x(object);
        int y = TiledProps.y(object);
        int w = TiledProps.width(object);
        int cx = x + w / 2;
        return factory.platformerPlayer(cx, y);
    }

    public Entity overheadPlayer(MapObject object) {
        int x = TiledProps.x(object);
        int y = TiledProps.y(object);
        int w = TiledProps.width(object);
        // TODO: probably get height and set pos as cx,cy so origin is center instead of bottom-center
        int cx = x + w / 2;
        return factory.overheadPlayer(cx, y);
    }

    public Entity bouncer(MapObject object) {
        var region = TiledProps.region(object);
        int x      = TiledProps.x(object);
        int y      = TiledProps.y(object);
        var bounce = TiledProps.require(object, "bounce", Float.class);
        if (bounce <= 0) throw new GdxRuntimeException("bouncer %s: bounce must be > 0 tiles".formatted(TiledProps.describe(object)));
        return factory.bouncer(region, x, y, bounce);
    }

    public Entity jumpthru(MapObject object) {
        int x = TiledProps.x(object);
        int y = TiledProps.y(object);
        int w = (object instanceof PolylineMapObject polyline)
                ? MathUtils.round(polyline.getPolyline().getLength())
                : TiledProps.width(object);
        if (w <= 0) throw new GdxRuntimeException("jumpthru %s: needs a polyline or a width".formatted(TiledProps.describe(object)));
        return factory.jumpthru(x, y, w);
    }

    public Entity spike(MapObject object) {
        var region = TiledProps.region(object);
        int x      = TiledProps.x(object);
        int y      = TiledProps.y(object);
        var dir    = TiledProps.enumOf(object, "direction", Direction.class);
        return factory.spike(region, x, y, dir);
    }

    /**
     * {@code target} is a Tiled object reference: libGDX resolves it to the referenced {@code MapObject}
     * at the end of the load ({@code BaseTiledMapLoader.loadObjectProperty}), null when unset, which
     * {@code require} reports as missing. The target's position is the destination.
     */
    public Entity teleporter(MapObject object) {
        var region = TiledProps.region(object);
        int x      = TiledProps.x(object);
        int y      = TiledProps.y(object);
        var target = TiledProps.require(object, "target", MapObject.class);
        var dwell  = TiledProps.require(object, "dwell",  Float.class);
        var effect = new Trigger.Teleport(TiledProps.x(target), TiledProps.y(target), dwell);
        return factory.trigger("teleporter", region, x, y, effect);
    }

    public Entity walker(MapObject object) {
        var region = TiledProps.region(object);
        int x      = TiledProps.x(object);
        int y      = TiledProps.y(object);
        var dir    = GameTiledProps.tileEnumOf(object, "direction", Direction.RIGHT, Direction.class);
        return factory.walker(region, x, y, dir);
    }

    public Entity flyer(MapObject object) {
        var region    = TiledProps.region(object);
        int x         = TiledProps.x(object);
        int y         = TiledProps.y(object);
        var dir       = TiledProps.enumOf(object, "direction", Direction.class);
        var speed     = TiledProps.require(object, "speed",     Float.class);
        var range     = TiledProps.require(object, "range",     Float.class);
        var amplitude = TiledProps.require(object, "amplitude", Float.class);
        var period    = TiledProps.require(object, "period",    Float.class);
        if (period <= 0) throw new GdxRuntimeException("flyer %s: period must be > 0 seconds".formatted(TiledProps.describe(object)));
        return factory.flyer(region, x, y, dir, speed, range, amplitude, period);
    }

    public Entity jumper(MapObject object) {
        var region    = TiledProps.region(object);
        int x         = TiledProps.x(object);
        int y         = TiledProps.y(object);
        var dir       = TiledProps.enumOf(object, "direction", Direction.class);
        var period    = TiledProps.require(object, "period", Float.class);
        var dist      = TiledProps.require(object, "dist",   Float.class);
        var height    = TiledProps.require(object, "height", Float.class);
        return factory.jumper(region, x, y, dir, period, dist, height);
    }

    /** {@code target} is an object reference like the teleporter's: the leaper's second point. */
    public Entity leaper(MapObject object) {
        var region = TiledProps.region(object);
        int x      = TiledProps.x(object);
        int y      = TiledProps.y(object);
        var target = TiledProps.require(object, "target", MapObject.class);
        var period = TiledProps.require(object, "period", Float.class);
        var height = TiledProps.require(object, "height", Float.class);
        return factory.leaper(region, x, y, TiledProps.x(target), TiledProps.y(target), period, height);
    }

    public Entity shooter(MapObject object) {
        var region   = TiledProps.region(object);
        int x        = TiledProps.x(object);
        int y        = TiledProps.y(object);
        var dir      = GameTiledProps.tileEnumOf(object, "direction", Direction.RIGHT, Direction.class);
        var interval = TiledProps.require(object, "interval", Float.class);
        var speed    = TiledProps.require(object, "speed",    Float.class);
        var gravity  = TiledProps.require(object, "gravity",  Boolean.class);
        return factory.shooter(region, x, y, dir, interval, speed, gravity);
    }

    public Entity ball(MapObject object) {
        var region = TiledProps.region(object);
        int x      = TiledProps.x(object);
        int y      = TiledProps.y(object);
        var dir    = GameTiledProps.tileEnumOf(object, "direction", Direction.RIGHT, Direction.class);
        var speed  = TiledProps.require(object, "speed",  Float.class);
        var height = TiledProps.require(object, "height", Float.class);
        return factory.ball(region, x, y, dir, speed, height);
    }

    public Entity patrol(MapObject object) {
        var region = TiledProps.region(object);
        int x      = TiledProps.x(object);
        int y      = TiledProps.y(object);
        var dir    = TiledProps.enumOf(object, "direction", Direction.class);
        // TODO: send position as center x,y instead?
        return factory.patrol(region, x, y, dir);
    }
}
