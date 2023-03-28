package weaponexpansion.combat.plugins;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.util.Misc;
import org.lwjgl.util.vector.Vector2f;
import weaponexpansion.util.Utils;

public class SwervingMissileAI implements MissileAIPlugin, GuidedMissileAI {

    private CombatEntityAPI target;
    private final MissileAPI missile;

    private final float randomOffset = Utils.randBetween(0f, 2f * (float) Math.PI);
    private static final float randomAmplitude = 60f;
    private Vector2f targetVelLastFrame = new Vector2f();

    public SwervingMissileAI(MissileAPI missile) {
        this.missile = missile;
    }

    @Override
    public void advance(float amount) {

        if (missile.isFizzling() || missile.getSource() == null) {
            return;
        }

        if (target == null) {
            // If owning ship has something selected, target that thing
            target = missile.getSource().getShipTarget();

            // Find the closest target...
            if (target == null) {
                target = Misc.findClosestShipEnemyOf(
                        missile.getSource(),
                        missile.getLocation(),
                        ShipAPI.HullSize.FIGHTER, missile.getMaxRange(),
                        true);
            }
        }

        if (!(target instanceof ShipAPI)) {
            return;
        }

        Vector2f targetPos = Utils.estimateInterceptPoint(missile, (ShipAPI) target, amount, targetVelLastFrame);//target.getLocation();//Global.getCombatEngine().getAimPointWithLeadForAutofire(missile, 1f, target, missile.getMaxSpeed());

        Vector2f targetVel = Misc.getUnitVector(missile.getLocation(), targetPos);
        targetVel.scale(missile.getMaxSpeed());

        Vector2f adjustedVel = new Vector2f();
        Vector2f.add(targetVel, target.getVelocity(), adjustedVel);

        float targetAngle = Misc.getAngleInDegrees(targetVel);
        float perturbFactor = Math.min(1f, Math.max(0f, Misc.getDistance(missile.getLocation(), target.getLocation()) - target.getCollisionRadius() - 100f) / missile.getMaxRange());

        // Perturb the target angle a bit
       // targetAngle += randomAmplitude * Math.sin(2f * missile.getElapsed() + randomOffset) * perturbFactor;

        float angleDiff = Utils.angleDiff(missile.getFacing(), targetAngle);


        if (angleDiff < 0f)  {
            missile.giveCommand(ShipCommand.TURN_RIGHT);
        }
        else if (angleDiff > 0f) {
            missile.giveCommand(ShipCommand.TURN_LEFT);
        }

        missile.giveCommand(ShipCommand.ACCELERATE);

        targetVelLastFrame = target.getVelocity();
    }

    @Override
    public CombatEntityAPI getTarget() {
        return target;
    }

    @Override
    public void setTarget(CombatEntityAPI target) {
        this.target = target;
    }
}
