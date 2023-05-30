package unthemedweapons.combat.shipsystems;

import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import com.fs.starfarer.api.combat.listeners.DamageTakenModifier;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import com.fs.starfarer.api.util.IntervalUtil;
import org.lwjgl.util.vector.Vector2f;
import unthemedweapons.util.MathUtils;

import java.awt.*;
import java.util.LinkedList;
import java.util.List;

/**
 *  Note: requires that repairLength + timeRepaired <= total activation time, as otherwise armor/hull could be increasing
 *  within the timeRepaired time frame and would need additional logic to pick the "best" time point to repair from.
 */
@SuppressWarnings("unused")
public class ReshaperStats extends BaseShipSystemScript {
    public static Object KEY_SHIP = new Object();
    public static final float timeRepaired = 5f;
    public static final float repairLength = 3f;
    public static final float dissipationMult = 1.5f;
    float hullRepairAmount = 0f;
    float armorRepairAmount = 0f;
    boolean isActive = false;

    public void apply(MutableShipStatsAPI stats, final String id, State state, float effectLevel) {
        if (!(stats.getEntity() instanceof ShipAPI)) return;
        ShipAPI ship = (ShipAPI) stats.getEntity();

        ship.fadeToColor(KEY_SHIP, new Color(64,96,128,255), 0.1f, 0.1f, effectLevel);
        //ship.fadeToColor(KEY_SHIP, new Color(100,100,100,255), 0.1f, 0.1f, effectLevel);
        ship.getEngineController().fadeToOtherColor(KEY_SHIP, new Color(0,0,0,0), new Color(0,0,0,0), effectLevel, 0.75f * effectLevel);
        //ship.setJitter(KEY_SHIP, new Color(100,165,255,5), effectLevel, 1, 0f, 5f);
        ship.setJitterUnder(KEY_SHIP, new Color(192,255,255,80), effectLevel, 30, 0f, 50f * effectLevel);
        //ship.setShowModuleJitterUnder(true);
        ship.addListener(new ReshaperDamageModifier());

        stats.getFluxDissipation().modifyMult(id, dissipationMult * effectLevel);
        List<ReshaperTracker> listeners = ship.getListeners(ReshaperTracker.class);
        if (listeners != null && !listeners.isEmpty()) {
            ReshaperTracker listener = listeners.get(0);
            if (!isActive) {
                ReshaperTracker.HullArmorData data = listener.getHullArmorNSecondsAgo(timeRepaired);
                hullRepairAmount = Math.max(0f, data.hull - ship.getHitpoints());
                float hullPerSecond = hullRepairAmount / repairLength;
                float[][] armorPerSecond = new float[data.armor.length][data.armor[0].length];
                for (int i = 0; i < armorPerSecond.length; i++) {
                    for (int j = 0; j < armorPerSecond[0].length; j++) {
                        float cellRepairAmount = Math.max(0f, data.armor[i][j] - ship.getArmorGrid().getArmorValue(i, j));
                        armorPerSecond[i][j] = cellRepairAmount / repairLength;
                        armorRepairAmount += cellRepairAmount;
                    }
                }
                listener.beginRepairing(armorPerSecond, hullPerSecond, repairLength);
                isActive = true;
            }
        }
    }


    public void unapply(MutableShipStatsAPI stats, String id) {
        if (!(stats.getEntity() instanceof ShipAPI)) return;
        ShipAPI ship = (ShipAPI) stats.getEntity();

        stats.getFluxDissipation().unmodify(id);
        ship.removeListenerOfClass(ReshaperDamageModifier.class);
        hullRepairAmount = 0f;
        armorRepairAmount = 0f;
        isActive = false;
    }


    public StatusData getStatusData(int index, State state, float effectLevel) {
        if (index == 0) {
            return new StatusData(String.format("At least %s%% less damage taken", (int) (ReshaperDamageModifier.minDamageReduction * 100f)), false);
        }

        if (index == 1) {
            return new StatusData(String.format("Flux dissipation improved by %s%%", (int) (100f * (dissipationMult - 1f))), false);
        }

        if (index == 2) {
            return new StatusData(String.format("Regenerating %s hull and %s armor", (int) hullRepairAmount, (int) armorRepairAmount), false);
        }
        return null;
    }

    private static class ReshaperDamageModifier implements DamageTakenModifier {
        public static final String modifyKey = "wpnxt_reshaper";
        public static final float minDamageReduction = 0.6f;
        @Override
        public String modifyDamageTaken(Object param, CombatEntityAPI entity, DamageAPI damage, Vector2f pt, boolean shieldHit) {
            if (shieldHit || !(entity instanceof ShipAPI)) return null;

            float newDamage = modifyDamage(damage.getDamage());
            float multiplier = newDamage / damage.getDamage();
            damage.getModifier().modifyMult(modifyKey, multiplier);

            return modifyKey;
        }

        public float modifyDamage(float amount) {
            return (float) Math.min(amount * (1f - minDamageReduction), Math.pow(amount, 0.85));
        }
    }


    /** There is no "applyOnShipCreation" in ship system code, so this listener needs to be added
     *  to the ship in question via a special hullmod or similar. */
    public static class ReshaperTracker implements AdvanceableListener {
        private final float secondsToTrack = timeRepaired;
        private final float secondsPerInterval = 0.5f;
        private final IntervalUtil trackingInterval = new IntervalUtil(secondsPerInterval, secondsPerInterval);
        private final LinkedList<HullArmorData> hullArmorData = new LinkedList<>();
        private final ShipAPI ship;
        private boolean isRepairing = false;
        private float[][] armorRepairPerSecond;
        private float hullRepairPerSecond;
        private float repairDuration = 0f;
        private float maxRepairDuration = 0f;

        public ReshaperTracker(ShipAPI ship) {
            this.ship = ship;

            for (int i = 0; i < (int) (secondsToTrack / secondsPerInterval) + 1; i++) {
                hullArmorData.add(
                        new HullArmorData(
                                MathUtils.clone2DArray(ship.getArmorGrid().getGrid()),
                                ship.getHitpoints()
                        )
                );
            }
        }

        @Override
        public void advance(float amount) {
            trackingInterval.advance(amount);

            if (trackingInterval.intervalElapsed()) {
                hullArmorData.addFirst(new HullArmorData(
                        MathUtils.clone2DArray(ship.getArmorGrid().getGrid()),
                        ship.getHitpoints()
                ));

                while (hullArmorData.size() > (int) (secondsToTrack / secondsPerInterval) + 1) {
                    hullArmorData.removeLast();
                }
            }

            if (isRepairing) {
                ship.setHitpoints(Math.min(ship.getMaxHitpoints(), ship.getHitpoints() + hullRepairPerSecond * amount));
                ArmorGridAPI armorGrid = ship.getArmorGrid();
                for (int i = 0; i < armorRepairPerSecond.length; i++) {
                    for (int j = 0; j < armorRepairPerSecond[0].length; j++) {
                        float curCellArmor = armorGrid.getArmorValue(i, j);
                        armorGrid.setArmorValue(i, j, Math.min(armorGrid.getMaxArmorInCell(), curCellArmor + armorRepairPerSecond[i][j] * amount));
                    }
                }
                if (repairDuration >= maxRepairDuration) {
                    stopRepairing();
                }
                repairDuration += amount;
            }
        }

        public HullArmorData getHullArmorNSecondsAgo(float n) {
            assert(n <= secondsToTrack);
            int index = (int) ((n - trackingInterval.getElapsed()) / secondsPerInterval);
            float[][] grid1 = index < 0 ? ship.getArmorGrid().getGrid() : hullArmorData.get(index).armor;
            float hull1 = index < 0 ? ship.getHitpoints() : hullArmorData.get(index).hull;
            float[][] grid2 = hullArmorData.get(index + 1).armor;
            float hull2 = hullArmorData.get(index + 1).hull;
            float interpolationAmount =
                    index < 0 ? n / trackingInterval.getElapsed()
                            : ((n - trackingInterval.getElapsed()) % secondsPerInterval) / secondsPerInterval;
            float[][] interpolatedArmor = new float[grid1.length][grid1[0].length];
            float interpolatedHull = MathUtils.interpolate(hull1, hull2, interpolationAmount);
            for (int i = 0; i < grid1.length; i++) {
                for (int j = 0; j < grid1[0].length; j++) {
                    interpolatedArmor[i][j] = MathUtils.interpolate(
                            grid1[i][j],
                            grid2[i][j],
                            interpolationAmount);
                }
            }
            return new HullArmorData(interpolatedArmor, interpolatedHull);
        }

        private void beginRepairing(float[][] armorRepairPerSecond, float hullRepairPerSecond, float maxDuration) {
            this.isRepairing = true;
            this.armorRepairPerSecond = armorRepairPerSecond;
            this.hullRepairPerSecond = hullRepairPerSecond;
            maxRepairDuration = maxDuration;
            repairDuration = 0f;

        }

        private void stopRepairing() {
            this.isRepairing = false;
        }

        public static class HullArmorData {
            final float[][] armor;
            final float hull;

            private HullArmorData(float[][] armor, float hull) {
                this.armor = armor;
                this.hull= hull;
            }
        }
    }
}
