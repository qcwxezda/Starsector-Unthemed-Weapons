package unthemedweapons.campaign.rulecmd;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.rules.MemKeys;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.impl.campaign.ids.MemFlags;
import com.fs.starfarer.api.impl.campaign.rulecmd.AddRemoveCommodity;
import com.fs.starfarer.api.impl.campaign.rulecmd.BaseCommandPlugin;
import com.fs.starfarer.api.impl.campaign.rulecmd.salvage.SalvageEntity;
import com.fs.starfarer.api.util.Misc;
import unthemedweapons.procgen.GenFortifiedCaches;
import unthemedweapons.procgen.GenSpecialCaches;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;

@SuppressWarnings("unused")
public class wpnxt_PrePerformSalvage extends BaseCommandPlugin {
    @Override
    public boolean execute(String ruleId, InteractionDialogAPI dialog, List<Misc.Token> params, final Map<String, MemoryAPI> memoryMap){
        if (dialog == null) return false;

        MemoryAPI localMemory = memoryMap.get(MemKeys.LOCAL);
        long seed = localMemory.getLong(MemFlags.SALVAGE_SEED);
        Random random = new Random(seed);

        String specialCacheType = localMemory.getString(GenSpecialCaches.cacheKey);
        if (specialCacheType == null) {
            GenFortifiedCaches.generateLootForCache(dialog.getInteractionTarget(), random);
        }
        else if ("BALLISTIC".equals(specialCacheType)) {
            GenSpecialCaches.populateCache(
                    dialog.getInteractionTarget(),
                    4,
                    150,
                    30,
                    WeaponAPI.WeaponType.BALLISTIC,
                    random
            );
        }
        else if ("ENERGY".equals(specialCacheType)) {
            GenSpecialCaches.populateCache(
                    dialog.getInteractionTarget(),
                    2,
                    150,
                    30,
                    WeaponAPI.WeaponType.ENERGY,
                    random
            );
        }
        else if ("MISSILE".equals(specialCacheType)) {
            GenSpecialCaches.populateCache(
                    dialog.getInteractionTarget(),
                    2,
                    150,
                    30,
                    WeaponAPI.WeaponType.MISSILE,
                    random
            );
        }

        Float amount = (Float) memoryMap.get(MemKeys.LOCAL).get(GenFortifiedCaches.numCreditsKey);
        if (amount == null || amount <= 0f) {
            new SalvageEntity().execute(ruleId, dialog, Collections.singletonList(new Misc.Token("performSalvage", Misc.TokenType.LITERAL)), memoryMap);
        }
        else {
            dialog.getTextPanel().addPara("You find some credits strewn about the various containers and storerooms.");
            Global.getSector().getPlayerFleet().getCargo().getCredits().add(amount);
            AddRemoveCommodity.addCreditsGainText((int) (float) amount, dialog.getTextPanel());
            dialog.getOptionPanel().clearOptions();
            dialog.getOptionPanel().addOption("Continue salvage operations", "wpnxt_proceedWithSalvage");
        }

        return true;
    }
}
