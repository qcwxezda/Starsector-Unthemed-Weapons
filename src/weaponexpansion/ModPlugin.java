package weaponexpansion;

import com.fs.starfarer.api.BaseModPlugin;
import com.fs.starfarer.api.PluginPick;
import com.fs.starfarer.api.campaign.CampaignPlugin;
import com.fs.starfarer.api.combat.MissileAIPlugin;
import com.fs.starfarer.api.combat.MissileAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.combat.ai.missile.MissileAISidewinder;
import weaponexpansion.combat.plugins.LOSMissileAI;
import weaponexpansion.combat.plugins.SwervingMissileAI;
import weaponexpansion.combat.plugins.TestMissileAI;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

@SuppressWarnings("unused")
public class ModPlugin extends BaseModPlugin {

    private final static Set<String> missileSet = new HashSet<>(Arrays.asList("wpnxt_minispiker", "wpnxt_energytorpedo", "wpnxt_energytorpedomed", "wpnxt_energytorpedolarge"));

    public PluginPick<MissileAIPlugin> pickMissileAI(MissileAPI missile, ShipAPI launchingShip) {
        if (missile.getWeapon() != null && missileSet.contains(missile.getWeapon().getId())) {
            return new PluginPick<MissileAIPlugin>(new LOSMissileAI(missile, missile.getWeapon().getRange()), CampaignPlugin.PickPriority.MOD_SPECIFIC);
        }
        return null;
    }
}
