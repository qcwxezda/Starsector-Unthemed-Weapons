package unthemedweapons.util;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.loading.MissileSpecAPI;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.Pair;
import org.lwjgl.util.vector.Vector2f;
import unthemedweapons.ModPlugin;
import unthemedweapons.combat.plugins.Action;
import unthemedweapons.combat.plugins.ActionPlugin;

import java.util.*;

public abstract class EngineUtils {
    public final static float maxRangeUseGrid = 300f;

    /** If the argument is a ship, returns that ship.
     *  If the argument is a wing, returns the wing's source ship.
     *  If the argument is a module, returns the module's base ship/station. */
    public static ShipAPI getBaseShip(ShipAPI shipOrModule) {
        if (shipOrModule == null) {
            return null;
        }
        if (shipOrModule.isStationModule()) {
            ShipAPI base = null;
            if (shipOrModule.getParentStation() == null) {
                // If the module has no parent station but has a fleet member,
                // just return the module itself
                if (shipOrModule.getFleetMember() != null) {
                    base = shipOrModule;
                }
            }
            else {
                base = getBaseShip(shipOrModule.getParentStation());
            }
            return base;
        }
        return shipOrModule;
    }

    public static boolean isFighter(CombatEntityAPI entity) {
        return entity instanceof ShipAPI && ((ShipAPI) entity).isFighter();
    }

    /** For big explosions. Applies damage to all entities around a ring. */
    public static void applyDamageOnRing(
            Vector2f origin,
            float radius,
            boolean friendlyFire,
            boolean canDamageSource,
            Collection<? extends CombatEntityAPI> ignoreList,
            float totalDamage,
            DamageType damageType,
            float totalEmp,
            boolean bypassShields,
            boolean dealsSoftFlux,
            DamagingProjectileAPI projSource,
            boolean playSound) {
        CombatEngineAPI engine = Global.getCombatEngine();
        ShipAPI source = projSource.getSource();
        // damage multiplier, points to apply damage to
        List<Pair<Float, List<Vector2f>>> damageList = new ArrayList<>();
        Iterator<Object> objItr = engine.getAllObjectGrid().getCheckIterator(origin, 2f*radius, 2f*radius);
        while (objItr.hasNext()) {
            Object o = objItr.next();
            if (!CollisionUtils.canCollide(o, ignoreList, source, friendlyFire) && (!(canDamageSource && o.equals(source)))) continue;

            CombatEntityAPI entity = (CombatEntityAPI) o;

            BoundsAPI bounds = entity.getExactBounds();
            if (bounds == null) {
                Pair<Vector2f, Vector2f> collisionPoints = CollisionUtils.intersectCircles(origin, radius, entity.getLocation(), entity.getCollisionRadius());
                if (collisionPoints != null) {
                    Vector2f p1 = Misc.getDiff(collisionPoints.one, origin);
                    Vector2f p2 = Misc.getDiff(collisionPoints.two, origin);
                    float theta1 = (float) Math.atan2(p1.y, p1.x) * Misc.DEG_PER_RAD;
                    float theta2 = (float) Math.atan2(p2.y, p2.x) * Misc.DEG_PER_RAD;
                    float subtendedAngle = Math.abs(MathUtils.angleDiff(theta1, theta2));

                    if (subtendedAngle > 0f) {
                        damageList.add(new Pair<>((float) Math.sqrt(subtendedAngle / 360f), Arrays.asList(collisionPoints.one, collisionPoints.two)));
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
                        Pair<Vector2f, Vector2f> shieldPts = CollisionUtils.intersectCircles(shieldCenter, shieldRadius, origin, radius);
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
                        collisionPoints.addAll(CollisionUtils.intersectSegmentCircle(shieldCenter, shieldEnd1, origin, radius));
                        collisionPoints.addAll(CollisionUtils.intersectSegmentCircle(shieldCenter, shieldEnd2, origin, radius));

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
                                float subtendedAngle = Math.abs(MathUtils.angleDiff(collisionAngles.get(a), collisionAngles.get(b)));

                                if (subtendedAngle > 0f) {
                                    Vector2f p1 = new Vector2f(
                                            origin.x + radius * (float) Math.cos(collisionAngles.get(a) * Misc.RAD_PER_DEG),
                                            origin.y + radius * (float) Math.sin(collisionAngles.get(a) * Misc.RAD_PER_DEG));
                                    Vector2f p2 = new Vector2f(
                                            origin.x + radius * (float) Math.cos(collisionAngles.get(b) * Misc.RAD_PER_DEG),
                                            origin.y + radius * (float) Math.sin(collisionAngles.get(b) * Misc.RAD_PER_DEG));
                                    List<Vector2f> points = new ArrayList<>();
                                    if (entity.getShield().isWithinArc(p1)) {
                                        points.add(p1);
                                    }
                                    if (entity.getShield().isWithinArc(p2)) {
                                        points.add(p2);
                                    }
                                    if (!points.isEmpty()) {
                                        damageList.add(new Pair<>((float) Math.sqrt(subtendedAngle / 360f), points));
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
                    List<Vector2f> pts = CollisionUtils.intersectSegmentCircle(segment.getP1(), segment.getP2(), origin, radius);
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
                        float subtendedAngle = Math.abs(MathUtils.angleDiff(collisionAngles.get(a), collisionAngles.get(b)));

                        if (subtendedAngle > 0f) {
                            Vector2f p1 = new Vector2f(
                                    origin.x + radius * (float) Math.cos(collisionAngles.get(a) * Misc.RAD_PER_DEG),
                                    origin.y + radius * (float) Math.sin(collisionAngles.get(a) * Misc.RAD_PER_DEG));
                            Vector2f p2 = new Vector2f(
                                    origin.x + radius * (float) Math.cos(collisionAngles.get(b) * Misc.RAD_PER_DEG),
                                    origin.y + radius * (float) Math.sin(collisionAngles.get(b) * Misc.RAD_PER_DEG));
                            List<Vector2f> points = new ArrayList<>();
                            if (entity.getShield() == null || !entity.getShield().isWithinArc(p1)) {
                                points.add(p1);
                            }
                            if (entity.getShield() == null || !entity.getShield().isWithinArc(p2)) {
                                points.add(p2);
                            }
                            if (!points.isEmpty()) {
                                damageList.add(new Pair<>((float) Math.sqrt(subtendedAngle / 360f), points));
                            }
                        }
                    }
                }
            }

            for (Pair<Float, List<Vector2f>> entry : damageList) {
                for (Vector2f pt : entry.two) {
                    engine.applyDamage(projSource, entity, pt, totalDamage * 0.5f * entry.one, damageType, totalEmp * 0.5f * entry.one, bypassShields, dealsSoftFlux, source, playSound);
                }
            }
        }
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

    public static Collection<ShipAPI> getAllShipsInRange(Vector2f location, float radius, boolean includeFighters, CombatEngineAPI engine) {
        Set<ShipAPI> ships = new HashSet<>();

        for (ShipAPI ship : engine.getShips()) {
            if (Misc.getDistance(ship.getLocation(), location) <= radius + ship.getCollisionRadius()) {
                if (!ship.isFighter() || includeFighters) {
                    ships.add(ship);
                }
            }
        }

        return ships;
    }

    public static Collection<CombatEntityAPI> getKNearestEntities(
            int k,
            Vector2f location,
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

    /** Deals damage instantaneously. Used in cluster mine so multiple mines don't explode on the same already-dead target */
    public static void spawnInstantaneousExplosion(Vector2f loc, float radius, float damageAmount, float empAmount, DamageType damageType, DamagingProjectileAPI projSource, Set<CombatEntityAPI> alreadyDamaged, CombatEngineAPI engine) {
        Iterator<Object> itr = engine.getAllObjectGrid().getCheckIterator(loc, 2f*radius, 2f*radius);
        ShipAPI source = projSource.getSource();
        if (alreadyDamaged == null) {
            alreadyDamaged = new HashSet<>();
        }
        else {
            // Abstract sets don't have the add operation
            alreadyDamaged = new HashSet<>(alreadyDamaged);
        }
        while (itr.hasNext()) {
            Object o = itr.next();
            if (!CollisionUtils.canCollide(o, null, source, true)) continue;
            CombatEntityAPI entity = (CombatEntityAPI) o;
            if (alreadyDamaged.contains(entity)) continue;

            alreadyDamaged.add(entity);
            Pair<Vector2f, Boolean> pair =
                    CollisionUtils.rayCollisionCheckEntity(
                            loc,
                            entity instanceof ShipAPI && !((ShipAPI) entity).isFighter() ? MathUtils.getVertexCenter(entity): entity.getLocation(),
                            entity);
            if (pair.one == null) continue;
            float dist = Misc.getDistance(pair.one, loc);
            if (dist > radius) continue;
            float damage = damageAmount*0.5f + damageAmount*0.5f * (radius - dist) / radius;
            float emp = empAmount*0.5f + empAmount*0.5f * (radius - dist) / radius;

            engine.applyDamage(entity, entity, pair.one, damage, damageType, emp, false, false, source, true);
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

    /** targetPoint could be target.getLocation(), or could account for target leading, etc. */
    public static boolean isInRange(WeaponAPI weapon, CombatEntityAPI target, Vector2f targetPoint) {
        if (Misc.getDistance(weapon.getLocation(), targetPoint) > weapon.getRange() + target.getCollisionRadius()) return false;
        return Misc.getAngleDiff(Misc.getAngleInDegrees(weapon.getLocation(), targetPoint), weapon.getCurrAngle()) <= weapon.getArc() / 2f;
    }
}
