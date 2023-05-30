package unthemedweapons.combat.scripts;

import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.impl.combat.GravitonBeamEffect;
import unthemedweapons.combat.plugins.Action;
import unthemedweapons.combat.plugins.ActionPlugin;

import java.util.HashSet;
import java.util.Set;

@SuppressWarnings("unused")
public class ImpactLanceEffect implements EveryFrameWeaponEffectPluginWithAdvanceAfter {
    static final String modificationSource = "wpnxt_gravitonarray";
    static final float debuff = 0.15f, debuffDuration = 3f;
    final Set<ShipAPI> affectedShips = new HashSet<>();

    @Override
    public void advanceAfter(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
        // Process targets; do it in advanceAfter so the normal graviton beam effects can happen first
        for (ShipAPI ship : affectedShips) {
            MutableStat shieldMult = ship.getMutableStats().getShieldDamageTakenMult();
            MutableStat.StatMod gravitonBeamMod = shieldMult.getMultStatMod(GravitonBeamEffect.DAMAGE_MOD_ID);
            float gravitonBeamMult = gravitonBeamMod == null
                    ? 1f
                    : gravitonBeamMod.getValue();
            float remainder = (1 + debuff) / gravitonBeamMult;
            shieldMult.modifyMult(modificationSource, remainder);
        }
    }

    @Override
    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
        // Add current targets to the set of affected ships
        for (BeamAPI beam : weapon.getBeams()) {
            if (beam.getDamage().getDpsDuration() <= 0f || !(beam.getDamageTarget() instanceof ShipAPI)) {
                continue;
            }

            final ShipAPI target = (ShipAPI) beam.getDamageTarget();
            if (!affectedShips.contains(target)) {
                affectedShips.add(target);
                ActionPlugin.queueAction(new Action() {
                    @Override
                    public void perform() {
                        affectedShips.remove(target);
                    }
                }, debuffDuration);
            }
        }
    }
}
