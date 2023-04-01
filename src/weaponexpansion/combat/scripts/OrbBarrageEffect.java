package weaponexpansion.combat.scripts;

import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.util.Misc;
import weaponexpansion.combat.plugins.AngleApproachMissileAI;

@SuppressWarnings("unused")
public class OrbBarrageEffect implements EveryFrameWeaponEffectPlugin, OnFireEffectPlugin {

    private float offset = 180f;
    @Override
    public void onFire(DamagingProjectileAPI proj, WeaponAPI weapon, CombatEngineAPI engine) {
        if (!(proj instanceof MissileAPI)) {
            return;
        }

        MissileAPI missile = (MissileAPI) proj;
        if (!(missile.getUnwrappedMissileAI() instanceof AngleApproachMissileAI)) {
            return;
        }

        AngleApproachMissileAI ai = (AngleApproachMissileAI)  missile.getUnwrappedMissileAI();
        ai.setApproachDir(Misc.getUnitVectorAtDegreeAngle(missile.getFacing() + offset));
        offset -= Math.signum(offset) * (180f / (weapon.getSpec().getBurstSize() - 1));
        offset *= -1;
    }

    @Override
    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
        // Reset offset after each set of shots
        if (weapon.getChargeLevel() < 0.9f) {
            offset = 180f;
        }
    }
}
