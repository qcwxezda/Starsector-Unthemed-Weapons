package weaponexpansion.procgen;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.impl.campaign.DerelictShipEntityPlugin;
import com.fs.starfarer.api.impl.campaign.ids.Entities;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.ids.Items;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.impl.campaign.procgen.DefenderDataOverride;
import com.fs.starfarer.api.impl.campaign.procgen.SalvageEntityGenDataSpec;
import com.fs.starfarer.api.impl.campaign.procgen.StarSystemGenerator;
import com.fs.starfarer.api.impl.campaign.procgen.themes.BaseThemeGenerator;
import com.fs.starfarer.api.impl.campaign.procgen.themes.SalvageSpecialAssigner;
import com.fs.starfarer.api.impl.campaign.rulecmd.salvage.special.ShipRecoverySpecial;
import com.fs.starfarer.api.impl.campaign.terrain.PulsarBeamTerrainPlugin;
import com.fs.starfarer.api.loading.WeaponSpecAPI;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.WeightedRandomPicker;
import org.lwjgl.util.vector.Vector2f;
import weaponexpansion.util.MathUtils;

import java.util.*;

public abstract class ProcGen {

    public static String cacheKey = "$wpnxt_specialCache";
    public static String cacheFlagKey = "$wpnxt_specialCacheFlag";
    public static Set<String> tagsToSkip = new HashSet<>();

    static {
        tagsToSkip.add(Tags.THEME_CORE);
        tagsToSkip.add(Tags.HAS_CORONAL_TAP);
        tagsToSkip.add(Tags.THEME_DERELICT_CRYOSLEEPER);
        tagsToSkip.add(Tags.THEME_DERELICT_MOTHERSHIP);
        tagsToSkip.add(Tags.THEME_SPECIAL);
        tagsToSkip.add(Tags.THEME_HIDDEN);
    }

    public static void initialize(SectorAPI sector) {
        // Look for a neutron star system, a black hole system, and a binary system
        List<StarSystemAPI> pulsar = new ArrayList<>();
        List<StarSystemAPI> blackHole = new ArrayList<>();
        List<StarSystemAPI> binary = new ArrayList<>();
        // Only used as a fallback in case one or more of the above don't exist
        List<StarSystemAPI> allCandidateSystems = new ArrayList<>();
        for (StarSystemAPI system : sector.getStarSystems()) {
            // No nebulae
            if (system.isNebula()) {
                continue;
            }
            // Ignore specific themed worlds
            Set<String> intersection = new HashSet<>(tagsToSkip);
            intersection.retainAll(system.getTags());
            if (!intersection.isEmpty()) {
                continue;
            }

            // Ensure the three chosen systems are unique
            if (system.hasBlackHole()) {
                blackHole.add(system);
            }
            // Neutron star system should have only neutron stars
            else if (system.getStar() != null && system.getStar().getSpec().isPulsar() &&
                    (system.getSecondary() == null || system.getSecondary().getSpec().isPulsar()) &&
                    (system.getTertiary() == null || system.getTertiary().getSpec().isPulsar())) {
                pulsar.add(system);
            }
            else if (StarSystemGenerator.StarSystemType.BINARY_CLOSE.equals(system.getType())
                || StarSystemGenerator.StarSystemType.TRINARY_2CLOSE.equals(system.getType())
                || StarSystemGenerator.StarSystemType.TRINARY_1CLOSE_1FAR.equals(system.getType())) {
                binary.add(system);
            }

            allCandidateSystems.add(system);
        }

        StarSystemAPI chosenPulsar, chosenBlackHole, chosenBinary;

        if (pulsar.isEmpty()) {
            chosenPulsar = getRandomSystem(allCandidateSystems);
            allCandidateSystems.remove(chosenPulsar);
        }
        else {
            chosenPulsar = getRandomSystem(pulsar);
        }

        if (blackHole.isEmpty()) {
            chosenBlackHole = getRandomSystem(allCandidateSystems);
            allCandidateSystems.remove(chosenBlackHole);
        }
        else {
            chosenBlackHole = getRandomSystem(blackHole);
        }

        if (binary.isEmpty()) {
            chosenBinary = getRandomSystem(allCandidateSystems);
            allCandidateSystems.remove(chosenBinary);
        }
        else {
            chosenBinary = getRandomSystem(binary);
        }

        // Add the "interesting" theme to the chosen systems
        // Underlying tags backed by hash set; don't need to check duplicates
        chosenPulsar.addTag(Tags.THEME_INTERESTING);
        chosenBlackHole.addTag(Tags.THEME_INTERESTING);
        chosenBinary.addTag(Tags.THEME_INTERESTING);

        Map<String, Float> ballisticRarities = new HashMap<>();
        Map<String, Float> energyRarities = new HashMap<>();
        Map<String, Float> missileRarities = new HashMap<>();

        separateRareWeaponBPs(ballisticRarities, energyRarities, missileRarities, 0.5f, 0.25f);

        addBallisticCache(chosenBinary, ballisticRarities);
        addMissileCache(chosenBlackHole, missileRarities);
        addEnergyCache(chosenPulsar, energyRarities);
    }

    private static void addEnergyCache(StarSystemAPI system, Map<String, Float> energyRarities) {
        List<PulsarBeamTerrainPlugin> pulsarPlugins = new ArrayList<>();
        for (CampaignTerrainAPI terrain : system.getTerrainCopy()) {
            if (terrain.getPlugin() instanceof PulsarBeamTerrainPlugin) {
                pulsarPlugins.add((PulsarBeamTerrainPlugin) terrain.getPlugin());
                break;
            }
        }
        Vector2f center = system.getStar() == null ? new Vector2f() : system.getStar().getLocation();
        Vector2f energyCacheLocation = MathUtils.randomPointInRing(center, 5000f, 6000f);

        // Ensure the pulsar beam isn't blocked by another planet
        if (system.getStar() != null) {
            int maxAttempts = 100;
            for (int i = 0; i < maxAttempts; i++) {
                boolean isOccluded = false;
                for (PlanetAPI planet : system.getPlanets()) {
                    if (!planet.equals(system.getStar())
                            && Misc.intersectSegmentAndCircle(center, energyCacheLocation, planet.getLocation(), planet.getRadius()) != null) {
                        isOccluded = true;
                        break;
                    }
                }
                if (isOccluded) {
                    energyCacheLocation = MathUtils.randomPointInRing(center, 5000f, 6000f);
                }
                else {
                    break;
                }
            }
        }

        SectorEntityToken energyCache = BaseThemeGenerator.addSalvageEntity(system, "wpnxt_energy_cache", null, null);
        energyCache.setFixedLocation(energyCacheLocation.x, energyCacheLocation.y);
        if (!pulsarPlugins.isEmpty()) {
            energyCache.addScript(new EnergyCacheScript(energyCache, pulsarPlugins));
        }

        populateCache(energyCache, 2, 120, 20, "ENERGY", energyRarities);
        energyCache.getMemoryWithoutUpdate().set(cacheKey, "ENERGY");
        energyCache.getMemoryWithoutUpdate().set(cacheFlagKey, true);
        Misc.setDefenderOverride(energyCache, new DefenderDataOverride(Factions.MERCENARY, 1f, 240f, 240f, 20));

        // Mark the correct neutron star system by adding a single pristine derelict near the star
        WeightedRandomPicker<String> hulls = new WeightedRandomPicker<>();
        hulls.add("apogee", 1f);
        hulls.add("aurora", 1f);
        hulls.add("champion", 1f);
        Vector2f starCenter = system.getStar() == null ? new Vector2f() : system.getStar().getLocation();
        float starRadius = system.getStar() == null ? 100f : system.getStar().getRadius();
        Vector2f point = MathUtils.randomPointInRing(starCenter, 3f*starRadius, 6f*starRadius);
        DerelictShipEntityPlugin.DerelictShipData params =
                DerelictShipEntityPlugin.createHull(
                        hulls.pick(),
                        Misc.random,
                        1f);
        params.ship.condition = ShipRecoverySpecial.ShipCondition.PRISTINE;
        params.ship.pruneWeapons = true;
        params.ship.addDmods = false;
        SectorEntityToken derelict = BaseThemeGenerator.addSalvageEntity(Misc.random, system, Entities.WRECK, null, params);
        derelict.setDiscoverable(true);
        SalvageSpecialAssigner.assignSpecials(derelict);
        if (system.getStar() == null) {
            derelict.setFixedLocation(point.x, point.y);
        } else {
            derelict.setCircularOrbit(system.getStar(), MathUtils.randBetween(0f, 360f), point.length(), MathUtils.randBetween(30f, 90f));
        }
    }

    private static final int numDerelictShips = 25;
    private static void addMissileCache(StarSystemAPI system, Map<String, Float> missileRarities) {
        SectorEntityToken missileCache = BaseThemeGenerator.addSalvageEntity(system, "wpnxt_missile_cache", null, null);
        populateCache(missileCache, 2, 120, 20, "MISSILE", missileRarities);

        // Shouldn't happen, but fallback just in case
        if (system.getStar() == null) {
            Vector2f loc = MathUtils.randomPointInRing(new Vector2f(), 80f, 100f);
            missileCache.setFixedLocation(loc.x, loc.y);
        }
        else {
            missileCache.setFixedLocation(1000000f, 1000000f);
            missileCache.addScript(new MissileCacheScript(missileCache, system.getStar()));
        }

        missileCache.getMemoryWithoutUpdate().set(cacheKey, "MISSILE");
        missileCache.getMemoryWithoutUpdate().set(cacheFlagKey, true);
        Misc.setDefenderOverride(missileCache, new DefenderDataOverride(Factions.MERCENARY, 1f, 240f, 240f, 20));

        // Mark the correct black hole system with a bunch of derelicts strewn about
        WeightedRandomPicker<String> factions = new WeightedRandomPicker<>(Misc.random);
        factions.add(Factions.PERSEAN, 10f);
        factions.add(Factions.INDEPENDENT, 8f);
        factions.add(Factions.LUDDIC_PATH, 4f);
        Vector2f starCenter = system.getStar() == null ? new Vector2f() : system.getStar().getLocation();
        float starRadius = system.getStar() == null ? 100f : system.getStar().getRadius();
        for (int i = 0; i < numDerelictShips; i++) {
            Vector2f point = MathUtils.randomPointInRing(starCenter, 3f*starRadius, 6f*starRadius);
            DerelictShipEntityPlugin.DerelictShipData params =
                DerelictShipEntityPlugin.createRandom(
                        factions.pick(),
                        null,
                        Misc.random,
                        DerelictShipEntityPlugin.getDefaultSModProb());
            SectorEntityToken derelict = BaseThemeGenerator.addSalvageEntity(Misc.random, system, Entities.WRECK, null, params);
            derelict.setDiscoverable(true);
            SalvageSpecialAssigner.assignSpecials(derelict);
            if (system.getStar() == null) {
                derelict.setFixedLocation(point.x, point.y);
            } else {
                derelict.setCircularOrbit(system.getStar(), MathUtils.randBetween(0f, 360f), point.length(), MathUtils.randBetween(30f, 90f));
            }
        }
    }

    private static final int pathSegments = 12;
    private static final float minSegmentLength = 2000f, maxSegmentLength = 3000f;

    private static void addBallisticCache(StarSystemAPI system, Map<String, Float> ballisticRarities) {
        Vector2f startPoint = MathUtils.randomPointInRing(new Vector2f(), 2500f, 3000f);

        List<Vector2f> pathPoints = new ArrayList<>();
        pathPoints.add(startPoint);

        for (int i = 0; i < pathSegments; i++) {
            Vector2f a = new Vector2f(pathPoints.get(i));
            float outwardsAngle = Misc.getAngleInDegrees(a);
            Vector2f l = Misc.getUnitVectorAtDegreeAngle(outwardsAngle + MathUtils.randBetween(-90f, 90f));
            l.scale(MathUtils.randBetween(minSegmentLength, maxSegmentLength));
            Vector2f.add(a, l, a);
            pathPoints.add(a);
        }

        for (int i = 0; i < pathPoints.size(); i++) {
            Vector2f point = pathPoints.get(i);
            if (i == pathPoints.size() - 1) {
                SectorEntityToken ballisticCache = BaseThemeGenerator.addSalvageEntity(system, "wpnxt_ballistic_cache", null, null);
                ballisticCache.setFixedLocation(point.x, point.y);
                ballisticCache.getMemoryWithoutUpdate().set(cacheKey, "BALLISTIC");
                ballisticCache.getMemoryWithoutUpdate().set(cacheFlagKey, true);
                populateCache(ballisticCache, 4, 120, 20, "BALLISTIC", ballisticRarities);
                Misc.setDefenderOverride(ballisticCache, new DefenderDataOverride(Factions.MERCENARY, 1f, 240f, 240f, 20));
            }
            else {
                WeightedRandomPicker<String> factions = new WeightedRandomPicker<>(Misc.random);
                factions.add(Factions.HEGEMONY, 10f);
                factions.add(Factions.LUDDIC_CHURCH, 8f);
                factions.add(Factions.INDEPENDENT, 4f);
                DerelictShipEntityPlugin.DerelictShipData params =
                        DerelictShipEntityPlugin.createRandom(
                                factions.pick(),
                                null,
                                Misc.random,
                                DerelictShipEntityPlugin.getDefaultSModProb());
                SectorEntityToken derelict = BaseThemeGenerator.addSalvageEntity(Misc.random, system, Entities.WRECK, null, params);
                derelict.setDiscoverable(true);
                SalvageSpecialAssigner.assignSpecials(derelict);
                derelict.setFixedLocation(point.x, point.y);
            }
        }
    }

    private static StarSystemAPI getRandomSystem(List<StarSystemAPI> systems) {
        return systems.get(Misc.random.nextInt(systems.size()));
    }

    public static Set<String> weaponTagsToSkip = new HashSet<>();

    static {
        weaponTagsToSkip.add(Tags.NO_BP_DROP);
        weaponTagsToSkip.add(Tags.NO_DROP);
        weaponTagsToSkip.add(Tags.HULLMOD_NO_DROP_SALVAGE);
        weaponTagsToSkip.add(Tags.RESTRICTED);
    }
    @SuppressWarnings("SameParameterValue")
    private static void separateRareWeaponBPs(
            Map<String, Float> ballisticRarities,
            Map<String, Float> energyRarities,
            Map<String, Float> missileRarities,
            float mediumWeaponRarityMultiplier,
            float largeWeaponRarityMultiplier) {
        for (WeaponSpecAPI spec : Global.getSettings().getAllWeaponSpecs()) {
            if (spec.getAIHints().contains(WeaponAPI.AIHints.SYSTEM)) continue;
            Set<String> tags = new HashSet<>(spec.getTags());
            if (!tags.contains(Items.TAG_RARE_BP)) continue;
            tags.retainAll(weaponTagsToSkip);
            if (!tags.isEmpty()) {
                continue;
            }
            float rarityMultiplier = WeaponAPI.WeaponSize.MEDIUM.equals(spec.getSize())
                    ? mediumWeaponRarityMultiplier
                    : WeaponAPI.WeaponSize.LARGE.equals(spec.getSize())
                    ? largeWeaponRarityMultiplier
                    : 1f;
            switch (spec.getType()) {
                case BALLISTIC: ballisticRarities.put(spec.getWeaponId(), spec.getRarity() * rarityMultiplier); break;
                case ENERGY: energyRarities.put(spec.getWeaponId(), spec.getRarity() * rarityMultiplier); break;
                case MISSILE: missileRarities.put(spec.getWeaponId(), spec.getRarity() * rarityMultiplier); break;
                default: break;
            }
        }
    }

    /** {@code weaponType} is one of BALLISTIC, ENERGY, or MISSILE. */
    @SuppressWarnings("SameParameterValue")
    private static void populateCache(SectorEntityToken cache, int uniqueWeaponCount, int randomWeaponCount, int randomBPCount, String weaponType, Map<String, Float> bpRarities) {
        String uniqueWeaponId = "BALLISTIC".equals(weaponType) ? "wpnxt_morphcannon" : "ENERGY".equals(weaponType) ? "wpnxt_energyballlauncher" : "wpnxt_phasetorpedo";
        SalvageEntityGenDataSpec.DropData uniqueWeaponDrop = new SalvageEntityGenDataSpec.DropData();
        uniqueWeaponDrop.addWeapon(uniqueWeaponId, 1f);
        uniqueWeaponDrop.chances = uniqueWeaponCount;
        cache.addDropRandom(uniqueWeaponDrop);
        SalvageEntityGenDataSpec.DropData randomWeapons = new SalvageEntityGenDataSpec.DropData();
        randomWeapons.chances = randomWeaponCount;
        randomWeapons.addCustom("wpn_:{weaponSize:SMALL,weaponType:" + weaponType + ",tags:[!no_drop,!restricted,!no_drop_salvage]}", 6f);
        randomWeapons.addCustom("wpn_:{weaponSize:MEDIUM,weaponType:" + weaponType + ",tags:[!no_drop,!restricted,!no_drop_salvage]}", 3f);
        randomWeapons.addCustom("wpn_:{weaponSize:LARGE,weaponType:" + weaponType + ",tags:[!no_drop,!restricted,!no_drop_salvage]}", 1f);
        cache.addDropRandom(randomWeapons);
        SalvageEntityGenDataSpec.DropData randomBPs = new SalvageEntityGenDataSpec.DropData();
        randomBPs.chances = randomBPCount;
        for (Map.Entry<String, Float> entry : bpRarities.entrySet()) {
            randomBPs.addCustom("item_weapon_bp:" + entry.getKey(), entry.getValue());
        }
        cache.addDropRandom(randomBPs);
    }
}