package weaponexpansion.combat.scripts;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.BoundsAPI.SegmentAPI;
import com.fs.starfarer.api.util.Misc;
import org.lwjgl.util.vector.Vector2f;
import weaponexpansion.util.GlowRenderer;

import java.awt.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

@SuppressWarnings("unused")
public class SuperRailgunEffect extends GlowOnFirePlugin {

    static final int maxPierce = 3;
    static final float minTimeBetweenHits = 0.08f;
    /** Fraction of current damage lost per pierce (not base damage) */
    static final float damageDecayPerHit = 0.3f;
    static final float speedLossDuringHit = 0.6f;
    static final Color hitGlowColor = new Color(255, 225, 128);
    static final Color chargeGlowColor = new Color(65, 130, 195);

    /** Whether hitting missiles counts toward the pierce limit */
    static final boolean passThroughMissiles = true;

    /** Whether hitting fighters counts toward the pierce limit */
    static final boolean passThroughFighters = true;

    GlowRenderer glowRenderer2;

    @Override
    public void init(WeaponAPI weapon) {
        glowRenderer2 = new GlowRenderer(weapon, true);
        glowRenderer2.setRenderColor(chargeGlowColor);
        Global.getCombatEngine().addLayeredRenderingPlugin(glowRenderer2);
        super.init(weapon);
    }

    private static class ProjectileData {
        private final DamagingProjectileAPI proj;
        private int timesPierced;
        private float timeSinceLastHit;
        private final Vector2f initialVelocity;
        //private float actualDamage, actualEmpDamage;

        private ProjectileData(DamagingProjectileAPI proj) {
            this.proj = proj;
            timesPierced = 0;
            timeSinceLastHit = 0f;
            initialVelocity = new Vector2f(proj.getVelocity());
//            float totalDamageRatio = getTotalBaseDamageMultiplier();
//            actualDamage = proj.getBaseDamageAmount();
//            actualEmpDamage = proj.getEmpAmount();
//            proj.setDamageAmount(totalDamageRatio * proj.getBaseDamageAmount());
//            proj.getDamage().setFluxComponent(totalDamageRatio * proj.getEmpAmount());
        }

        private void advance(float amount) {
            timeSinceLastHit += amount;
        }

        private void registerHit(CombatEntityAPI target) {
            timeSinceLastHit = 0f;
            boolean incrementPierced = !(target instanceof MissileAPI) || !passThroughMissiles;
            if (target instanceof ShipAPI && ((ShipAPI) target).isFighter() && passThroughFighters) {
                incrementPierced = false;
            }
            if (incrementPierced) {
                timesPierced++;
                proj.setDamageAmount(proj.getBaseDamageAmount() * (1 - damageDecayPerHit));
                proj.getDamage().setFluxComponent(proj.getEmpAmount() * (1 - damageDecayPerHit));
//                proj.setDamageAmount(proj.getBaseDamageAmount() - actualDamage);
//                proj.getDamage().setFluxComponent(proj.getEmpAmount() - actualEmpDamage);
//                actualDamage *= 1 - damageDecayPerHit;
//                actualEmpDamage *= 1 - damageDecayPerHit;
            }
        }

        private float getTotalBaseDamageMultiplier() {
            float sum = 0f, multiplier = 1f;
            for (int i = 0; i <= maxPierce; i++) {
                sum += multiplier;
                multiplier *= 1 - damageDecayPerHit;
            }
            return sum;
        }
    }

    private static class ClosestCollisionData {
        private float distance;
        private Vector2f point;
        private CombatEntityAPI entity;
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

    private final List<ProjectileData> projectiles = new LinkedList<>();

    @Override
    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
        glowRenderer2.setAlpha(weapon.getChargeLevel());

        super.advance(amount, engine, weapon);

        Iterator<ProjectileData> itr = projectiles.iterator();
        while (itr.hasNext()) {
            ProjectileData data = itr.next();
            DamagingProjectileAPI proj = data.proj;

            // TIme since last hit should advance in world time, not ship time
            float realAmount = amount;
            if (weapon.getShip() != null) {
                MutableStat timeMult = weapon.getShip().getMutableStats().getTimeMult();
                if (timeMult != null) {
                    realAmount /= timeMult.getModifiedValue();
                }
            }
            data.advance(realAmount);

            if (data.timeSinceLastHit <= minTimeBetweenHits) {
                continue;
            } else {
                proj.getVelocity().set(data.initialVelocity);
            }

            if (proj.isExpired()) {
                itr.remove();
                continue;
            }

            Vector2f location = proj.getLocation();
            Vector2f prevLocation = new Vector2f();
            Vector2f scaledVelocity = new Vector2f(proj.getVelocity().x * amount, proj.getVelocity().y * amount);
            Vector2f.sub(location, scaledVelocity, prevLocation);

            Iterator<Object> objItr = engine.getAllObjectGrid().getCheckIterator(
                    location,
                    proj.getMoveSpeed(),
                    proj.getMoveSpeed());

            // Keep track of the closest collision point as that is the one that we will end up using
            // Distance to previous location is tracked
            ClosestCollisionData closest = new ClosestCollisionData();

            while (objItr.hasNext()) {
                Object obj = objItr.next();
                if (!(obj instanceof CombatEntityAPI)) continue;
                if (obj.equals(weapon.getShip())) continue;
                if (obj.equals(proj)) continue;
                if ((obj instanceof DamagingProjectileAPI) && !(obj instanceof MissileAPI)) continue;

                CombatEntityAPI o = (CombatEntityAPI) obj;

                // Pre-check collision radius, exit early if outside to prevent unnecessary computation
                Vector2f collisionRadiusPoint =
                        Misc.intersectSegmentAndCircle(
                                prevLocation,
                                location,
                                o.getLocation(),
                                o.getCollisionRadius());
                if (collisionRadiusPoint == null) {
                    continue;
                }

                // If it's a ship, check collision with shields
                if (o instanceof ShipAPI) {
                    ShipAPI ship = (ShipAPI) o;
                    if (ship.isPhased()) continue;

                    if (ship.getShield() != null) {

                        // Actual point itself is inside shield
                        //
                        if (Misc.getDistance(ship.getShieldCenterEvenIfNoShield(), prevLocation) < ship.getShieldRadiusEvenIfNoShield()
                                && ship.getShield().isWithinArc(prevLocation)) {
                            closest.updateClosest(prevLocation, o, 0f);
                            // Obviously nothing can be closer; break
                            break;
                        }

                        Vector2f collisionPoint =
                                Misc.intersectSegmentAndCircle(
                                        prevLocation,
                                        location,
                                        ship.getShieldCenterEvenIfNoShield(),
                                        ship.getShieldRadiusEvenIfNoShield());
                        if (collisionPoint != null && ship.getShield().isWithinArc(collisionPoint)) {
                            float dist = Misc.getDistance(prevLocation, collisionPoint);
                            if (dist < closest.distance) {
                                closest.updateClosest(collisionPoint, o, dist);
                            }
                        }
                    }
                }

                // No exact bounds -- check collision radius
                if (o.getExactBounds() == null) {

                    // Actual point itself is inside radius
                    if (Misc.getDistance(prevLocation, o.getLocation()) <= o.getCollisionRadius()) {
                        closest.updateClosest(prevLocation, o, 0f);
                        // Obviously nothing else can be closer
                        break;
                    }

                    float dist = Misc.getDistance(prevLocation, collisionRadiusPoint);
                    if (dist < closest.distance) {
                            closest.updateClosest(collisionRadiusPoint, o, dist);
                    }
                    continue;
                }

                // Check exact bounds
                BoundsAPI bounds = o.getExactBounds();
                bounds.update(o.getLocation(), o.getFacing());
                List<SegmentAPI> segments = bounds.getSegments();

                // Check if point itself is inside bounds
                List<Vector2f> boundVerts = new ArrayList<>();
                boundVerts.add(segments.get(0).getP1());
                for (SegmentAPI segment : segments) {
                    boundVerts.add(segment.getP2());
                }
                if (Misc.isPointInBounds(prevLocation, boundVerts)) {
                    closest.updateClosest(prevLocation, o, 0f);
                    break;
                }

                for (SegmentAPI segment : segments) {
                    Vector2f collisionPoint =
                            Misc.intersectSegments(segment.getP1(), segment.getP2(), prevLocation, location);
                    if (collisionPoint != null) {
                        float dist = Misc.getDistance(prevLocation, collisionPoint);
                        if (dist < closest.distance) {
                            closest.updateClosest(collisionPoint, o, dist);
                        }
                    }
                }
            }

            if (closest.isEmpty) {
                continue;
            }

            CombatEntityAPI target = closest.entity;
//            Vector2f newLoc = new Vector2f(closest.point);
//
//            // If timeSinceLastHit - minTimeBetweenHits is less than the frame time (amount), the hit was registered
//            // on the first possible frame. Move the projectile back based on how "late" this frame was.
//            if (data.timeSinceLastHit - minTimeBetweenHits < amount) {
//                float amountToMove = data.timeSinceLastHit - minTimeBetweenHits;
//                Vector2f.sub(newLoc, new Vector2f(proj.getVelocity().x * amountToMove, proj.getVelocity().y * amountToMove), newLoc);
//            }
//
//            // Now we have every collision point for this frame, we set the location of the projectile to
//            // be the closest one and apply the appropriate amount of damage
//            proj.getLocation().set(newLoc);

            proj.getVelocity().set(new Vector2f(data.initialVelocity.x * (1f - speedLossDuringHit), data.initialVelocity.y * (1f - speedLossDuringHit)));

            engine.applyDamage(
                    target,
                    closest.point,
                    proj.getDamageAmount(),
                    proj.getDamageType(),
                    proj.getEmpAmount(),
                    false,
                    proj.isFading(),
                    weapon.getShip(),
                    true);
            engine.addHitParticle(
                    closest.point,
                    target.getVelocity(),
                    proj.getProjectileSpec().getHitGlowRadius(),
                    1f,
                    hitGlowColor);
            data.registerHit(target);

            if (data.timesPierced > maxPierce) {
                engine.removeEntity(proj);
                itr.remove();
            }
        }
    }

    @Override
    public void onFire(DamagingProjectileAPI proj, WeaponAPI weapon, CombatEngineAPI engine) {
        super.onFire(proj, weapon, engine);
        projectiles.add(new ProjectileData(proj));
    }
}
