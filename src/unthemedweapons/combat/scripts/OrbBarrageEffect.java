package unthemedweapons.combat.scripts;

import com.fs.starfarer.api.combat.*;
import unthemedweapons.combat.ai.AngleApproachMissileAI;

// TODO: Why does the AI autofire reset this weapon's cooldown if it vents in the middle of firing, but not the player?
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
        ai.setApproachOffset(offset);
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
