package unthemedweapons.util;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CustomCampaignEntityAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.ids.Personalities;
import com.fs.starfarer.api.impl.campaign.ids.Skills;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.loading.VariantSource;
import unthemedweapons.procgen.GenSpecialCaches;

import java.util.*;

public abstract class CampaignUtils {
    public static PersonAPI createOfficer(String factionId, String personality, List<String> skills, List<Integer> eliteSkillIndices, Random random) {
        PersonAPI officer = Global.getSector().getFaction(factionId).createRandomPerson(random);
        officer.setFaction(factionId);

        officer.getStats().setSkipRefresh(true);

        if (personality != null) {
            officer.setPersonality(personality);
        }
        officer.getStats().setLevel(skills.size());
        for (String skill : skills) {
            officer.getStats().setSkillLevel(skill, 1);
        }

        for (int i : eliteSkillIndices) {
            officer.getStats().setSkillLevel(skills.get(i), 2);
        }

        officer.getStats().setSkipRefresh(false);
        return officer;
    }

    public static FleetMemberAPI addToFleet(CampaignFleetAPI fleet, String variantId, String shipName, PersonAPI officer) {
        FleetMemberAPI member = fleet.getFleetData().addFleetMember(variantId);

        if (shipName != null) {
            member.setShipName(shipName);
        }
        if (officer != null) {
            member.setCaptain(officer);
        }

        return member;
    }

    public static void finalizeFleet(CampaignFleetAPI fleet,  List<String> tagsToAdd) {
        for (FleetMemberAPI member : fleet.getFleetData().getMembersListWithFightersCopy()) {
            member.getRepairTracker().setCR(member.getRepairTracker().getMaxCR());
        }

        for (FleetMemberAPI member : fleet.getFleetData().getMembersListCopy()) {
            ShipVariantAPI v = member.getVariant().clone();
            v.setSource(VariantSource.REFIT);
            if (tagsToAdd != null) {
                for (String tag : tagsToAdd) {
                    v.addTag(tag);
                }
            }
            member.setVariant(v, false, false);
        }
    }

    public static void generateFleetForEnergyCache(CampaignFleetAPI fleet, Random random) {
        List<String> radiantSkills = Arrays.asList(Skills.ENERGY_WEAPON_MASTERY, Skills.TARGET_ANALYSIS, Skills.IMPACT_MITIGATION, Skills.GUNNERY_IMPLANTS, Skills.ORDNANCE_EXPERTISE, Skills.HELMSMANSHIP, Skills.SYSTEMS_EXPERTISE);
        List<Integer> radiantEliteSkills = Arrays.asList( 0, 1, 2, 4, 5);
        PersonAPI commander = CampaignUtils.createOfficer(Factions.MERCENARY, Personalities.STEADY, radiantSkills, radiantEliteSkills, random);
        commander.getStats().setSkipRefresh(true);
        commander.getStats().setSkillLevel(Skills.BEST_OF_THE_BEST, 1);
        commander.getStats().setSkillLevel(Skills.COORDINATED_MANEUVERS, 1);
        commander.getStats().setSkillLevel(Skills.FLUX_REGULATION, 1);
        commander.getStats().setSkillLevel(Skills.CREW_TRAINING, 1);
        commander.getStats().setSkillLevel(Skills.ELECTRONIC_WARFARE, 1);
        commander.getStats().setSkillLevel(Skills.WOLFPACK_TACTICS, 1);
        commander.getStats().setSkillLevel(Skills.HULL_RESTORATION, 1);
        commander.getStats().setSkipRefresh(false);
        CampaignUtils.addToFleet(fleet, "wpnxt_radiant_Cache", null, commander).getVariant().addTag(Tags.VARIANT_ALWAYS_RECOVERABLE);

        List<String> paragonSkills = Arrays.asList(Skills.HELMSMANSHIP, Skills.TARGET_ANALYSIS, Skills.FIELD_MODULATION, Skills.ORDNANCE_EXPERTISE, Skills.GUNNERY_IMPLANTS, Skills.ENERGY_WEAPON_MASTERY, Skills.IMPACT_MITIGATION);
        List<Integer> paragonEliteSkills = Arrays.asList(0, 2, 3, 5, 6);
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

    public static void generateFleetForMissileCache(CampaignFleetAPI fleet, Random random) {
        List<String> pegasusSkills = Arrays.asList(Skills.HELMSMANSHIP, Skills.TARGET_ANALYSIS, Skills.IMPACT_MITIGATION, Skills.ORDNANCE_EXPERTISE, Skills.MISSILE_SPECIALIZATION, Skills.FIELD_MODULATION, Skills.COMBAT_ENDURANCE);
        List<Integer> pegasusEliteSkills = Arrays.asList(0, 2, 3, 4, 5);
        PersonAPI commander = CampaignUtils.createOfficer(Factions.MERCENARY, Personalities.STEADY, pegasusSkills, pegasusEliteSkills, random);
        commander.getStats().setSkipRefresh(true);
        commander.getStats().setSkillLevel(Skills.BEST_OF_THE_BEST, 1);
        commander.getStats().setSkillLevel(Skills.WOLFPACK_TACTICS, 1);
        commander.getStats().setSkillLevel(Skills.FIGHTER_UPLINK, 1);
        commander.getStats().setSkillLevel(Skills.CARRIER_GROUP, 1);
        commander.getStats().setSkipRefresh(false);
        CampaignUtils.addToFleet(fleet, "wpnxt_pegasus_Cache", null, commander);

        List<String> conquestSkills = Arrays.asList(Skills.HELMSMANSHIP, Skills.TARGET_ANALYSIS, Skills.MISSILE_SPECIALIZATION, Skills.BALLISTIC_MASTERY, Skills.FIELD_MODULATION, Skills.IMPACT_MITIGATION, Skills.GUNNERY_IMPLANTS);
        List<Integer> conquestEliteSkills = Arrays.asList(0, 1, 2, 4, 5);
        int conquestCount = 1;
        for (int i = 0; i < conquestCount; i++) {
            CampaignUtils.addToFleet(fleet, "wpnxt_conquest_Cache", null, CampaignUtils.createOfficer(Factions.MERCENARY, Personalities.STEADY, conquestSkills, conquestEliteSkills, random));
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

    public static void generateFleetForBallisticCache(CampaignFleetAPI fleet, Random random) {
        List<String> invictusSkills = Arrays.asList(Skills.IMPACT_MITIGATION, Skills.COMBAT_ENDURANCE, Skills.DAMAGE_CONTROL, Skills.POLARIZED_ARMOR, Skills.ORDNANCE_EXPERTISE, Skills.BALLISTIC_MASTERY, Skills.TARGET_ANALYSIS);
        List<Integer> invictusEliteSkills = Arrays.asList(0, 1, 2, 3, 4);
        PersonAPI commander = CampaignUtils.createOfficer(Factions.MERCENARY, Personalities.RECKLESS, invictusSkills, invictusEliteSkills, random);
        commander.getStats().setSkipRefresh(true);
        commander.getStats().setSkillLevel(Skills.BEST_OF_THE_BEST, 1);
        commander.getStats().setSkillLevel(Skills.CREW_TRAINING, 1);
        commander.getStats().setSkillLevel(Skills.COORDINATED_MANEUVERS, 1);
        commander.getStats().setSkillLevel(Skills.FLUX_REGULATION, 1);
        commander.getStats().setSkipRefresh(false);
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
            CampaignUtils.addToFleet(fleet, "wpnxt_retribution_Cache", null, CampaignUtils.createOfficer(Factions.MERCENARY, Personalities.RECKLESS, retributionSkills, retributionEliteSkills, random));
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

    public static void findSpecialCaches() {
        for (StarSystemAPI system : Global.getSector().getStarSystems()) {
            for (SectorEntityToken entity : system.getCustomEntities()) {
                if (entity.getMemoryWithoutUpdate().contains(GenSpecialCaches.cacheKey)) {
                    System.out.println(system.getName());
                }
            }
        }
    }

    public static void findLargestCaches() {
        List<SectorEntityToken> caches = new ArrayList<>();
        for (StarSystemAPI system : Global.getSector().getStarSystems()) {
            for (SectorEntityToken entity : system.getCustomEntities()) {
                if ("wpnxt_fortified_cache".equals(entity.getCustomEntitySpec().getId())) {
                    caches.add(entity);
                }
            }
        }
        Collections.sort(caches, new Comparator<SectorEntityToken>() {
            @Override
            public int compare(SectorEntityToken o1, SectorEntityToken o2) {
                return Float.compare(o2.getRadius(), o1.getRadius());
            }
        });

        System.out.println("Total number of caches: " + caches.size());
        for (int i = 0; i < Math.min(caches.size(), 10); i++) {
            System.out.println(caches.get(i).getStarSystem() + ": " + caches.get(i).getRadius());
        }
    }
}
