package unthemedweapons.util;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.Pair;
import com.sun.istack.internal.NotNull;
import org.lwjgl.util.vector.Vector2f;

import java.util.*;

public abstract class CollisionUtils {

    @SuppressWarnings({"RedundantIfStatement", "BooleanMethodIsAlwaysInverted"})
    public static boolean canCollide(Object o, Collection<? extends CombatEntityAPI> ignoreList, ShipAPI source, boolean friendlyFire) {
        int owner = source == null ? 100 : source.getOwner();
        // Ignore things that aren't combat entities
        if (!(o instanceof CombatEntityAPI)) return false;
        // Ignore non-missile projectiles
        if ((o instanceof DamagingProjectileAPI) && !(o instanceof MissileAPI)) return false;
        // Ignore explicitly added items to ignore list
        if (ignoreList != null && ignoreList.contains(o)) return false;

        CombatEntityAPI entity = (CombatEntityAPI) o;

        // Ignore objects with NONE collision class
        if (CollisionClass.NONE.equals(entity.getCollisionClass())) return false;
        // Ignore phased ships
        if (entity instanceof ShipAPI && ((ShipAPI) entity).isPhased()) return false;
        // Always ignore source and source modules
        if (entity instanceof ShipAPI && EngineUtils.getBaseShip((ShipAPI) entity).equals(EngineUtils.getBaseShip(source))) return false;
        // Always ignore friendly fighters and missiles
        if (entity.getOwner() == owner && (o instanceof MissileAPI || EngineUtils.isFighter(entity))) return false;
        // Ignore all friendlies if friendly fire is off
        if (!friendlyFire && entity.getOwner() == owner) return false;

        return true;
    }

    /** Get the two extremal points of the shield's active arc */
    public static Pair<Vector2f, Vector2f> getShieldBounds(@NotNull ShieldAPI shield) {
        Vector2f shieldBound1 = Misc.getUnitVectorAtDegreeAngle(shield.getFacing() + shield.getActiveArc() / 2f);
        shieldBound1.scale(shield.getRadius());
        Vector2f.add(shieldBound1, shield.getLocation(), shieldBound1);
        Vector2f shieldBound2 = Misc.getUnitVectorAtDegreeAngle(shield.getFacing() - shield.getActiveArc() / 2f);
        shieldBound2.scale(shield.getRadius());
        Vector2f.add(shieldBound2, shield.getLocation(), shieldBound2);
        return new Pair<>(shieldBound1, shieldBound2);
    }

    /** Returns the point closest to {@code a} that intersects with {@code shield}, or {@code null} if no such
     *  point exists. */
    public static Vector2f rayCollisionCheckShield(Vector2f a, Vector2f b, ShieldAPI shield) {
        if (shield == null) return null;

        // Check if [a] itself is inside the shield
        if (Misc.getDistance(a, shield.getLocation()) <= shield.getRadius() && shield.isWithinArc(a)) {
            return a;
        }

        // Shield consists of 3 parts; two segments and an arc. Check collision with each of them separately and
        // return the closest point.
        Pair<Vector2f, Vector2f> shieldBounds = getShieldBounds(shield);

        List<Vector2f> pts = new ArrayList<>();
        pts.add(Misc.intersectSegments(a, b, shield.getLocation(), shieldBounds.one));
        pts.add(Misc.intersectSegments(a, b, shield.getLocation(), shieldBounds.two));
        pts.addAll(intersectSegmentCircle(a, b, shield.getLocation(), shield.getRadius()));

        Vector2f closestPt = null;
        float closestDist = Float.MAX_VALUE;
        for (Vector2f pt : pts) {
            if (pt == null || !shield.isWithinArc(pt)) continue;
            float dist = Misc.getDistance(a, pt);
            if ((closestPt == null) || Misc.getDistance(a, pt) < closestDist) {
                closestPt = pt;
                closestDist = dist;
            }
        }

        return closestPt;
    }

    public static Vector2f circleCollisionCheckShield(Vector2f o, float r, ShieldAPI shield) {
        if (shield == null) return null;

        // Check if [o] itself is inside the shield
        if (Misc.getDistance(o, shield.getLocation()) < shield.getRadius() && shield.isWithinArc(o)) {
            return o;
        }

        // Shield consists of 3 parts; two segments and an arc. Check collision with each of them separately and
        // return the closest point.
        Pair<Vector2f, Vector2f> shieldBounds = getShieldBounds(shield);

        List<Vector2f> pts = new ArrayList<>();
        pts.addAll(intersectSegmentCircle(shield.getLocation(), shieldBounds.one, o, r));
        pts.addAll(intersectSegmentCircle(shield.getLocation(), shieldBounds.two, o, r));
        Pair<Vector2f, Vector2f> circlePts = intersectCircles(shield.getLocation(), shield.getRadius(), o, r);
        if (circlePts != null) {
            pts.add(circlePts.one);
            pts.add(circlePts.two);
        }

        Vector2f closestPt = null;
        float closestDist = Float.MAX_VALUE;
        for (Vector2f pt : pts) {
            if (pt == null || !shield.isWithinArc(pt)) continue;
            float dist = Misc.getDistance(o, pt);
            if ((closestPt == null) || Misc.getDistance(o, pt) < closestDist) {
                closestPt = pt;
                closestDist = dist;
            }
        }

        return closestPt;
    }

    /** Returns the point closest to {@code a} that intersects with {@code entity}'s bounds, or {@code null} if no such
     *  point exists. */
    public static Vector2f rayCollisionCheckBounds(Vector2f a, Vector2f b, CombatEntityAPI entity) {
        // If [a] and [b] are both outside of the collision radius, and the a-b segment doesn't intersect
        // the collision circle, then there cannot be any collisions, so exit early.
        if (Misc.getDistance(a, entity.getLocation()) > entity.getCollisionRadius()
                && Misc.getDistance(b, entity.getLocation()) > entity.getCollisionRadius()
                && Misc.intersectSegmentAndCircle(a, b, entity.getLocation(), entity.getCollisionRadius()) == null) {
            return null;
        }

        return rayCollisionCheckBoundsNoEarlyExit(a, b, entity);
    }

    public static Vector2f circleCollisionCheckBounds(Vector2f o, float r, CombatEntityAPI entity) {
        // Pre-check collision radius
        if (Misc.getDistance(o, entity.getLocation()) > r + entity.getCollisionRadius()) {
            return null;
        }

        return circleCollisionCheckBoundsNoEarlyExit(o, r, entity);
    }

    private static Vector2f rayCollisionCheckBoundsNoEarlyExit(Vector2f a, Vector2f b, CombatEntityAPI entity) {
        BoundsAPI bounds = entity.getExactBounds();

        // If the object has no bounds or is a projectile, then use the collision radius
        if (bounds == null || entity instanceof DamagingProjectileAPI) {
            // [a] is inside the collision radius
            if (Misc.getDistance(a, entity.getLocation()) <= entity.getCollisionRadius()) {
                return a;
            }
            return Misc.intersectSegmentAndCircle(a, b, entity.getLocation(), entity.getCollisionRadius());
        }

        // Check exact bounds
        bounds.update(entity.getLocation(), entity.getFacing());
        List<BoundsAPI.SegmentAPI> segments = bounds.getSegments();

        // Check if [a] is inside bounds
        List<Vector2f> boundVerts = new ArrayList<>();
        boundVerts.add(segments.get(0).getP1());
        for (BoundsAPI.SegmentAPI segment : segments) {
            boundVerts.add(segment.getP2());
        }
        if (Misc.isPointInBounds(a, boundVerts)) {
            return a;
        }

        // Find the closest collision with a segment of the bounds
        Vector2f closestPoint = null;
        float closestDist = Float.MAX_VALUE;
        for (BoundsAPI.SegmentAPI segment : segments) {
            Vector2f collisionPoint =
                    Misc.intersectSegments(segment.getP1(), segment.getP2(), a, b);
            if (collisionPoint != null) {
                float dist = Misc.getDistance(a, collisionPoint);
                if (dist < closestDist) {
                    closestDist = dist;
                    closestPoint = collisionPoint;
                }
            }
        }

        return closestPoint;
    }

    private static Vector2f circleCollisionCheckBoundsNoEarlyExit(Vector2f o, float r, CombatEntityAPI entity) {
        BoundsAPI bounds = entity.getExactBounds();

        // If the object has no bounds or is a projectile, then use the collision radius
        if (bounds == null || entity instanceof DamagingProjectileAPI) {
            // [o] is inside the collision radius
            if (Misc.getDistance(o, entity.getLocation()) <= entity.getCollisionRadius()) {
                return o;
            }
            Pair<Vector2f, Vector2f> pts = intersectCircles(entity.getLocation(), entity.getCollisionRadius(), o, r);
            if (pts == null) return null;
            if (Misc.getDistance(o, pts.one) <= Misc.getDistance(o, pts.two)) {
                return pts.one;
            }
            else {
                return pts.two;
            }
        }

        // Check exact bounds
        bounds.update(entity.getLocation(), entity.getFacing());
        List<BoundsAPI.SegmentAPI> segments = bounds.getSegments();

        // Check if [a] is inside bounds
        List<Vector2f> boundVerts = new ArrayList<>();
        boundVerts.add(segments.get(0).getP1());
        for (BoundsAPI.SegmentAPI segment : segments) {
            boundVerts.add(segment.getP2());
        }
        if (Misc.isPointInBounds(o, boundVerts)) {
            return o;
        }

        // Find the closest collision with a segment of the bounds
        Vector2f closestPoint = null;
        float closestDist = Float.MAX_VALUE;
        for (BoundsAPI.SegmentAPI segment : segments) {
            List<Vector2f> collisionPoints = intersectSegmentCircle(segment.getP1(), segment.getP2(), o, r);
            for (Vector2f pt : collisionPoints) {
                if (pt != null) {
                    float dist = Misc.getDistance(o, pt);
                    if (dist < closestDist) {
                        closestDist = dist;
                        closestPoint = pt;
                    }
                }
            }
        }

        return closestPoint;
    }

    /** Considers both the entity's shield and bounds. Second return value is whether the hit was a shield hit. */
    public static Pair<Vector2f, Boolean> rayCollisionCheckEntity(Vector2f a, Vector2f b, CombatEntityAPI entity) {

        // If [a] and [b] are both outside of the collision radius, and the a-b segment doesn't intersect
        // the collision circle, then there cannot be any collisions, so exit early.
        if (Misc.getDistance(a, entity.getLocation()) > entity.getCollisionRadius()
                && Misc.getDistance(b, entity.getLocation()) > entity.getCollisionRadius()
                && Misc.intersectSegmentAndCircle(a, b, entity.getLocation(), entity.getCollisionRadius()) == null) {
            return new Pair<>(null, false);
        }

        ShieldAPI thisShield = entity.getShield();
        Vector2f bestShieldHitPt = null;
        float bestShieldHitDist = Float.MAX_VALUE;

        // Find the closest collision point with this entity's shield, or any of its child modules' shields,
        // or its parent module's shields, or any of its parent's children's shields.

        List<ShieldAPI> allShields = new ArrayList<>();
        if (thisShield != null) {
            allShields.add(thisShield);
        }
        if (entity instanceof ShipAPI) {
            ShipAPI ship = (ShipAPI) entity;
            // [entity] is itself a parent module
            for (ShipAPI child : ship.getChildModulesCopy()) {
                if (child.getShield() != null) {
                    allShields.add(child.getShield());
                }
            }
            // [entity] is a child module
            if (ship.getParentStation() != null) {
                ShipAPI parent = ship.getParentStation();
                // Check if parent has covering shields
                if (parent.getShield() != null) {
                    allShields.add(parent.getShield());
                }
                else {
                    // Check if any of parent's children have covering shields
                    for (ShipAPI child : parent.getChildModulesCopy()) {
                        if (child.getShield() != null) {
                            allShields.add(child.getShield());
                        }
                    }
                }
            }
        }

        for (ShieldAPI shield : allShields) {
            Vector2f hitLoc = rayCollisionCheckShield(a, b, shield);
            if (hitLoc != null) {
                float dist = Misc.getDistance(a, hitLoc);
                if (dist < bestShieldHitDist) {
                    bestShieldHitDist = dist;
                    bestShieldHitPt = hitLoc;
                }
            }
        }

        Vector2f boundsHitLoc = rayCollisionCheckBounds(a, b, entity);

        if (bestShieldHitPt == null) return new Pair<>(boundsHitLoc, false);
        if (boundsHitLoc == null) return new Pair<>(bestShieldHitPt, true);

        float boundsHitDist = Misc.getDistance(a, boundsHitLoc);

        return bestShieldHitDist < boundsHitDist ? new Pair<>(bestShieldHitPt, true) : new Pair<>(boundsHitLoc, false);
    }

    public static Pair<Vector2f, Boolean> circleCollisionCheckEntity(Vector2f o, float r, CombatEntityAPI entity) {
        // Pre-check collision radius
        if (Misc.getDistance(o, entity.getLocation()) > r + entity.getCollisionRadius()) {
            return new Pair<>(null, false);
        }

        Vector2f shieldHitLoc = circleCollisionCheckShield(o, r, entity.getShield());
        Vector2f boundsHitLoc = circleCollisionCheckBounds(o, r, entity);

        if (shieldHitLoc == null) return new Pair<>(boundsHitLoc, false);
        if (boundsHitLoc == null) return new Pair<>(shieldHitLoc, true);

        float shieldHitDist = Misc.getDistance(o, shieldHitLoc);
        float boundsHitDist = Misc.getDistance(o, boundsHitLoc);

        return shieldHitDist < boundsHitDist ? new Pair<>(shieldHitLoc, true) : new Pair<>(boundsHitLoc, false);
    }

    /**
     * Checks if the segment from a to b collides with an entity and returns the collision point closest to a.
     * Returns null if there was no collision.
     */
    public static ClosestCollisionData rayCollisionCheck(
            Vector2f a,
            Vector2f b,
            Collection<? extends CombatEntityAPI> ignoreList,
            ShipAPI source,
            boolean friendlyFire,
            CombatEngineAPI engine) {
        float length = Misc.getDistance(a, b);
        return rayCollisionCheck(a, b, ignoreList, source, friendlyFire, engine.getAllObjectGrid().getCheckIterator(a, length, length));
    }

    /** Helper method to avoid repeaing engine.getAllObjectGrid().getCheckIterator calls */
    private static ClosestCollisionData rayCollisionCheck(
            Vector2f a,
            Vector2f b,
            Collection<? extends CombatEntityAPI> ignoreList,
            ShipAPI source,
            boolean friendlyFire,
            Iterator<Object> itr) {

        // Keep track of the closest collision point as that is the one that we will end up using
        // Distance to previous location is tracked
        ClosestCollisionData closest = new ClosestCollisionData();

        while (itr.hasNext()) {
            Object obj = itr.next();
            if (!canCollide(obj, ignoreList, source, friendlyFire)) continue;

            CombatEntityAPI o = (CombatEntityAPI) obj;
            Pair<Vector2f, Boolean> pair = rayCollisionCheckEntity(a, b, o);
            Vector2f closestPoint = pair.one;
            if (closestPoint != null) {
                float dist = Misc.getDistance(a, closestPoint);
                closest.updateClosest(closestPoint, o, dist, pair.two);
                // If the point is just [a], then nothing can be closer, so break early.
                if (dist <= 0.001f) {
                    break;
                }
            }
        }

        if (closest.isEmpty) {
            return null;
        }

        return closest;
    }

    public static ClosestCollisionData circleCollisionCheck(
            Vector2f o,
            float r,
            Collection<? extends CombatEntityAPI> ignoreList,
            ShipAPI source,
            boolean friendlyFire,
            CombatEngineAPI engine) {

        Iterator<Object> itr = engine.getAllObjectGrid().getCheckIterator(o, 2f*r, 2f*r);
        // Keep track of the closest collision point as that is the one that we will end up using
        // Distance to previous location is tracked
        ClosestCollisionData closest = new ClosestCollisionData();

        while (itr.hasNext()) {
            Object obj = itr.next();
            if (!canCollide(obj, ignoreList, source, friendlyFire)) continue;

            CombatEntityAPI entity = (CombatEntityAPI) obj;
            Pair<Vector2f, Boolean> pair = circleCollisionCheckEntity(o, r, entity);
            Vector2f closestPoint = pair.one;
            if (closestPoint != null) {
                float dist = Misc.getDistance(o, closestPoint);
                closest.updateClosest(closestPoint, entity, dist, pair.two);
                // If the point is just [a], then nothing can be closer, so break early.
                if (dist <= 0.001f) {
                    break;
                }
            }
        }

        if (closest.isEmpty) {
            return null;
        }

        return closest;
    }

    public static ClosestCollisionData rayCollisionCheck(Vector2f a, Vector2f b, ShipAPI source, boolean friendlyFire, CombatEngineAPI engine) {
        return rayCollisionCheck(a, b, null, source, friendlyFire, engine);
    }

    /** Checks if a shot would hit an ally, crudely using only the collision radius */
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public static boolean rayCollisionCheckAlly(Vector2f a, Vector2f b, ShipAPI source, float increaseRadius, boolean includeHulks) {
        List<ShipAPI> ships = Global.getCombatEngine().getShips();
        for (ShipAPI ship : ships) {
            // Can't hit friendly fighters
            if (ship.isFighter()) continue;
            if (!(includeHulks && ship.isHulk())) {
                if (ship.getOwner() != source.getOwner()) continue;
                if (ship.isPhased()) continue;
                if (source.equals(ship)) continue;
            }

            if (Misc.intersectSegmentAndCircle(a, b, ship.getLocation(), ship.getCollisionRadius() + increaseRadius) != null) {
                return true;
            }
        }
        return false;
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

        float vx = MathUtils.sgnPos(dy)*dx*(float) Math.sqrt(disc);
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

    public static class ClosestCollisionData {
        public float distance;
        public Vector2f point;
        public CombatEntityAPI entity;
        public boolean shieldHit;
        private boolean isEmpty = true;

        private ClosestCollisionData() {
            distance = Float.POSITIVE_INFINITY;
            point = null;
            entity = null;
            shieldHit = false;
        }

        private void updateClosest(Vector2f newPt, CombatEntityAPI newEntity, float newDist, boolean shieldHit) {
            if (newDist < distance) {
                distance = newDist;
                entity = newEntity;
                point = newPt;
                isEmpty = false;
                this.shieldHit = shieldHit;
            }
        }
    }
}
