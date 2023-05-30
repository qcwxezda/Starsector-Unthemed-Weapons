package unthemedweapons.campaign;

import com.fs.starfarer.api.campaign.BaseCampaignEventListener;
import com.fs.starfarer.api.campaign.EngagementResultForFleetAPI;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.loading.VariantSource;
import unthemedweapons.procgen.CacheDefenderPlugin;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Removes certain weapons from disabled/destroyed enemy ships that should not be recoverable from battle. */
public class ShipRecoveryWeaponsRemover extends BaseCampaignEventListener {

    public static final Set<String> weaponsToRemove = new HashSet<>();

    static {
        weaponsToRemove.add("wpnxt_phasetorpedo");
        weaponsToRemove.add("wpnxt_energyballlauncher");
        weaponsToRemove.add("wpnxt_morphcannon");
    }

    public ShipRecoveryWeaponsRemover(boolean permaRegister) {
        super(permaRegister);
    }

    @Override
    public void reportPlayerEngagement(EngagementResultAPI result) {
        if (result.getLoserResult().isPlayer()) return;

        EngagementResultForFleetAPI loserResult = result.getLoserResult();
        if (!loserResult.getFleet().getMemoryWithoutUpdate().contains(CacheDefenderPlugin.specialFleetKey)) return;

        List<FleetMemberAPI> destroyedOrDisabled = new ArrayList<>();
        destroyedOrDisabled.addAll(loserResult.getDestroyed());
        destroyedOrDisabled.addAll(loserResult.getDisabled());

        for (FleetMemberAPI member : destroyedOrDisabled) {
            ShipVariantAPI variant = member.getVariant().clone();
            variant.setOriginalVariant(null);
            variant.setSource(VariantSource.REFIT);
            List<String> toRemove = new ArrayList<>();

            for (String id : variant.getNonBuiltInWeaponSlots()) {
                if (weaponsToRemove.contains(variant.getWeaponId(id))) {
                    toRemove.add(id);
                }
            }

            for (String id : toRemove) {
                variant.clearSlot(id);
            }

            member.setVariant(variant, false, false);
        }
    }
}
