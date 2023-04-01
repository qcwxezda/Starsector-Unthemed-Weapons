package weaponexpansion.combat.scripts;

import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.util.Misc;
import weaponexpansion.combat.plugins.AngleApproachMissileAI;

@SuppressWarnings("unused")
public class ImpalerEffect implements EveryFrameWeaponEffectPlugin, OnFireEffectPlugin {

    private float offset = 90f;
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
        offset -= 45f;
    }

    @Override
    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
        // Reset offset after each set of shots
        offset = 90f;
    }
}
