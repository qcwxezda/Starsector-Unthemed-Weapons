package weaponexpansion.combat.ai;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.Misc;
import org.lwjgl.util.vector.Vector2f;
import weaponexpansion.combat.scripts.EnergyBallLauncherEffect;
import weaponexpansion.util.CollisionUtils;
import weaponexpansion.util.EngineUtils;

public class EnergyBallLauncherAI implements AutofireAIPlugin {

    private final WeaponAPI weapon;
    private final ShipAPI firingShip;
    private final IntervalUtil findTargetInterval = new IntervalUtil(0.4f, 0.5f);

    private boolean shouldFire = false;
    private ShipAPI targetShip;
    private Vector2f targetLocation;

    // Array of maximum charge levels the AI will attempt to hold before firing, in order of target ship size
    // from fighter to capital:
    // DEFAULT, FIGHTER, FRIGATE, DESTROYER, CRUISER, CAPITAL
    private final float[] maxChargeLevels = new float[] {0f, 0f, 0f, 0f, 0f, 0f};

    public EnergyBallLauncherAI(WeaponAPI weapon) {
        this.weapon = weapon;
        firingShip = weapon.getShip();
    }

    private boolean isTargetValid(CombatEntityAPI target, Vector2f aimLocation) {
        CombatEngineAPI engine = Global.getCombatEngine();

        if (target == null || !engine.isEntityInPlay(target)) {
            return false;
        }
        if (!(target instanceof ShipAPI)) return false;
        ShipAPI ship = (ShipAPI) target;

        if (ship.isHulk() || ship.getOwner() == firingShip.getOwner() || ship.getOwner() == 100 || ship.isPhased()) {
            return false;
        }

        // Make sure the target is in range
        if (!EngineUtils.isInRange(weapon, target, aimLocation)) {
            return false;
        }

        return !CollisionUtils.rayCollisionCheckAlly(
                weapon.getFirePoint(0),
                aimLocation,
                firingShip,
                50f
        );
    }

    private Vector2f getAimPoint(CombatEntityAPI target) {
        if (target == null) return null;
        return Global.getCombatEngine().getAimPointWithLeadForAutofire(
                firingShip,
                1f,
                target,
                EnergyBallLauncherEffect.getMoveSpeed(EnergyBallLauncherEffect.mapChargeLevelToDamageMult(weapon.getChargeLevel())));
    }

    private ShipAPI pickBestTarget() {
        // If firing ship's target is valid for this weapon, pick that (focus fire)
        ShipAPI shipTarget = firingShip.getShipTarget();
        if (isTargetValid(shipTarget, getAimPoint(shipTarget))) {
            return shipTarget;
        }

        // Otherwise, pick the closest suitable target in range (in terms of angle error from weapon's current arc)
        float minAngleError = Float.MAX_VALUE;
        ShipAPI bestTarget = null;
        for (ShipAPI ship : Global.getCombatEngine().getShips()) {
            Vector2f aimPoint = getAimPoint(ship);
            float angleError = Misc.getAngleDiff(weapon.getCurrAngle(), Misc.getAngleInDegrees(weapon.getFirePoint(0), aimPoint));
            if (angleError < minAngleError && isTargetValid(ship, aimPoint)) {
                minAngleError = angleError;
                bestTarget = ship;
            }
        }

        return bestTarget;
    }

    private void getMaxChargeLevel(ShipAPI target) {
        //target.getArmorGrid().get
    }

    private void tryToNotFire() {
        float chargeLevel = weapon.getChargeLevel();

        // Not charging, so safe to set shouldFire to false
        if (chargeLevel <= 0f) {
           shouldFire = false;
           return;
        }

        // Charging; if it's safe to stop charging (i.e. no friendly fire), then do so. Otherwise,
        // keep shouldFire to true
        Vector2f projectileReachPoint = Misc.getUnitVectorAtDegreeAngle(weapon.getCurrAngle());
        projectileReachPoint.scale(weapon.getRange());
        Vector2f.add(projectileReachPoint, weapon.getFirePoint(0), projectileReachPoint);
        if (!CollisionUtils.rayCollisionCheckAlly(
                weapon.getFirePoint(0),
                projectileReachPoint,
                firingShip,
                50f)) {
            shouldFire = false;
            return;
        }

        shouldFire = true;
    }

    @Override
    public void advance(float amount) {

        findTargetInterval.advance(amount);
        // Check if enough flux to fire
        FluxTrackerAPI fluxTracker = firingShip.getFluxTracker();
        if (fluxTracker.getCurrFlux() + 2f * weapon.getFluxCostToFire() >= fluxTracker.getMaxFlux()) {
            tryToNotFire();
            return;
        }

        Vector2f aimPoint = targetLocation = getAimPoint(targetShip);
        boolean changedTarget = false;
        if (!isTargetValid(targetShip, aimPoint)) {
            if (findTargetInterval.intervalElapsed()) {
                targetShip = pickBestTarget();
                changedTarget = true;
            }
            else {
                targetShip = null;
                tryToNotFire();
                return;
            }
        }

        // Still no target
        if (targetShip == null) {
            tryToNotFire();
            return;
        }

        if (changedTarget) {
            aimPoint = targetLocation = getAimPoint(targetShip);
        }

        float angleError = Misc.getAngleDiff(weapon.getCurrAngle(), Misc.getAngleInDegrees(weapon.getFirePoint(0), aimPoint));

        // If shot would hit shield, pepper with low power shots
        // Reminder: letting go of fire button causes weapon to fire, so setting shouldFire to false when weapon.getChargeLevel() > 0
        // will fire a shot the next frame
        if (targetShip.getShield() != null
                && targetShip.getShield().isWithinArc(weapon.getFirePoint(0))
                && angleError <= 3f
                && weapon.getChargeLevel() > 0f) {
            shouldFire = false;
            return;
        }

        // Fire shots of varied maximum power depending on the target ship's hull size
        int hullSizeOrd = targetShip.getHullSize().ordinal();
        if (weapon.getChargeLevel() > maxChargeLevels[hullSizeOrd] && angleError <= 3f) {
            shouldFire = false;
            return;
        }

        // Safe to begin charging
        shouldFire = true;
    }

    @Override
    public boolean shouldFire() {
        return shouldFire;
    }

    @Override
    public void forceOff() {
        shouldFire = false;
    }

    @Override
    public Vector2f getTarget() {
        return targetLocation;
    }

    @Override
    public ShipAPI getTargetShip() {
        return targetShip;
    }

    @Override
    public WeaponAPI getWeapon() {
        return weapon;
    }

    @Override
    public MissileAPI getTargetMissile() {
        return null;
    }
}
