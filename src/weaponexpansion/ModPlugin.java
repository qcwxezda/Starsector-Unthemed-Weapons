package weaponexpansion;

import com.fs.starfarer.api.BaseModPlugin;
import com.fs.starfarer.api.PluginPick;
import com.fs.starfarer.api.campaign.CampaignPlugin;
import com.fs.starfarer.api.combat.MissileAIPlugin;
import com.fs.starfarer.api.combat.MissileAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.combat.ai.ProximityFuseAI;
import weaponexpansion.combat.plugins.AngleApproachMissileAI;
import weaponexpansion.combat.plugins.LOSMissileAI;
import weaponexpansion.combat.plugins.LeadingMissileAI;
import weaponexpansion.combat.plugins.ProximityMineRandomDelay;

import java.util.*;

@SuppressWarnings("unused")
public class ModPlugin extends BaseModPlugin {
    private final Map<String, MakeMissilePlugin> customMissiles = new HashMap<>();
    public static final String dummyMissileWeapon = "wpnxt_dummy_m", dummyProjWeapon = "wpnxt_dummy_p";

    @Override
    public PluginPick<MissileAIPlugin> pickMissileAI(MissileAPI missile, ShipAPI launchingShip) {
        String projId = missile.getProjectileSpecId();

        MakeMissilePlugin pluginGen = customMissiles.get(projId);
        if (pluginGen != null) {
            return new PluginPick<>(pluginGen.make(missile), CampaignPlugin.PickPriority.MOD_SPECIFIC);
        }
        return null;
    }

    @Override
    public void onGameLoad(boolean newGame) {
        customMissiles.clear();
        customMissiles.put("wpnxt_energytorpedo_shot", new MakeMissilePlugin() {
            @Override
            public MissileAIPlugin make(MissileAPI missile) {
                return new LeadingMissileAI(missile, 1f);
            }
        });
        customMissiles.put("wpnxt_energytorpedolarge_shot", new MakeMissilePlugin() {
            @Override
            public MissileAIPlugin make(MissileAPI missile) {
                return new LeadingMissileAI(missile, 1f);
            }
        });
        customMissiles.put("wpnxt_minispiker_shot", new MakeMissilePlugin() {
            @Override
            public MissileAIPlugin make(MissileAPI missile) {
                return new LOSMissileAI(missile, 1f);
            }
        });
        customMissiles.put("wpnxt_impaler_shot", new MakeMissilePlugin() {
            @Override
            public MissileAIPlugin make(MissileAPI missile) {
                return new AngleApproachMissileAI(missile, 1f, 5f);
            }
        });
        customMissiles.put("wpnxt_orb_shot", new MakeMissilePlugin() {
            @Override
            public MissileAIPlugin make(MissileAPI missile) {
                return new AngleApproachMissileAI(missile, 1f, 2f);
            }
        });
        customMissiles.put("wpnxt_clustermine_spawn", new MakeMissilePlugin() {
            @Override
            public MissileAIPlugin make(MissileAPI missile) {
                return new ProximityMineRandomDelay(missile, 0.2f);
            }
        });
    }

    private interface MakeMissilePlugin {
        MissileAIPlugin make(MissileAPI missile);
    }
}
