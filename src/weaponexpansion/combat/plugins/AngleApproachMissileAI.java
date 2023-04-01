package weaponexpansion.combat.plugins;

import com.fs.starfarer.api.combat.MissileAPI;
import com.fs.starfarer.api.combat.ShipCommand;
import com.fs.starfarer.api.util.Misc;
import org.lwjgl.util.vector.Vector2f;
import weaponexpansion.util.Utils;

public class AngleApproachMissileAI extends BaseMissileAI {

    private Vector2f approachDir;
    private final float circleDist;

    public AngleApproachMissileAI(MissileAPI missile, float maxSeekRange, float circleDist) {
        super(missile, maxSeekRange);
        approachDir = Misc.getUnitVectorAtDegreeAngle(missile.getFacing());
        this.circleDist = circleDist;
    }

    @Override
    public void advance(float amount) {

        if (!preAdvance(amount)) {
            missile.giveCommand(ShipCommand.ACCELERATE);
            return;
        }

        Vector2f interceptionPoint = getInterceptionPoint();
        Vector2f los = new Vector2f();
        Vector2f.sub(interceptionPoint, missile.getLocation(), los);
        Utils.safeNormalize(los);

        Vector2f tangentPoint = Misc.getPerp(los);
        float tangentStrength = 0.5f * (circleDist + target.getCollisionRadius()) * (1f - Vector2f.dot(los, approachDir)) * (1f + missile.getElapsed() / missile.getMaxFlightTime());
        //tangentStrength = Math.min(tangentStrength, Misc.getDistance(missile.getLocation(), interceptionPoint) + target.getCollisionRadius());
        tangentPoint.scale(tangentStrength * (Utils.isClockwise(los, approachDir) ? -1f : 1f));

        Vector2f targetPoint = new Vector2f();
        Vector2f.add(interceptionPoint, tangentPoint ,targetPoint);
        Vector2f newLos = new Vector2f();
        Vector2f.sub(targetPoint, missile.getLocation(), newLos);

        float desiredAngle = Misc.getAngleInDegrees(newLos);

        missile.giveCommand(ShipCommand.ACCELERATE);

        smoothTurn(desiredAngle, Utils.angleDiff(missile.getFacing(), desiredAngle) >= 0f);
    }

    public void setApproachDir(Vector2f dir) {
        approachDir = new Vector2f(dir);
        Utils.safeNormalize(approachDir);
    }
}
