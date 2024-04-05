package unthemedweapons.combat.scripts;

import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.impl.campaign.ids.HullMods;
import com.fs.starfarer.api.util.Misc;
import unthemedweapons.util.MathUtils;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("unused")
public class VoidRayWeaponEffect implements EveryFrameWeaponEffectPlugin, WeaponEffectPluginWithInit, BeamEffectPlugin, EveryFrameWeaponEffectPluginWithAdvanceAfter {

    static float maxSpread = 9f;

    /** Per second */
    static float beamAngleSpeed = 20f;
    static float maxWidth = 18f;
    /** Per second */
    static float fullChargeGrowRate = 1f;

    /** Amount of damage to transfer to full strength first beam per second. Approximate */
    static float transferPerSecond = 0.15f;

    /** Need both spreads and angleOffsets since angleOffsets is shared between every voidray weapon */
    float spread = 0f;
    boolean randomizedSpreads = false;
    float convergeLevel = 0f;
    /** Per second, as fraction of remaining convergence arc */
    float convergeLevelRate = 0.8f;

    List<Float> angleOffsets = new ArrayList<>();
    List<Float> randomFloats = new ArrayList<>();
    float time = 0f;

    @Override
    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
        time += amount;

        // Weapon has stopped firing and beams disappeared, so reset the angle offsets
        if (weapon.getChargeLevel() <= 0f) {
            spread = maxSpread;
            convergeLevel = 0f;
            if (!randomizedSpreads) {
                for (int i = 0; i < angleOffsets.size(); i++) {
                    randomFloats.set(i, MathUtils.randBetween(0f, 6.28f));
                }
                randomizedSpreads = true;
            }
            return;
        }

        if (weapon.getChargeLevel() >= 1f) {
            convergeLevel = Math.min(1f, convergeLevel + convergeLevelRate * (1.05f - convergeLevel) * amount);
        }

        // Weapon is at max charge, slowly transfer damage to the first beam (makes it slowly stronger against armor)
        if (convergeLevel >= 1f) {
            float totalDamageTransferred = 0f;
            for (int i = 1; i < weapon.getBeams().size(); i++) {
                BeamAPI beam = weapon.getBeams().get(i);
                float damage = beam.getDamage().getBaseDamage();
                totalDamageTransferred += damage * transferPerSecond * amount;
                beam.getDamage().setDamage(damage * (1 - transferPerSecond * amount));
            }
            BeamAPI firstBeam = weapon.getBeams().get(0);
            firstBeam.getDamage().setDamage(firstBeam.getDamage().getBaseDamage() + totalDamageTransferred);
            for (BeamAPI beam : weapon.getBeams()) {
                beam.setWidth(Math.min(maxWidth, beam.getWidth() + fullChargeGrowRate * amount));
            }
            return;
        }

        // If not fully converged, max width is half of usual maxwidth
        for (BeamAPI beam : weapon.getBeams()) {
            beam.setWidth(maxWidth * (0.15f + convergeLevel * 0.35f));
        }

        // In between min and max strength, lower the beam dispersion
        spread = maxSpread * (1f - convergeLevel);
        for (int i = 0; i < angleOffsets.size(); i++) {
            float newSpread = spread / 2f * (float) Math.sin(beamAngleSpeed / (2f * Math.PI) * time + randomFloats.get(i));
            angleOffsets.set(i, newSpread);
        }
        randomizedSpreads = false;
    }

    @Override
    public void advanceAfter(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
        // Reset the spec's angle offsets. Unsure if necessary
        for (int i = 0; i < angleOffsets.size(); i++) {
            angleOffsets.set(i, 0f);
        }
    }

    @Override
    public void advance(float amount, CombatEngineAPI engine, BeamAPI beam) {
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
        beam.getDamage().setForceHardFlux(Misc.random.nextFloat() < Math.min(0.5f, beam.getSource().getHardFluxLevel()));
    }

    @Override
    public void init(WeaponAPI weapon) {
        angleOffsets = weapon.getSlot().isTurret() ? weapon.getSpec().getTurretAngleOffsets()
                     : weapon.getSlot().isHardpoint() ? weapon.getSpec().getHardpointAngleOffsets()
                     : weapon.getSpec().getHiddenAngleOffsets();

        for (int i = 0; i < angleOffsets.size(); i++) {
            randomFloats.add(MathUtils.randBetween(0f, 6.28f));
        }
    }
}
