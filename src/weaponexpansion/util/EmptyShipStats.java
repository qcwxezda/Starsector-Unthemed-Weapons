package weaponexpansion.util;

import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.CombatListenerManagerAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.util.DynamicStatsAPI;

import java.util.List;

public class EmptyShipStats implements MutableShipStatsAPI {
    @Override
    public CombatEntityAPI getEntity() {
        return null;
    }

    @Override
    public FleetMemberAPI getFleetMember() {
        return null;
    }

    @Override
    public MutableStat getMaxSpeed() {
        return null;
    }

    @Override
    public MutableStat getAcceleration() {
        return null;
    }

    @Override
    public MutableStat getDeceleration() {
        return null;
    }

    @Override
    public MutableStat getMaxTurnRate() {
        return null;
    }

    @Override
    public MutableStat getTurnAcceleration() {
        return null;
    }

    @Override
    public MutableStat getFluxCapacity() {
        return null;
    }

    @Override
    public MutableStat getFluxDissipation() {
        return null;
    }

    @Override
    public MutableStat getWeaponMalfunctionChance() {
        return null;
    }

    @Override
    public MutableStat getEngineMalfunctionChance() {
        return null;
    }

    @Override
    public MutableStat getCriticalMalfunctionChance() {
        return null;
    }

    @Override
    public MutableStat getShieldMalfunctionChance() {
        return null;
    }

    @Override
    public MutableStat getShieldMalfunctionFluxLevel() {
        return null;
    }

    @Override
    public MutableStat getMaxCombatReadiness() {
        return null;
    }

    @Override
    public StatBonus getCRPerDeploymentPercent() {
        return null;
    }

    @Override
    public StatBonus getPeakCRDuration() {
        return null;
    }

    @Override
    public StatBonus getCRLossPerSecondPercent() {
        return null;
    }

    @Override
    public MutableStat getFluxDamageTakenMult() {
        return null;
    }

    @Override
    public MutableStat getEmpDamageTakenMult() {
        return null;
    }

    @Override
    public MutableStat getHullDamageTakenMult() {
        return null;
    }

    @Override
    public MutableStat getArmorDamageTakenMult() {
        return null;
    }

    @Override
    public MutableStat getShieldDamageTakenMult() {
        return null;
    }

    @Override
    public MutableStat getEngineDamageTakenMult() {
        return null;
    }

    @Override
    public MutableStat getWeaponDamageTakenMult() {
        return null;
    }

    @Override
    public MutableStat getBeamDamageTakenMult() {
        return null;
    }

    @Override
    public MutableStat getMissileDamageTakenMult() {
        return null;
    }

    @Override
    public MutableStat getProjectileDamageTakenMult() {
        return null;
    }

    @Override
    public MutableStat getEnergyDamageTakenMult() {
        return null;
    }

    @Override
    public MutableStat getKineticDamageTakenMult() {
        return null;
    }

    @Override
    public MutableStat getHighExplosiveDamageTakenMult() {
        return null;
    }

    @Override
    public MutableStat getFragmentationDamageTakenMult() {
        return null;
    }

    @Override
    public MutableStat getBeamShieldDamageTakenMult() {
        return null;
    }

    @Override
    public MutableStat getMissileShieldDamageTakenMult() {
        return null;
    }

    @Override
    public MutableStat getProjectileShieldDamageTakenMult() {
        return null;
    }

    @Override
    public MutableStat getEnergyShieldDamageTakenMult() {
        return null;
    }

    @Override
    public MutableStat getKineticShieldDamageTakenMult() {
        return null;
    }

    @Override
    public MutableStat getHighExplosiveShieldDamageTakenMult() {
        return null;
    }

    @Override
    public MutableStat getFragmentationShieldDamageTakenMult() {
        return null;
    }

    @Override
    public MutableStat getBeamWeaponDamageMult() {
        return null;
    }

    @Override
    public MutableStat getEnergyWeaponDamageMult() {
        return null;
    }

    @Override
    public MutableStat getBallisticWeaponDamageMult() {
        return null;
    }

    @Override
    public MutableStat getMissileWeaponDamageMult() {
        return null;
    }

    @Override
    public StatBonus getEnergyWeaponFluxCostMod() {
        return null;
    }

    @Override
    public StatBonus getBallisticWeaponFluxCostMod() {
        return null;
    }

    @Override
    public StatBonus getMissileWeaponFluxCostMod() {
        return null;
    }

    @Override
    public MutableStat getBeamWeaponFluxCostMult() {
        return null;
    }

    @Override
    public MutableStat getShieldUpkeepMult() {
        return null;
    }

    @Override
    public MutableStat getShieldAbsorptionMult() {
        return null;
    }

    @Override
    public MutableStat getShieldTurnRateMult() {
        return null;
    }

    @Override
    public MutableStat getShieldUnfoldRateMult() {
        return null;
    }

    @Override
    public MutableStat getMissileRoFMult() {
        return null;
    }

    @Override
    public MutableStat getBallisticRoFMult() {
        return null;
    }

    @Override
    public MutableStat getEnergyRoFMult() {
        return null;
    }

    @Override
    public StatBonus getPhaseCloakActivationCostBonus() {
        return null;
    }

    @Override
    public StatBonus getPhaseCloakUpkeepCostBonus() {
        return null;
    }

    @Override
    public StatBonus getEnergyWeaponRangeBonus() {
        return null;
    }

    @Override
    public StatBonus getBallisticWeaponRangeBonus() {
        return null;
    }

    @Override
    public StatBonus getMissileWeaponRangeBonus() {
        return null;
    }

    @Override
    public StatBonus getBeamWeaponRangeBonus() {
        return null;
    }

    @Override
    public StatBonus getWeaponTurnRateBonus() {
        return null;
    }

    @Override
    public StatBonus getBeamWeaponTurnRateBonus() {
        return null;
    }

    @Override
    public MutableStat getCombatEngineRepairTimeMult() {
        return null;
    }

    @Override
    public MutableStat getCombatWeaponRepairTimeMult() {
        return null;
    }

    @Override
    public StatBonus getWeaponHealthBonus() {
        return null;
    }

    @Override
    public StatBonus getEngineHealthBonus() {
        return null;
    }

    @Override
    public StatBonus getArmorBonus() {
        return null;
    }

    @Override
    public StatBonus getHullBonus() {
        return null;
    }

    @Override
    public StatBonus getShieldArcBonus() {
        return null;
    }

    @Override
    public StatBonus getBallisticAmmoBonus() {
        return null;
    }

    @Override
    public StatBonus getEnergyAmmoBonus() {
        return null;
    }

    @Override
    public StatBonus getMissileAmmoBonus() {
        return null;
    }

    @Override
    public MutableStat getEccmChance() {
        return null;
    }

    @Override
    public MutableStat getMissileGuidance() {
        return null;
    }

    @Override
    public StatBonus getSightRadiusMod() {
        return null;
    }

    @Override
    public MutableStat getHullCombatRepairRatePercentPerSecond() {
        return null;
    }

    @Override
    public MutableStat getMaxCombatHullRepairFraction() {
        return null;
    }

    @Override
    public MutableStat getHullRepairRatePercentPerSecond() {
        return null;
    }

    @Override
    public MutableStat getMaxHullRepairFraction() {
        return null;
    }

    @Override
    public StatBonus getEffectiveArmorBonus() {
        return null;
    }

    @Override
    public StatBonus getHitStrengthBonus() {
        return null;
    }

    @Override
    public MutableStat getDamageToTargetEnginesMult() {
        return null;
    }

    @Override
    public MutableStat getDamageToTargetWeaponsMult() {
        return null;
    }

    @Override
    public MutableStat getDamageToTargetShieldsMult() {
        return null;
    }

    @Override
    public MutableStat getAutofireAimAccuracy() {
        return null;
    }

    @Override
    public MutableStat getMaxRecoilMult() {
        return null;
    }

    @Override
    public MutableStat getRecoilPerShotMult() {
        return null;
    }

    @Override
    public MutableStat getRecoilDecayMult() {
        return null;
    }

    @Override
    public StatBonus getOverloadTimeMod() {
        return null;
    }

    @Override
    public MutableStat getZeroFluxSpeedBoost() {
        return null;
    }

    @Override
    public MutableStat getZeroFluxMinimumFluxLevel() {
        return null;
    }

    @Override
    public MutableStat getCrewLossMult() {
        return null;
    }

    @Override
    public MutableStat getHardFluxDissipationFraction() {
        return null;
    }

    @Override
    public StatBonus getFuelMod() {
        return null;
    }

    @Override
    public StatBonus getFuelUseMod() {
        return null;
    }

    @Override
    public StatBonus getMinCrewMod() {
        return null;
    }

    @Override
    public StatBonus getMaxCrewMod() {
        return null;
    }

    @Override
    public StatBonus getCargoMod() {
        return null;
    }

    @Override
    public StatBonus getHangarSpaceMod() {
        return null;
    }

    @Override
    public StatBonus getMissileMaxSpeedBonus() {
        return null;
    }

    @Override
    public StatBonus getMissileAccelerationBonus() {
        return null;
    }

    @Override
    public StatBonus getMissileMaxTurnRateBonus() {
        return null;
    }

    @Override
    public StatBonus getMissileTurnAccelerationBonus() {
        return null;
    }

    @Override
    public MutableStat getProjectileSpeedMult() {
        return null;
    }

    @Override
    public MutableStat getVentRateMult() {
        return null;
    }

    @Override
    public MutableStat getBaseCRRecoveryRatePercentPerDay() {
        return null;
    }

    @Override
    public MutableStat getMaxBurnLevel() {
        return null;
    }

    @Override
    public MutableStat getFighterRefitTimeMult() {
        return null;
    }

    @Override
    public MutableStat getRepairRatePercentPerDay() {
        return null;
    }

    @Override
    public MutableStat getSensorProfile() {
        return null;
    }

    @Override
    public MutableStat getSensorStrength() {
        return null;
    }

    @Override
    public DynamicStatsAPI getDynamic() {
        return null;
    }

    @Override
    public MutableStat getSuppliesToRecover() {
        return null;
    }

    @Override
    public MutableStat getSuppliesPerMonth() {
        return null;
    }

    @Override
    public MutableStat getWeaponRangeThreshold() {
        return null;
    }

    @Override
    public MutableStat getWeaponRangeMultPastThreshold() {
        return null;
    }

    @Override
    public MutableStat getTimeMult() {
        return null;
    }

    @Override
    public StatBonus getBeamPDWeaponRangeBonus() {
        return null;
    }

    @Override
    public StatBonus getNonBeamPDWeaponRangeBonus() {
        return null;
    }

    @Override
    public MutableStat getMinArmorFraction() {
        return null;
    }

    @Override
    public MutableStat getMaxArmorDamageReduction() {
        return null;
    }

    @Override
    public MutableStat getNumFighterBays() {
        return null;
    }

    @Override
    public StatBonus getMissileHealthBonus() {
        return null;
    }

    @Override
    public StatBonus getPhaseCloakCooldownBonus() {
        return null;
    }

    @Override
    public StatBonus getSystemCooldownBonus() {
        return null;
    }

    @Override
    public StatBonus getSystemRegenBonus() {
        return null;
    }

    @Override
    public StatBonus getSystemUsesBonus() {
        return null;
    }

    @Override
    public StatBonus getSystemRangeBonus() {
        return null;
    }

    @Override
    public MutableStat getKineticArmorDamageTakenMult() {
        return null;
    }

    @Override
    public MutableStat getDamageToFighters() {
        return null;
    }

    @Override
    public MutableStat getDamageToMissiles() {
        return null;
    }

    @Override
    public MutableStat getDamageToFrigates() {
        return null;
    }

    @Override
    public MutableStat getDamageToDestroyers() {
        return null;
    }

    @Override
    public MutableStat getDamageToCruisers() {
        return null;
    }

    @Override
    public MutableStat getDamageToCapital() {
        return null;
    }

    @Override
    public StatBonus getCriticalMalfunctionDamageMod() {
        return null;
    }

    @Override
    public MutableStat getBreakProb() {
        return null;
    }

    @Override
    public StatBonus getFighterWingRange() {
        return null;
    }

    @Override
    public ShipVariantAPI getVariant() {
        return null;
    }

    @Override
    public MutableStat getRecoilPerShotMultSmallWeaponsOnly() {
        return null;
    }

    @Override
    public MutableStat getEnergyWeaponFluxBasedBonusDamageMagnitude() {
        return null;
    }

    @Override
    public MutableStat getEnergyWeaponFluxBasedBonusDamageMinLevel() {
        return null;
    }

    @Override
    public MutableStat getAllowZeroFluxAtAnyLevel() {
        return null;
    }

    @Override
    public CombatListenerManagerAPI getListenerManager() {
        return null;
    }

    @Override
    public void addListener(Object o) {

    }

    @Override
    public void removeListener(Object o) {

    }

    @Override
    public void removeListenerOfClass(Class<?> aClass) {

    }

    @Override
    public boolean hasListener(Object o) {
        return false;
    }

    @Override
    public boolean hasListenerOfClass(Class<?> aClass) {
        return false;
    }

    @Override
    public <T> List<T> getListeners(Class<T> aClass) {
        return null;
    }

    @Override
    public MutableStat getBallisticProjectileSpeedMult() {
        return null;
    }

    @Override
    public MutableStat getEnergyProjectileSpeedMult() {
        return null;
    }

    @Override
    public MutableStat getMissileAmmoRegenMult() {
        return null;
    }

    @Override
    public MutableStat getEnergyAmmoRegenMult() {
        return null;
    }

    @Override
    public MutableStat getBallisticAmmoRegenMult() {
        return null;
    }
}
