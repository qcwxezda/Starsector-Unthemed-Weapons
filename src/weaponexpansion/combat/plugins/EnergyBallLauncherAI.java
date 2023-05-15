package weaponexpansion.combat.plugins;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.util.Misc;
import org.lwjgl.util.vector.Vector2f;
import weaponexpansion.combat.scripts.EnergyBallLauncherEffect;
import weaponexpansion.util.Utils;

import java.util.Collections;

public class EnergyBallLauncherAI implements AutofireAIPlugin {

    private final WeaponAPI weapon;
    private final ShipAPI firingShip;

    private boolean shouldFire = false;
    private ShipAPI targetShip;
    private Vector2f targetLocation;

    // Array of maximum charge levels the AI will attempt to hold before firing, in order of target ship size
    // from fighter to capital:
    // DEFAULT, FIGHTER, FRIGATE, DESTROYER, CRUISER, CAPITAL
    private final float[] maxChargeLevels = new float[] {0f, 0f, 0f, 0.1f, 0.2f, 0.3f};


    private final Utils.TargetChecker targetChecker = new Utils.TargetChecker() {
        @Override
        public boolean check(CombatEntityAPI entity) {
            if (entity == null || !Global.getCombatEngine().isEntityInPlay(entity)) {
                return false;
            }
            if (!(entity instanceof ShipAPI)) return false;
            ShipAPI ship = (ShipAPI) entity;

            return !ship.isHulk()
                    && ship.getOwner() != firingShip.getOwner()
                    && ship.getOwner() != 100
                    && !ship.isPhased();
        }
    };

    public EnergyBallLauncherAI(WeaponAPI weapon) {
        this.weapon = weapon;
        firingShip = weapon.getShip();
    }

    private ShipAPI pickBestTarget() {
        // If firing ship's target is valid for this weapon and inside the weapon arc, pick that (focus fire)
        ShipAPI shipTarget = firingShip.getShipTarget();
        if (targetChecker.check(shipTarget) && Utils.isInRange(weapon, shipTarget, shipTarget.getLocation())) {
            return shipTarget;
        }

        // Otherwise, pick the closest suitable target in range
        float minDist = Float.MAX_VALUE;
        ShipAPI bestTarget = null;
        for (ShipAPI ship : Global.getCombatEngine().getShips()) {
            float dist = Misc.getDistance(weapon.getLocation(), ship.getLocation());
            if (targetChecker.check(ship) && Utils.isInRange(weapon, ship, ship.getLocation()) && dist < minDist) {
                minDist = dist;
                bestTarget = ship;
            }
        }

        return bestTarget;
    }

    @Override
    public void advance(float v) {
        if (targetShip == null) {
            targetShip = pickBestTarget();
        }

        // Still no target
        if (targetShip == null) {
            shouldFire = false;
            return;
        }

        Vector2f targetLeadingLocation =
                Global.getCombatEngine().getAimPointWithLeadForAutofire(
                        firingShip,
                        1f,
                        targetShip,
                        EnergyBallLauncherEffect.getMoveSpeed(EnergyBallLauncherEffect.mapChargeLevelToDamageMult(weapon.getChargeLevel())));

        // Check if target is no longer valid or inside weapon range
        if (!targetChecker.check(targetShip) || !Utils.isInRange(weapon, targetShip, targetLeadingLocation)) {
            shouldFire = false;
            targetShip = null;
            targetLocation = null;
            return;
        }

        // Check if enough flux to fire
        FluxTrackerAPI fluxTracker = firingShip.getFluxTracker();
        if (fluxTracker.getCurrFlux() + 2f * weapon.getFluxCostToFire() >= fluxTracker.getMaxFlux()) {
            shouldFire = false;
            return;
        }

        // Check that the weapon is pointed at the right place
        float angleError = Math.abs(Utils.angleDiff(weapon.getCurrAngle(), Misc.getAngleInDegrees(weapon.getFirePoint(0), targetLeadingLocation)));
        if (angleError > 3f) {
            shouldFire = false;
            return;
        }

        // Check that we aren't shooting an ally in the back
        if (Utils.rayCollisionCheckAlly(weapon.getFirePoint(0), targetLeadingLocation, firingShip, 50f)) {
            shouldFire = false;
            return;
        }

        // If shot would hit shield, pepper with low power shots
        // Reminder: letting go of fire button causes weapon to fire, so setting shouldFire to false when weapon.getChargeLevel() > 0
        // will fire a shot the next frame
        if (targetShip.getShield() != null && targetShip.getShield().isWithinArc(weapon.getFirePoint(0)) && weapon.getChargeLevel() > 0f) {
            shouldFire = false;
            return;
        }

        // Fire shots of varied maximum power depending on the target ship's hull size
        int hullSizeOrd = targetShip.getHullSize().ordinal();
        if (weapon.getChargeLevel() > maxChargeLevels[hullSizeOrd]) {
            shouldFire = false;
            return;
        }

        // Safe to fire
        shouldFire = true;
        targetLocation = targetLeadingLocation;
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
