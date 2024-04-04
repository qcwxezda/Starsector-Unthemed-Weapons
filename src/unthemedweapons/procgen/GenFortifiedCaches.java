package unthemedweapons.procgen;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.impl.campaign.ids.*;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.impl.campaign.procgen.DefenderDataOverride;
import com.fs.starfarer.api.impl.campaign.procgen.DropGroupRow;
import com.fs.starfarer.api.impl.campaign.procgen.SalvageEntityGenDataSpec;
import com.fs.starfarer.api.impl.campaign.procgen.themes.BaseThemeGenerator;
import com.fs.starfarer.api.loading.FighterWingSpecAPI;
import com.fs.starfarer.api.loading.WeaponSpecAPI;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.WeightedRandomPicker;
import org.lwjgl.util.vector.Vector2f;
import unthemedweapons.util.MathUtils;

import java.util.*;

public abstract class GenFortifiedCaches {
    public static final String fortifiedFlag = "$wpnxt_fortifiedCacheFlag";
    public static final String numCreditsKey = "$wpnxt_creditsAmount";
    public static final String modifyKey = "wpnxt_fortifiedCache";

    public static final Set<String> tagsToSkip = new HashSet<>();
    static {
        tagsToSkip.add(Tags.THEME_CORE);
        tagsToSkip.add(Tags.THEME_HIDDEN);
    }

    public static float minCacheSize = 27f, maxCacheSize = 60f;
    public static float mediumSizeThreshold = 30f, largeSizeThreshold = 40f, hugeSizeThreshold = 50f;
    public static float maxCacheSizeXPMultiplier = 20f;
    public static float baseCacheValue = 5000f, maxCacheSizeValueMultiplier = 79f;
    public static float systemWeightPenaltyPerCache = 0.3f;
    public static String getCacheStringBySize(CacheSize size) {
        switch (size) {
            case SMALL:
                return "Small Fortified Cache";
            case LARGE:
                return "Large Fortified Cache";
            case HUGE:
                return "Huge Fortified Cache";
            default: return "Fortified Cache";
        }
    }

    public static WeightedRandomPicker<StarSystemAPI> getSystemPicker(Random random) {
        WeightedRandomPicker<StarSystemAPI> systemPicker = new WeightedRandomPicker<>(random);
        for (StarSystemAPI system : Global.getSector().getStarSystems()) {
            Set<String> blacklist = new HashSet<>(tagsToSkip);
            blacklist.retainAll(system.getTags());
            if (!blacklist.isEmpty()) {
                continue;
            }
            // Certain things should make fortified caches more or less likely to appear:
            //   -- THEME_INTERESTING or THEME_INTERESTING_MINOR: more likely
            //   -- THEME_REMNANT_MAIN or THEME_REMNANT_SECONDARY: less likely
            //   -- Dangerous primary star (neutron star, black hole): more likely
            //   -- Presence of habitable world(s): less likely

            float weight = 1f;
            if (system.hasTag(Tags.THEME_INTERESTING)) {
                weight *= 2f;
            }
            else if (system.hasTag(Tags.THEME_INTERESTING_MINOR)) {
                weight *= 1.5f;
            }

            if (system.hasTag(Tags.THEME_REMNANT_MAIN)) {
                weight *= 0.2f;
            }
            else if (system.hasTag(Tags.THEME_REMNANT_SECONDARY)) {
                weight *= 0.5f;
            }

            PlanetAPI primaryStar = system.getStar();
            // Nebula
            if (primaryStar == null) {
                weight *= 0.5f;
            }
            // Black hole
            else if (primaryStar.getSpec().isBlackHole()) {
                weight *= 2f;
            }
            // Pulsar
            else if (primaryStar.getSpec().isPulsar()) {
                weight *= 1.5f;
            }

            // Check for habitable worlds, 0.75x multiplicative penalty for each one
            for (PlanetAPI planet : system.getPlanets()) {
                if (planet.hasCondition(Conditions.HABITABLE)) {
                    weight *= 0.75f;
                }
            }

            // Check for existing fortified caches; 30% penalty each
            for (SectorEntityToken entity : system.getCustomEntities()) {
                if ("wpnxt_fortified_cache".equals(entity.getCustomEntitySpec().getId())) {
                    weight *= (1f - systemWeightPenaltyPerCache);
                }
            }

            // Favor worlds with fewer existing custom entities in general
            weight *= 1f / Math.max(1f, (float) Math.sqrt(system.getCustomEntities().size()));

            systemPicker.add(system, weight);
        }
        return systemPicker;
    }

    public static WeightedRandomPicker<FactionAPI> getFactionPicker(Random random) {
        WeightedRandomPicker<FactionAPI> factionPicker = new WeightedRandomPicker<>(random);
        for (FactionAPI faction : Global.getSector().getAllFactions()) {
            if (faction.isPlayerFaction()) continue;
            if (!faction.isShowInIntelTab()) continue;

            factionPicker.add(faction);
        }
        return factionPicker;
    }

    public static void addCache(StarSystemAPI system, WeightedRandomPicker<FactionAPI> factionPicker) {
        // 17.4% chance of small cache
        // 45.9% chance of medium cache
        // 27.5% chance of large cache
        // 9.2% chance of huge cache
        float size = Math.min(MathUtils.randBetween(minCacheSize, maxCacheSize), MathUtils.randBetween(minCacheSize, maxCacheSize));
        CacheSize cacheSize = CacheSize.getSize(size);
        CustomCampaignEntityAPI cache = (CustomCampaignEntityAPI) BaseThemeGenerator.addSalvageEntity(system, "wpnxt_fortified_cache",null, null);
        cache.getMemoryWithoutUpdate().set(fortifiedFlag, true);
        cache.getMemoryWithoutUpdate().set(numCreditsKey, 0f);
        cache.setRadius(size);
        float sizeFrac = (size - minCacheSize) / (maxCacheSize - minCacheSize);
        // Double the detection range at max size
        cache.getDetectedRangeMod().modifyMult(modifyKey, 1f + sizeFrac);
        // Bigger caches get more salvage XP
        cache.setSalvageXP(cache.getSalvageXP() * (1f + maxCacheSizeXPMultiplier*sizeFrac*sizeFrac));
        cache.setName(getCacheStringBySize(CacheSize.getSize(size)));

        WeightedRandomPicker<LocationType> locationPicker = new WeightedRandomPicker<>();
        // If the system has stars and/or planets
        if (system.getPlanets() != null && !system.getPlanets().isEmpty()) {
            locationPicker.add(LocationType.ORBITING_STAR_OR_PLANET, 3f);
        }
        // If the system has any asteroids
        if (system.getAsteroids() != null && !system.getAsteroids().isEmpty()) {
            locationPicker.add(LocationType.INSIDE_ASTEROID_FIELD, 1f);
        }
        // If the system has any stable locations
        List<SectorEntityToken> stableLocations = system.getEntitiesWithTag(Tags.STABLE_LOCATION);
        if (stableLocations != null && !stableLocations.isEmpty()) {
            locationPicker.add(LocationType.NEAR_STABLE_LOCATION, 2f);
        }
        // If the system has any jump points
        if (system.getJumpPoints() != null && !system.getJumpPoints().isEmpty()) {
            locationPicker.add(LocationType.ORBITING_JUMP_POINT, 2f);
        }
        // Random fixed location as a fallback guaranteed option
        locationPicker.add(LocationType.RANDOM_FIXED_LOCATION, 1f);

        LocationType locationType = locationPicker.pick();

        Vector2f fixedLocation;
        float orbitRadius;
        // Place the cache at a desired location
        switch (locationType) {
            case ORBITING_STAR_OR_PLANET:
                PlanetAPI planet = system.getPlanets().get(Misc.random.nextInt(system.getPlanets().size()));
                orbitRadius = MathUtils.randBetween(planet.getRadius() * 1.5f, planet.getRadius() * 3f);
                cache.setCircularOrbit(planet, MathUtils.randBetween(0f, 360f), orbitRadius, MathUtils.randBetween(30f, 90f) / 100f * Math.min(100f, orbitRadius));
                break;
            case INSIDE_ASTEROID_FIELD:
                SectorEntityToken asteroid = system.getAsteroids().get(Misc.random.nextInt(system.getAsteroids().size()));
                // Fixed location near the selected asteroid
                fixedLocation = MathUtils.randomPointInRing(asteroid.getLocation(), asteroid.getRadius(), 300f + asteroid.getRadius());
                cache.setFixedLocation(fixedLocation.x, fixedLocation.y);
                break;
            case NEAR_STABLE_LOCATION:
                assert stableLocations != null;
                SectorEntityToken stableLocation = stableLocations.get(Misc.random.nextInt( stableLocations.size()));
                // Fixed location near the selected stable location
                orbitRadius = MathUtils.randBetween(stableLocation.getRadius() + 100f, stableLocation.getRadius() + 400f);
                cache.setCircularOrbit(stableLocation, MathUtils.randBetween(0f, 360f), orbitRadius, 50000f);
                break;
            case ORBITING_JUMP_POINT:
                SectorEntityToken jumpPoint = system.getJumpPoints().get(Misc.random.nextInt(system.getJumpPoints().size()));
                orbitRadius = MathUtils.randBetween(jumpPoint.getRadius() + 100f, jumpPoint.getRadius() + 400f);
                cache.setCircularOrbit(jumpPoint, MathUtils.randBetween(0f, 360f), orbitRadius, MathUtils.randBetween(30f, 90f));
            case RANDOM_FIXED_LOCATION:
                // Shouldn't be farther away from the center of the system than the farthest already-existing entity,
                // or a certain fixed value
                Vector2f center = system.getStar() == null ? new Vector2f() : system.getStar().getLocation();
                float farthestEntityDist = 1000f;
                if (system.getAllEntities() != null) {
                    for (SectorEntityToken entity : system.getAllEntities()) {
                        float dist = Misc.getDistance(center, entity.getLocation());
                        if (dist > farthestEntityDist) {
                            farthestEntityDist = dist;
                        }
                    }
                }
                // Don't be farther than 6k units away from center
                farthestEntityDist = Math.min(farthestEntityDist, 6000f);
                fixedLocation = MathUtils.randomPointInRing(center, system.getStar() == null ? 0f : system.getStar().getRadius() * 1.5f, farthestEntityDist);
                cache.setFixedLocation(fixedLocation.x, fixedLocation.y);
                break;
        }

        // Strength from 20 to 320 DP
        float defenderStrength = 20f * (1f + (float)Math.pow(sizeFrac, 1.25f)*15f);
        DefenderDataOverride ddo = new DefenderDataOverride(factionPicker.pick().getId(), 1f, defenderStrength * 0.8f, defenderStrength * 1.2f, 30);
        switch (cacheSize) {
            case SMALL:
            case MEDIUM:
                ddo.maxDefenderSize = 2;
                break;
            case LARGE:
                ddo.maxDefenderSize = 3;
                break;
            case HUGE:
                ddo.maxDefenderSize = 1000;
                break;
        }
        Misc.setDefenderOverride(cache, ddo);
        // Doesn't seem to do anything
//            SalvageSpecialAssigner.assignSpecials(cache);
    }

    public static void initialize(int numCaches) {
        WeightedRandomPicker<StarSystemAPI> systemPicker = getSystemPicker(Misc.random);
        WeightedRandomPicker<FactionAPI> factionPicker = getFactionPicker(Misc.random);

        for (int i = 0; i < numCaches; i++) {
            StarSystemAPI system = systemPicker.pick();
            // After picking a system, add it back but reduce its weight by 30%
            // Obviously this is faster than calling getSystemPicker() each iteration
            float weight = systemPicker.getWeight(system);
            systemPicker.remove(system);
            systemPicker.add(system, weight * (1f - systemWeightPenaltyPerCache));

            addCache(system, factionPicker);
        }
    }

    /** This should be called when the cache is about to be looted, so that the save file isn't littered with random DGRow entries. */
    public static void generateLootForCache(SectorEntityToken cache, Random random) {
        float size = cache.getRadius();
        CacheSize cacheSize = CacheSize.getSize(size);
        float sizeFrac = (size - minCacheSize) / (maxCacheSize - minCacheSize);
        // Populate the cache with items
        WeightedRandomPicker<CacheTheme> cacheThemePicker = new WeightedRandomPicker<>(random);
        cacheThemePicker.add(CacheTheme.CREDITS, 2.5f);
        cacheThemePicker.add(CacheTheme.COMMODITIES_BASIC, 1f);
        cacheThemePicker.add(CacheTheme.COMMODITIES_GOODS, 0.75f);
        cacheThemePicker.add(CacheTheme.COMMODITIES_SUPPLY, 1.5f);
        cacheThemePicker.add(CacheTheme.COMMODITIES_EXTENDED, 0.75f);
        cacheThemePicker.add(CacheTheme.BALLISTIC_WEAPONS, 1f);
        cacheThemePicker.add(CacheTheme.ENERGY_WEAPONS, 1f);
        cacheThemePicker.add(CacheTheme.MISSILE_WEAPONS, 1f);
        cacheThemePicker.add(CacheTheme.FIGHTER_LPCS, 2f);
        // Only medium-size caches and up can have BPs and special items
        if (cacheSize.getSizeOrdinal() >= 1) {
            cacheThemePicker.add(CacheTheme.WEAPON_BLUEPRINTS, 1f);
            cacheThemePicker.add(CacheTheme.FIGHTER_BLUEPRINTS, 1f);
            cacheThemePicker.add(CacheTheme.SHIP_BLUEPRINTS, 1f);
            cacheThemePicker.add(CacheTheme.AI_CORES, 1.5f);
        }
        // Only large-size caches and up can have rare tech
        if (cacheSize.getSizeOrdinal() >= 2) {
            cacheThemePicker.add(CacheTheme.RARE_TECH, 1.5f);
        }

        int numRolls = cacheSize.getSizeOrdinal() + 1;
        // Huge caches get an additional "free" roll
        for (int j = 0; j < numRolls + (cacheSize == CacheSize.HUGE ? 1 : 0); j++) {
            CacheTheme cacheTheme = cacheThemePicker.pick();
            float cacheValue = baseCacheValue
                    * (1f + sizeFrac * sizeFrac * maxCacheSizeValueMultiplier)
                    * MathUtils.randBetween(0.8f, 1.2f, random) / numRolls;
            if (cacheTheme == CacheTheme.CREDITS) {
                Float curValue = (Float) cache.getMemoryWithoutUpdate().get(numCreditsKey);
                cache.getMemoryWithoutUpdate().set(numCreditsKey, curValue == null ? cacheValue : cacheValue + curValue);
            } else {
                WeightedRandomPicker<DropGroupRow> picker = getPickerForCache(cacheSize, cacheTheme);
                SalvageEntityGenDataSpec.DropData data = new SalvageEntityGenDataSpec.DropData();

                boolean dropValue = false;
                switch (cacheTheme) {
                    case COMMODITIES_BASIC:
                    case COMMODITIES_GOODS:
                    case COMMODITIES_SUPPLY:
                    case COMMODITIES_EXTENDED:
                        // Multiply commodity value by 1.75 since most are trash
                        data.value = (int) (cacheValue * 1.75f);
                        dropValue = true;
                        break;
                    case BALLISTIC_WEAPONS:
                    case ENERGY_WEAPONS:
                    case MISSILE_WEAPONS:
                        // Give anywhere from 15 to 105 weapons (divided by number of rolls)
                        data.chances = (int) Math.ceil(15f * (1f + sizeFrac * sizeFrac * 6f) * MathUtils.randBetween(0.8f, 1.2f, random)) / numRolls;
                        break;
                    case FIGHTER_LPCS:
                        // Give anywhere from 8 to 80 LPCs (divided by number of rolls)
                        data.chances = (int) Math.ceil(8f * (1f + sizeFrac * sizeFrac * 9f) * MathUtils.randBetween(0.8f, 1.2f, random)) / numRolls;
                        break;
                    case WEAPON_BLUEPRINTS:
                        // Give anywhere from 4 to 32 BPs (divided by number of rolls)
                        data.chances = (int) Math.ceil(4f * (1f + sizeFrac * sizeFrac * 7f) * MathUtils.randBetween(0.8f, 1.2f, random)) / numRolls;
                        break;
                    case FIGHTER_BLUEPRINTS:
                        // Give anywhere from 3 to 27 BPS (divided by number of rolls)
                        data.chances = (int) Math.ceil(3f * (1f + sizeFrac * sizeFrac * 8f) * MathUtils.randBetween(0.8f, 1.2f, random)) / numRolls;
                        break;
                    case SHIP_BLUEPRINTS:
                        // Give anywhere from 3 to 15 BPs (divided by number of rolls)
                        data.chances = (int) Math.ceil(3f * (1f + sizeFrac * sizeFrac * 4f) * MathUtils.randBetween(0.8f, 1.2f, random)) / numRolls;
                        break;
                    case AI_CORES:
                        // Give anywhere from 4 to 20 cores (divided by number of rolls)
                        data.chances = (int) Math.ceil(4f * (1f + sizeFrac * sizeFrac * 4f) * MathUtils.randBetween(0.8f, 1.2f, random)) / numRolls;
                        break;
                    case RARE_TECH:
                        // Give anywhere from 1 to 3 items (divided by number of rolls, so basically always 1)
                        data.chances = (int) Math.ceil(1f * (1f + sizeFrac * sizeFrac * 2f) * MathUtils.randBetween(0.8f, 1.2f, random)) / numRolls;
                        break;
                }

                data.initCustom();
                data.getCustom().addAll(picker);

                if (dropValue) {
                    cache.addDropValue(data);
                } else {
                    cache.addDropRandom(data);
                }
            }
        }
    }

    public static WeightedRandomPicker<DropGroupRow> getDropGroupClearNothing(String dropGroupId) {
        WeightedRandomPicker<DropGroupRow> picker = DropGroupRow.getPicker(dropGroupId);
        List<DropGroupRow> toRemove = new ArrayList<>();
        for (DropGroupRow row : picker.getItems()) {
            if (row.isNothing()) {
                toRemove.add(row);
            }
        }
        for (DropGroupRow row : toRemove) {
            picker.remove(row);
        }

        return picker;
    }

    public static WeightedRandomPicker<DropGroupRow> getPickerForCache(CacheSize size, CacheTheme theme) {
        // Credits caches don't contain any items
        if (theme == CacheTheme.CREDITS) {
            return null;
        }

        WeightedRandomPicker<DropGroupRow> picker = new WeightedRandomPicker<>();
        float packageChance;
        switch (theme) {
            case COMMODITIES_BASIC:
                picker.addAll(getDropGroupClearNothing("basic"));
                break;
            case COMMODITIES_GOODS:
                picker.addAll(getDropGroupClearNothing("goods"));
                break;
            case COMMODITIES_SUPPLY:
                picker.addAll(getDropGroupClearNothing("supply"));
                break;
            case COMMODITIES_EXTENDED:
                picker.addAll(getDropGroupClearNothing("extended"));
                break;
            case BALLISTIC_WEAPONS: case ENERGY_WEAPONS: case MISSILE_WEAPONS:
                String weaponType;
                if (theme == CacheTheme.BALLISTIC_WEAPONS) {
                    weaponType = "BALLISTIC";
                }
                else if (theme == CacheTheme.ENERGY_WEAPONS) {
                    weaponType = "ENERGY";
                }
                else {
                    weaponType = "MISSILE";
                }

                picker.add(new DropGroupRow("wpn_:{weaponSize:SMALL,weaponType:" + weaponType + ",tags:[!no_drop,!restricted,!no_drop_salvage]}", "", 1f), 1f);
                if (size.getSizeOrdinal() >= 1) {
                    picker.add(new DropGroupRow("wpn_:{weaponSize:MEDIUM,weaponType:" + weaponType + ",tags:[!no_drop,!restricted,!no_drop_salvage]}", "", 0.5f), 0.5f);
                }
                if (size.getSizeOrdinal() >= 2) {
                    picker.add(new DropGroupRow("wpn_:{weaponSize:LARGE,weaponType:" + weaponType + ",tags:[!no_drop,!restricted,!no_drop_salvage]}", "", 0.25f), 0.25f);
                }
                if (size.getSizeOrdinal() >= 3) {
                    picker.add(new DropGroupRow("wpn_:{weaponType:" + weaponType + ",tags:[!no_drop,!restricted,!no_drop_salvage,rare_bp]}", "", 0.2f), 0.2f);
                }
                break;
            case FIGHTER_LPCS:
                // Fighter LPCs sorted by OP cost
                // Small only gives 0-5 OP fighters
                // Medium gives 0-10 OP fighters
                // Large gives 0-20 OP fighters
                // Huge gives anything
                for (FighterWingSpecAPI spec : Global.getSettings().getAllFighterWingSpecs()) {
                    if (spec.hasTag(Tags.NO_DROP) || spec.hasTag(Tags.HULLMOD_NO_DROP_SALVAGE) || spec.hasTag(Tags.RESTRICTED)) continue;
                    float op = spec.getOpCost(null);
                    boolean shouldAdd = false;
                    if (op <= 5f) {
                        shouldAdd = true;
                    }
                    else if (op <= 10f && size.getSizeOrdinal() >= 1) {
                        shouldAdd = true;
                    }
                    else if (op <= 20f && size.getSizeOrdinal() >= 2) {
                        shouldAdd = true;
                    }
                    else if (size.getSizeOrdinal() >= 3) {
                        shouldAdd = true;
                    }
                    if (shouldAdd) {
                        picker.add(new DropGroupRow("ftr_" + spec.getId(), "", 1f));
                    }
                }
//                picker.add(new DropGroupRow("ftr_:{tags:[wpnxt_fighterUnder5Op, !no_drop, !restricted, !no_drop_salvage]}", "", 1f));
//                if (CacheSize.getSizeOrdinal(size) >= 1) {
//                    picker.add(new DropGroupRow("ftr_:{tags:[wpnxt_fighter5To10Op, !no_drop, !restricted, !no_drop_salvage]}", "", 1f));
//
//                }
//                if (CacheSize.getSizeOrdinal(size) >= 2) {
//                    picker.add(new DropGroupRow("ftr_:{tags:[wpnxt_fighter10To20Op, !no_drop, !restricted, !no_drop_salvage]}", "", 1f));
//                }
//                if (CacheSize.getSizeOrdinal(size) >= 3) {
//                    picker.add(new DropGroupRow("ftr_:{tags:[wpnxt_fighterOver20Op, !no_drop, !restricted, !no_drop_salvage]}", "", 0.5f));
//                }
                break;
            case WEAPON_BLUEPRINTS:
                // Small caches can't have blueprints
                if (size.getSizeOrdinal() <= 0) {
                    return null;
                }
                // Weapon blueprints:
                //  - Medium gives small and medium BPs
                //  - Large gives any weapon BPs, large weapon BPs are rare
                //  - Huge gives any weapon BPs, large weapon BPs are more common
                for (WeaponSpecAPI spec : Global.getSettings().getAllWeaponSpecs()) {
                    if (spec.hasTag(Tags.NO_DROP) || spec.hasTag(Tags.RESTRICTED) || !spec.hasTag(Items.TAG_RARE_BP)) continue;
                    if (spec.getAIHints().contains(WeaponAPI.AIHints.SYSTEM)) continue;
                    switch (spec.getSize()) {
                        case SMALL:
                        case MEDIUM:
                            picker.add(new DropGroupRow("item_weapon_bp:" + spec.getWeaponId(), "", spec.getRarity()), spec.getRarity());
                            break;
                        case LARGE:
                            if (size.getSizeOrdinal() >= 3) {
                                picker.add(new DropGroupRow("item_weapon_bp:" + spec.getWeaponId(), "", spec.getRarity()), spec.getRarity());
                            }
                            else if (size.getSizeOrdinal() == 2) {
                                picker.add(new DropGroupRow("item_weapon_bp:" + spec.getWeaponId(), "", 0.5f * spec.getRarity()), 0.5f * spec.getRarity());
                            }
                            break;
                    }
                }
                // All BP rolls have a small chance to give a package BP
                packageChance = picker.getTotal() * 0.025f;
                picker.add(new DropGroupRow("item_:{tags:[package_bp, !no_drop, !restricted]}","",packageChance), packageChance);
//                picker.add(new DropGroupRow("item_weapon_bp:{tags:[wpnxt_weaponSmall, rare_bp, !no_drop, !restricted]}", "", 1f));
//                if (CacheSize.getSizeOrdinal(size) >= 2) {
//                    picker.add(new DropGroupRow("item_weapon_bp:{tags:[wpnxt_weaponMedium, rare_bp, !no_drop, !restricted]}", "", 1f));
//                }
//                if (CacheSize.getSizeOrdinal(size) >= 3) {
//                    picker.add(new DropGroupRow("item_weapon_bp:{tags:[wpnxt_weaponLarge, rare_bp, !no_drop, !restricted]}", "", 1f));
//                }
                break;
            case FIGHTER_BLUEPRINTS:
                // Small caches can't have blueprints
                if (size.getSizeOrdinal() <= 0) {
                    return null;
                }
                // Fighter blueprints:
                //  - Medium gives 0-10 OP BPs
                //  - Large gives 0-20 OP BPs
                //  - Huge give any BPs
                for (FighterWingSpecAPI spec : Global.getSettings().getAllFighterWingSpecs()) {
                    if (spec.hasTag(Tags.NO_DROP) || spec.hasTag(Tags.RESTRICTED) || !spec.hasTag(Items.TAG_RARE_BP)) continue;
                    float op = spec.getOpCost(null);
                    if (op <= 10f) {
                        picker.add(new DropGroupRow("item_fighter_bp:" + spec.getId(), "", spec.getRarity()), spec.getRarity());
                    }
                    else if (op <= 20f && size.getSizeOrdinal() >= 2) {
                        picker.add(new DropGroupRow("item_fighter_bp:" + spec.getId(), "", spec.getRarity()), spec.getRarity());
                    }
                    else if (size.getSizeOrdinal() >= 3) {
                        picker.add(new DropGroupRow("item_fighter_bp:" + spec.getId(), "", spec.getRarity()), spec.getRarity());
                    }
                }
                // All BP rolls have a small chance to give a package BP
                packageChance = picker.getTotal() * 0.025f;
                picker.add(new DropGroupRow("item_:{tags:[package_bp, !no_drop, !restricted]}","",packageChance), packageChance);
//                picker.add(new DropGroupRow("item_fighter_bp:{tags:[wpnxt_fighterUnder5Op, rare_bp, !no_drop, !restricted]}", "", 1f));
//                picker.add(new DropGroupRow("item_fighter_bp:{tags:[wpnxt_fighter5To10Op, rare_bp, !no_drop, !restricted]}", "", 1f));
//                if (CacheSize.getSizeOrdinal(size) >= 2) {
//                    picker.add(new DropGroupRow("item_fighter_bp:{tags:[wpnxt_fighter10To20Op, rare_bp, !no_drop, !restricted]}", "", 1f));
//                }
//                if (CacheSize.getSizeOrdinal(size) >= 3) {
//                    picker.add(new DropGroupRow("item_fighter_bp:{tags:[wpnxt_fighterOver20Op, rare_bp, !no_drop, !restricted]}", "", 1f));
//                }
                break;
            case SHIP_BLUEPRINTS:
                // Small caches can't have blueprints
                if (size.getSizeOrdinal() <= 0) {
                    return null;
                }
                // Ship blueprints:
                //  - Medium gives up to destroyer
                //  - Large gives up to cruiser
                //  - Huge gives up to capital
                for (ShipHullSpecAPI spec : Global.getSettings().getAllShipHullSpecs()) {
                    if (!spec.isBaseHull()) continue;
                    if (!spec.hasTag(Items.TAG_RARE_BP) || spec.hasTag(Tags.NO_DROP) || spec.hasTag(Tags.RESTRICTED)) continue;
                    switch (spec.getHullSize()) {
                        case DEFAULT:
                        case FIGHTER:
                            break;
                        case FRIGATE:
                        case DESTROYER:
                            picker.add(new DropGroupRow("item_ship_bp:" + spec.getBaseHullId(), "", spec.getRarity()), spec.getRarity());
                            break;
                        case CRUISER:
                            if (size.getSizeOrdinal() >= 2) {
                                picker.add(new DropGroupRow("item_ship_bp:" + spec.getBaseHullId(), "", spec.getRarity()), spec.getRarity());
                            }
                            else {
                                picker.add(new DropGroupRow("item_ship_bp:" + spec.getBaseHullId(), "", 0.25f * spec.getRarity()), 0.25f * spec.getRarity());
                            }
                            break;
                        case CAPITAL_SHIP:
                            if (size.getSizeOrdinal() >= 3) {
                                picker.add(new DropGroupRow("item_ship_bp:" + spec.getBaseHullId(), "", spec.getRarity()), spec.getRarity());
                            }
                            else if (size.getSizeOrdinal() >= 2) {
                                picker.add(new DropGroupRow("item_ship_bp:" + spec.getBaseHullId(), "", 0.25f * spec.getRarity()), 0.25f * spec.getRarity());
                            }
                            break;
                    }
                }
                // All BP rolls have a small chance to give a package BP
                packageChance = picker.getTotal() * 0.025f;
                picker.add(new DropGroupRow("item_:{tags:[package_bp, !no_drop, !restricted]}","",packageChance), packageChance);
//                picker.add(new DropGroupRow("item_ship_bp:{tags:[wpnxt_frigate, rare_bp, !no_drop, !restricted]}", "", 1f));
//                picker.add(new DropGroupRow("item_ship_bp:{tags:[wpnxt_destroyer, rare_bp, !no_drop, !restricted]}", "", 1f));
//                if (CacheSize.getSizeOrdinal(size) >= 2) {
//                    picker.add(new DropGroupRow("item_ship_bp:{tags:[wpnxt_cruiser, rare_bp, !no_drop, !restricted]}", "", 1f));
//                }
//                if (CacheSize.getSizeOrdinal(size) >= 3) {
//                    picker.add(new DropGroupRow("item_ship_bp:{tags:[wpnxt_capital, rare_bp, !no_drop, !restricted]}", "", 1f));
//                }
                break;
            case AI_CORES:
                // Small caches can't have special items
                if (size.getSizeOrdinal() <= 0) {
                    return null;
                }
                WeightedRandomPicker<DropGroupRow> aiCorePicker = getDropGroupClearNothing("ai_cores3");
                for (DropGroupRow row : aiCorePicker.getItems()) {
                    if (Commodities.ALPHA_CORE.equals(row.getCommodity())) {
                        row.setFreq(size.getSizeOrdinal() >= 3 ? 1.5f : size.getSizeOrdinal() >= 2 ? 0.5f : 0.05f);
                    }
                    else if (Commodities.BETA_CORE.equals(row.getCommodity())) {
                        row.setFreq(size.getSizeOrdinal() >= 2 ? 1f : 0.6f);
                    }
                    else if (Commodities.GAMMA_CORE.equals(row.getCommodity())) {
                        row.setFreq(1f);
                    }
                }
                picker.addAll(aiCorePicker);
                break;
            case RARE_TECH:
                // Small and medium caches can't have rare tech
                if (size.getSizeOrdinal() <= 1) {
                    return null;
                }
                picker.addAll(getDropGroupClearNothing("rare_tech"));
                break;
        }

        return picker;
    }

    public enum LocationType {
        ORBITING_STAR_OR_PLANET,
        INSIDE_ASTEROID_FIELD,
        NEAR_STABLE_LOCATION,
        ORBITING_JUMP_POINT,
        RANDOM_FIXED_LOCATION
    }

    public enum CacheTheme {
        CREDITS,
        COMMODITIES_BASIC,
        COMMODITIES_GOODS,
        COMMODITIES_SUPPLY,
        COMMODITIES_EXTENDED,
        BALLISTIC_WEAPONS,
        ENERGY_WEAPONS,
        MISSILE_WEAPONS,
        FIGHTER_LPCS,
        WEAPON_BLUEPRINTS,
        FIGHTER_BLUEPRINTS,
        SHIP_BLUEPRINTS,
        AI_CORES,
        RARE_TECH
    }

    public enum CacheSize {
        SMALL,
        MEDIUM,
        LARGE,
        HUGE;

        public static CacheSize getSize(float cacheRadius) {
            if (cacheRadius >= hugeSizeThreshold) {
                return HUGE;
            }
            if (cacheRadius >= largeSizeThreshold) {
                return LARGE;
            }
            if (cacheRadius >= mediumSizeThreshold) {
                return MEDIUM;
            }
            return SMALL;
        }

        public int getSizeOrdinal() {
            switch (this) {
                case SMALL:
                    return 0;
                case LARGE:
                    return 2;
                case HUGE:
                    return 3;
                default: return 1;
            }
        }
    }
}
