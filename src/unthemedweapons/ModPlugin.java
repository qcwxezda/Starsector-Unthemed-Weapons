package unthemedweapons;

import com.fs.starfarer.api.*;
import com.fs.starfarer.api.campaign.CampaignPlugin;
import com.fs.starfarer.api.campaign.GenericPluginManagerAPI;
import com.fs.starfarer.api.campaign.listeners.ListenerManagerAPI;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.loading.*;
import org.json.JSONObject;
import unthemedweapons.campaign.FortifiedCacheRegenerator;
import unthemedweapons.campaign.RefitTabListenerAndScript;
import unthemedweapons.campaign.ShipRecoveryWeaponsRemover;
import unthemedweapons.combat.ai.*;
import unthemedweapons.procgen.CacheDefenderPlugin;
import unthemedweapons.procgen.GenFortifiedCaches;
import unthemedweapons.procgen.GenSpecialCaches;

import java.awt.*;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;

@SuppressWarnings("unused")
public class ModPlugin extends BaseModPlugin {
    private final Map<String, MakeMissilePlugin> customMissiles = new HashMap<>();
    public static final String dummyMissileWeapon = "wpnxt_dummy_m", dummyProjWeapon = "wpnxt_dummy_p";
    public static boolean particleEngineEnabled = false;
    private static final Set<String> replaceExplosionWithParticles = new HashSet<>();
    public static final String initializerKey = "wpnxt_wasInitialized";

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

//    @Override
//    public void onApplicationLoad() {
//        // Add the relevant tags to weapon, fighter, and ship specs
//        // If adding tags to every spec breaks something, there is an alternative option: write an extension of, e.g. WeaponBlueprintItemPlugin and make a duplicate special item in special_items.csv with that plugin
//        // However, this has the issue that it doesn't work for fighter LPCs, where the resolution to specific LPC is hard-coded
//        addTagsToSpecs();
//    }
//
//    @Override
//    public void onDevModeF8Reload() {
//        addTagsToSpecs();
//    }
//
//    private void addTagsToSpecs() {
//        for (WeaponSpecAPI spec : Global.getSettings().getAllWeaponSpecs()) {
//            switch (spec.getSize()) {
//                case SMALL:
//                    spec.addTag(Tags.TAG_WEAPON_SMALL);
//                    break;
//                case MEDIUM:
//                    spec.addTag(Tags.TAG_WEAPON_MEDIUM);
//                    break;
//                case LARGE:
//                    spec.addTag(Tags.TAG_WEAPON_LARGE);
//                    break;
//            }
//        }
//        for (FighterWingSpecAPI spec : Global.getSettings().getAllFighterWingSpecs()) {
//            float op = spec.getOpCost(null);
//            if (op <= 5f) {
//                spec.addTag(Tags.TAG_FIGHTER_UNDER5OP);
//            }
//            else if (op <= 10f) {
//                spec.addTag(Tags.TAG_FIGHTER_5TO10OP);
//            }
//            else if (op <= 20f) {
//                spec.addTag(Tags.TAG_FIGHTER_10TO20OP);
//            }
//            else {
//                spec.addTag(Tags.TAG_FIGHTER_OVER20OP);
//            }
//        }
//        for (ShipHullSpecAPI spec : Global.getSettings().getAllShipHullSpecs()) {
//            if (!spec.isBaseHull()) continue;
//            switch (spec.getHullSize()) {
//                case DEFAULT:
//                case FIGHTER:
//                    break;
//                case FRIGATE:
//                    spec.addTag(Tags.TAG_FRIGATE);
//                    break;
//                case DESTROYER:
//                    spec.addTag(Tags.TAG_DESTROYER);
//                    break;
//                case CRUISER:
//                    spec.addTag(Tags.TAG_CRUISER);
//                    break;
//                case CAPITAL_SHIP:
//                    spec.addTag(Tags.TAG_CAPITAL);
//                    break;
//            }
//        }
//    }

    @Override
    public void onApplicationLoad() {
        particleEngineEnabled = Global.getSettings().getModManager().isModEnabled("particleengine");
        if (particleEngineEnabled) {
            String versionString = Global.getSettings().getModManager().getModSpec("particleengine").getVersion();
            String[] version = versionString.split("\\.");
            if (Integer.parseInt(version[0]) < 1 && Integer.parseInt(version[1]) < 5) {
                throw new RuntimeException("Particle Engine is enabled but out of date. Get the latest version here: https://fractalsoftworks.com/forum/index.php?topic=26453.0.");
            }
        }
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

    @Override
    public void onGameLoad(boolean newGame) {
        //CampaignUtils.generateFleetForEnergyCache(Global.getSector().getPlayerFleet(), Misc.random);
        float cacheFreq;
        boolean regenerateCaches;
        boolean qolEnabled;
        try {
            JSONObject json = Global.getSettings().loadJSON("wpnxt_mod_settings.json");
            cacheFreq = (float) json.getDouble("fortifiedCachesPerSystem");
            regenerateCaches = json.getBoolean("regenerateFortifiedCaches");
            qolEnabled = json.getBoolean("enableQoL");
        } catch (Exception e) {
            throw new RuntimeException("Could not load wpnxt_mod_settings.json: " + e, e);
        }
        int numCaches = (int) (Global.getSector().getStarSystems().size() * cacheFreq);

        if (!Global.getSector().getPersistentData().containsKey(initializerKey)) {
            // Ensure the initialization only happens once
            Global.getSector().getPersistentData().put(initializerKey, true);

            GenSpecialCaches.initialize(Global.getSector());
            GenFortifiedCaches.initialize(numCaches);
        }

        ListenerManagerAPI listeners = Global.getSector().getListenerManager();
        if (regenerateCaches) {
            FortifiedCacheRegenerator regenerator = new FortifiedCacheRegenerator(numCaches, false);
            if (!listeners.hasListenerOfClass(FortifiedCacheRegenerator.class)) {
                listeners.addListener(regenerator, true);
            }
            Global.getSector().addTransientListener(regenerator);
        }
        if (qolEnabled) {
            try {
                EveryFrameScript refitModifier = (EveryFrameScript) getClassLoader().loadClass("unthemedweapons.campaign.RefitTabListenerAndScript").newInstance();
                Global.getSector().addTransientScript(refitModifier);
                if (!listeners.hasListenerOfClass(RefitTabListenerAndScript.class)) {
                    listeners.addListener(refitModifier, true);
                }
            } catch (Exception e) {
                throw new RuntimeException("Failed to add refit tab listener; consider setting enableQoL to false in wpnxt_mod_settings.json", e);
            }
        }

        GenericPluginManagerAPI plugins = Global.getSector().getGenericPlugins();
        if (!plugins.hasPlugin(CacheDefenderPlugin.class)) {
            plugins.addPlugin(new CacheDefenderPlugin(), true);
        }
        Global.getSector().addTransientListener(new ShipRecoveryWeaponsRemover(false));
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


    private static final String[] reflectionWhitelist = new String[] {
            "unthemedweapons.campaign.RefitTabListenerAndScript",
            "unthemedweapons.util.ReflectionUtils",
            "unthemedweapons.util.DynamicWeaponStats"
    };

    private static ReflectionEnabledClassLoader getClassLoader() {
        URL url = ModPlugin.class.getProtectionDomain().getCodeSource().getLocation();
        return new ReflectionEnabledClassLoader(url, ModPlugin.class.getClassLoader());
    }

    public static class ReflectionEnabledClassLoader extends URLClassLoader {

        public ReflectionEnabledClassLoader(URL url, ClassLoader parent) {
            super(new URL[] {url}, parent);
        }

        @Override
        public Class<?> loadClass(String name) throws ClassNotFoundException {
            if (name.startsWith("java.lang.reflect")) {
                return ClassLoader.getSystemClassLoader().loadClass(name);
            }
            return super.loadClass(name);
        }

        @Override
        public Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
            Class<?> c = findLoadedClass(name);
            if (c != null) {
                return c;
            }
            // Be the defining classloader for all classes in the reflection whitelist
            // For classes defined by this loader, classes in java.lang.reflect will be loaded directly
            // by the system classloader, without the intermediate delegations.
            for (String str : reflectionWhitelist) {
                if (name.startsWith(str)) {
                    return findClass(name);
                }
            }
            return super.loadClass(name, resolve);
        }
    }
}
