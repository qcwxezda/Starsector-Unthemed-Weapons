package unthemedweapons.combat.shipsystems;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.Misc;
import org.lwjgl.util.vector.Vector2f;

import java.util.Iterator;

@SuppressWarnings("unused")
public class ReshaperAI implements ShipSystemAIScript {

    private ShipAPI ship;
    // How much of current HP should be lost in the repairable interval to warrant immediate use where possible, as a fraction
    private ShipSystemAPI system;
    private static final float hpLossFractionMajor = 0.6f;

    // Each entry represents 10% of the repairable interval. i.e. if taking more than damageTakenThreshold[0] in the interval
    // (currentTime - repairableInterval, currentTime - repairableInterval * 0.9), activate if possible.
    // Note: represents threshold at a single charge. AI will be more liberal if it has more charges to spare
    private static final float[] damageTakenThresholds = new float[] {1500f, 3000f, 4000f, 4500f, 5000f, 6000f, 9000f, 15000f, 1000000f, 1000000f};
    private static final float checkIntervalAmount = 0.5f;
    private static final float checkProjectilesRadius = 200f;
    private final IntervalUtil checkInterval = new IntervalUtil(checkIntervalAmount, checkIntervalAmount);
    private ReshaperStats.ReshaperTracker listener;

    @Override
    public void init(ShipAPI ship, ShipSystemAPI system, ShipwideAIFlags flags, CombatEngineAPI engine) {
        this.ship = ship;
        this.system = system;
        this.listener = ship.getListeners(ReshaperStats.ReshaperTracker.class).get(0);
    }

    @Override
    public void advance(float amount, Vector2f missileDangerDir, Vector2f collisionDangerDir, ShipAPI target) {
        checkInterval.advance(amount);
        if (system.getCooldownRemaining() > 0f || system.isActive() || system.getAmmo() <= 0) return;

        if (checkInterval.intervalElapsed()) {
            float repairableDuration = ReshaperStats.timeRepaired;

            float damageTakenAccumulator = 0f;
            ReshaperStats.ReshaperTracker.HullArmorData prevData = listener.getHullArmorNSecondsAgo(repairableDuration);

            // Check if we took enough hull damage to warrant using the system immediately
            if (ship.getHitpoints() / prevData.hull <= 1 - hpLossFractionMajor) {
                ship.useSystem();
                return;
            }

            // Check if we should repair previously taken damage
            for (int i = 0; i < damageTakenThresholds.length; i++) {
                float t = repairableDuration * (1 - (i+1)/(float) damageTakenThresholds.length);
                ReshaperStats.ReshaperTracker.HullArmorData curData = listener.getHullArmorNSecondsAgo(t);
                damageTakenAccumulator += getTotalDamageTaken(prevData.armor, curData.armor, prevData.hull, curData.hull);
                if (damageTakenAccumulator >= damageTakenThresholds[i] / system.getAmmo()) {
                    // Took enough damage in the interval, so use the system
                    ship.useSystem();
                    return;
                }
                prevData = curData;
            }

            // Check if we need to use the system stop a very high damage projectile or missile
            Iterator<Object> projectiles =
                    Global.getCombatEngine().getAllObjectGrid().getCheckIterator(ship.getLocation(), 2f*(ship.getCollisionRadius() + checkProjectilesRadius), 2f*(ship.getCollisionRadius()+checkProjectilesRadius));

            while (projectiles.hasNext()) {
                Object o = projectiles.next();
                if (!(o instanceof DamagingProjectileAPI)) continue;

                DamagingProjectileAPI proj = (DamagingProjectileAPI) o;
                boolean checkDamage = false;
                if (o instanceof MissileAPI) {
                    MissileAPI missile = (MissileAPI) o;
                    // Trust that guided missiles will reach their target
                    if (missile.isGuided() && missile.getOwner() != ship.getOwner()) {
                        checkDamage = true;
                    }
                    // For unguided missiles, check ray collision with collision circle
                    if (!missile.isGuided() && Misc.intersectSegmentAndCircle(missile.getLocation(), getPredictedLocationIn(missile, 2f), ship.getLocation(), ship.getCollisionRadius() + 50f) != null) {
                        checkDamage = true;
                    }
                }
                else {
                    checkDamage = Misc.intersectSegmentAndCircle(proj.getLocation(), getPredictedLocationIn(proj, 1f), ship.getLocation(), ship.getCollisionRadius() + 50f) != null;
                }

                if (checkDamage && proj.getDamageAmount() >= ship.getHitpoints() * 0.8f) {
                    // The projectile would destroy the ship or come near enough
                    // We should check finally that our shields aren't going to stop the projectile
                    float remainingShieldHp = ship.getMutableStats().getShieldDamageTakenMult().getModifiedValue() * (ship.getMaxFlux() - ship.getCurrFlux());
                    float projShieldDamage = proj.getDamageType().getShieldMult() * proj.getDamageAmount();
                    if (ship.getShield() == null || !ship.getShield().isWithinArc(proj.getLocation()) || projShieldDamage >= remainingShieldHp * 0.9f) {
                        ship.useSystem();
                        return;
                    }
                }
            }

            // If at least 2 charges remain, see if flux is high, and can use a charge just to vent some flux
            if (system.getAmmo() >= 2 && ship.getHardFluxLevel() >= 0.75f && ship.getCurrFlux() >= 0.9f * ship.getMaxFlux()) {
                ship.useSystem();
            }
        }
    }

    private Vector2f getPredictedLocationIn(DamagingProjectileAPI proj, float time) {
        return new Vector2f(proj.getLocation().x + proj.getVelocity().x * time, proj.getLocation().y + proj.getVelocity().y + time);
    }

    private float getTotalDamageTaken(float[][] prevArmor, float[][] curArmor, float prevHp, float curHp) {
        float damage = 0f;
        damage += Math.max(0f, prevHp - curHp);
        for (int i = 0; i < prevArmor.length; i++) {
            for (int j = 0; j < prevArmor[0].length; j++) {
                damage += Math.max(0f, prevArmor[i][j] - curArmor[i][j]);
            }
        }
        return damage;
    }
}
