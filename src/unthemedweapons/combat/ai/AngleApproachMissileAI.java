package unthemedweapons.combat.ai;

import com.fs.starfarer.api.combat.MissileAPI;
import com.fs.starfarer.api.combat.ShipCommand;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import org.lwjgl.util.vector.Vector2f;
import unthemedweapons.util.MathUtils;

public class AngleApproachMissileAI extends BaseGuidedMissileAI {

    private float approachDir = 0f, approachOffset = 0f;
    private final float circleDist;

    /** circleDistFactor is a fraction of the missile's max speed */
    public AngleApproachMissileAI(MissileAPI missile, float maxSeekRangeFactor, float circleDistFactor) {
        super(missile, maxSeekRangeFactor);
        circleDist = circleDistFactor * missile.getMaxSpeed();
    }

    @Override
    public void setTarget(CombatEntityAPI target) {
        super.setTarget(target);
        // reset the approach direction
        if (target != null) {
            approachDir = Misc.getAngleInDegrees(Misc.getDiff(target.getLocation(), missile.getLocation()));
        }
    }

    @Override
    public void advance(float amount) {

        if (!preAdvance(amount)) {
            missile.giveCommand(ShipCommand.ACCELERATE);
            return;
        }

        Vector2f interceptionPoint = getInterceptionPoint(1f);
        Vector2f los = new Vector2f();
        Vector2f.sub(interceptionPoint, missile.getLocation(), los);
        MathUtils.safeNormalize(los);

        Vector2f tangentPoint = Misc.getPerp(los);
        Vector2f approachVector = Misc.getUnitVectorAtDegreeAngle(approachDir + approachOffset);
        float tangentStrength = 0.5f * (circleDist + target.getCollisionRadius()) * (1f - Vector2f.dot(los, approachVector)) * (1f + missile.getElapsed() / missile.getMaxFlightTime());
        //tangentStrength = Math.min(tangentStrength, Misc.getDistance(missile.getLocation(), interceptionPoint) + target.getCollisionRadius());
        tangentPoint.scale(tangentStrength * (MathUtils.isClockwise(los, approachVector) ? -1f : 1f));

        Vector2f targetPoint = new Vector2f();
        Vector2f.add(interceptionPoint, tangentPoint ,targetPoint);
        Vector2f newLos = new Vector2f();
        Vector2f.sub(targetPoint, missile.getLocation(), newLos);

        float desiredAngle = Misc.getAngleInDegrees(newLos);
        float velAngle = Misc.getAngleInDegrees(missile.getVelocity());
        float velError = MathUtils.angleDiff(desiredAngle, velAngle);

        if (Math.abs(velError) < 90f && Math.abs(velError) > 8f) {
            desiredAngle += Math.signum(velError) * Math.min(Math.abs(velError), 10f);
        }

        missile.giveCommand(ShipCommand.ACCELERATE);
        smoothTurn(desiredAngle, MathUtils.angleDiff(missile.getFacing(), desiredAngle) >= 0f);
    }

    public void setApproachOffset(float offset) {
        approachOffset = offset;
    }
}
