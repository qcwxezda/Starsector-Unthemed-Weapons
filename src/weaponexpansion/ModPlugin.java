package weaponexpansion;

import com.fs.starfarer.api.BaseModPlugin;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.PluginPick;
import com.fs.starfarer.api.campaign.CampaignPlugin;
import com.fs.starfarer.api.combat.MissileAIPlugin;
import com.fs.starfarer.api.combat.MissileAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.loading.MissileSpecAPI;
import com.fs.starfarer.combat.ai.missile.MirvAI;
import org.json.JSONObject;
import weaponexpansion.combat.plugins.*;
import weaponexpansion.shaders.ParticleShader;
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
    public void onApplicationLoad() {
        ParticleShader.init("shaders/particle.vert", "shaders/particle.frag");
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

        addDumbfireMirv("wpnxt_clusterminelauncher", "wpnxt_clustermine_shot");
        addDumbfireMirv("wpnxt_clusterlauncherbig", "wpnxt_clusterminebig_shot");
    }

    public void addDumbfireMirv(String weaponSpec, String projSpec) {
        MissileSpecAPI missileSpec = (MissileSpecAPI) Global.getSettings().getWeaponSpec(weaponSpec).getProjectileSpec();
        JSONObject clusterBehaviorJSON = missileSpec.getBehaviorJSON();
        final int numShots = clusterBehaviorJSON.optInt("spawnCount", 0);
        final float timeToSplit = (float) clusterBehaviorJSON.optDouble("timeToSplit", 0);
        final float splitArc = (float) clusterBehaviorJSON.optDouble("arc", 0);
        final boolean evenSpread = clusterBehaviorJSON.optBoolean("evenSpread", false);
        final String spawnSpec = clusterBehaviorJSON.optString("spawnSpec", "");
        customMissiles.put(projSpec, new MakeMissilePlugin() {
            @Override
            public MissileAIPlugin make(MissileAPI missile) {
                return new DumbfireTimedMirv(
                        missile,
                        spawnSpec,
                        numShots,
                        timeToSplit,
                        splitArc,
                        evenSpread
                );
            }
        });
    }

    private interface MakeMissilePlugin {
        MissileAIPlugin make(MissileAPI missile);
    }
}
