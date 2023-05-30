package unthemedweapons.campaign;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.listeners.ShowLootListener;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.WeightedRandomPicker;
import unthemedweapons.procgen.GenFortifiedCaches;

public class FortifiedCacheRegenerator extends BaseCampaignEventListener implements ShowLootListener  {

    float desiredCacheCount;
    int currentCacheCount = 0;

    public FortifiedCacheRegenerator(float desiredCacheCount, boolean permaRegister) {
        super(permaRegister);
        this.desiredCacheCount = desiredCacheCount;

        for (StarSystemAPI system : Global.getSector().getStarSystems()) {
            for (SectorEntityToken entity : system.getCustomEntities()) {
                if ("wpnxt_fortified_cache".equals(entity.getCustomEntitySpec().getId())) {
                    currentCacheCount++;
                }
            }
        }
    }

    /** Regenerate 1 cache max per economy tick */
    @Override
    public void reportEconomyTick(int iter) {
        if (currentCacheCount < desiredCacheCount) {
            WeightedRandomPicker<StarSystemAPI> systemPicker = GenFortifiedCaches.getSystemPicker(Misc.random);
            // Removing null is fine
            systemPicker.remove(Global.getSector().getPlayerFleet().getStarSystem());
            StarSystemAPI system = systemPicker.pick();
            GenFortifiedCaches.addCache(system, GenFortifiedCaches.getFactionPicker(Misc.random));
            currentCacheCount++;
        }
    }

    @Override
    public void reportAboutToShowLootToPlayer(CargoAPI cargo, InteractionDialogAPI dialog) {
        if (dialog == null || dialog.getInteractionTarget() == null || dialog.getInteractionTarget().getCustomEntitySpec() == null) return;
        if ("wpnxt_fortified_cache".equals(dialog.getInteractionTarget().getCustomEntitySpec().getId())) {
            currentCacheCount--;
        }
    }
}
