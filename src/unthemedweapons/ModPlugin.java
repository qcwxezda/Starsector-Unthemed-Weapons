package unthemedweapons;

import com.fs.starfarer.api.*;
import com.fs.starfarer.api.campaign.CampaignPlugin;
import com.fs.starfarer.api.campaign.GenericPluginManagerAPI;
import com.fs.starfarer.api.campaign.listeners.ListenerManagerAPI;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.loading.*;
import org.json.JSONObject;
import unthemedweapons.campaign.CampaignPluginImpl;
import unthemedweapons.campaign.FortifiedCacheRegenerator;
import unthemedweapons.campaign.RefitTabListenerAndScript;
import unthemedweapons.campaign.ShipRecoveryWeaponsRemover;
import unthemedweapons.combat.ai.*;
import unthemedweapons.procgen.CacheDefenderPlugin;
import unthemedweapons.procgen.GenFortifiedCaches;
import unthemedweapons.procgen.GenSpecialCaches;

import java.awt.*;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
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
    public static float CACHE_FREQ;
    public static boolean REGENERATE_CACHES;
    public static boolean QOL_ENABLED;

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
            return new PluginPick<>(new EnergyBallLauncherAI(weapon), CampaignPlugin.PickPriority.MOD_SPECIFIC);
        }
        return null;
    }

    @Override
    public void onApplicationLoad() {
        try {
            JSONObject json = Global.getSettings().loadJSON("wpnxt_mod_settings.json");
            CACHE_FREQ = (float) json.getDouble("fortifiedCachesPerSystem");
            REGENERATE_CACHES = json.getBoolean("regenerateFortifiedCaches");
            QOL_ENABLED = json.getBoolean("enableQoL");
        } catch (Exception e) {
            throw new RuntimeException("Could not load wpnxt_mod_settings.json: " + e, e);
        }

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
        customMissiles.put("wpnxt_energytorpedo_shot", missile -> new LeadingMissileAI(missile, 1.2f));
        customMissiles.put("wpnxt_spike_shot", missile -> new LeadingMissileAI(missile, 1.2f));
        customMissiles.put("wpnxt_energytorpedolarge_shot", missile -> new LeadingMissileAI(missile, 1.2f));
        customMissiles.put("wpnxt_minispiker_shot", missile -> new LOSMissileAI(missile, 1.2f));
        customMissiles.put("wpnxt_impaler_shot", missile -> new AngleApproachMissileAI(missile, 1.2f, 5f));
        customMissiles.put("wpnxt_orb_shot", missile -> new AngleApproachMissileAI(missile, 1.2f, 2f));
        customMissiles.put("wpnxt_clustermine_spawn", missile -> new ProximityMineRandomDelay(missile, 0.2f));
        customMissiles.put("wpnxt_phasetorpedo_shot", missile -> new PhaseTorpedoAI(missile, 1.2f));

        addDumbfireMirv("wpnxt_clusterminelauncher", "wpnxt_clustermine_shot");
        addDumbfireMirv("wpnxt_clusterlauncherbig", "wpnxt_clusterminebig_shot");

        // Remove default explosions if replacing with particles
        if (particleEngineEnabled) {
            for (WeaponSpecAPI spec : Global.getSettings().getAllWeaponSpecs()) {
                Object o = spec.getProjectileSpec();
                if (o instanceof ProjectileSpecAPI pSpec) {
                    if (replaceExplosionWithParticles.contains(pSpec.getId())) {
                        pSpec.setHitGlowRadius(0f);
                    }
                }
                if (o instanceof MissileSpecAPI mSpec) {
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
        Global.getSector().registerPlugin(new CampaignPluginImpl());

        int numCaches = (int) (Global.getSector().getStarSystems().size() * CACHE_FREQ);

        if (!Global.getSector().getPersistentData().containsKey(initializerKey)) {
            // Ensure the initialization only happens once
            Global.getSector().getPersistentData().put(initializerKey, true);

            GenSpecialCaches.initialize(Global.getSector());
            GenFortifiedCaches.initialize(numCaches);
        }

        ListenerManagerAPI listeners = Global.getSector().getListenerManager();
        if (REGENERATE_CACHES) {
            FortifiedCacheRegenerator regenerator = new FortifiedCacheRegenerator(numCaches, false);
            if (!listeners.hasListenerOfClass(FortifiedCacheRegenerator.class)) {
                listeners.addListener(regenerator, true);
            }
            Global.getSector().addTransientListener(regenerator);
        }
        if (QOL_ENABLED) {
            try {
                Class<?> cls = getClassLoader().loadClass("unthemedweapons.campaign.RefitTabListenerAndScript");
                MethodHandles.Lookup lookup = MethodHandles.lookup();
                MethodHandle mh = lookup.findConstructor(cls, MethodType.methodType(void.class));
                EveryFrameScript refitModifier = (EveryFrameScript) mh.invoke();
                Global.getSector().addTransientScript(refitModifier);
                if (!listeners.hasListenerOfClass(RefitTabListenerAndScript.class)) {
                    listeners.addListener(refitModifier, true);
                }
            } catch (Throwable e) {
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
        customMissiles.put(projSpec, missile -> new DumbfireTimedMirv(
                missile,
                spawnSpec,
                numShots,
                timeToSplit,
                splitArc,
                evenSpread
        ));
    }

    private interface MakeMissilePlugin {
        MissileAIPlugin make(MissileAPI missile);
    }


    private static final String[] reflectionWhitelist = new String[] {
            "unthemedweapons.campaign.RefitTabListenerAndScript",
            "unthemedweapons.util.ReflectionUtils",
            "unthemedweapons.util.DynamicWeaponStats"
    };

    public static ReflectionEnabledClassLoader getClassLoader() {
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
