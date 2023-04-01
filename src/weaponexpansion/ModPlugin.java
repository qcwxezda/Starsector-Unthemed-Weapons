package weaponexpansion;

import com.fs.starfarer.api.BaseModPlugin;
import com.fs.starfarer.api.PluginPick;
import com.fs.starfarer.api.campaign.CampaignPlugin;
import com.fs.starfarer.api.combat.MissileAIPlugin;
import com.fs.starfarer.api.combat.MissileAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import weaponexpansion.combat.plugins.AngleApproachMissileAI;
import weaponexpansion.combat.plugins.LOSMissileAI;
import weaponexpansion.combat.plugins.LeadingMissileAI;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

@SuppressWarnings("unused")
public class ModPlugin extends BaseModPlugin {

    private final static Set<String> leadingMissileAISet = new HashSet<>(Arrays.asList("wpnxt_energytorpedo_shot", "wpnxt_energytorpedolarge_shot"));
    private final static Set<String> losMissileAISet = new HashSet<>(Collections.singletonList("wpnxt_minispiker_shot"));

    private final static Set<String> angleApproachMissileAISet = new HashSet<>(Arrays.asList("wpnxt_impaler_shot", "wpnxt_orb_shot"));

    public PluginPick<MissileAIPlugin> pickMissileAI(MissileAPI missile, ShipAPI launchingShip) {
        String projId = missile.getProjectileSpecId();
        float seekRange = missile.getWeapon() == null ? 0f : missile.getWeapon().getRange();
        if (leadingMissileAISet.contains(projId)) {
            return new PluginPick<MissileAIPlugin>(new LeadingMissileAI(missile, seekRange), CampaignPlugin.PickPriority.MOD_SPECIFIC);
        }
        if (losMissileAISet.contains(projId)) {
            return new PluginPick<MissileAIPlugin>(new LOSMissileAI(missile, seekRange), CampaignPlugin.PickPriority.MOD_SPECIFIC);
        }
        if (angleApproachMissileAISet.contains(projId)) {
            return new PluginPick<MissileAIPlugin>(new AngleApproachMissileAI(missile, seekRange, 3f * missile.getMaxSpeed()), CampaignPlugin.PickPriority.MOD_SPECIFIC);
        }
        return null;
    }
}
