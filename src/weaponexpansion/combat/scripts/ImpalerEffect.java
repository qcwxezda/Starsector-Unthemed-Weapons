package weaponexpansion.combat.scripts;

import com.fs.starfarer.api.combat.*;
import weaponexpansion.combat.plugins.AngleApproachMissileAI;
import weaponexpansion.util.Utils;

@SuppressWarnings("unused")
public class ImpalerEffect implements EveryFrameWeaponEffectPlugin, OnFireEffectPlugin {

    private float offset = 60f;
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
        ai.setApproachOffset( offset + Utils.randBetween(-10f, 10f));
        offset -= 30f;
    }

    @Override
    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
        // Reset offset after each set of shots
        offset = 60f;
    }
}
