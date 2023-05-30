package unthemedweapons.combat.scripts;

import com.fs.starfarer.api.combat.*;

@SuppressWarnings("unused")
public class EnhTaclaserEffect implements BeamEffectPluginWithReset {

    ShipAPI affectedShip;

    static final String customDataKey = "wpnxt_enhtaclasercount";
    static final String modificationSource = "wpnxt_enhtaclaser";
    static final float debuffPerBeam = 0.03f, maxDebuff = 0.15f;

    @Override
    public void advance(float amount, CombatEngineAPI engine, BeamAPI beam) {
        // Ignore frames where the beam does no damage
        if (beam.getDamage().getDpsDuration() <= 0) {
            return;
        }

        if (!(beam.getDamageTarget() instanceof ShipAPI)) {
            // Beam is no longer hitting the target; clear affectedShip
            if (affectedShip != null) {
                modifyBeamCount(affectedShip, -1);
                affectedShip = null;
            }
            return;
        }

        ShipAPI target = (ShipAPI) beam.getDamageTarget();
        if (!target.getCustomData().containsKey(customDataKey)) {
            target.setCustomData(customDataKey, 0);
        }

        if (!target.equals(affectedShip)) {
            // Clear affectedShip as a beam can only affect one ship at time
            if (affectedShip != null) {
                modifyBeamCount(affectedShip, -1);
            }

            modifyBeamCount(target, 1);

            affectedShip = target;
        }
    }

    @Override
    public void reset() {
        if (affectedShip != null) {
            modifyBeamCount(affectedShip, -1);
            affectedShip = null;
        }
    }

    private void modifyBeamCount(ShipAPI ship, int modifyAmount) {
        int cnt = (int) ship.getCustomData().get(customDataKey) + modifyAmount;
        ship.setCustomData(customDataKey, cnt);
        float debuff = Math.min(cnt * debuffPerBeam, maxDebuff);
        ship.getMutableStats().getEnergyWeaponFluxCostMod().modifyMult(modificationSource, 1f+debuff);
        ship.getMutableStats().getBallisticWeaponFluxCostMod().modifyMult(modificationSource, 1f+debuff);
    }
}