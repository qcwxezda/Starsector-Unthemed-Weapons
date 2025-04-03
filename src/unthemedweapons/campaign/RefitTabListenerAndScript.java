package unthemedweapons.campaign;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CoreUIAPI;
import com.fs.starfarer.api.campaign.CoreUITabId;
import com.fs.starfarer.api.campaign.listeners.CoreUITabListener;
import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.EveryFrameCombatPlugin;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.loading.MissileSpecAPI;
import com.fs.starfarer.api.loading.ProjectileWeaponSpecAPI;
import com.fs.starfarer.api.loading.WeaponSlotAPI;
import com.fs.starfarer.api.loading.WeaponSpecAPI;
import com.fs.starfarer.api.ui.ButtonAPI;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.UIPanelAPI;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.combat.CombatEngine;
import com.fs.starfarer.coreui.refit.ModPickerDialogV3;
import com.fs.starfarer.title.TitleScreenState;
import com.fs.starfarer.ui.impl.CargoTooltipFactory;
import com.fs.state.AppDriver;
import com.fs.util.container.Pair;
import com.fs.starfarer.coreui.refit.WeaponPickerDialog;
import com.fs.starfarer.ui.impl.StandardTooltipV2Expandable;
import unthemedweapons.ModPlugin;
import unthemedweapons.util.DynamicWeaponStats;
import unthemedweapons.util.ReflectionUtils;

import java.awt.*;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RefitTabListenerAndScript extends BaseEveryFrameCombatPlugin implements CoreUITabListener, EveryFrameScript {
    private boolean insideRefitScreen = false;
    private WeaponPickerDialog wpd = null;
    private ShipAPI selectedShip = null;
    private StandardTooltipV2Expandable hoveredTooltip = null; // Hovered over an existing weapon
    private StandardTooltipV2Expandable currTooltip = null; // Pressing CTRL when picking a weapon
    private StandardTooltipV2Expandable activeTooltip = null; // Selected weapon when picking a weapon
    private ButtonAPI firstModifiedButton = null; // Keep track of the first modified button to know when the weapons list changes
    private int lastWeaponDialogIndex = -1;

    private static String wpdSlotFieldName = null;
    private static String weaponSpecFieldName = null;
    private static String rendererPanelFieldName = null;
    private static String refitScreenWeaponSlotFieldName = null;
    private static String modDisplayWeaponFluxFieldName = null;
    private static final Map<Class<? extends WeaponAPI>, String> weaponWeaponSlotFieldNames = new HashMap<>();
    private static final Color greenColor = Global.getSettings().getColor("textFriendColor");
    private static final Color redColor = Global.getSettings().getColor("textEnemyColor");
    private static final Color darkGreenColor = new Color(75, 125, 0);
    private static final Color darkRedColor = new Color(128, 50, 0);

    @Override
    public void reportAboutToOpenCoreTab(CoreUITabId id, Object param) {
        if (CoreUITabId.REFIT.equals(id) && !insideRefitScreen) {
            insideRefitScreen = true;
        }
    }

    @Override
    public boolean isDone() {
        return false;
    }

    @Override
    public boolean runWhilePaused() {
        return true;
    }

    private void advance(UIPanelAPI screenPanel, UIPanelAPI core, UIPanelAPI currentCoreTab) {
        // Update the selected ship and repopulate the weapon mapping if the selected ship has changed
        updateSelectedShip(currentCoreTab);
        updateTotalWeaponFluxDisplayIfNeeded(core, currentCoreTab);

        // Try to get the hovered-over tooltip
        // This exists inside the campaign UI's "screenpanel"
        // and is some inner class of CargoTooltipFactory
        List<?> screenPanelChildren = (List<?>) ReflectionUtils.invokeMethod(screenPanel, "getChildrenNonCopy");
        for (Object child : screenPanelChildren) {
            if (child.getClass().getEnclosingClass() == CargoTooltipFactory.class) {
                Object prev = ReflectionUtils.invokeMethod(child, "getPrev");
                if (prev instanceof StandardTooltipV2Expandable) {
                    if (prev != hoveredTooltip) {
                        hoveredTooltip = (StandardTooltipV2Expandable) prev;
                        Runnable getBeforeShowing = !(child instanceof StandardTooltipV2Expandable) ? null : ((StandardTooltipV2Expandable) child).getBeforeShowing();
                        if (getBeforeShowing != null
                                // WeaponPickerDialog stuff handled in later code; don't handle it here also
                                && !(getBeforeShowing.getClass().getEnclosingClass() == WeaponPickerDialog.class)
                                // This excludes fighter stuff, as the fighter "getBeforeShowing" is a UIPanelAPI
                                && !(UIPanelAPI.class.isAssignableFrom(getBeforeShowing.getClass().getEnclosingClass()))) {

                            // Find an instance of the enclosing class
                            WeaponSlotAPI slot = null;
                            for (Field field : getBeforeShowing.getClass().getDeclaredFields()) {
                                if (getBeforeShowing.getClass().getEnclosingClass().isAssignableFrom(field.getType())) {
                                    Object refit = ReflectionUtils.getField(getBeforeShowing, field.getName());
                                    if (refit == null) continue;

                                    if (refitScreenWeaponSlotFieldName == null) {
                                        for (Field refitField : refit.getClass().getDeclaredFields()) {
                                            if (WeaponSlotAPI.class.isAssignableFrom(refitField.getType())) {
                                                refitScreenWeaponSlotFieldName = refitField.getName();
                                            }
                                        }
                                    }

                                    if (refitScreenWeaponSlotFieldName != null) {
                                        slot = (WeaponSlotAPI) ReflectionUtils.getField(refit, refitScreenWeaponSlotFieldName);
                                    }
                                    break;
                                }
                            }

                            // Note: getPanel and getChildrenNonCopy are public, but the return types are obfuscated
                            // and therefore may be different between Linux and Windows versions
                            UIPanelAPI panel = (UIPanelAPI) ReflectionUtils.invokeMethod(prev, "getPanel");
                            updateGrids((List<?>) ReflectionUtils.invokeMethod(panel, "getChildrenNonCopy"), getWeaponSpecFromTooltip((StandardTooltipV2Expandable) child), slot);
                        }
                    }
                }
            }
        }

        // Try to get the weapon picker dialog
        // This exists when a weapon slot has been selected
        List<?> children = (List<?>) ReflectionUtils.invokeMethod(core, "getChildrenNonCopy");
        WeaponPickerDialog dialog = findWeaponPickerDialogInList(children);

        if (dialog != wpd) {
            wpd = dialog;
        }

        // Do this every frame
        if (wpd != null) {
            WeaponSlotAPI slot;
            if (wpdSlotFieldName == null) {
                for (Field field : wpd.getClass().getDeclaredFields()) {
                    if (WeaponSlotAPI.class.isAssignableFrom(field.getType())) {
                        wpdSlotFieldName = field.getName();
                        break;
                    }
                }
            }
            slot = (WeaponSlotAPI) ReflectionUtils.getField(wpd, wpdSlotFieldName);

            updateWeaponPickerRangesIfNeeded(slot);

            StandardTooltipV2Expandable currTooltip = (StandardTooltipV2Expandable) ReflectionUtils.getField(wpd, "tooltipForCurr");
            if (currTooltip != this.currTooltip) {
                this.currTooltip = currTooltip;
                if (currTooltip != null) {
                    updateGrids(getStatsPanelChildren(currTooltip), getWeaponSpecFromTooltip(currTooltip), slot);
                }
            }
            //noinspection unchecked
            com.fs.starfarer.api.util.Pair<?, StandardTooltipV2Expandable> activeTooltip = (com.fs.starfarer.api.util.Pair<?, StandardTooltipV2Expandable>) ReflectionUtils.invokeMethodExt(wpd, "getActiveTooltip", true);
            if (this.activeTooltip != null && activeTooltip == null) {
                this.activeTooltip = null;
            }
            if (activeTooltip != null) {
                if (activeTooltip.two != this.activeTooltip) {
                    this.activeTooltip = activeTooltip.two;
                    if (activeTooltip.two != null) {
                        updateGrids(getStatsPanelChildren(activeTooltip.two), getWeaponSpecFromTooltip(activeTooltip.two), slot);
                    }
                }
            }
        }
    }

    @Override
    public void init(CombatEngineAPI engine) {
        if (!(getClass().getClassLoader() instanceof ModPlugin.ReflectionEnabledClassLoader)) {
            try {
                Class<?> cls = ModPlugin.getClassLoader().loadClass("unthemedweapons.campaign.RefitTabListenerAndScript");
                MethodHandles.Lookup lookup = MethodHandles.lookup();
                MethodHandle mh = lookup.findConstructor(cls, MethodType.methodType(void.class));
                EveryFrameCombatPlugin refitModifier = (EveryFrameCombatPlugin) mh.invoke();
                CombatEngine.getInstance().addPlugin(refitModifier);

                CombatEngine.getInstance().removePlugin(this);
            }
            catch (Throwable e) {
                throw new RuntimeException("Failed to add refit tab listener; consider setting enableQoL to false in wpnxt_mod_settings.json", e);
            }
        }
    }

    @Override
    public void advance(float amount, List<InputEventAPI> events) {
        // Refit screen only shows up in combat in missions
        var currentState = AppDriver.getInstance().getCurrentState();
        if (!(currentState instanceof TitleScreenState)) return;

        // Wrong classloader, will crash on reflection
        if (!(getClass().getClassLoader() instanceof ModPlugin.ReflectionEnabledClassLoader)) return;

        UIPanelAPI screenPanel = (UIPanelAPI) ReflectionUtils.getField(currentState, "screenPanel");
        // Find a child that is an instance of CoreUIAPI
        UIPanelAPI coreUI = null;
        List<?> children = (List<?>) ReflectionUtils.invokeMethod(screenPanel, "getChildrenNonCopy");
        for (Object child : children) {
            List<?> subChildren = (List<?>) ReflectionUtils.invokeMethod(child, "getChildrenNonCopy");
            if (subChildren != null && subChildren.get(0) instanceof CoreUIAPI) {
                coreUI = (UIPanelAPI) subChildren.get(0);
                break;
            }
        }
        if (coreUI == null) return;
        Object currentTabData = ReflectionUtils.invokeMethod(coreUI, "getCurrentTabData");
        UIPanelAPI currentTab = null;
        for (Field field : currentTabData.getClass().getDeclaredFields()) {
            if (UIPanelAPI.class.isAssignableFrom(field.getType())) {
                currentTab = (UIPanelAPI) ReflectionUtils.getField(currentTabData, field.getName());
                break;
            }
        }

        if (currentTab == null) return;
        advance(screenPanel, screenPanel, currentTab);
    }

    @Override
    public void advance(float amount) {
        if (!insideRefitScreen) return;
        if (Global.getSector() == null || Global.getSector().getCampaignUI() == null) return;

        var ui = Global.getSector().getCampaignUI();
        if (!CoreUITabId.REFIT.equals(ui.getCurrentCoreTab())) {
            insideRefitScreen = false;
            return;
        }
        // Due to a bug, if the player ESCs out of the refit screen in a market, the core tab is still shown as REFIT
        // even though it's been closed. To combat this, check if the savedOptionList is empty. If it is, we're still
        // in the refit screen; otherwise, we've ESCed out of the refit screen.
        else if (ui.getCurrentInteractionDialog() != null
                && ui.getCurrentInteractionDialog().getOptionPanel() != null
                && !ui.getCurrentInteractionDialog().getOptionPanel().getSavedOptionList().isEmpty()) {
            insideRefitScreen = false;
            return;
        }

        UIPanelAPI screenPanel = (UIPanelAPI) ReflectionUtils.getField(ui, "screenPanel");
        UIPanelAPI core = ReflectionUtils.getCoreUI();
        UIPanelAPI currentCoreTab = (UIPanelAPI) ReflectionUtils.invokeMethod(core, "getCurrentTab");
        advance(screenPanel, core, currentCoreTab);
    }

    private void updateWeaponPickerRangesIfNeeded(WeaponSlotAPI slot) {
        if (selectedShip == null) return;

        Object innerPanel = ReflectionUtils.invokeMethod(wpd, "getInnerPanel");
        List<?> wpdChildren = (List<?>) ReflectionUtils.invokeMethod(innerPanel, "getChildrenNonCopy");
        for (Object child : wpdChildren) {
            if (child instanceof ButtonAPI) {
                if (firstModifiedButton == child) {
                    return;
                }
                updateWeaponPickerRanges(slot);
                return;
            }
            if (child instanceof UIPanelAPI) {
                try {
                    List<?> innerPanelChildren = (List<?>) ReflectionUtils.invokeMethodNoCatch(child, "getItems");
                    for (Object subChild : innerPanelChildren) {
                        if (subChild instanceof ButtonAPI) {
                            if (firstModifiedButton == subChild) {
                                return;
                            }
                            updateWeaponPickerRanges(slot);
                            return;
                        }
                    }
                }
                catch (Exception ignore) {}
            }
        }
    }

    private void updateWeaponPickerRanges(WeaponSlotAPI slot) {
        // Look for all children and sub-children that are buttons
        List<ButtonAPI> buttons = new ArrayList<>();
        Object innerPanel = ReflectionUtils.invokeMethod(wpd, "getInnerPanel");
        List<?> wpdChildren = (List<?>) ReflectionUtils.invokeMethod(innerPanel, "getChildrenNonCopy");
        boolean isFirst = false;
        for (Object child : wpdChildren) {
            if (child instanceof ButtonAPI) {
                buttons.add((ButtonAPI) child);
                if (!isFirst) {
                    firstModifiedButton = (ButtonAPI) child;
                    isFirst = true;
                }
            }
            if (child instanceof UIPanelAPI) {
                try {
                    List<?> innerPanelChildren = (List<?>) ReflectionUtils.invokeMethodNoCatch(child, "getItems");
                    for (Object subChild : innerPanelChildren) {
                        if (subChild instanceof ButtonAPI) {
                            buttons.add((ButtonAPI) subChild);
                            if (!isFirst) {
                                firstModifiedButton = (ButtonAPI) subChild;
                                isFirst = true;
                            }
                        }
                    }
                }
                catch (Exception ignore) {}
            }
        }

        for (ButtonAPI button : buttons) {
            Object renderer = ReflectionUtils.invokeMethod(button, "getRenderer");
            boolean isEnabled = button.isEnabled();
            Object rendererPanel = null;
            if (rendererPanelFieldName != null) {
                rendererPanel = ReflectionUtils.getField(renderer, rendererPanelFieldName);
            }
            else {
                for (Field field : renderer.getClass().getDeclaredFields()) {
                    if (UIPanelAPI.class.isAssignableFrom(field.getType())) {
                        rendererPanelFieldName = field.getName();
                        rendererPanel = ReflectionUtils.getField(renderer, rendererPanelFieldName);
                    }
                }
            }

            if (rendererPanel != null) {
                Object info = ReflectionUtils.invokeMethod(rendererPanel, "getInfo");
                WeaponSpecAPI spec = (WeaponSpecAPI) ReflectionUtils.invokeMethod(info, "getSpec");
                List<?> rendererChildren = (List<?>) ReflectionUtils.invokeMethod(rendererPanel, "getChildrenNonCopy");

                for (Object child : rendererChildren) {
                    if (child instanceof LabelAPI label) {
                        float textHeight = label.getPosition().getHeight();
                        // Found the OP label
                        if (textHeight == 24f) {
                            int baseOP = Math.round(spec.getOrdnancePointCost(null));
                            int newOP = Math.round(spec.getOrdnancePointCost(Global.getSector().getPlayerStats(), selectedShip.getMutableStats()));
                            String baseOPString = Integer.toString(baseOP);
                            String newOPString = Integer.toString(newOP);
                            boolean cannotEquip = Misc.getNegativeHighlightColor().equals(label.getColor());
                            if (baseOP != newOP) {
                                String newText = "(" + newOPString + ")";
                                label.setText(baseOPString + " " + newText);
                                label.setHighlight(baseOPString.length(), label.getText().length());
                                if (cannotEquip) {
                                    label.setColor(Misc.getGrayColor());
                                    label.setHighlightColor(darkRedColor);
                                }
                                else {
                                    label.setHighlightColor(baseOP < newOP ? redColor : greenColor);
                                }
                                ReflectionUtils.invokeMethod(label, "autoSize");
                            }
                            else if (cannotEquip) {
                                label.setColor(darkRedColor);
                            }
                        }
                        // Found the range label
                        if (label.getText().contains("range ")) {
                            WeaponAPI weapon = Global.getCombatEngine().createFakeWeapon(selectedShip, spec.getWeaponId());
                            setSlot(weapon, slot);
                            float trueRange = weapon.getRange();
                            if (trueRange != spec.getMaxRange()) {
                                String newText = "(" + formatNumber(trueRange, spec.getMaxRange(), false) + ")";
                                label.setText(label.getText() + " " + newText);
                                label.setHighlight(newText);
                                label.setHighlightColor(trueRange < spec.getMaxRange() ? (isEnabled ? redColor : darkRedColor) : (isEnabled ? greenColor : darkGreenColor));
                                ReflectionUtils.invokeMethod(label, "autoSize");
                            }
                        }
                    }
                }
            }
        }
    }

    private WeaponSpecAPI getWeaponSpecFromTooltip(StandardTooltipV2Expandable tooltip) {
        if (weaponSpecFieldName == null) {
            for (Field field : tooltip.getClass().getDeclaredFields()) {
                if (WeaponSpecAPI.class.isAssignableFrom(field.getType())) {
                    weaponSpecFieldName = field.getName();
                }
            }
        }

        if (weaponSpecFieldName == null) {
            return null;
        }

        try {
            // Will be null if it's a weapon mounted on a fighter and cycled through with F1
            return (WeaponSpecAPI) ReflectionUtils.getFieldNoCatch(tooltip, weaponSpecFieldName);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Returns whether the ship display changed
     */
    private void updateSelectedShip(UIPanelAPI currentCoreTab) {
        ShipAPI newShip;
        try {
            Object refitPanel = ReflectionUtils.invokeMethodNoCatch(currentCoreTab, "getRefitPanel");
            Object shipDisplay = ReflectionUtils.invokeMethodNoCatch(refitPanel, "getShipDisplay");
            newShip = (ShipAPI) ReflectionUtils.invokeMethodNoCatch(shipDisplay, "getShip");
        }
        catch (Exception e) {
            newShip = null;
        }
        selectedShip = newShip;
    }

    private void updateTotalWeaponFluxDisplayIfNeeded(UIPanelAPI core, UIPanelAPI currentCoreTab) {
        if (selectedShip == null) return;

        // There are two possible places for the flux display to be: either in the refit panel's getModDisplay,
        // or in a ModPickerDialogV3; the latter will overwrite the former

        Object refitPanel = ReflectionUtils.invokeMethod(currentCoreTab, "getRefitPanel");
        Object modDisplay = ReflectionUtils.invokeMethod(refitPanel, "getModDisplay");

        List<?> coreChildren = (List<?>) ReflectionUtils.invokeMethod(core, "getChildrenNonCopy");

        outer:
        for (Object child : coreChildren) {
            if (child instanceof ModPickerDialogV3) {
                List<?> subChildren = (List<?>) ReflectionUtils.invokeMethod(child, "getChildrenNonCopy");
                for (Object subChild : subChildren) {
                    if (subChild.getClass().equals(modDisplay.getClass())) {
                        modDisplay = subChild;
                        break outer;
                    }
                }
            }
        }

        // Find something in the modDisplay that has a LabelAPI field named "label" with the text "weapon flux/sec"
        if (modDisplayWeaponFluxFieldName == null) {
            for (Field field : modDisplay.getClass().getDeclaredFields()) {
                try {
                    Object container = ReflectionUtils.getField(modDisplay, field.getName());
                    LabelAPI label = (LabelAPI) ReflectionUtils.getFieldNoCatch(container, "label");
                    if (label != null && "weapon flux/sec".equals(label.getText())) {
                        modDisplayWeaponFluxFieldName = field.getName();
                    }
                } catch (Exception ignore) {
                }
            }
        }

        if (modDisplayWeaponFluxFieldName != null) {
            Object container = ReflectionUtils.getField(modDisplay, modDisplayWeaponFluxFieldName);
            LabelAPI valueLabel = (LabelAPI) ReflectionUtils.getField(container, "valueLabel");

            // We'll mark our custom number with an initial empty space
            // Whenever syncWithVariant gets called, the empty space will get deleted
            // This way we know when to recompute the modified weapon flux values
            if (valueLabel != null && !valueLabel.getText().startsWith(" ")) {
                float baseSustainedFlux = Float.parseFloat(valueLabel.getText());
                float modifiedSustainedFlux = 0f;
                for (WeaponAPI weapon : selectedShip.getAllWeapons()) {
                    if (weapon.isDecorative()) continue;
                    DynamicWeaponStats stats = new DynamicWeaponStats(weapon);
                    // baseDamage doesn't matter here as it only affects fluxPerDamage
                    modifiedSustainedFlux += stats.fluxData().fluxPerSecondSustained;
                }
                String newText = formatNumber(modifiedSustainedFlux, baseSustainedFlux, true);
                valueLabel.setText(" " + newText);
                float diff = (int) modifiedSustainedFlux - baseSustainedFlux;
                if (Math.abs(diff) > 0.5f) {
                    valueLabel.setHighlight(newText);
                    valueLabel.setHighlightColor(diff > 0f ? redColor : greenColor);
                }
                else {
                    valueLabel.setHighlight("");
                }
            }
        }
    }


    private List<?> getStatsPanelChildren(StandardTooltipV2Expandable tooltip) {
        StandardTooltipV2Expandable statsBlock = (StandardTooltipV2Expandable)
                ReflectionUtils.invokeMethodExtWithClasses(
                        wpd,
                        "getStatsBlock",
                        true,
                        new Class<?>[] {StandardTooltipV2Expandable.class},
                        new Object[] {tooltip});
        // Note: getPanel and getChildrenNonCopy are public, but the return types are obfuscated
        // and therefore may be different between Linux and Windows versions
        UIPanelAPI panel = (UIPanelAPI) ReflectionUtils.invokeMethod(statsBlock, "getPanel");

        return statsBlock == null ? new ArrayList<>() : (List<?>) ReflectionUtils.invokeMethod(panel, "getChildrenNonCopy");
    }

    private void updateGrids(List<?> items, WeaponSpecAPI spec, WeaponSlotAPI slot) {
        if (spec == null || slot == null) return;

        WeaponAPI weapon = Global.getCombatEngine().createFakeWeapon(selectedShip, spec.getWeaponId());
        // Underlying class implements Cloneable
        setSlot(weapon, (WeaponSlotAPI) ReflectionUtils.invokeMethod(slot, "clone"));

        for (Object item : items) {
            if (item instanceof UIPanelAPI) {
                List<?> children = (List<?>) ReflectionUtils.invokeMethod(item, "getChildrenNonCopy");
                if (children == null) continue;
                for (Object subItem : children) {
                    // Should be the grid object
                    try {
                        updateGrid(ReflectionUtils.invokeMethodNoCatch(subItem, "getPrev"), new DynamicWeaponStats(weapon));
                    }
                    catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException ignore) {}
                }
            }
        }
    }

    private void updateGrid(Object grid, DynamicWeaponStats stats) {
        Map<Pair<Integer, Integer>, ?> map = null;
        // Look for a field that's a Map, there should just be one
        for (Field field : grid.getClass().getDeclaredFields()) {
            if (Map.class.isAssignableFrom(field.getType())) {
                String name = field.getName();
                //noinspection unchecked
                map = (Map<Pair<Integer, Integer>, ?>) ReflectionUtils.getField(grid, name);
                break;
            }
        }

        if (map != null) {
            for (Pair<Integer, Integer> key : map.keySet()) {
                LabelAPI name = (LabelAPI) ReflectionUtils.getField(map.get(key), "nameLabel");
                if (name != null) {
                    updateGridValue(grid, stats, name.getText(), key.one, key.two);
                }
            }
        }
    }

    private static class TextWithModifier {
        private String fullText;
        private final String[] highlights;
        private final Color[] highlightColors;

        private TextWithModifier(String full, String[] highlights, Color[] highlightColors) {
            fullText = full;
            this.highlights = highlights;
            this.highlightColors = highlightColors;
        }
    }

    private String formatNumber(float number, float sigFigsReference, boolean forceTruncateInt) {
        if (forceTruncateInt) {
            return Integer.toString((int) number);
        }

        if (Math.abs(Math.round(number) - number) < 0.01f) {
            return Integer.toString(Math.round(number));
        }

        int numDigits = Math.max((int) (Math.log10(sigFigsReference)) + 1, (int) (Math.log10(number)) + 1);
        if (numDigits == 1) {
            return String.format("%.2f", number);
        }
        if (numDigits == 2) {
            return String.format("%.1f", number);
        }
        return Integer.toString(Math.round(number));
    }

    private void makeAdditionalText(float base, float actual, StringBuilder sb, List<String> highlights, List<Color> highlightColors, boolean reverseColors, boolean forceTruncateInt) {
        float diff = actual - base;
        if (Math.abs(diff) > 0.01f) {
            String additionalText = "(" + formatNumber(actual, base, forceTruncateInt) + ")";
            sb.append(" ").append(additionalText);
            highlights.add(additionalText);
            highlightColors.add((diff < 0f && !reverseColors) || (diff > 0f && reverseColors) ? redColor : greenColor);
        }
    }

    private TextWithModifier generateModifierText(float base, float baseSustained, float actual, float actualSustained, boolean reverseColors) {
        StringBuilder sb = new StringBuilder();
        List<String> highlights = new ArrayList<>();
        List<Color> highlightColors = new ArrayList<>();
        sb.append(formatNumber(base, actual, false));
        makeAdditionalText(base, actual, sb, highlights, highlightColors, reverseColors, false);
        sb.append(" (");
        sb.append(formatNumber(baseSustained, actualSustained, false));
        makeAdditionalText(baseSustained, actualSustained, sb, highlights, highlightColors, reverseColors, false);
        sb.append(")");
        return new TextWithModifier(sb.toString(), highlights.toArray(new String[0]), highlightColors.toArray(new Color[0]));
    }

    private TextWithModifier generateModifierText(float base, float actual, boolean reverseColors, boolean forceTruncateInt) {
        StringBuilder sb = new StringBuilder();
        List<String> highlights = new ArrayList<>();
        List<Color> highlightColors = new ArrayList<>();
        sb.append(formatNumber(base, actual,forceTruncateInt));
        makeAdditionalText(base, actual, sb, highlights, highlightColors, reverseColors, forceTruncateInt);
        return new TextWithModifier(sb.toString(), highlights.toArray(new String[0]), highlightColors.toArray(new Color[0]));
    }

    /** Returns:
     *    -(n, null) if [text] is of the form n
     *    -(n, m) if [text] is of the form nxm
     *    -null if [text] is neither, e.g. is SPECIAL
     * */
    private Pair<Float, String> splitDamageText(String text) {
        int index = text.indexOf('x');
        if (index == -1) {
            try {
                float dam = Float.parseFloat(text);
                return new Pair<>(dam, null);
            }
            catch (Exception e) {
                return null;
            }
        }
        else {
            return new Pair<>(Float.parseFloat(text.substring(0, index)), text.substring(index));
        }
    }

    private void setSlot(WeaponAPI weapon, WeaponSlotAPI slot) {
        if (slot != null) {
            String slotFieldName = weaponWeaponSlotFieldNames.get(weapon.getClass());
            if (slotFieldName == null) {
                for (Field field : weapon.getClass().getDeclaredFields()) {
                    if (WeaponSlotAPI.class.isAssignableFrom(field.getType())) {
                        slotFieldName = field.getName();
                        weaponWeaponSlotFieldNames.put(weapon.getClass(), slotFieldName);
                        break;
                    }
                }
            }
            if (slotFieldName != null) {
                ReflectionUtils.setField(weapon, slotFieldName, slot);
            }
        }
    }

    private void updateGridValue(Object grid, DynamicWeaponStats stats, String name, int i, int j) {
        if (selectedShip == null) return;
        String[] highlight;
        Color[] highlightColor;

        Object entry = ReflectionUtils.invokeMethod(grid, "getRow", i, j);
        LabelAPI valueLabel = (LabelAPI) ReflectionUtils.getField(entry, "valueLabel");

        if (valueLabel == null) return;

        WeaponSpecAPI spec = stats.weapon.getSpec();
        TextWithModifier newText = null;
        Pair<Float, String> split;
        float orig;
        switch (name) {
            case "Ordnance points":
                int baseOP = Math.round(spec.getOrdnancePointCost(null));
                int modifiedOP = Math.round(spec.getOrdnancePointCost(Global.getSector().getPlayerStats(), selectedShip.getMutableStats()));
                newText = generateModifierText(baseOP, modifiedOP, true, true);
                break;
            case "Hitpoints":
                if (spec.getProjectileSpec() instanceof MissileSpecAPI mSpec) {
                    var baseHitpoints = mSpec.getHullSpec().getHitpoints();
                    float actualHitpoints = selectedShip.getMutableStats().getMissileHealthBonus().computeEffective(baseHitpoints);
                    newText = generateModifierText(baseHitpoints, actualHitpoints, false, false);
                }
                break;
            case "Range":
                newText = generateModifierText(spec.getMaxRange(), stats.range(), false, false);
                break;
            case "Damage":
                split = splitDamageText(valueLabel.getText());
                if (split != null) {
                    String right = split.two;
                    stats.setBaseDamage(split.one);
                    newText = generateModifierText(
                            split.one,
                            stats.damage(), false, false);
                    if (right != null) {
                        newText.fullText += (newText.highlights.length > 0 ? " " : "") + right;
                    }
                }
                break;
            case "Damage / second":
                newText = generateModifierText(spec.getDerivedStats().getDps(), stats.dpsData().dps, false, false);
                break;
            case "Damage / second (sustained)":
                // Rename the label because it's too long
                ReflectionUtils.invokeMethod(grid, "updateRowText", i, j, "D/s (sustained)");
                newText = generateModifierText(spec.getDerivedStats().getDps(), spec.getDerivedStats().getSustainedDps(), stats.dpsData().dps, stats.dpsData().dpsSustained, false);
                break;
            case "Flux / second":
                newText = generateModifierText(spec.getDerivedStats().getFluxPerSecond(), stats.fluxData().fluxPerSecond, true, false);
                break;
            case "Flux / second (sustained)":
                // Rename the label because it's too long
                ReflectionUtils.invokeMethod(grid, "updateRowText", i, j, "F/s (sustained)");
                newText = generateModifierText(
                        spec.getDerivedStats().getFluxPerSecond(),
                        spec.getDerivedStats().getSustainedFluxPerSecond(),
                        stats.fluxData().fluxPerSecond,
                        stats.fluxData().fluxPerSecondSustained,
                        true
                );
                break;
            case "Flux / shot":
                orig = spec instanceof ProjectileWeaponSpecAPI ? ((ProjectileWeaponSpecAPI) spec).getEnergyPerShot() : 0f;
                newText = generateModifierText(orig, stats.fluxData().fluxPerShot, true, false);
                break;
            case "Flux / non-EMP damage": case "Flux / damage":
                newText = generateModifierText(spec.getDerivedStats().getFluxPerDam(), stats.fluxData().fluxPerDamage, true, false);
                break;
            case "Max charges": case "Max ammo": case "Charges": case "Ammo":
                newText = generateModifierText(spec.getMaxAmmo(), stats.maxAmmo(), false, true);
                break;
            case "Seconds / recharge": case "Seconds / reload":
                orig = spec.getAmmoPerSecond() <= 0f ? 0f : spec.getReloadSize() / spec.getAmmoPerSecond();
                newText = generateModifierText(orig, stats.secondsPerReload(), true, false);
                break;
            case "Charges gained": case "Reload size":
                newText = generateModifierText(spec.getReloadSize(), stats.reloadSize(), false, true);
                break;
            case "Refire delay (seconds)":
                orig = Float.parseFloat(valueLabel.getText());
                newText = generateModifierText(orig, stats.refireDelay(orig), true, false);
            default: break;
        }

        if (newText != null) {
            highlight = newText.highlights;
            highlightColor = newText.highlightColors;
            ReflectionUtils.invokeMethod(grid, "updateRow", i, j, newText.fullText);
            valueLabel.setHighlight(highlight);
            valueLabel.setHighlightColors(highlightColor);
        }
        // Special case: the limited ammo (x) text should also be replaced, this has no name
        else if (valueLabel.getText().contains("imited ammo (")
                || valueLabel.getText().contains("imited charges (")) {
            int actualAmmo = (int) stats.maxAmmo();
            int baseAmmo = spec.getMaxAmmo();
            if (actualAmmo != baseAmmo) {
                String additionalText = "(" + actualAmmo + ")";
                ReflectionUtils.invokeMethod(grid, "updateRow", i, j, valueLabel.getText() + " " + additionalText);
                valueLabel.setHighlight(additionalText);
                valueLabel.setHighlightColor(actualAmmo > spec.getMaxAmmo() ? greenColor : redColor);
            }
        }
    }

    private WeaponPickerDialog findWeaponPickerDialogInList(List<?> items) {
        if (items != null && !items.isEmpty()) {
            // Try the last known index first
            if (lastWeaponDialogIndex >= 0 && lastWeaponDialogIndex < items.size() && items.get(lastWeaponDialogIndex) instanceof WeaponPickerDialog) {
                return (WeaponPickerDialog) items.get(lastWeaponDialogIndex);
            }
            // Not found, have to search through the entire list
            for (int i = 0; i < items.size(); i++) {
                Object comp = items.get(i);
                if (comp instanceof WeaponPickerDialog) {
                    lastWeaponDialogIndex = i;
                    return (WeaponPickerDialog) comp;
                }
            }
        }
        return null;
    }
}