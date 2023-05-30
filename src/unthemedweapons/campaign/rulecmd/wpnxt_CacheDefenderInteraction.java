package unthemedweapons.campaign.rulecmd;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.combat.BattleCreationContext;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.FleetEncounterContext;
import com.fs.starfarer.api.impl.campaign.FleetInteractionDialogPluginImpl;
import com.fs.starfarer.api.impl.campaign.ids.*;
import com.fs.starfarer.api.impl.campaign.procgen.SalvageEntityGenDataSpec;
import com.fs.starfarer.api.impl.campaign.rulecmd.BaseCommandPlugin;
import com.fs.starfarer.api.impl.campaign.rulecmd.FireBest;
import com.fs.starfarer.api.impl.campaign.rulecmd.salvage.FleetAdvanceScript;
import com.fs.starfarer.api.impl.campaign.rulecmd.salvage.SalvageEntity;
import com.fs.starfarer.api.impl.campaign.rulecmd.salvage.SalvageGenFromSeed;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.Misc.Token;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Copied from SalvageDefenderInteraction. Need to set text to remove references to "automated" defenses, but unfortunately
 *  the text is hard-coded as a literal.
 *  Also: don't impact allies' reputations.
 *  Also: don't drop AI cores since no AI core officers.
 *
 * <p>
 *  Note: calling new SalvageDefenderInteration().execute() and then setting the option text doesn't work due to
 *  re-engagement still referring to the defenses as automated. */
@SuppressWarnings("unused")
public class wpnxt_CacheDefenderInteraction extends BaseCommandPlugin {
    public boolean execute(String ruleId, InteractionDialogAPI dialog, List<Token> params, final Map<String, MemoryAPI> memoryMap) {
        if (dialog == null) return false;

        final SectorEntityToken entity = dialog.getInteractionTarget();
        final MemoryAPI memory = getEntityMemory(memoryMap);

        final CampaignFleetAPI defenders = memory.getFleet("$defenderFleet");
        if (defenders == null) return false;

        dialog.setInteractionTarget(defenders);

        final FleetInteractionDialogPluginImpl.FIDConfig config = new FleetInteractionDialogPluginImpl.FIDConfig();
        config.leaveAlwaysAvailable = true;
        config.showCommLinkOption = false;
        config.showEngageText = false;
        config.showFleetAttitude = false;
        config.showTransponderStatus = false;
        config.showWarningDialogWhenNotHostile = false;
        config.alwaysAttackVsAttack = true;
        config.impactsAllyReputation = false;
        config.impactsEnemyReputation = false;
        config.pullInAllies = false;
        config.pullInEnemies = false;
        config.pullInStations = false;
        config.lootCredits = true;

        config.firstTimeEngageOptionText = "Engage the defenses";
        config.afterFirstTimeEngageOptionText = "Re-engage the defenses";
        config.noSalvageLeaveOptionText = "Continue";

        config.dismissOnLeave = false;
        config.printXPToDialog = true;

        long seed = memory.getLong(MemFlags.SALVAGE_SEED);
        config.salvageRandom = Misc.getRandom(seed, 75);


        final FleetInteractionDialogPluginImpl plugin = new FleetInteractionDialogPluginImpl(config);

        final InteractionDialogPlugin originalPlugin = dialog.getPlugin();
        config.delegate = new FleetInteractionDialogPluginImpl.BaseFIDDelegate() {
            @Override
            public void notifyLeave(InteractionDialogAPI dialog) {
                // nothing in there we care about keeping; clearing to reduce savefile size
                defenders.getMemoryWithoutUpdate().clear();
                // there's a "standing down" assignment given after a battle is finished that we don't care about
                defenders.clearAssignments();
                defenders.deflate();

                dialog.setPlugin(originalPlugin);
                dialog.setInteractionTarget(entity);

                //Global.getSector().getCampaignUI().clearMessages();

                if (plugin.getContext() instanceof FleetEncounterContext) {
                    FleetEncounterContext context = (FleetEncounterContext) plugin.getContext();
                    if (context.didPlayerWinEncounterOutright()) {

                        SalvageGenFromSeed.SDMParams p = new SalvageGenFromSeed.SDMParams();
                        p.entity = entity;
                        p.factionId = defenders.getFaction().getId();

                        SalvageGenFromSeed.SalvageDefenderModificationPlugin plugin = Global.getSector().getGenericPlugins().pickPlugin(
                                SalvageGenFromSeed.SalvageDefenderModificationPlugin.class, p);
                        if (plugin != null) {
                            plugin.reportDefeated(p, entity, defenders);
                        }

                        memory.unset("$hasDefenders");
                        memory.unset("$defenderFleet");
                        memory.set("$defenderFleetDefeated", true);
                        entity.removeScriptsOfClass(FleetAdvanceScript.class);
                        FireBest.fire(null, dialog, memoryMap, "BeatDefendersContinue");
                    } else {
                        boolean persistDefenders = false;
                        if (context.isEngagedInHostilities()) {
                            persistDefenders = !Misc.getSnapshotMembersLost(defenders).isEmpty();
                            for (FleetMemberAPI member : defenders.getFleetData().getMembersListCopy()) {
                                if (member.getStatus().needsRepairs()) {
                                    persistDefenders = true;
                                    break;
                                }
                            }
                        }

                        if (persistDefenders) {
                            if (!entity.hasScriptOfClass(FleetAdvanceScript.class)) {
                                defenders.setDoNotAdvanceAI(true);
                                defenders.setContainingLocation(entity.getContainingLocation());
                                // somewhere far off where it's not going to be in terrain or whatever
                                defenders.setLocation(1000000, 1000000);
                                entity.addScript(new FleetAdvanceScript(defenders));
                            }
                            memory.expire("$defenderFleet", 10); // defenders may have gotten damaged; persist them for a bit
                        }
                        dialog.dismiss();
                    }
                } else {
                    dialog.dismiss();
                }
            }
            @Override
            public void battleContextCreated(InteractionDialogAPI dialog, BattleCreationContext bcc) {
                bcc.aiRetreatAllowed = false;
                bcc.objectivesAllowed = false;
                bcc.enemyDeployAll = true;
            }
            @Override
            public void postPlayerSalvageGeneration(InteractionDialogAPI dialog, FleetEncounterContext context, CargoAPI salvage) {
                FleetEncounterContextPlugin.DataForEncounterSide winner = context.getWinnerData();
                FleetEncounterContextPlugin.DataForEncounterSide loser = context.getLoserData();

                if (winner == null || loser == null) return;

                float playerContribMult = context.computePlayerContribFraction();

                List<SalvageEntityGenDataSpec.DropData> dropRandom = new ArrayList<SalvageEntityGenDataSpec.DropData>();
                List<SalvageEntityGenDataSpec.DropData> dropValue = new ArrayList<SalvageEntityGenDataSpec.DropData>();

                float valueMultFleet = Global.getSector().getPlayerFleet().getStats().getDynamic().getValue(Stats.BATTLE_SALVAGE_MULT_FLEET);
                float valueModShips = context.getSalvageValueModPlayerShips();
                float fuelMult = Global.getSector().getPlayerFleet().getStats().getDynamic().getValue(Stats.FUEL_SALVAGE_VALUE_MULT_FLEET);
                //float fuel = salvage.getFuel();
                //salvage.addFuel((int) Math.round(fuel * fuelMult));

                CargoAPI extra = SalvageEntity.generateSalvage(config.salvageRandom, valueMultFleet + valueModShips, 1f, 1f, fuelMult, dropValue, dropRandom);
                for (CargoStackAPI stack : extra.getStacksCopy()) {
                    if (stack.isFuelStack()) {
                        stack.setSize((int)(stack.getSize() * fuelMult));
                    }
                    salvage.addFromStack(stack);
                }
            }

        };


        dialog.setPlugin(plugin);
        plugin.init(dialog);

        return true;
    }
}
