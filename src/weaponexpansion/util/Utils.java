package weaponexpansion.util;

import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.util.Misc;
import org.lwjgl.util.vector.Vector2f;
import weaponexpansion.combat.plugins.CombatPlugin;
import weaponexpansion.combat.scripts.SuperRailgunEffect;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
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

    /**
     * Checks if the segment from a to b collides with an entity and returns the collision point closest to a.
     * Returns null if there was no collision.
     */
    public static ClosestCollisionData collisionCheck(Vector2f a, Vector2f b, Collection<? extends CombatEntityAPI> ignoreList, CombatEngineAPI engine) {
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

    public static float angleDiff(float a, float b) {
        return ((b - a) % 360 + 540) % 360 - 180;
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
}
