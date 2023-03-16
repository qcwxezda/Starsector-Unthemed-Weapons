package weaponexpansion.combat.scripts;

import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.impl.campaign.ids.HullMods;
import com.fs.starfarer.api.util.Misc;

@SuppressWarnings("unused")
public class VoidRayEffect implements BeamEffectPlugin {

//    /** Chance per 1/10 second (beam hit interval) of an EMP arc */
//    private static final float EMP_CHANCE = 0.2f;

    @Override
    public void advance(float time, CombatEngineAPI engine, BeamAPI beam) {

        // Ignore frames where the beam didn't do any damage
        if (beam.getDamage().getDpsDuration() <= 0) {
            return;
        }

        // Not a ship ==> no shields
        CombatEntityAPI target = beam.getDamageTarget();
        if (!(target instanceof ShipAPI)) {
            return;
        }

        ShipAPI source = beam.getSource();
        if (source == null || source.getVariant().hasHullMod(HullMods.HIGH_SCATTER_AMP)) {
            return;
        }

        beam.getDamage().setForceHardFlux(Misc.random.nextFloat() < beam.getSource().getHardFluxLevel());


//        float dist = Misc.getDistance(beam.getFrom(), beam.getTo());
//        float distFrac = Math.min(1f, Math.max(0f, dist / beam.getWeapon().getRange()));
//
//        float pierceChance = 1f;
//
//        // Shield hit
//        if (target.getShield() != null && target.getShield().isWithinArc(beam.getTo())) {
//            if (source == null || !source.getVariant().hasHullMod("high_scatter_amp")) {
//                beam.getDamage().setForceHardFlux(distFrac < Math.random());
//            }
//            pierceChance *= targetShip.getHardFluxLevel() - 0.1f;
//            pierceChance *= targetShip.getMutableStats().getDynamic().getValue(Stats.SHIELD_PIERCED_MULT);
//        }
//
//        // Every hit has chance to spawn EMP arcs
//        if (EMP_CHANCE >= Math.random()
//                && beam.getWeapon().getChargeLevel() >= 0.9f
//                && distFrac > Math.random()
//                && pierceChance > Math.random()) {
//            float arcEmp = beam.getDamage().getDamage() * 0.2f;
//            float arcDamage = beam.getDamage().getDamage() * 0.1f;
//            engine.spawnEmpArcPierceShields(
//                    beam.getSource(),
//                    beam.getRayEndPrevFrame(),
//                    target,
//                    target,
//                    DamageType.ENERGY,
//                    arcDamage,
//                    arcEmp,
//                    100000f,
//                    "tachyon_lance_emp_impact",
//                    beam.getWidth() * 0.5f,
//                    beam.getFringeColor(),
//                    beam.getCoreColor()
//            );
//        }
    }
}
