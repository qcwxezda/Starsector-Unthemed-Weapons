package unthemedweapons.procgen;

import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.BaseGenericPlugin;
import com.fs.starfarer.api.impl.campaign.fleets.DefaultFleetInflater;
import com.fs.starfarer.api.impl.campaign.fleets.DefaultFleetInflaterParams;
import com.fs.starfarer.api.impl.campaign.fleets.FleetFactoryV3;
import com.fs.starfarer.api.impl.campaign.fleets.FleetParamsV3;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.ids.Personalities;
import com.fs.starfarer.api.impl.campaign.ids.Skills;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.impl.campaign.rulecmd.salvage.SalvageGenFromSeed;
import com.fs.starfarer.api.impl.campaign.rulecmd.salvage.SalvageGenFromSeed.SDMParams;
import unthemedweapons.util.CampaignUtils;
import unthemedweapons.util.MathUtils;

import java.util.*;

public class CacheDefenderPlugin extends BaseGenericPlugin implements SalvageGenFromSeed.SalvageDefenderModificationPlugin {

    public static String specialFleetKey = "$wpnxt_specialFleet";
    public static String cacheDefenseFleetKey = "$wpnxt_cacheDefenseFleet";

    @Override
    public float getStrength(SDMParams p, float strength, Random random, boolean withOverride) {
        return strength;
    }

    @Override
    public float getProbability(SDMParams p, float probability, Random random, boolean withOverride) {
        return probability;
    }

    @Override
    public float getQuality(SDMParams p, float quality, Random random, boolean withOverride) {
        return quality;
    }

    @Override
    public float getMaxSize(SDMParams p, float maxSize, Random random, boolean withOverride) {
        return maxSize;
    }

    @Override
    public float getMinSize(SDMParams p, float minSize, Random random, boolean withOverride) {
        return minSize;
    }

    @Override
    public void modifyFleet(SDMParams p, CampaignFleetAPI fleet, Random random, boolean withOverride) {

        if (p.entity == null) return;

        fleet.setName("Cache Defenses");
        fleet.setNoFactionInName(true);
        fleet.getFleetData().setShipNameRandom(random);
        // Set a flag so that we can consume the BeginFleetEncounter trigger
        fleet.getMemoryWithoutUpdate().set(cacheDefenseFleetKey, true);

        Object cacheValue = p.entity.getMemoryWithoutUpdate().get(GenSpecialCaches.cacheKey);

        if (cacheValue != null) {
            fleet.getFleetData().clear();
            switch ((String) cacheValue) {
                case "BALLISTIC":
                    generateFleetForBallisticCache(fleet, random);
                    break;
                case "ENERGY":
                    generateFleetForEnergyCache(fleet, random);
                    break;
                case "MISSILE":
                    generateFleetForMissileCache(fleet, random);
                    break;
                default:
                    break;
            }
            fleet.getMemoryWithoutUpdate().set(specialFleetKey, true);

            CampaignUtils.finalizeFleet(fleet, Arrays.asList(Tags.TAG_NO_AUTOFIT, Tags.VARIANT_ALWAYS_RETAIN_SMODS_ON_SALVAGE, Tags.TAG_RETAIN_SMODS_ON_RECOVERY));
        }
        else {
            // Generic defense fleet
            float size = p.entity.getRadius();
            float sizeFrac = (size - GenFortifiedCaches.minCacheSize) / (GenFortifiedCaches.maxCacheSize - GenFortifiedCaches.minCacheSize);

//            // Remove capitals from < huge crates, cruisers from < large crates, and destroyers from < medium crates
//            List<FleetMemberAPI> toRemove = new ArrayList<>();
//            for (FleetMemberAPI member : fleet.getFleetData().getMembersListCopy()) {
//                switch (member.getVariant().getHullSize()) {
//                    case DEFAULT:
//                    case FIGHTER:
//                    case FRIGATE:
//                        break;
//                    case DESTROYER:
//                        if (GenFortifiedCaches.CacheSize.getSizeOrdinal(cacheSize) < 1) {
//                            toRemove.add(member);
//                        }
//                        break;
//                    case CRUISER:
//                        if (GenFortifiedCaches.CacheSize.getSizeOrdinal(cacheSize) < 2) {
//                            toRemove.add(member);
//                        }
//                        break;
//                    case CAPITAL_SHIP:
//                        if (GenFortifiedCaches.CacheSize.getSizeOrdinal(cacheSize) < 3) {
//                            toRemove.add(member);
//                        }
//                        break;
//                }
//            }
//
//            float lostFP = 0f;
//            for (FleetMemberAPI member : toRemove) {
//                // Don't remove the last fleet member
//                if (fleet.getNumShips() > 1) {
//                    lostFP += member.getFleetPointCost();
//                    fleet.getFleetData().removeFleetMember(member);
//                }
//            }
//
//
            FleetParamsV3 fleetParams = new FleetParamsV3();
//
//            // Replace the lost FP with the correct size ships
//            if (lostFP >= 1f) {
//                switch (cacheSize) {
//                    case SMALL:
//                        fleetParams.maxShipSize = 1;
//                        break;
//                    case MEDIUM:
//                        fleetParams.maxShipSize = 2;
//                        break;
//                    case LARGE:
//                        fleetParams.maxShipSize = 3;
//                        break;
//                    case HUGE:
//                        break;
//                }
//
//                // Random market as source, since the quality is getting overwritten anyway
//                // Will NPE if source is null
//                // Should be fine, base game does this too
//                fleetParams.setSource(Global.getFactory().createMarket("fake", "fake", 5), false);
//
//                FactionDoctrineAPI doctrine = fleet.getFaction().getDoctrine();
//                float sum = doctrine.getWarships() + doctrine.getCarriers() + doctrine.getPhaseShips();
//                float warshipFP = doctrine.getWarships() / sum * lostFP;
//                float carrierFP = doctrine.getCarriers() / sum * lostFP;
//                float phaseFP = doctrine.getPhaseShips() / sum * lostFP;
//                FleetFactoryV3.addCombatFleetPoints(fleet, random, warshipFP, carrierFP, phaseFP, fleetParams);
//            }

            float quality, averageSMods, numOfficers, maxOfficerLevel;

            quality = 1.8f * sizeFrac * MathUtils.randBetween(0.7f, 1.25f, random);
            averageSMods = 4f * sizeFrac * sizeFrac * MathUtils.randBetween(0.9f, 1.25f, random) - 1f;
            numOfficers = 13f * sizeFrac * MathUtils.randBetween(0.8f, 1.2f, random);
            maxOfficerLevel = Math.min(7f, 9f * sizeFrac * MathUtils.randBetween(0.8f, 1.25f, random));

            if (fleet.getInflater() instanceof DefaultFleetInflater) {
                DefaultFleetInflater inflater = (DefaultFleetInflater) fleet.getInflater();
                DefaultFleetInflaterParams params = (DefaultFleetInflaterParams) inflater.getParams();
                params.averageSMods = averageSMods < 0f ? null : (int) averageSMods;
                params.quality = quality;
            }

            // Add officers to the fleet using a FleetParams, but don't actually generate the fleet, just the officers
            fleetParams.maxOfficersToAdd = (int) numOfficers;
            fleetParams.officerLevelLimit = (int) maxOfficerLevel;
            FleetFactoryV3.addCommanderAndOfficersV2(fleet, fleetParams, random);
            fleet.inflateIfNeeded();

            CampaignUtils.finalizeFleet(fleet, Arrays.asList(Tags.VARIANT_ALWAYS_RETAIN_SMODS_ON_SALVAGE, Tags.TAG_RETAIN_SMODS_ON_RECOVERY));
        }
    }

    private void generateFleetForEnergyCache(CampaignFleetAPI fleet, Random random) {
        List<String> radiantSkills = Arrays.asList(Skills.GUNNERY_IMPLANTS, Skills.TARGET_ANALYSIS, Skills.IMPACT_MITIGATION, Skills.COMBAT_ENDURANCE, Skills.ORDNANCE_EXPERTISE, Skills.HELMSMANSHIP, Skills.SYSTEMS_EXPERTISE);
        List<Integer> radiantEliteSkills = Arrays.asList( 1, 2, 3, 4, 5);
        PersonAPI commander = CampaignUtils.createOfficer(Factions.MERCENARY, Personalities.STEADY, radiantSkills, radiantEliteSkills, random);
        commander.getStats().setSkipRefresh(true);
        commander.getStats().setSkillLevel(Skills.BEST_OF_THE_BEST, 1);
        commander.getStats().setSkillLevel(Skills.COORDINATED_MANEUVERS, 1);
        commander.getStats().setSkillLevel(Skills.FLUX_REGULATION, 1);
        commander.getStats().setSkillLevel(Skills.CREW_TRAINING, 1);
        commander.getStats().setSkillLevel(Skills.ELECTRONIC_WARFARE, 1);
        commander.getStats().setSkillLevel(Skills.WOLFPACK_TACTICS, 1);
        commander.getStats().setSkillLevel(Skills.CARRIER_GROUP, 1);
        commander.getStats().setSkipRefresh(false);
        fleet.setCommander(commander);
        CampaignUtils.addToFleet(fleet, "wpnxt_radiant_Cache", null, commander).getVariant().addTag(Tags.VARIANT_ALWAYS_RECOVERABLE);

        List<String> paragonSkills = Arrays.asList(Skills.HELMSMANSHIP, Skills.TARGET_ANALYSIS, Skills.FIELD_MODULATION, Skills.ORDNANCE_EXPERTISE, Skills.GUNNERY_IMPLANTS, Skills.COMBAT_ENDURANCE, Skills.IMPACT_MITIGATION);
        List<Integer> paragonEliteSkills = Arrays.asList(0, 1, 2, 3, 6);
        int paragonCount = 1;
        for (int i = 0; i < paragonCount; i++) {
            CampaignUtils.addToFleet(fleet, "wpnxt_paragon_Cache", null, CampaignUtils.createOfficer(Factions.MERCENARY, Personalities.STEADY, paragonSkills, paragonEliteSkills, random)).getVariant().addTag(Tags.VARIANT_ALWAYS_RECOVERABLE);
        }

        List<String> medusaSkills = Arrays.asList(Skills.COMBAT_ENDURANCE, Skills.TARGET_ANALYSIS, Skills.FIELD_MODULATION, Skills.ORDNANCE_EXPERTISE, Skills.GUNNERY_IMPLANTS, Skills.SYSTEMS_EXPERTISE, Skills.ENERGY_WEAPON_MASTERY);
        List<Integer> medusaEliteSkills = Arrays.asList(1, 2, 3, 4 ,6);
        int medusaCount = 9;
        for (int i = 0; i < medusaCount; i++) {
            CampaignUtils.addToFleet(fleet, "wpnxt_medusa_Cache", null, CampaignUtils.createOfficer(Factions.MERCENARY, Personalities.STEADY, medusaSkills, medusaEliteSkills, random));
        }

        int omenCount = 8;
        for (int i = 0; i < omenCount; i++) {
            CampaignUtils.addToFleet(fleet, "wpnxt_omen_Cache", null ,null);
        }
    }

    private void generateFleetForMissileCache(CampaignFleetAPI fleet, Random random) {
        List<String> pegasusSkills = Arrays.asList(Skills.HELMSMANSHIP, Skills.TARGET_ANALYSIS, Skills.IMPACT_MITIGATION, Skills.ORDNANCE_EXPERTISE, Skills.MISSILE_SPECIALIZATION, Skills.FIELD_MODULATION, Skills.COMBAT_ENDURANCE);
        List<Integer> pegasusEliteSkills = Arrays.asList(0, 2, 3, 4, 5);
        PersonAPI commander = CampaignUtils.createOfficer(Factions.MERCENARY, Personalities.STEADY, pegasusSkills, pegasusEliteSkills, random);
        commander.getStats().setSkipRefresh(true);
        commander.getStats().setSkillLevel(Skills.BEST_OF_THE_BEST, 1);
        commander.getStats().setSkillLevel(Skills.WOLFPACK_TACTICS, 1);
        commander.getStats().setSkillLevel(Skills.COORDINATED_MANEUVERS, 1);
        commander.getStats().setSkillLevel(Skills.CREW_TRAINING, 1);
        commander.getStats().setSkillLevel(Skills.FIGHTER_UPLINK, 1);
        commander.getStats().setSkillLevel(Skills.CARRIER_GROUP, 1);
        commander.getStats().setSkillLevel(Skills.DERELICT_CONTINGENT, 1);
        commander.getStats().setSkipRefresh(false);
        fleet.setCommander(commander);
        CampaignUtils.addToFleet(fleet, "wpnxt_pegasus_Cache", null, commander).getVariant().addTag(Tags.VARIANT_ALWAYS_RECOVERABLE);

        List<String> conquestSkills = Arrays.asList(Skills.HELMSMANSHIP, Skills.TARGET_ANALYSIS, Skills.MISSILE_SPECIALIZATION, Skills.BALLISTIC_MASTERY, Skills.FIELD_MODULATION, Skills.IMPACT_MITIGATION, Skills.GUNNERY_IMPLANTS);
        List<Integer> conquestEliteSkills = Arrays.asList(0, 1, 2, 4, 5);
        int conquestCount = 2;
        for (int i = 0; i < conquestCount; i++) {
            CampaignUtils.addToFleet(fleet, "wpnxt_conquest_Cache", null, CampaignUtils.createOfficer(Factions.MERCENARY, Personalities.STEADY, conquestSkills, conquestEliteSkills, random));
        }

        List<String> gryphonSkills = Arrays.asList(Skills.HELMSMANSHIP, Skills.TARGET_ANALYSIS, Skills.MISSILE_SPECIALIZATION, Skills.SYSTEMS_EXPERTISE, Skills.COMBAT_ENDURANCE, Skills.FIELD_MODULATION, Skills.IMPACT_MITIGATION);
        List<Integer> gryphonEliteSkills = Arrays.asList(0, 2, 4, 5, 6);
        int gryphonCount = 4;
        for (int i = 0; i < gryphonCount; i++) {
            CampaignUtils.addToFleet(fleet, "wpnxt_gryphon_Cache", null, CampaignUtils.createOfficer(Factions.MERCENARY, Personalities.STEADY, gryphonSkills, gryphonEliteSkills, random));
        }

        int heronCount = 6;
        for (int i = 0; i < heronCount; i++) {
            CampaignUtils.addToFleet(fleet, "wpnxt_heron_Cache", null, null);
        }

        List<String> vigilanceSkills = Arrays.asList(Skills.HELMSMANSHIP, Skills.TARGET_ANALYSIS, Skills.MISSILE_SPECIALIZATION, Skills.COMBAT_ENDURANCE, Skills.FIELD_MODULATION, Skills.GUNNERY_IMPLANTS, Skills.ORDNANCE_EXPERTISE);
        List<Integer> vigilianceEliteSkills = Arrays.asList(0, 2, 4, 5, 6);
        int officeredVigilanceCount = 5;
        int unofficeredVigilanceCount = 7;

        for (int i = 0; i < officeredVigilanceCount; i++) {
            CampaignUtils.addToFleet(fleet, "wpnxt_vigilance_Cache", null, CampaignUtils.createOfficer(Factions.MERCENARY, Personalities.STEADY, vigilanceSkills, vigilianceEliteSkills, random));
        }

        for (int i = 0; i < unofficeredVigilanceCount; i++) {
            CampaignUtils.addToFleet(fleet, "wpnxt_vigilance_Cache", null, null);
        }
    }

    private void generateFleetForBallisticCache(CampaignFleetAPI fleet, Random random) {
        List<String> invictusSkills = Arrays.asList(Skills.IMPACT_MITIGATION, Skills.COMBAT_ENDURANCE, Skills.DAMAGE_CONTROL, Skills.POLARIZED_ARMOR, Skills.ORDNANCE_EXPERTISE, Skills.BALLISTIC_MASTERY, Skills.TARGET_ANALYSIS);
        List<Integer> invictusEliteSkills = Arrays.asList(0, 1, 2, 3, 4);
        PersonAPI commander = CampaignUtils.createOfficer(Factions.MERCENARY, Personalities.RECKLESS, invictusSkills, invictusEliteSkills, random);
        commander.getStats().setSkipRefresh(true);
        commander.getStats().setSkillLevel(Skills.BEST_OF_THE_BEST, 1);
        commander.getStats().setSkillLevel(Skills.CREW_TRAINING, 1);
        commander.getStats().setSkillLevel(Skills.WOLFPACK_TACTICS, 1);
        commander.getStats().setSkillLevel(Skills.COORDINATED_MANEUVERS, 1);
        commander.getStats().setSkillLevel(Skills.ELECTRONIC_WARFARE, 1);
        commander.getStats().setSkillLevel(Skills.TACTICAL_DRILLS, 1);
        commander.getStats().setSkillLevel(Skills.FLUX_REGULATION, 1);
        commander.getStats().setSkipRefresh(false);
        fleet.setCommander(commander);
        CampaignUtils.addToFleet(fleet, "wpnxt_invictus_Cache", null, commander);
//        FleetMemberAPI flagship = fleet.getFleetData().addFleetMember("wpnxt_invictus_Cache");
//        flagship.setCaptain(commander);
//        ShipVariantAPI v =  flagship.getVariant().clone();
//        v.setSource(VariantSource.REFIT);
//        v.addTag(Tags.TAG_NO_AUTOFIT);
//        flagship.setVariant(v, false, true);

        List<String> retributionSkills = Arrays.asList(Skills.HELMSMANSHIP, Skills.GUNNERY_IMPLANTS, Skills.IMPACT_MITIGATION, Skills.FIELD_MODULATION, Skills.ORDNANCE_EXPERTISE, Skills.BALLISTIC_MASTERY, Skills.TARGET_ANALYSIS);
        List<Integer> retributionEliteSkills = Arrays.asList(0, 2, 3, 4, 6);
        int retributionCount = 2;
        for (int i = 0; i < retributionCount; i++) {
            CampaignUtils.addToFleet(fleet, "wpnxt_retribution_Cache", null, CampaignUtils.createOfficer(Factions.MERCENARY, Personalities.RECKLESS, retributionSkills, retributionEliteSkills, random)).getVariant().addTag(Tags.VARIANT_ALWAYS_RECOVERABLE);
        }

        List<String> eradicatorSkills = Arrays.asList(Skills.SYSTEMS_EXPERTISE, Skills.MISSILE_SPECIALIZATION, Skills.IMPACT_MITIGATION, Skills.FIELD_MODULATION, Skills.ORDNANCE_EXPERTISE, Skills.BALLISTIC_MASTERY, Skills.TARGET_ANALYSIS);
        List<Integer> eradicatorEliteSkills = Arrays.asList(0, 2, 3, 4, 6);
        int eradicatorCount = 5;
        for (int i = 0; i < eradicatorCount; i++) {
            CampaignUtils.addToFleet(fleet, "wpnxt_eradicator_Cache", null, CampaignUtils.createOfficer(Factions.MERCENARY, Personalities.RECKLESS, eradicatorSkills, eradicatorEliteSkills, random));
        }

        List<String> lasherSkills = Arrays.asList(Skills.HELMSMANSHIP, Skills.TARGET_ANALYSIS, Skills.BALLISTIC_MASTERY, Skills.FIELD_MODULATION, Skills.ORDNANCE_EXPERTISE, Skills.GUNNERY_IMPLANTS, Skills.MISSILE_SPECIALIZATION);
        List<Integer> lasherEliteSkills = Arrays.asList(0, 3, 4, 5, 6);
        int officeredLasherCount = 3;
        int unofficeredLasherCount = 7;

        for (int i = 0; i < officeredLasherCount; i++) {
            CampaignUtils.addToFleet(fleet, "wpnxt_lasher_Cache", null, CampaignUtils.createOfficer(Factions.MERCENARY, Personalities.RECKLESS, lasherSkills, lasherEliteSkills, random));
        }
        for (int i = 0; i < unofficeredLasherCount; i++) {
            CampaignUtils.addToFleet(fleet, "wpnxt_lasher_Cache", null, null);
        }

    }

    @Override
    public void reportDefeated(SDMParams p, SectorEntityToken entity, CampaignFleetAPI fleet) {

    }

    @Override
    public int getHandlingPriority(Object params) {
        if (!(params instanceof SDMParams)) return -1;
        SDMParams p = (SDMParams) params;

        if (p.entity == null) {
            return -1;
        }

        if (p.entity.getMemoryWithoutUpdate().contains(GenSpecialCaches.cacheKey)) {
            return GenericPluginManagerAPI.MOD_SPECIFIC;
        }

        if (p.entity.getMemoryWithoutUpdate().contains(GenFortifiedCaches.fortifiedFlag)) {
            return GenericPluginManagerAPI.MOD_GENERAL;
        }

        return -1;
    }
}
