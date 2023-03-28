package weaponexpansion.combat.plugins;

import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.util.Misc;
import org.lwjgl.util.vector.Vector2f;
import weaponexpansion.util.Utils;

public class TestMissileAI implements MissileAIPlugin, GuidedMissileAI {

    private CombatEntityAPI target;
    private final MissileAPI missile;
    public TestMissileAI(MissileAPI missile) {
        this.missile = missile;
    }

    @Override
    public CombatEntityAPI getTarget() {
        return target;
    }

    @Override
    public void setTarget(CombatEntityAPI target) {
        this.target = target;
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

        Vector2f targetVel = new Vector2f();
        Vector2f.sub(target.getVelocity(), missile.getVelocity(), targetVel);

        missile.giveCommand(ShipCommand.ACCELERATE);
    }
}
