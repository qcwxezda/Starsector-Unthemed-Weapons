package unthemedweapons.util;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.MutableStat;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.loading.ProjectileWeaponSpecAPI;
import com.fs.starfarer.api.loading.WeaponSpecAPI;

public class DynamicWeaponStats {

    public WeaponAPI weapon;
    // baseDamage might not be weapon.getDamage(), i.e. in case of MIRV projectiles
    public float baseDamage;

    public void setBaseDamage(float value) {
        baseDamage = value;
        // Memo stuff needs to be reset
        dpsDataRecord = null;
        fluxDataRecord = null;
    }

    public float range() {
        return weapon.getRange();
    }

    // Non-linear with flat, percent, and multiplier components
    public float damage() {
        MutableShipStatsAPI stats = weapon.getShip().getMutableStats();
        float[] bonuses = new float[] {0f, 0f, 1f};
        if (weapon.isBeam()) {
            bonuses[0] += stats.getBeamWeaponDamageMult().getFlatMod();
            bonuses[1] += stats.getBeamWeaponDamageMult().getPercentMod();
            bonuses[2] *= stats.getBeamWeaponDamageMult().getMult();
        }
        MutableStat relevantStat = null;
        switch (weapon.getType()) {
            case BALLISTIC: relevantStat = stats.getBallisticWeaponDamageMult(); break;
            case ENERGY: relevantStat = stats.getEnergyWeaponDamageMult(); break;
            case MISSILE: relevantStat = stats.getMissileWeaponDamageMult(); break;
        }

        if (relevantStat != null) {
            bonuses[0] += relevantStat.getFlatMod();
            bonuses[1] += relevantStat.getPercentMod();
            bonuses[2] *= relevantStat.getMult();
        }

        return (baseDamage + baseDamage * bonuses[1] / 100f + bonuses[0]) * bonuses[2];
    }

    public static class DpsData {
        public float dps;
        public float dpsSustained;
    }

    private DpsData dpsDataRecord;

    public DpsData dpsData() {
        if (dpsDataRecord != null) {
            return dpsDataRecord;
        }

        WeaponSpecAPI spec = weapon.getSpec();
        float base = spec.getDerivedStats().getDps();
        float baseSustained = spec.getDerivedStats().getSustainedDps();
        float rofMult = 1f;
        if (spec instanceof ProjectileWeaponSpecAPI) {
            rofMult = getRoFMult(weapon);
        }
        else if (weapon.isBurstBeam()) {
            // Burst beam: rate of fire multiplier only affects cooldown, not burst duration
            // Total refire delay is getBurstDuration() + getCooldown() + getChargeupTime, getChargeupTime is not exposed
            float origDelay = spec.getBurstDuration() + weapon.getCooldown();
            origDelay += (float) ReflectionUtils.invokeMethod(spec, "getChargeupTime");
            float newDelay = spec.getBurstDuration() + weapon.getCooldown() / getRoFMult(weapon);
            newDelay += (float) ReflectionUtils.invokeMethod(spec, "getChargeupTime");
            rofMult = origDelay / newDelay;
        }

        float damageRatio = damage() / baseDamage;
        float modified = base * damageRatio * rofMult;
        float modifiedSustained = baseSustained * damageRatio * getAmmoRegenRateMult(weapon);

        DpsData data = new DpsData();
        data.dps = modified;
        data.dpsSustained = modifiedSustained;

        // If weapon doesn't actually use ammo, then sustained DPS should be the same as burst DPS
        if (!(weapon.usesAmmo() && weapon.getAmmoPerSecond() > 0f)) {
            data.dpsSustained = data.dps;
        }
        dpsDataRecord = data;
        return data;
    }

    public static class FluxData {
        public float fluxPerSecond;
        public float fluxPerSecondSustained;
        public float fluxPerShot;
        public float fluxPerDamage;
    }

    private FluxData fluxDataRecord;

    public FluxData fluxData() {
        if (fluxDataRecord != null) {
            return fluxDataRecord;
        }

        WeaponSpecAPI spec = weapon.getSpec();
        float baseFluxPerSecond = spec.getDerivedStats().getFluxPerSecond();
        float baseFluxPerSecondSustained = spec.getDerivedStats().getSustainedFluxPerSecond();
        float baseFluxPerShot = spec instanceof ProjectileWeaponSpecAPI ? ((ProjectileWeaponSpecAPI) spec).getEnergyPerShot() : 0f;
        float baseFluxPerDamage = spec.getDerivedStats().getFluxPerDam();

        float fluxRatio;
        float rofMult = getRoFMult(weapon);
        float regenMult = getAmmoRegenRateMult(weapon);
        if (spec instanceof ProjectileWeaponSpecAPI) {
            int burstSize = spec.getBurstSize();
            if (spec.isInterruptibleBurst()) {
                burstSize = 1;
            }
            fluxRatio = weapon.getFluxCostToFire() * getFluxCostMult(weapon) / burstSize / ((ProjectileWeaponSpecAPI) spec).getEnergyPerShot();
        }
        else {
            if (weapon.isBurstBeam()) {
                float totalTime = spec.getBurstDuration() + weapon.getCooldown();
                totalTime += (float) ReflectionUtils.invokeMethod(spec, "getChargeupTime");
                fluxRatio = weapon.getFluxCostToFire() * getFluxCostMult(weapon) / (totalTime) / spec.getDerivedStats().getFluxPerSecond();
                float origDelay = totalTime;
                float newDelay = totalTime - weapon.getCooldown() + weapon.getCooldown() / getRoFMult(weapon);
                rofMult = origDelay / newDelay;
            }
            else {
                fluxRatio = weapon.getFluxCostToFire() * getFluxCostMult(weapon) / spec.getDerivedStats().getFluxPerSecond();
            }
        }

        FluxData data = new FluxData();
        data.fluxPerSecond = baseFluxPerSecond <= 0f ? 0f : baseFluxPerSecond * fluxRatio;
        data.fluxPerSecondSustained = baseFluxPerSecondSustained <= 0f ? 0f : baseFluxPerSecondSustained * fluxRatio;
        if (!weapon.isBeam() || weapon.isBurstBeam()) {
            data.fluxPerSecond *= rofMult;
            data.fluxPerSecondSustained *= regenMult;
        }

        // If weapon doesn't actually regen ammo, then sustained flux per second should be same as burst flux per second
        if (!(weapon.usesAmmo() && weapon.getAmmoPerSecond() > 0f)) {
            data.fluxPerSecondSustained = data.fluxPerSecond;
        }

        data.fluxPerShot = baseFluxPerShot <= 0f ? 0f : baseFluxPerShot * fluxRatio;
        float invDamageRatio = baseDamage / damage();
        data.fluxPerDamage = baseFluxPerDamage <= 0f ? 0f : baseFluxPerDamage * fluxRatio * invDamageRatio;

        fluxDataRecord = data;
        return data;
    }

    public float maxAmmo() {
        return weapon.getMaxAmmo();
    }
    public float secondsPerReload() {
        WeaponSpecAPI spec = weapon.getSpec();
        return spec.getAmmoPerSecond() <= 0f ? 0f : spec.getReloadSize() / (spec.getAmmoPerSecond() * getAmmoRegenRateMult(weapon));
    }
    public float reloadSize() {
        return weapon.getAmmoTracker().getReloadSize();
    }

    // Refire delay only affects cooldown on burst beams
    public float refireDelay(float baseRefireDelay) {
        float diff = 0f;
        float cooldownRatio = 1f - 1f / getRoFMult(weapon);
        if (weapon.isBurstBeam()) {
            diff = weapon.getCooldown() * cooldownRatio;
        }
        else if (weapon.getSpec() instanceof ProjectileWeaponSpecAPI) {
            diff = baseRefireDelay * cooldownRatio;
        }
        return baseRefireDelay - diff;
    }

    public DynamicWeaponStats(WeaponAPI weapon) {
        this.weapon = weapon;
        baseDamage = weapon.getDamage().getBaseDamage();
    }

    // RoFMult is a simple multiplier using only getModifiedValue
    private static float getRoFMult(WeaponAPI weapon) {
        MutableShipStatsAPI stats = weapon.getShip().getMutableStats();
        float mult = 1f;
        switch (weapon.getType()) {
            case BALLISTIC:
                mult *= stats.getBallisticRoFMult().getModifiedValue();
                break;
            case ENERGY:
                mult *= stats.getEnergyRoFMult().getModifiedValue();
                break;
            case MISSILE:
                mult *= stats.getMissileRoFMult().getModifiedValue();
                break;
            default:
                break;
        }
        return mult;
    }

    // *AmmoRegenMult is a simple multiplier using only getModifiedValue
    private static float getAmmoRegenRateMult(WeaponAPI weapon) {
        MutableShipStatsAPI stats = weapon.getShip().getMutableStats();
        float mult = 1f;
        switch (weapon.getType()) {
            case BALLISTIC:
                mult *= stats.getBallisticAmmoRegenMult().getModifiedValue();
                break;
            case ENERGY:
                mult *= stats.getEnergyAmmoRegenMult().getModifiedValue();
                break;
            case MISSILE:
                mult *= stats.getMissileAmmoRegenMult().getModifiedValue();
                break;
            default:
                break;
        }
        return mult;
    }

    // Anything that's a StatBonus should already be taken into account,
    // anything that's a MutableStat needs to be manually incorporated
    // getBeamWeaponFluxCostMult is just a multiplier!
    private static float getFluxCostMult(WeaponAPI weapon) {
        MutableShipStatsAPI stats = weapon.getShip().getMutableStats();
        float mult = 1f;
        if (weapon.isBeam()) {
            mult *= stats.getBeamWeaponFluxCostMult().getModifiedValue();
        }
        return mult;
    }

}
