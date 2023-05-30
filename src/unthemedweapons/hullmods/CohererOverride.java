package unthemedweapons.hullmods;

import java.awt.Color;

import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.combat.WeaponAPI.WeaponType;
import com.fs.starfarer.api.combat.listeners.WeaponBaseRangeModifier;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import unthemedweapons.combat.shipsystems.ReshaperStats;

/** Mostly copied from EnergyBoltCoherer.java, just removes crewed hull penalty
 *  ALSO MAKES SOME MODIFICATIONS TO THE MOD RADIANT STATS AND ADDS THE LISTENER FOR THE SHIP SYSTEM */
@SuppressWarnings("unused")
public class CohererOverride extends BaseHullMod {

    public static float RANGE_BONUS = 200;

    @Override
    public void applyEffectsBeforeShipCreation(HullSize hullSize, MutableShipStatsAPI stats, String id) {
        if (stats.getEntity() instanceof ShipAPI
                && ("wpnxt_mod_radiant".equals(((ShipAPI) stats.getEntity()).getHullSpec().getHullId()))) {
            stats.getAcceleration().modifyMult(id, 2f);
            stats.getDeceleration().modifyMult(id, 1.5f);
            stats.getTurnAcceleration().modifyMult(id, 2f);
            stats.getMaxTurnRate().modifyMult(id, 1.5f);
        }
    }

    @Override
    public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
        ship.addListener(new EnergyBoltCohererRangeModifier());
        if ("wpnxt_reshaper".equals(ship.getSystem().getId())
                && !ship.hasListenerOfClass(ReshaperStats.ReshaperTracker.class)) {
            ship.addListener(new ReshaperStats.ReshaperTracker(ship));
        }
    }

    public static class EnergyBoltCohererRangeModifier implements WeaponBaseRangeModifier {
        @Override
        public float getWeaponBaseRangePercentMod(ShipAPI ship, WeaponAPI weapon) {
            return 0;
        }
        @Override
        public float getWeaponBaseRangeMultMod(ShipAPI ship, WeaponAPI weapon) {
            return 1f;
        }
        @Override
        public float getWeaponBaseRangeFlatMod(ShipAPI ship, WeaponAPI weapon) {
            if (weapon.isBeam()) return 0f;
            if (weapon.getType() == WeaponType.ENERGY || weapon.getType() == WeaponType.HYBRID) {
                return RANGE_BONUS;
            }
            return 0f;
        }
    }

    public String getDescriptionParam(int index, HullSize hullSize) {
        return null;
    }

    @Override
    public boolean shouldAddDescriptionToTooltip(HullSize hullSize, ShipAPI ship, boolean isForModSpec) {
        return false;
    }

    @Override
    public void addPostDescriptionSection(TooltipMakerAPI tooltip, HullSize hullSize, ShipAPI ship, float width, boolean isForModSpec) {
        float pad = 3f;
        float opad = 10f;
        Color h = Misc.getHighlightColor();
        Color bad = Misc.getNegativeHighlightColor();

        tooltip.addPara("Increases the base range of all non-beam Energy and Hybrid weapons by %s.", opad, h,
                "" + (int)RANGE_BONUS);
        tooltip.addSectionHeading("Interactions with other modifiers", Alignment.MID, opad);
        tooltip.addPara("Since the base range is increased, this range modifier"
                + " - unlike most other flat modifiers in the game - "
                + "is increased by percentage modifiers from other hullmods and skills.", opad);
    }
}









