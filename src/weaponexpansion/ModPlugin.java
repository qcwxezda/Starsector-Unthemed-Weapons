package weaponexpansion;

import com.fs.starfarer.api.BaseModPlugin;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.PluginPick;
import com.fs.starfarer.api.campaign.CampaignPlugin;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.loading.DamagingExplosionSpec;
import com.fs.starfarer.api.loading.MissileSpecAPI;
import com.fs.starfarer.api.loading.ProjectileSpecAPI;
import com.fs.starfarer.api.loading.WeaponSpecAPI;
import org.json.JSONObject;
import weaponexpansion.combat.ai.*;

import java.awt.*;
import java.util.*;

@SuppressWarnings("unused")
public class ModPlugin extends BaseModPlugin {
    private final Map<String, MakeMissilePlugin> customMissiles = new HashMap<>();
    public static final String dummyMissileWeapon = "wpnxt_dummy_m", dummyProjWeapon = "wpnxt_dummy_p";
    public static boolean particleEngineEnabled = false;
    private static final Set<String> replaceExplosionWithParticles = new HashSet<>();

    static {
        replaceExplosionWithParticles.add("wpnxt_energytorpedo_shot");
        replaceExplosionWithParticles.add("wpnxt_energytorpedolarge_shot");
        replaceExplosionWithParticles.add("wpnxt_explosiveshell_shot");
        replaceExplosionWithParticles.add("wpnxt_iontorpedo_shot");
        replaceExplosionWithParticles.add("wpnxt_phasetorpedo_shot");
        replaceExplosionWithParticles.add("wpnxt_voidcannon_shot");
    }

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
    public PluginPick<AutofireAIPlugin> pickWeaponAutofireAI(WeaponAPI weapon) {
        String weaponId = weapon.getId();

        if ("wpnxt_energyballlauncher".equals(weaponId)) {
            return new PluginPick<AutofireAIPlugin>(new EnergyBallLauncherAI(weapon), CampaignPlugin.PickPriority.MOD_SPECIFIC);
        }
        return null;
    }

    @Override
    public void onGameLoad(boolean newGame) {

        particleEngineEnabled = Global.getSettings().getModManager().isModEnabled("particleengine");

        // Populate custom missile AI
        customMissiles.clear();
        customMissiles.put("wpnxt_energytorpedo_shot", new MakeMissilePlugin() {
            @Override
            public MissileAIPlugin make(MissileAPI missile) {
                return new LeadingMissileAI(missile, 1.2f);
            }
        });
        customMissiles.put("wpnxt_spike_shot", new MakeMissilePlugin() {
            @Override
            public MissileAIPlugin make(MissileAPI missile) {
                return new LeadingMissileAI(missile, 1.2f);
            }
        });
        customMissiles.put("wpnxt_energytorpedolarge_shot", new MakeMissilePlugin() {
            @Override
            public MissileAIPlugin make(MissileAPI missile) {
                return new LeadingMissileAI(missile, 1.2f);
            }
        });
        customMissiles.put("wpnxt_minispiker_shot", new MakeMissilePlugin() {
            @Override
            public MissileAIPlugin make(MissileAPI missile) {
                return new LOSMissileAI(missile, 1.2f);
            }
        });
        customMissiles.put("wpnxt_impaler_shot", new MakeMissilePlugin() {
            @Override
            public MissileAIPlugin make(MissileAPI missile) {
                return new AngleApproachMissileAI(missile, 1.2f, 5f);
            }
        });
        customMissiles.put("wpnxt_orb_shot", new MakeMissilePlugin() {
            @Override
            public MissileAIPlugin make(MissileAPI missile) {
                return new AngleApproachMissileAI(missile, 1.2f, 2f);
            }
        });
        customMissiles.put("wpnxt_clustermine_spawn", new MakeMissilePlugin() {
            @Override
            public MissileAIPlugin make(MissileAPI missile) {
                return new ProximityMineRandomDelay(missile, 0.2f);
            }
        });
        customMissiles.put("wpnxt_phasetorpedo_shot", new MakeMissilePlugin() {
            @Override
            public MissileAIPlugin make(MissileAPI missile) {
                return new PhaseTorpedoAI(missile, 1.2f);
            }
        });

        addDumbfireMirv("wpnxt_clusterminelauncher", "wpnxt_clustermine_shot");
        addDumbfireMirv("wpnxt_clusterlauncherbig", "wpnxt_clusterminebig_shot");

        // Remove default explosions if replacing with particles
        if (particleEngineEnabled) {
            for (WeaponSpecAPI spec : Global.getSettings().getAllWeaponSpecs()) {
                Object o = spec.getProjectileSpec();
                if (o instanceof ProjectileSpecAPI) {
                    ProjectileSpecAPI pSpec = (ProjectileSpecAPI) o;
                    if (replaceExplosionWithParticles.contains(pSpec.getId())) {
                        pSpec.setHitGlowRadius(0f);
                    }
                }
                if (o instanceof MissileSpecAPI) {
                    MissileSpecAPI mSpec = (MissileSpecAPI) o;
                    if (replaceExplosionWithParticles.contains(mSpec.getHullSpec().getHullId())) {
                        mSpec.setUseHitGlowWhenDealingDamage(false);
                        DamagingExplosionSpec eSpec = mSpec.getExplosionSpec();
                        if (eSpec != null) {
                            eSpec.setUseDetailedExplosion(false);
                            eSpec.setExplosionColor(new Color(0, 0, 0, 0));
                            eSpec.setParticleCount(0);
                        }
                    }
                }
            }
        }
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
