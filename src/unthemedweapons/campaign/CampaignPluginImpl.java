package unthemedweapons.campaign;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.PluginPick;
import com.fs.starfarer.api.campaign.BaseCampaignPlugin;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.ReputationActionResponsePlugin;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.characters.PersonAPI;
import unthemedweapons.procgen.CacheDefenderPlugin;

public class CampaignPluginImpl extends BaseCampaignPlugin {

    @Override
    public String getId() {
        return "uwc_CampaignPluginImpl";
    }

    @Override
    public PluginPick<ReputationActionResponsePlugin> pickReputationActionResponsePlugin(Object action, String factionId) {
        if (Global.getSector().getCampaignUI() == null) return null;
        InteractionDialogAPI dialog = Global.getSector().getCampaignUI().getCurrentInteractionDialog();
        if (dialog == null) return null;
        SectorEntityToken token = dialog.getInteractionTarget();
        if (!(token instanceof CampaignFleetAPI)) return null;
        CampaignFleetAPI fleet = (CampaignFleetAPI) token;
        if (!fleet.getMemoryWithoutUpdate().contains(CacheDefenderPlugin.cacheDefenseFleetKey)) return null;
        return new PluginPick<ReputationActionResponsePlugin>(
                new ReputationActionResponsePlugin() {
                    @Override
                    public ReputationAdjustmentResult handlePlayerReputationAction(Object action, String factionId) {
                        return new ReputationActionResponsePlugin.ReputationAdjustmentResult(0f);
                    }

                    @Override
                    public ReputationAdjustmentResult handlePlayerReputationAction(Object action, PersonAPI person) {
                        return new ReputationActionResponsePlugin.ReputationAdjustmentResult(0f);
                    }
                },
                PickPriority.MOD_SPECIFIC);
    }
}
