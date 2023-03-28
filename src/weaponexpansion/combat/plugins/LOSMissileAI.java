package weaponexpansion.combat.plugins;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.Misc;
import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector3f;
import weaponexpansion.util.Utils;

public class LOSMissileAI implements MissileAIPlugin, GuidedMissileAI {

    private CombatEntityAPI target;
    private final MissileAPI missile;
    private final float maxSeekRange;
    private final boolean canTargetFighters;
    private final IntervalUtil findTargetInterval = new IntervalUtil(0.2f, 0.3f);

    public LOSMissileAI(MissileAPI missile, float maxSeekRange) {
        this.missile = missile;
        this.maxSeekRange = maxSeekRange;
        canTargetFighters =
                missile.getWeapon() != null && missile.getWeapon().hasAIHint(WeaponAPI.AIHints.ANTI_FTR);
    }

    @Override
    public CombatEntityAPI getTarget() {
        return target;
    }

    @Override
    public void setTarget(CombatEntityAPI target) {
        this.target = target;
    }

    private ShipAPI findNewTarget() {
        if (missile.getSource() != null) {
            ShipAPI sourceTarget = missile.getSource().getShipTarget();
            if (sourceTarget != null && Misc.getDistance(missile.getLocation(), sourceTarget.getLocation()) <= maxSeekRange + sourceTarget.getCollisionRadius()) {
                return sourceTarget;
            }
        }

        return Misc.findClosestShipEnemyOf(
                missile.getSource(),
                missile.getLocation(),
                canTargetFighters ? ShipAPI.HullSize.FIGHTER : ShipAPI.HullSize.FRIGATE,
                maxSeekRange,
                true
        );
    }

    private boolean isTargetInvalid(CombatEntityAPI entity) {
        if (entity == null) return true;
        if (!Global.getCombatEngine().isEntityInPlay(entity)) return true;
        // This AI doesn't seek non-ship targets, so a non-ship target would have to have been
        // forced via setTarget; assume it's valid.
        if (!(entity instanceof ShipAPI)) return false;

        ShipAPI ship = (ShipAPI) entity;
        return ship.isHulk() || ship.equals(missile.getSource()) || !ship.isAlive();
    }

    @Override
    public void advance(float amount) {
        if (missile.isFizzling()) {
            return;
        }

        // This missile is always accelerating, cannot decelerate
        // (decelerating missiles look weird)
        missile.giveCommand(ShipCommand.ACCELERATE);

        findTargetInterval.advance(amount);
        if (isTargetInvalid(target) && findTargetInterval.intervalElapsed()) {
            target = findNewTarget();
        }

        if (isTargetInvalid(target)) {
            return;
        }

        if (target instanceof ShipAPI && ((ShipAPI) target).isPhased()) {
            return;
        }

        // First, check if the target is in one of the missile's two "blind spots"
        // These are roughly two circles of radius maxSpeed / maxTurnRate (in radians) to the left and
        // right of the missile that, even if it turned as fast as it could for the entire time, the missile
        // wouldn't be able to reach the target.
        float radius = missile.getMoveSpeed() / (missile.getMaxTurnRate() * Misc.RAD_PER_DEG);
        if (missile.getVelocity().lengthSquared() > 0) {
            Vector2f perpR = Misc.getPerp(missile.getVelocity());
            perpR.normalise();
            perpR.scale(radius);
            Vector2f perpL = new Vector2f();
            perpR.negate(perpL);
            Vector2f centerL = new Vector2f(), centerR = new Vector2f();
            Vector2f.add(missile.getLocation(), perpR, centerR);
            Vector2f.add(missile.getLocation(), perpL, centerL);
            // Inside right blind spot
            if (Misc.getDistance(target.getLocation(), centerR) + target.getCollisionRadius() < radius) {
                missile.giveCommand(ShipCommand.TURN_LEFT);
                missile.giveCommand(ShipCommand.ACCELERATE);
                return;
            }
            // Inside left blind spot
            if (Misc.getDistance(target.getLocation(), centerL) + target.getCollisionRadius() < radius) {
                missile.giveCommand(ShipCommand.TURN_RIGHT);
                missile.giveCommand(ShipCommand.ACCELERATE);
                return;
            }
        }

        // Proportional navigation
        float N = 3f;
        Vector3f Rt = new Vector3f(target.getLocation().x, target.getLocation().y, 0f);
        Vector3f Rm = new Vector3f(missile.getLocation().x, missile.getLocation().y, 0f);
        Vector3f Vt = new Vector3f(target.getVelocity().x, target.getVelocity().y, 0f);
        Vector3f Vm = new Vector3f(missile.getVelocity().x, missile.getVelocity().y, 0f);
        Vector3f Rd = new Vector3f(), Vd = new Vector3f();
        Vector3f.sub(Rt, Rm, Rd);
        Vector3f.sub(Vt, Vm, Vd);

        Vector3f res = new Vector3f();
        Vector3f.cross(Rd, Vd, res);
        res.scale(1f / Rd.lengthSquared());
        Vector3f.cross(Vm, res, res);
        res.scale(-N * Vd.length() / Vm.length());

        Vector2f a = new Vector2f(res.x, res.y);

        // a = v^2 / r = w^2 r ==> w = a / v
        float w = a.length() / missile.getMoveSpeed() * ((Utils.isClockwise(missile.getVelocity(), a) ? -1f : 1f));

        // If the shot was backwards, then turn as fast as possible
        // This handles the edge case of change in LOS being close to 0 when
        // the missile is moving almost directly away from the LOS.
        if (Vector3f.dot(Rd, Vm) < 0f) {
            w = (w < 0f ? -1f : 1f) * 1000000f;
        }

        if (missile.getAngularVelocity() < w * Misc.DEG_PER_RAD) {
            missile.giveCommand(ShipCommand.TURN_LEFT);
        } else {
            missile.giveCommand(ShipCommand.TURN_RIGHT);
        }

//        Vector2f vD = new Vector2f(); // difference in velocity
//        Vector2f.sub(target.getVelocity(), missile.getVelocity(), vD);
//
//        Vector2f pD = new Vector2f(); // difference in position
//        Vector2f.sub(target.getLocation(), missile.getLocation(), pD);
//
//        float sC = -Vector2f.dot(vD, pD) / pD.length(); // closing speed
//
//        Vector2f los = new Vector2f();
//        Vector2f.sub(target.getLocation(), missile.getLocation(), los);
////
////        Vector2f losD = new Vector2f();
////        Vector2f.sub(prevLos, los, losD);
////
////        float sC = los.length();
//
//        // If the closing speed is negative, just turn toward the target as quickly as possible
//        // atan2 calculates the signed angle between the two vectors
//        float angleDiff = (float) Math.atan2(los.y * prevLos.x - los.x * prevLos.y, los.x * prevLos.x + los.y * prevLos.y) * Misc.DEG_PER_RAD;
//        float losRate = angleDiff / amount;
//
////        Vector2f targetAcc = new Vector2f();
////        Vector2f.sub(target.getVelocity(), targetVelLastFrame, targetAcc);
////        targetAcc.scale(1f / amount);
////
////        Vector2f perpLos = Misc.getPerp(los);
//
//        // Proportional navigation
//        float a = 6f * losRate * sC;// + 1.5f * Vector2f.dot(targetAcc, perpLos) / perpLos.lengthSquared();
//
//        // a = v^2 / r = w^2 r ==> w = a / v
//        float w = a / (Math.max(sC, -0.01f));
//
//        if (missile.getAngularVelocity() < w) {
//            missile.giveCommand(ShipCommand.TURN_LEFT);
//        } else {
//            missile.giveCommand(ShipCommand.TURN_RIGHT);
//        }
//
//        missile.giveCommand(ShipCommand.ACCELERATE);
//        prevLos = los;
//        targetVelLastFrame = target.getVelocity();
    }
}
