package unthemedweapons.combat.scripts;

import com.fs.starfarer.api.combat.*;
import unthemedweapons.util.MathUtils;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("unused")
public class VoidRayWeaponEffect implements EveryFrameWeaponEffectPlugin, WeaponEffectPluginWithInit, EveryFrameWeaponEffectPluginWithAdvanceAfter {

    static float maxSpread = 9f;

    /** Per second */
    static float beamAngleSpeed = 20f;
    static float maxWidth = 18f;
    /** Per second */
    static float fullChargeGrowRate = 1f;

    /** Amount of damage to transfer to full strength first beam per second. */
    static float transferPerSecond = 0.1f;
    static float transferSecondsElapsed = 0f;
    final static float MAX_ADDITIONAL_ARMOR_DAMAGE = 1000f;
    final static float ARMOR_DAMAGE_MULTIPLIER = 3f;
    final static String TRANSFER_DAMAGE_KEY = "wpnxt_voidRayTransfer";
    final static String ARMOR_DAMAGE_KEY = "wpnxt_voidRayExtraDamage";
    final static String BASE_ARMOR_DAMAGE_MULTIPLIER_KEY = "wpnxt_voidRayArmorDamageMultiplier";

    /** Need both spreads and angleOffsets since angleOffsets is shared between every voidray weapon */
    float spread = 0f;
    boolean randomizedSpreads = false;
    float convergeLevel = 0f;
    /** Per second, as fraction of remaining convergence arc */
    float convergeLevelRate = 0.8f;
    /** This multiplier only affects the non-primary beams, so to get the true damage multiplier multiply (mult-1) by 4/5 */
    float convergeDamageMult = 1.375f;

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
            transferSecondsElapsed = 0f;
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
            transferSecondsElapsed += amount;
            float transferProgress = Math.min(1f, transferSecondsElapsed * transferPerSecond);
            int numBeams = weapon.getBeams().size();
            for (int i = 1; i < numBeams; i++) {
                BeamAPI beam = weapon.getBeams().get(i);
                beam.getDamage().getModifier().modifyMult(TRANSFER_DAMAGE_KEY, 1f - transferProgress);
            }
            BeamAPI firstBeam = weapon.getBeams().get(0);
            firstBeam.getDamage().getModifier().modifyPercent(TRANSFER_DAMAGE_KEY, 100f * (numBeams - 1) * transferProgress);

            if (firstBeam.getDamage().getDpsDuration() > 0f) {
                CombatEntityAPI target = firstBeam.getDamageTarget();
                boolean hitHull = target instanceof ShipAPI && (target.getShield() == null || !target.getShield().isWithinArc(firstBeam.getTo()));
                if (hitHull) {
                    ShipAPI ship = (ShipAPI) target;
                    int[] xy = ship.getArmorGrid().getCellAtLocation(firstBeam.getTo());
                    float baseDamage = firstBeam.getDamage().getBaseDamage();
                    if (xy != null) {
                        float armor = ship.getMutableStats().getEffectiveArmorBonus()
                                .computeEffective(15f * ship.getArmorGrid().getArmorValue(xy[0], xy[1]));
                        armor = Math.max(armor, ship.getArmorGrid().getArmorRating() * ship.getMutableStats().getMinArmorFraction().getModifiedValue());
                        firstBeam.getDamage().getModifier().modifyMult(BASE_ARMOR_DAMAGE_MULTIPLIER_KEY, 10f / 15f);
                        firstBeam.getDamage().getModifier().modifyPercent(
                                ARMOR_DAMAGE_KEY,
                                100f * 15f / 10f * transferProgress * Math.min(MAX_ADDITIONAL_ARMOR_DAMAGE, ARMOR_DAMAGE_MULTIPLIER * armor) / baseDamage);
                    }
                } else {
                    firstBeam.getDamage().getModifier().unmodify(ARMOR_DAMAGE_KEY);
                    firstBeam.getDamage().getModifier().unmodify(BASE_ARMOR_DAMAGE_MULTIPLIER_KEY);
                }
                for (BeamAPI beam : weapon.getBeams()) {
                    beam.setWidth(Math.min(maxWidth, beam.getWidth() + fullChargeGrowRate * amount));
                }
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
    public void init(WeaponAPI weapon) {
        angleOffsets = weapon.getSlot().isTurret() ? weapon.getSpec().getTurretAngleOffsets()
                     : weapon.getSlot().isHardpoint() ? weapon.getSpec().getHardpointAngleOffsets()
                     : weapon.getSpec().getHiddenAngleOffsets();

        for (int i = 0; i < angleOffsets.size(); i++) {
            randomFloats.add(MathUtils.randBetween(0f, 6.28f));
        }
    }
}
