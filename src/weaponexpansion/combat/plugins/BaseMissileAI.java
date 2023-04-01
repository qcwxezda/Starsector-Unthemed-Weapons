package weaponexpansion.combat.plugins;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.Misc;
import org.lwjgl.util.vector.Vector2f;
import weaponexpansion.util.Utils;

public abstract class BaseMissileAI implements MissileAIPlugin, GuidedMissileAI {

    protected CombatEntityAPI target;
    protected MissileAPI missile;
    protected final float maxSeekRange;
    private final boolean canTargetFighters;
    private final IntervalUtil findTargetInterval = new IntervalUtil(0.2f, 0.3f);

    public BaseMissileAI(MissileAPI missile, float maxSeekRange) {
        this.missile = missile;
        this.maxSeekRange = maxSeekRange;
        canTargetFighters =
                missile.getWeapon() != null && missile.getWeapon().hasAIHint(WeaponAPI.AIHints.ANTI_FTR);
    }

    protected void smoothTurn(float targetAngle, boolean clockwise) {
        float facingAngle = missile.getFacing();
        float turnSpeed = missile.getAngularVelocity();
        float maxAngularAcc = missile.getTurnAcceleration();

        float facingError = ((targetAngle - facingAngle) * (clockwise ? -1f : 1f) + 720f) % 360f;

        if (turnSpeed < 0f && !clockwise) {
            missile.giveCommand(ShipCommand.TURN_LEFT);
        }
        else if (turnSpeed > 0f && clockwise) {
            missile.giveCommand(ShipCommand.TURN_RIGHT);
        }
        else {
            // If stopping turning now would reach or overshoot the target, stop turning
            // by accelerating in the opposite direction
            float stoppingDist = turnSpeed*turnSpeed/(2f*maxAngularAcc);
            if (stoppingDist >= facingError) {
                missile.giveCommand(clockwise ? ShipCommand.TURN_LEFT : ShipCommand.TURN_RIGHT);
            }
            else {
                missile.giveCommand(clockwise ? ShipCommand.TURN_RIGHT : ShipCommand.TURN_LEFT);
            }
        }
    }

    private ShipAPI findNewTarget() {
        if (missile.getSource() != null) {
            ShipAPI sourceTarget = missile.getSource().getShipTarget();
            if (sourceTarget != null && Misc.getDistance(missile.getLocation(), sourceTarget.getLocation()) <= maxSeekRange + sourceTarget.getCollisionRadius()) {
                return sourceTarget;
            }
        }
        return Utils.getClosestEnemyShip(
                missile.getLocation(),
                missile.getOwner(),
                canTargetFighters ? ShipAPI.HullSize.FIGHTER : ShipAPI.HullSize.FRIGATE,
                maxSeekRange,
                true,
                null
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

    public Vector2f getInterceptionLoS() {
        Vector2f interceptor = getInterceptionPoint();
        Vector2f.sub(interceptor, missile.getLocation(), interceptor);
        return interceptor;
    }

    public Vector2f getInterceptionPoint() {
        return Global.getCombatEngine().getAimPointWithLeadForAutofire(missile, 1f, target, missile.getMaxSpeed());
    }

    @Override
    public CombatEntityAPI getTarget() {
        return target;
    }

    @Override
    public void setTarget(CombatEntityAPI target) {
        this.target = target;
    }

    /** Returns whether the missile should continue seeking a target. */
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean preAdvance(float amount) {
        if (missile.isFizzling() || missile.isExpired()) {
            return false;
        }

        findTargetInterval.advance(amount);
        if (isTargetInvalid(target) && findTargetInterval.intervalElapsed()) {
            target = findNewTarget();
        }

        if (isTargetInvalid(target)) {
            return false;
        }

        if (target instanceof ShipAPI) {
            ShipAPI ship = (ShipAPI) target;
            return !ship.isPhased() && !ship.getCollisionClass().equals(CollisionClass.NONE);
        }

        return true;
    }
}
