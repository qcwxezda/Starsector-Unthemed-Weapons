package weaponexpansion.util;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.loading.MissileSpecAPI;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.Pair;
import org.lwjgl.BufferUtils;
import org.lwjgl.util.vector.Vector2f;
import particleengine.Emitter;
import particleengine.Particles;
import weaponexpansion.ModPlugin;
import weaponexpansion.combat.plugins.Action;
import weaponexpansion.combat.plugins.ActionPlugin;

import java.awt.*;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.FloatBuffer;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.List;

public class Utils {

    public static class ClosestCollisionData {
        public float distance;
        public Vector2f point;
        public CombatEntityAPI entity;
        private boolean isEmpty = true;

        private ClosestCollisionData() {
            distance = Float.POSITIVE_INFINITY;
            point = null;
            entity = null;
        }

        private void updateClosest(Vector2f newPt, CombatEntityAPI newEntity, float newDist) {
            distance = newDist;
            entity = newEntity;
            point = newPt;
            isEmpty = false;
        }
    }

    public static ClosestCollisionData collisionCheck(Vector2f a, Vector2f b, Collection<? extends CombatEntityAPI> ignoreList, CombatEngineAPI engine) {
        return collisionCheck(a, b, ignoreList, -1, engine);
    }

    public static ClosestCollisionData collisionCheck(Vector2f a, Vector2f b, int ignoreOwner, CombatEngineAPI engine) {
        return collisionCheck(a, b, null, ignoreOwner, engine);
    }

    /**
     * Checks if the segment from a to b collides with an entity and returns the collision point closest to a.
     * Returns null if there was no collision.
     */
    public static ClosestCollisionData collisionCheck(Vector2f a, Vector2f b, Collection<? extends CombatEntityAPI> ignoreList, int ignoreOwner, CombatEngineAPI engine) {
        // TIme since last hit should advance in world time, not ship time
        float length = Misc.getDistance(a, b);

        Iterator<Object> objItr = engine.getAllObjectGrid().getCheckIterator(a, length, length);

        // Keep track of the closest collision point as that is the one that we will end up using
        // Distance to previous location is tracked
        ClosestCollisionData closest = new ClosestCollisionData();

        while (objItr.hasNext()) {
            Object obj = objItr.next();
            if (!(obj instanceof CombatEntityAPI)) continue;
            if ((obj instanceof DamagingProjectileAPI) && !(obj instanceof MissileAPI)) continue;
            if (ignoreList != null && ignoreList.contains(obj)) continue;

            CombatEntityAPI o = (CombatEntityAPI) obj;

            if (o.getOwner() == ignoreOwner) continue;
            if (CollisionClass.NONE.equals(o.getCollisionClass())) continue;

            // Pre-check collision radius, exit early if outside to prevent unnecessary computation
            Vector2f collisionRadiusPoint = Misc.intersectSegmentAndCircle(a, b, o.getLocation(), o.getCollisionRadius());
            if (collisionRadiusPoint == null) {
                continue;
            }

            // If it's a ship, check collision with shields
            if (o instanceof ShipAPI) {
                ShipAPI ship = (ShipAPI) o;
                if (ship.isPhased()) continue;

                if (ship.getShield() != null) {
                    // Actual point itself is inside shield
                    if (Misc.getDistance(ship.getShieldCenterEvenIfNoShield(), a) < ship.getShieldRadiusEvenIfNoShield()
                            && ship.getShield().isWithinArc(a)) {
                        closest.updateClosest(a, o, 0f);
                        // Obviously nothing can be closer; break
                        break;
                    }

                    Vector2f collisionPoint =
                            Misc.intersectSegmentAndCircle(
                                    a,
                                    b,
                                    ship.getShieldCenterEvenIfNoShield(),
                                    ship.getShieldRadiusEvenIfNoShield());
                    if (collisionPoint != null && ship.getShield().isWithinArc(collisionPoint)) {
                        float dist = Misc.getDistance(a, collisionPoint);
                        if (dist < closest.distance) {
                            closest.updateClosest(collisionPoint, o, dist);
                        }
                    }
                }
            }

            // No exact bounds -- check collision radius
            if (o.getExactBounds() == null) {

                // Actual point itself is inside radius
                if (Misc.getDistance(a, o.getLocation()) <= o.getCollisionRadius()) {
                    closest.updateClosest(a, o, 0f);
                    // Obviously nothing else can be closer
                    break;
                }

                float dist = Misc.getDistance(a, collisionRadiusPoint);
                if (dist < closest.distance) {
                    closest.updateClosest(collisionRadiusPoint, o, dist);
                }
                continue;
            }

            // Check exact bounds
            BoundsAPI bounds = o.getExactBounds();
            bounds.update(o.getLocation(), o.getFacing());
            List<BoundsAPI.SegmentAPI> segments = bounds.getSegments();

            // Check if point itself is inside bounds
            List<Vector2f> boundVerts = new ArrayList<>();
            boundVerts.add(segments.get(0).getP1());
            for (BoundsAPI.SegmentAPI segment : segments) {
                boundVerts.add(segment.getP2());
            }
            if (Misc.isPointInBounds(a, boundVerts)) {
                closest.updateClosest(a, o, 0f);
                break;
            }

            for (BoundsAPI.SegmentAPI segment : segments) {
                Vector2f collisionPoint =
                        Misc.intersectSegments(segment.getP1(), segment.getP2(), a, b);
                if (collisionPoint != null) {
                    float dist = Misc.getDistance(a, collisionPoint);
                    if (dist < closest.distance) {
                        closest.updateClosest(collisionPoint, o, dist);
                    }
                }
            }
        }

        if (closest.isEmpty) {
            return null;
        }

        return closest;
    }

    /** For big explosions. Applies damage to all entities around a ring. */
    public static void applyDamageOnRing(
            Vector2f origin,
            float radius,
            int owner,
            boolean friendlyFire,
            Collection<? extends CombatEntityAPI> ignoreList,
            float totalDamage,
            DamageType damageType,
            float totalEmp,
            boolean bypassShields,
            boolean dealsSoftFlux,
            Object source,
            boolean playSound) {
        CombatEngineAPI engine = Global.getCombatEngine();
        Iterator<Object> objItr = engine.getAllObjectGrid().getCheckIterator(origin, 2f*radius, 2f*radius);
        while (objItr.hasNext()) {
            Object o = objItr.next();
            if (!(o instanceof CombatEntityAPI)) continue;
            if ((o instanceof DamagingProjectileAPI) && !(o instanceof MissileAPI)) continue;

            CombatEntityAPI entity = (CombatEntityAPI) o;
            if (ignoreList != null && ignoreList.contains(o)) continue;
            if (!friendlyFire && owner == entity.getOwner()) continue;
            if (entity instanceof ShipAPI && ((ShipAPI) entity).isPhased()) continue;
            if (CollisionClass.NONE.equals(entity.getCollisionClass())) continue;

            BoundsAPI bounds = entity.getExactBounds();
            if (bounds == null) {
                Pair<Vector2f, Vector2f> collisionPoints = intersectCircles(origin, radius, entity.getLocation(), entity.getCollisionRadius());
                if (collisionPoints != null) {
                    Vector2f p1 = Misc.getDiff(collisionPoints.one, origin);
                    Vector2f p2 = Misc.getDiff(collisionPoints.two, origin);
                    float theta1 = (float) Math.atan2(p1.y, p1.x) * Misc.DEG_PER_RAD;
                    float theta2 = (float) Math.atan2(p2.y, p2.x) * Misc.DEG_PER_RAD;
                    float subtendedAngle = Math.abs(angleDiff(theta1, theta2));

                    if (subtendedAngle > 0f) {
                        float damage = totalDamage * subtendedAngle / 360f;
                        float empDamage = totalEmp * subtendedAngle / 360f;
                        engine.applyDamage(entity, collisionPoints.one, damage / 2f, damageType, empDamage / 2f, bypassShields, dealsSoftFlux, source, playSound);
                        engine.applyDamage(entity, collisionPoints.two, damage / 2f, damageType, empDamage / 2f, bypassShields, dealsSoftFlux, source, playSound);
                    }
                }
            }
            else {
                if (entity instanceof ShipAPI) {
                    ShipAPI ship = (ShipAPI) entity;
                    if (ship.getShield() != null) {
                        List<Float> collisionAngles = new ArrayList<>();
                        List<Vector2f> collisionPoints = new ArrayList<>();
                        Vector2f shieldCenter = ship.getShieldCenterEvenIfNoShield();
                        float shieldRadius = ship.getShieldRadiusEvenIfNoShield();
                        Pair<Vector2f, Vector2f> shieldPts = intersectCircles(shieldCenter, shieldRadius, origin, radius);
                        if (shieldPts != null) {
                            if (ship.getShield().isWithinArc(shieldPts.one)) {
                                collisionPoints.add(shieldPts.one);
                            }
                            if (ship.getShield().isWithinArc(shieldPts.two)) {
                                collisionPoints.add(shieldPts.two);
                            }
                        }
                        float t1 = ship.getShield().getFacing() - ship.getShield().getActiveArc() / 2f;
                        float t2 = ship.getShield().getFacing() + ship.getShield().getActiveArc() / 2f;
                        Vector2f shieldEnd1 = new Vector2f(shieldCenter.x + shieldRadius*(float)Math.cos(t1), shieldCenter.y + shieldRadius*(float)Math.sin(t1));
                        Vector2f shieldEnd2 = new Vector2f(shieldCenter.x + shieldRadius*(float)Math.cos(t2), shieldCenter.y + shieldRadius*(float)Math.sin(t2));
                        collisionPoints.addAll(intersectSegmentCircle(shieldCenter, shieldEnd1, origin, radius));
                        collisionPoints.addAll(intersectSegmentCircle(shieldCenter, shieldEnd2, origin, radius));

                        for (Vector2f pt : collisionPoints) {
                            Vector2f p = Misc.getDiff(pt, origin);
                            collisionAngles.add((float) Math.atan2(p.y, p.x) * Misc.DEG_PER_RAD);
                        }

                        if (collisionAngles.size() >= 2) {
                            Collections.sort(collisionAngles);

                            // (0, 1) is an arc inside the ship bounds, as is (2, 3), etc.
                            // unless angle 0 is inside the ship bounds, then it's (1, 2), (3, 4), etc.
                            int startIndex = 0;

                            // Check if point at angle 0 is inside bounds
                            Vector2f checkPt = new Vector2f(radius + origin.x, origin.y);
                            if (ship.getShield().isWithinArc(checkPt) && Misc.getDistance(checkPt, shieldCenter) <= shieldRadius) {
                                startIndex = 1;
                            }

                            for (int i = 0; i < collisionAngles.size(); i += 2) {
                                int a = (startIndex + i) % collisionAngles.size();
                                int b = (startIndex + i + 1) % collisionAngles.size();
                                float subtendedAngle = Math.abs(angleDiff(collisionAngles.get(a), collisionAngles.get(b)));

                                if (subtendedAngle > 0f) {
                                    float damage = totalDamage * subtendedAngle / 360f;
                                    float empDamage = totalEmp * subtendedAngle / 360f;
                                    Vector2f p1 = new Vector2f(
                                            origin.x + radius * (float) Math.cos(collisionAngles.get(a) * Misc.RAD_PER_DEG),
                                            origin.y + radius * (float) Math.sin(collisionAngles.get(a) * Misc.RAD_PER_DEG));
                                    Vector2f p2 = new Vector2f(
                                            origin.x + radius * (float) Math.cos(collisionAngles.get(b) * Misc.RAD_PER_DEG),
                                            origin.y + radius * (float) Math.sin(collisionAngles.get(b) * Misc.RAD_PER_DEG));
                                    if (entity.getShield().isWithinArc(p1)) {
                                        engine.applyDamage(entity, p1, damage / 2f, damageType, empDamage / 2f, bypassShields, dealsSoftFlux, source, playSound);
                                    }
                                    if (entity.getShield().isWithinArc(p2)) {
                                        engine.applyDamage(entity, p2, damage / 2f, damageType, empDamage / 2f, bypassShields, dealsSoftFlux, source, playSound);
                                    }
                                }
                            }
                        }
                    }
                }

                bounds.update(entity.getLocation(), entity.getFacing());
                List<Float> collisionAngles = new ArrayList<>();
                List<BoundsAPI.SegmentAPI> segments = bounds.getSegments();

                for (BoundsAPI.SegmentAPI segment : segments) {
                    List<Vector2f> pts = intersectSegmentCircle(segment.getP1(), segment.getP2(), origin, radius);
                    for (Vector2f pt : pts) {
                        Vector2f p = Misc.getDiff(pt, origin);
                        collisionAngles.add((float) Math.atan2(p.y, p.x) * Misc.DEG_PER_RAD);
                    }
                }

                if (collisionAngles.size() >= 2) {
                    Collections.sort(collisionAngles);
                    // (0, 1) is an arc inside the ship bounds, as is (2, 3), etc.
                    // unless angle 0 is inside the ship bounds, then it's (1, 2), (3, 4), etc.
                    int startIndex = 0;

                    // Check if point at angle 0 is inside bounds
                    List<Vector2f> boundVerts = new ArrayList<>();
                    boundVerts.add(segments.get(0).getP1());
                    for (BoundsAPI.SegmentAPI segment : segments) {
                        boundVerts.add(segment.getP2());
                    }
                    Vector2f checkPt = new Vector2f(radius + origin.x, origin.y);
                    if (Misc.isPointInBounds(checkPt, boundVerts)) {
                        startIndex = 1;
                    }

                    for (int i = 0; i < collisionAngles.size(); i += 2) {
                        int a = (startIndex + i) % collisionAngles.size();
                        int b = (startIndex + i + 1) % collisionAngles.size();
                        float subtendedAngle = Math.abs(angleDiff(collisionAngles.get(a), collisionAngles.get(b)));

                        if (subtendedAngle > 0f) {
                            float damage = totalDamage * subtendedAngle / 360f;
                            float empDamage = totalEmp * subtendedAngle / 360f;
                            Vector2f p1 = new Vector2f(
                                    origin.x + radius * (float) Math.cos(collisionAngles.get(a) * Misc.RAD_PER_DEG),
                                    origin.y + radius * (float) Math.sin(collisionAngles.get(a) * Misc.RAD_PER_DEG));
                            Vector2f p2 = new Vector2f(
                                    origin.x + radius * (float) Math.cos(collisionAngles.get(b) * Misc.RAD_PER_DEG),
                                    origin.y + radius * (float) Math.sin(collisionAngles.get(b) * Misc.RAD_PER_DEG));
                            if (entity.getShield() == null || !entity.getShield().isWithinArc(p1)) {
                                engine.applyDamage(entity, p1, damage / 2f, damageType, empDamage / 2f, bypassShields, dealsSoftFlux, source, playSound);
                            }
                            if (entity.getShield() == null || !entity.getShield().isWithinArc(p2)) {
                                engine.applyDamage(entity, p2, damage / 2f, damageType, empDamage / 2f, bypassShields, dealsSoftFlux, source, playSound);
                            }
                        }
                    }
                }
            }
        }

//        Map<CombatEntityAPI, List<Vector2f>> entitiesWithBounds = new HashMap<>();
//        while (objItr.hasNext()) {
//            Object o = objItr.next();
//            if (!(o instanceof CombatEntityAPI)) continue;
//            if ((o instanceof DamagingProjectileAPI) && !(o instanceof MissileAPI)) continue;
//
//            CombatEntityAPI entity = (CombatEntityAPI) o;
//            if (ignoreList != null && ignoreList.contains(o)) continue;
//            if (!friendlyFire && owner == entity.getOwner()) continue;
//            if (entity instanceof ShipAPI && ((ShipAPI) entity).isPhased()) continue;
//            if (CollisionClass.NONE.equals(entity.getCollisionClass())) continue;
//
//            BoundsAPI bounds = entity.getExactBounds();
//            if (bounds == null) {
//                entitiesWithBounds.put(entity, null);
//            }
//            else {
//                bounds.update(entity.getLocation(), entity.getFacing());
//                List<BoundsAPI.SegmentAPI> segments = bounds.getSegments();
//
//                // Check if point itself is inside bounds
//                List<Vector2f> boundVerts = new ArrayList<>();
//                boundVerts.add(segments.get(0).getP1());
//                for (BoundsAPI.SegmentAPI segment : segments) {
//                    boundVerts.add(segment.getP2());
//                }
//                entitiesWithBounds.put(entity, boundVerts);
//            }
//        }
//
//        float numPoints = (float) Math.floor(2f*Math.PI * radius / distBetweenSamples);
//        float inc = (float) (2f*Math.PI) / numPoints;
//
//        for (float a = 0f; a < 2f*Math.PI; a += inc) {
//            Vector2f pt = new Vector2f(origin.x + radius*(float) Math.cos(a), origin.y + radius*(float) Math.sin(a));
//
//            for (Map.Entry<CombatEntityAPI, List<Vector2f>> entityEntry : entitiesWithBounds.entrySet()) {
//                CombatEntityAPI entity = entityEntry.getKey();
//                List<Vector2f> boundVerts = entityEntry.getValue();
//                if (Misc.getDistance(pt, entity.getLocation()) <= entity.getCollisionRadius()) {
//                    boolean collision = false;
//                    if (entity instanceof ShipAPI) {
//                        ShipAPI ship = (ShipAPI) entity;
//                        if (ship.getShield() != null
//                                && ship.getShield().isWithinArc(pt)
//                                && Misc.getDistance(pt, ship.getShieldCenterEvenIfNoShield()) <= ship.getShieldRadiusEvenIfNoShield()) {
//                            collision = true;
//                        }
//                    }
//
//                    if (!collision) {
//                        if (boundVerts == null) {
//                            collision = true;
//                        } else {
//                            if (Misc.isPointInBounds(pt, boundVerts)) {
//                                collision = true;
//                            }
//                        }
//                    }
//
//                    if (collision) {
//                        engine.applyDamage(
//                                entity,
//                                pt,
//                                totalDamage / numPoints,
//                                damageType,
//                                empAmount,
//                                bypassShields,
//                                dealsSoftFlux,
//                                source,
//                                playSound
//                        );
//                    }
//                }
//            }
//        }
    }


    /** Gives both intersection points if there's more than one */
    public static List<Vector2f> intersectSegmentCircle(Vector2f a, Vector2f b, Vector2f o, float r) {
        List<Vector2f> pts = new ArrayList<>();

        float x1 = a.x - o.x, y1 = a.y - o.y;
        float x2 = b.x - o.x, y2 = b.y - o.y;
        float dx = x2 - x1, dy = y2 - y1, dr = (float) Math.sqrt(dx*dx + dy*dy), D = x1*y2 - x2*y1;

        float disc = r*r*dr*dr - D*D;
        if (disc < 0) {
            return pts;
        }

        float vx = sgnPos(dy)*dx*(float) Math.sqrt(disc);
        float vy = Math.abs(dy)*(float) Math.sqrt(disc);
        float rx1 = (D*dy + vx) / (dr*dr) + o.x, rx2 = (D*dy - vx) / (dr*dr) + o.x;
        float ry1 = (-D*dx + vy) / (dr*dr) + o.y, ry2 = (-D*dx - vy) / (dr*dr) + o.y;

        float mx = Math.min(a.x, b.x), Mx = Math.max(a.x, b.x);
        float my = Math.min(a.y, b.y), My = Math.max(a.y, b.y);
        if (rx1 <= Mx && rx1 >= mx && ry1 <= My && ry1 >= my) {
            pts.add(new Vector2f(rx1, ry1));
        }
        if (rx2 <= Mx && rx2 >= mx && ry2 <= My && ry2 >= my) {
            pts.add(new Vector2f(rx2, ry2));
        }
        return pts;
    }

    public static float sgnPos(float x) {
        return x < 0 ? -1f : 1f;
    }

    public static Pair<Vector2f, Vector2f> intersectCircles(Vector2f o1, float r1, Vector2f o2, float r2) {
        float dist = Misc.getDistance(o1, o2);
        float a = (r1*r1 - r2*r2 + dist*dist)/(2f*dist);
        float b = r1*r1 - a*a;
        if (b < 0) return null;
        float c = (float) Math.sqrt(b);
        Vector2f p1 = new Vector2f(a/dist * (o2.x - o1.x) + c/dist * (o2.y - o1.y) + o1.x, a/dist * (o2.y - o1.y) - c/dist * (o2.x - o1.x) + o1.y);
        Vector2f p2 = new Vector2f(a/dist * (o2.x - o1.x) - c/dist * (o2.y - o1.y) + o1.x, a/dist * (o2.y - o1.y) + c/dist * (o2.x - o1.x) + o1.y);
        return new Pair<>(p1, p2);
    }

    public static float angleDiff(float a, float b) {
        return ((a - b) % 360 + 540) % 360 - 180;
    }

    public static float randBetween(float a, float b) {
        return Misc.random.nextFloat() * (b - a) + a;
    }

    public static float estimateInterceptTime(MissileAPI missile, CombatEntityAPI entity) {
        float dirToShip = Misc.getAngleInDegrees(missile.getLocation(), entity.getLocation());
        // If the missile were turned toward the ship, how long to close the distance?
        float v0 = Vector2f.dot(missile.getVelocity(), Misc.getUnitVectorAtDegreeAngle(dirToShip));
        float vM = missile.getMaxSpeed();
        float a = missile.getAcceleration();
        float d = Misc.getDistance(missile.getLocation(), entity.getLocation());
        float T = (vM - v0) / a; // time for missile to reach max speed
        // interception before T
        float t = (float) (Math.sqrt(4*v0*v0 + 4*a*d) - 2*v0) / (2*a);
        // interception after T
        if (t > T) {
            t = (d + vM*T - a*T*T/2f - v0*T) / (vM);
        }
        return t;
    }

    public static Vector2f estimateInterceptPoint(MissileAPI missile, CombatEntityAPI entity, float frameTime, Vector2f shipVelLastFrame) {
        float t = estimateInterceptTime(missile, entity);

        // Where will the target have moved to in timeEst, assuming constant acceleration?
        Vector2f V0 = new Vector2f(entity.getVelocity());
        Vector2f acc = new Vector2f();
        Vector2f.sub(V0, shipVelLastFrame, acc);
        acc.scale(1f / frameTime);

        // How long will the target take to reach maximum speed?
        float aDotV0 = Vector2f.dot(V0, acc);
        float maxSpeed = entity instanceof ShipAPI ? ((ShipAPI) entity).getMaxSpeed() : entity.getVelocity().length();
        float TMax;
        if (acc.lengthSquared() > 0) {
            TMax = (float) (Math.sqrt(4*aDotV0*aDotV0 + 4*acc.lengthSquared()*(maxSpeed*maxSpeed - V0.lengthSquared())) - 2*aDotV0) / (2*acc.lengthSquared());
        }
        else {
            TMax = 0f;
        }

        Vector2f newPos = new Vector2f(entity.getLocation());
        if (t <= TMax) {
            V0.scale(t);
            acc.scale(0.5f*t*t);
            Vector2f.add(newPos, V0, newPos);
            Vector2f.add(newPos, acc, newPos);
        }
        else {
            Vector2f VT = new Vector2f(V0);
            Vector2f ATemp = new Vector2f(acc);
            ATemp.scale(TMax);
            Vector2f.add(VT, ATemp, VT);
            VT.scale(t - TMax);

            V0.scale(TMax);
            acc.scale(0.5f*TMax*TMax);
            Vector2f.add(newPos, V0, newPos);
            Vector2f.add(newPos, acc, newPos);
            Vector2f.add(newPos, VT, newPos);
        }
        return newPos;
    }

    public static boolean isClockwise(Vector2f v1, Vector2f v2) {
        return v1.y * v2.x > v1.x * v2.y;
    }

    /** The Misc version requires a ShipAPI as the anchor location, this can take an arbitrary anchor point */
    public static ShipAPI getClosestEntity(
            Vector2f location,
            ShipAPI.HullSize smallestToNote,
            float maxRange,
            boolean considerShipRadius,
            TargetChecker checker) {
        ShipAPI closest = null;
        float closestDist = Float.MAX_VALUE;
        for (ShipAPI ship : Global.getCombatEngine().getShips()) {
            if (!checker.check(ship) || (smallestToNote != null && ship.getHullSize().compareTo(smallestToNote) < 0)) {
                continue;
            }

            float dist = Misc.getDistance(location, ship.getLocation());
            if (dist <= maxRange + (ship.getCollisionRadius() * (considerShipRadius ? 1f : 0f))
                    && dist < closestDist) {
                closest = ship;
                closestDist = dist;
            }
        }
        return closest;
    }

    /** Can be negative for points inside the entity */
    public static float getDistWithEntity(Vector2f location, CombatEntityAPI entity, boolean considerRadius) {
        return Misc.getDistance(location, entity.getLocation()) - (considerRadius ? entity.getCollisionRadius() : 0f);
    }

    public final static float maxRangeUseGrid = 300f;

    public static Collection<CombatEntityAPI> getKNearestEntities(
            int k,
            final Vector2f location,
            ShipAPI.HullSize smallestToNote,
            boolean includeMissiles,
            float maxRange,
            final boolean considerRadius,
            TargetChecker checker) {

        CombatEngineAPI engine = Global.getCombatEngine();
        // second entry is distance to location
        List<Pair<CombatEntityAPI, Float>> shipsAndMissiles = new ArrayList<>();

        if (maxRange <= maxRangeUseGrid) {
            Iterator<Object> itr = engine.getAllObjectGrid().getCheckIterator(location, 2f*maxRange, 2f*maxRange);
            while (itr.hasNext()) {
                Object o = itr.next();
                if (o instanceof ShipAPI) {
                    ShipAPI ship = (ShipAPI) o;
                    float dist = getDistWithEntity(location, ship, considerRadius);
                    if (dist <= maxRange && checker.check(ship) && (smallestToNote == null || ship.getHullSize().compareTo(smallestToNote) >= 0)) {
                        shipsAndMissiles.add(new Pair<CombatEntityAPI, Float>(ship, dist));
                    }
                }
                else if (o instanceof MissileAPI && includeMissiles) {
                    MissileAPI missile = (MissileAPI) o;
                    float dist = getDistWithEntity(location, missile, considerRadius);
                    if (dist <= maxRange && checker.check(missile)) {
                        shipsAndMissiles.add(new Pair<CombatEntityAPI, Float>(missile, dist));
                    }
                }
            }
        }
        else {
            for (ShipAPI ship : engine.getShips()) {
                float dist = getDistWithEntity(location, ship, considerRadius);
                if (dist <= maxRange && checker.check(ship) && (smallestToNote == null || ship.getHullSize().compareTo(smallestToNote) >= 0)) {
                    shipsAndMissiles.add(new Pair<CombatEntityAPI, Float>(ship, dist));
                }
            }
            if (includeMissiles) {
                for (MissileAPI missile : engine.getMissiles()) {
                    float dist = getDistWithEntity(location, missile, considerRadius);
                    if (dist <= maxRange && checker.check(missile)) {
                        shipsAndMissiles.add(new Pair<CombatEntityAPI, Float>(missile, dist));
                    }
                }
            }
        }
        Collections.sort(shipsAndMissiles, new Comparator<Pair<CombatEntityAPI, Float>>() {
            @Override
            public int compare(Pair<CombatEntityAPI, Float> p1, Pair<CombatEntityAPI, Float> p2) {
                return Float.compare(p1.two, p2.two);
            }
        });

        List<CombatEntityAPI> kNearest = new ArrayList<>();
        for (int i = 0; i < Math.min(k, shipsAndMissiles.size()); i++) {
            kNearest.add(shipsAndMissiles.get(i).one);
        }

        return kNearest;
    }

    /** Uses the collision grid. For small maxRange values. Set smallestToNote to null to include missiles. */
    public static boolean isEntityNearby(
            Vector2f location,
            ShipAPI.HullSize smallestToNote,
            float maxShipRange,
            float maxMissileRange,
            boolean considerCollisionRadius,
            TargetChecker checker) {
        float maxRange = Math.max(maxShipRange, maxMissileRange);
        Iterator<Object> itr = Global.getCombatEngine().getAllObjectGrid().getCheckIterator(location, 2f*maxRange, 2f*maxRange);
        while (itr.hasNext()) {
            Object obj = itr.next();
            if (!(obj instanceof CombatEntityAPI)) continue;

            CombatEntityAPI entity = (CombatEntityAPI) obj;
            if (!checker.check(entity)) {
                continue;
            }

            float dist = Misc.getDistance(location, entity.getLocation()) - (entity.getCollisionRadius() * (considerCollisionRadius ? 1f : 0f));

            if (dist <= maxMissileRange && entity instanceof MissileAPI && smallestToNote == null) {
                return true;
            }

            if (entity instanceof ShipAPI) {
                ShipAPI ship = (ShipAPI) entity;
                if (dist <= maxShipRange && (smallestToNote == null || ship.getHullSize().compareTo(smallestToNote) >= 0)) {
                    return true;
                }
            }
        }
        return false;
    }

    public static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    public static void safeNormalize(Vector2f v) {
        if (v.x*v.x + v.y*v.y > 0) {
            v.normalise();
        }
    }

    public static float modPositive(float x, float mod) {
        x = x % mod;
        return x < 0 ? x + mod : x;
    }

    /** returns f(b) - f(a) */
    public static float applyAtLimits(Function f, float a, float b) {
        return f.apply(b) - f.apply(a);
    }

    public static boolean doesSegmentHitShield(Vector2f a, Vector2f b, ShieldAPI shield) {
        if (shield == null) {
            return false;
        }

        Vector2f pt = Misc.intersectSegmentAndCircle(a, b, shield.getLocation(), shield.getRadius());
        return shield.isWithinArc(pt);
    }

    public static Vector2f randomPointInCircle(Vector2f center, float radius) {
        float theta = Misc.random.nextFloat() * 2f *  (float) Math.PI;
        float r = radius * (float) Math.sqrt(Misc.random.nextFloat());
        return new Vector2f(center.x + r*(float)Math.cos(theta), center.y + r*(float)Math.sin(theta));
    }

    public interface Function {
        float apply(float x);
    }

    public interface TargetChecker {
        boolean check(CombatEntityAPI entity);
    }

    public static class CommonChecker implements TargetChecker {
        public int side;

        public CommonChecker() {
            side = 100;
        }
        public CommonChecker(CombatEntityAPI owner) {
            side = owner.getOwner();
        }

        public void setSide(int side) {
            this.side = side;
        }

        @Override
        public boolean check(CombatEntityAPI entity) {
            return entity != null && Global.getCombatEngine().isEntityInPlay(entity) && entity.getHitpoints() > 0 && entity.getOwner() != side && entity.getOwner() != 100;
        }
    }

    public static void spawnFakeMine(Vector2f loc, float fakeRadius, float fakeDamageAmount, DamageType fakeDamageType, float dur) {
        String dummyWeapon = ModPlugin.dummyMissileWeapon;
        // Set the dummy spec to the appropriate values
        MissileSpecAPI dummyProjSpec = (MissileSpecAPI) Global.getSettings().getWeaponSpec(dummyWeapon).getProjectileSpec();
        dummyProjSpec.getDamage().setDamage(fakeDamageAmount);
        dummyProjSpec.setLaunchSpeed(0f);
        dummyProjSpec.getDamage().setType(fakeDamageType);


        final MissileAPI dummyProj = (MissileAPI) Global.getCombatEngine().spawnProjectile(null, null, dummyWeapon, loc, 0f, new Vector2f());
        dummyProj.setMine(true);
        dummyProj.setNoMineFFConcerns(true);
        dummyProj.setMinePrimed(true);
        dummyProj.setUntilMineExplosion(0f);
        dummyProj.setMineExplosionRange(fakeRadius);
        ActionPlugin.queueAction(new Action() {
            @Override
            public void perform() {
                Global.getCombatEngine().removeEntity(dummyProj);
            }
        }, dur);
    }
}
