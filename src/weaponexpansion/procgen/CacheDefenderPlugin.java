package weaponexpansion.procgen;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.GenericPluginManagerAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.impl.campaign.BaseGenericPlugin;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.ids.Personalities;
import com.fs.starfarer.api.impl.campaign.ids.Skills;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.impl.campaign.rulecmd.salvage.SalvageGenFromSeed;
import com.fs.starfarer.api.impl.campaign.rulecmd.salvage.SalvageGenFromSeed.SDMParams;
import weaponexpansion.util.CampaignUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class CacheDefenderPlugin extends BaseGenericPlugin implements SalvageGenFromSeed.SalvageDefenderModificationPlugin {

    public static String specialFleetKey = "$wpnxt_specialFleet";

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

        Object cacheValue = p.entity.getMemoryWithoutUpdate().get(ProcGen.cacheKey);
        if (cacheValue == null) return;

        fleet.getFleetData().clear();
        fleet.setName("Cache Defenses");
        fleet.getFleetData().setShipNameRandom(random);

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
            default: break;
        }

        fleet.getMemoryWithoutUpdate().set(specialFleetKey, true);
        CampaignUtils.finalizeFleet(fleet, Collections.singletonList(Tags.TAG_NO_AUTOFIT));
    }

    private void generateFleetForEnergyCache(CampaignFleetAPI fleet, Random random) {
        List<String> radiantSkills = Arrays.asList(Skills.DAMAGE_CONTROL, Skills.TARGET_ANALYSIS, Skills.IMPACT_MITIGATION, Skills.FIELD_MODULATION, Skills.ORDNANCE_EXPERTISE, Skills.COMBAT_ENDURANCE, Skills.SYSTEMS_EXPERTISE);
        List<Integer> radiantEliteSkills = Arrays.asList( 0, 2, 3, 4, 5);
        PersonAPI commander = CampaignUtils.createOfficer(Factions.MERCENARY, Personalities.STEADY, radiantSkills, radiantEliteSkills, random);
        commander.getStats().setSkipRefresh(true);
        commander.getStats().setSkillLevel(Skills.BEST_OF_THE_BEST, 1);
        commander.getStats().setSkillLevel(Skills.CARRIER_GROUP, 1);
        commander.getStats().setSkillLevel(Skills.FLUX_REGULATION, 1);
        commander.getStats().setSkillLevel(Skills.CREW_TRAINING, 1);
        commander.getStats().setSkipRefresh(false);
        fleet.setCommander(commander);
        CampaignUtils.addToFleet(fleet, "wpnxt_radiant_Cache", null, commander).getVariant().addTag(Tags.VARIANT_ALWAYS_RECOVERABLE);

        List<String> paragonSkills = Arrays.asList(Skills.HELMSMANSHIP, Skills.TARGET_ANALYSIS, Skills.FIELD_MODULATION, Skills.ORDNANCE_EXPERTISE, Skills.GUNNERY_IMPLANTS, Skills.ENERGY_WEAPON_MASTERY, Skills.IMPACT_MITIGATION);
        List<Integer> paragonEliteSkills = Arrays.asList(0, 2, 3, 5, 6);
        int paragonCount = 1;
        for (int i = 0; i < paragonCount; i++) {
            CampaignUtils.addToFleet(fleet, "wpnxt_paragon_Cache", null, CampaignUtils.createOfficer(Factions.MERCENARY, Personalities.STEADY, paragonSkills, paragonEliteSkills, random)).getVariant();
        }

        List<String> apogeeSkills = Arrays.asList(Skills.HELMSMANSHIP, Skills.TARGET_ANALYSIS, Skills.FIELD_MODULATION, Skills.ORDNANCE_EXPERTISE, Skills.GUNNERY_IMPLANTS, Skills.COMBAT_ENDURANCE, Skills.MISSILE_SPECIALIZATION);
        List<Integer> apogeeEliteSkills = Arrays.asList(0, 1, 2, 3, 6);
        int apogeeCount = 3;
        for (int i = 0; i < apogeeCount; i++) {
            CampaignUtils.addToFleet(fleet, "wpnxt_apogee_Cache", null, CampaignUtils.createOfficer(Factions.MERCENARY, Personalities.STEADY, apogeeSkills, apogeeEliteSkills, random));
        }

        List<String> medusaSkills = Arrays.asList(Skills.COMBAT_ENDURANCE, Skills.TARGET_ANALYSIS, Skills.FIELD_MODULATION, Skills.ORDNANCE_EXPERTISE, Skills.GUNNERY_IMPLANTS, Skills.SYSTEMS_EXPERTISE, Skills.ENERGY_WEAPON_MASTERY);
        List<Integer> medusaEliteSkills = Arrays.asList(1, 2, 3, 4 ,6);
        int medusaCount = 4;
        for (int i = 0; i < medusaCount; i++) {
            CampaignUtils.addToFleet(fleet, "wpnxt_medusa_Cache", null, CampaignUtils.createOfficer(Factions.MERCENARY, Personalities.STEADY, medusaSkills, medusaEliteSkills, random));
        }

        List<String> omenSkills = Arrays.asList(Skills.HELMSMANSHIP, Skills.FIELD_MODULATION, Skills.GUNNERY_IMPLANTS, Skills.SYSTEMS_EXPERTISE, Skills.COMBAT_ENDURANCE, Skills.POINT_DEFENSE, Skills.MISSILE_SPECIALIZATION);
        List<Integer> omenEliteSkills = Arrays.asList( 0, 1, 2, 5, 6);
        int officeredOmenCount = 2;
        int unofficeredOmenCount = 8;
        for (int i = 0; i < officeredOmenCount; i++) {
            CampaignUtils.addToFleet(fleet, "wpnxt_omen_Cache", null, CampaignUtils.createOfficer(Factions.MERCENARY, Personalities.STEADY, omenSkills, omenEliteSkills, random));
        }
        for (int i = 0; i < unofficeredOmenCount; i++) {
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
        commander.getStats().setSkillLevel(Skills.FIGHTER_UPLINK, 1);
        commander.getStats().setSkillLevel(Skills.CARRIER_GROUP, 1);
        commander.getStats().setSkipRefresh(false);
        fleet.setCommander(commander);
        CampaignUtils.addToFleet(fleet, "wpnxt_pegasus_Cache", null, commander).getVariant().addTag(Tags.VARIANT_ALWAYS_RECOVERABLE);

        List<String> conquestSkills = Arrays.asList(Skills.HELMSMANSHIP, Skills.TARGET_ANALYSIS, Skills.MISSILE_SPECIALIZATION, Skills.BALLISTIC_MASTERY, Skills.FIELD_MODULATION, Skills.IMPACT_MITIGATION, Skills.GUNNERY_IMPLANTS);
        List<Integer> conquestEliteSkills = Arrays.asList(0, 1, 2, 4, 5);
        int conquestCount = 1;
        for (int i = 0; i < conquestCount; i++) {
            CampaignUtils.addToFleet(fleet, "wpnxt_conquest_Cache", null, CampaignUtils.createOfficer(Factions.MERCENARY, Personalities.STEADY, conquestSkills, conquestEliteSkills, random)).getVariant();
        }

        List<String> gryphonSkills = Arrays.asList(Skills.HELMSMANSHIP, Skills.TARGET_ANALYSIS, Skills.MISSILE_SPECIALIZATION, Skills.SYSTEMS_EXPERTISE, Skills.COMBAT_ENDURANCE, Skills.FIELD_MODULATION, Skills.IMPACT_MITIGATION);
        List<Integer> gryphonEliteSkills = Arrays.asList(0, 2, 4, 5, 6);
        int gryphonCount = 3;
        for (int i = 0; i < gryphonCount; i++) {
            CampaignUtils.addToFleet(fleet, "wpnxt_gryphon_Cache", null, CampaignUtils.createOfficer(Factions.MERCENARY, Personalities.STEADY, gryphonSkills, gryphonEliteSkills, random));
        }

        int heronCount = 4;
        for (int i = 0; i < heronCount; i++) {
            CampaignUtils.addToFleet(fleet, "wpnxt_heron_Cache", null, null);
        }

        List<String> vigilanceSkills = Arrays.asList(Skills.HELMSMANSHIP, Skills.TARGET_ANALYSIS, Skills.MISSILE_SPECIALIZATION, Skills.COMBAT_ENDURANCE, Skills.FIELD_MODULATION, Skills.GUNNERY_IMPLANTS, Skills.ORDNANCE_EXPERTISE);
        List<Integer> vigilianceEliteSkills = Arrays.asList(0, 2, 4, 5, 6);
        int officeredVigilanceCount = 6;
        int unofficeredVigilanceCount = 4;

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
        commander.getStats().setSkillLevel(Skills.COORDINATED_MANEUVERS, 1);
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

        if (p.entity != null && p.entity.getMemoryWithoutUpdate().contains(ProcGen.cacheKey)) {
            return GenericPluginManagerAPI.MOD_SPECIFIC;
        }

        return -1;
    }
}
