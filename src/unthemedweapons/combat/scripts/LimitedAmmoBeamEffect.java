package unthemedweapons.combat.scripts;

import com.fs.starfarer.api.combat.BeamAPI;
import com.fs.starfarer.api.combat.BeamEffectPlugin;
import com.fs.starfarer.api.combat.CombatEngineAPI;

@SuppressWarnings("unused")
public class LimitedAmmoBeamEffect implements BeamEffectPlugin {
    @Override
    public void advance(float amount, CombatEngineAPI engine, BeamAPI beam) {
        if (beam.getDamage().getDpsDuration() <= 0f) {
            return;
        }

        if (!beam.getWeapon().usesAmmo()) {
            return;
        }

        beam.getWeapon().getAmmoTracker().deductOneAmmo();
        if (beam.getWeapon().getAmmo() <= 0) {
            beam.getWeapon().disable(true);
        }
    }
}
