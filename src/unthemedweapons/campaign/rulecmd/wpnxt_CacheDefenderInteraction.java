package unthemedweapons.campaign.rulecmd;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.combat.BattleCreationContext;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.DModManager;
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
import org.lwjgl.input.Keyboard;
import unthemedweapons.procgen.GenSpecialCaches;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.ToDoubleFunction;

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

    public static final float CAN_BYPASS_MULT = 2.5f;

    private boolean isPlayerFleetMuchStronger(CampaignFleetAPI defenders) {
        ToDoubleFunction<FleetMemberAPI> mapper = fm -> {
            if (fm.isMothballed()) return 0f;
            double score = fm.getFleetPointCost();
            if (fm.isCivilian()) score *= 0.25f;
            if (fm.getCaptain() != null) {
                score *= 1f + 0.15f*fm.getCaptain().getStats().getLevel();
            }
            score *= 1f + 0.1f*fm.getVariant().getSMods().size();
            score *= Math.pow(0.9f, DModManager.getNumDMods(fm.getVariant()));
            return score;
        };

        double playerScore = Global.getSector().getPlayerFleet()
                .getFleetData()
                .getMembersListCopy()
                .stream()
                .mapToDouble(mapper)
                .sum();
        double enemyScore = defenders.getFleetData().getMembersListCopy().stream().mapToDouble(mapper).sum();
        return enemyScore * CAN_BYPASS_MULT < playerScore;
    }

    private void onBeatDefenders(InteractionDialogAPI dialog, SectorEntityToken entity, Map<String, MemoryAPI> memoryMap, InteractionDialogPlugin originalPlugin) {
        final MemoryAPI memory = getEntityMemory(memoryMap);
        final CampaignFleetAPI defenders = memory.getFleet("$defenderFleet");
        if (defenders == null) return;

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
    }

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

        config.firstTimeEngageOptionText = "Engage the defenders";
        config.afterFirstTimeEngageOptionText = "Re-engage the defenders";
        config.noSalvageLeaveOptionText = "Continue";

        config.dismissOnLeave = false;
        config.printXPToDialog = true;

        long seed = memory.getLong(MemFlags.SALVAGE_SEED);
        config.salvageRandom = Misc.getRandom(seed, 75);

        final InteractionDialogPlugin originalPlugin = dialog.getPlugin();

        final FleetInteractionDialogPluginImpl plugin = new FleetInteractionDialogPluginImpl(config) {
            @Override
            protected void updateEngagementChoice(boolean withText) {
                super.updateEngagementChoice(withText);
                if (isPlayerFleetMuchStronger(defenders) && !memory.contains(GenSpecialCaches.cacheKey) && !dialog.getOptionPanel().hasOption(FleetInteractionDialogPluginImpl.OptionId.AUTORESOLVE_PURSUE)) {
                    dialog.getOptionPanel().removeOption(FleetInteractionDialogPluginImpl.OptionId.LEAVE);
                    String tooltipText = getString("tooltipPursueAutoresolve");
                    dialog.getOptionPanel().addOption("Order your second-in-command to handle it", FleetInteractionDialogPluginImpl.OptionId.AUTORESOLVE_PURSUE, tooltipText);
                    dialog.getOptionPanel().addOption("Leave", FleetInteractionDialogPluginImpl.OptionId.LEAVE, null);
                    dialog.getOptionPanel().setShortcut(FleetInteractionDialogPluginImpl.OptionId.LEAVE, Keyboard.KEY_ESCAPE, false, false, false, true);
                }
            }
        };

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

                if (plugin.getContext() instanceof FleetEncounterContext context) {
                    if (context.didPlayerWinEncounterOutright()) {
                        onBeatDefenders(dialog, entity, memoryMap, originalPlugin);
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

                List<SalvageEntityGenDataSpec.DropData> dropRandom = new ArrayList<>();
                List<SalvageEntityGenDataSpec.DropData> dropValue = new ArrayList<>();

                float valueMultFleet = Global.getSector().getPlayerFleet().getStats().getDynamic().getValue(Stats.BATTLE_SALVAGE_MULT_FLEET);
                float valueModShips = context.getSalvageValueModPlayerShips();
                float fuelMult = Global.getSector().getPlayerFleet().getStats().getDynamic().getValue(Stats.FUEL_SALVAGE_VALUE_MULT_FLEET);

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
