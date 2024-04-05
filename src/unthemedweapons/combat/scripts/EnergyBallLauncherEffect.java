package unthemedweapons.combat.scripts;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.DamageTakenModifier;
import com.fs.starfarer.api.impl.campaign.skills.DamageControl;
import com.fs.starfarer.api.loading.ProjectileSpecAPI;
import com.fs.starfarer.api.loading.ProjectileWeaponSpecAPI;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.Pair;
import org.lwjgl.util.vector.Vector2f;
import unthemedweapons.ModPlugin;
import unthemedweapons.fx.particles.EnergyBallExplosion;
import unthemedweapons.fx.particles.EnergyBallMuzzleFlash;
import unthemedweapons.util.CollisionUtils;
import unthemedweapons.fx.render.EnergyBallRenderer;
import unthemedweapons.util.MathUtils;

import java.util.*;
import java.util.List;

// Stuff to remember:
// Charge rate doesn't increase damage linearly; rather, damage increases proportionally to (charge rate)^1.5.
// Put this in description?
public class EnergyBallLauncherEffect implements EveryFrameWeaponEffectPluginWithAdvanceAfter, WeaponEffectPluginWithInit, OnFireEffectPlugin {

    public static final float maxProjectileSize = 120f;
    public static final float fullChargeDamageMultiplier = 12.5f;
    public static final float fullChargeFluxCostMultiplier = 12.5f;
    public static final float chargeTime = 20f;
    public static final float minExplosionRadius = 111f;
    public static final float ignoreDefenseMin = 0.1f, ignoreDefenseMax = 1f;
    public static final String modifyKey = "wpnxt_energyball";

    private final List<ProjectileData> projectiles = new LinkedList<>();
    private EnergyBallRenderer projRenderer;
    private float chargeLevelOnAdvance = 0f;

    public static class ProjectileData {
        DamagingProjectileAPI proj;

        float initialDamage, currDamage, maxDamage;

        private ProjectileData(DamagingProjectileAPI proj, float initialDamage, float maxDamage) {
            this.proj = proj;
            this.initialDamage = currDamage = initialDamage;
            this.maxDamage = maxDamage;
        }

        public float getSize() {
            return maxProjectileSize * (float) Math.sqrt(proj.getBaseDamageAmount() / maxDamage);
        }
    }

    @Override
    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {

        chargeLevelOnAdvance = weapon.getCooldownRemaining() > 0f ? 0f : weapon.getChargeLevel();

        if (chargeLevelOnAdvance > 0f) {
            Global.getSoundPlayer().playLoop(
                    "wpnxt_energyballlauncher_loop",
                    weapon,
                    0.5f + 1.25f*chargeLevelOnAdvance,
                    0.5f + 0.75f*chargeLevelOnAdvance,
                    weapon.getFirePoint(0),
                    weapon.getShip().getVelocity());

            float fluxCostThisFrame = (weapon.getFluxCostToFire() * fullChargeFluxCostMultiplier) / chargeTime * amount;
            FluxTrackerAPI tracker = weapon.getShip().getFluxTracker();
            if (tracker.getCurrFlux() + fluxCostThisFrame > tracker.getMaxFlux()) {
                onFailToFire(weapon);
            } else {
                tracker.setCurrFlux(tracker.getCurrFlux() + fluxCostThisFrame);
            }
        }

        Iterator<ProjectileData> itr = projectiles.iterator();
        while (itr.hasNext()) {
            ProjectileData data = itr.next();
            DamagingProjectileAPI proj = data.proj;

            if (proj.isExpired()) {
                itr.remove();
                continue;
            }

            // Projectile destroyed by outside factor
            if (!proj.isFading() && !engine.isEntityInPlay(proj) && !proj.didDamage()) {
                destroyProjectile(data, null, engine);
                itr.remove();
                continue;
            }

            CollisionUtils.ClosestCollisionData collisionData = CollisionUtils.circleCollisionCheck(
                    proj.getLocation(),
                    data.getSize() * 0.5f,
                    null,
                    proj.getSource(),
                    true,
                    engine);

            if (collisionData == null) {
                continue;
            }

            CombatEntityAPI target = collisionData.entity;
            float targetPrevFlux = target instanceof ShipAPI ? ((ShipAPI) target).getFluxTracker().getCurrFlux() : 0f;

            applyDamage(proj, target, collisionData.point, proj.getDamageAmount(), data, engine, true);

            // Pass through everything it destroys
            if (target.getHitpoints() <= 0f && !(target instanceof ShipAPI)) {
                continue;
            }
            // Pass through missiles
            if (target instanceof DamagingProjectileAPI) {
                continue;
            }
            if (target instanceof ShipAPI) {
                ShipAPI ship = (ShipAPI) target;
                // Pass through fighters
                if (ship.isFighter()) {
                    continue;
                }
                // Pass through overloaded ships,
                // but reduce damage by amount dealt to shield
                if (collisionData.shieldHit && ship.getFluxTracker().isOverloaded()) {
                    float shieldDamageDealt = (ship.getMaxFlux() - targetPrevFlux) / ship.getShield().getFluxPerPointOfDamage();
                    // setDamageAmount sets the base damage, and the argument is actual damage, so need to divide
                    // by the ratio to convert the actual damage to base damage
                    float damageRatio = proj.getDamageAmount() / proj.getBaseDamageAmount();
                    float newDamageAmount = (proj.getDamageAmount() - shieldDamageDealt) / damageRatio;
                    if (newDamageAmount < proj.getProjectileSpec().getDamage().getBaseDamage()) {
                        destroyProjectile(data, collisionData, engine);
                        itr.remove();
                    }
                    else {
                        proj.setDamageAmount(newDamageAmount);
                        data.currDamage = newDamageAmount;
                    }
                    continue;
                }
            }

            // Otherwise, destroy this projectile
            destroyProjectile(data, collisionData, engine);
            itr.remove();
        }
    }

    private float getModifyMultAmount(float ignoreDefenseFrac, float originalMult) {
        return 1 + ignoreDefenseFrac * (1f / MathUtils.clamp(originalMult, 0.001f, 1f) - 1f);
    }

    private void prepareShipForHit(ShipAPI ship, ProjectileData data, List<DamageTakenModifier> listenersToRemoveRef) {
        float ignoreDefense = ignoreDefenseMin + (ignoreDefenseMax - ignoreDefenseMin) * (data.currDamage / data.maxDamage);
        MutableShipStatsAPI stats = ship.getMutableStats();
        stats.getShieldDamageTakenMult().modifyMult(modifyKey, getModifyMultAmount(ignoreDefense, stats.getShieldDamageTakenMult().getModifiedValue()));
        stats.getArmorDamageTakenMult().modifyMult(modifyKey, getModifyMultAmount(ignoreDefense, stats.getArmorDamageTakenMult().getModifiedValue()));
        stats.getHullDamageTakenMult().modifyMult(modifyKey, getModifyMultAmount(ignoreDefense, stats.getHullDamageTakenMult().getModifiedValue()));
        stats.getProjectileDamageTakenMult().modifyMult(modifyKey, getModifyMultAmount(ignoreDefense, stats.getProjectileDamageTakenMult().getModifiedValue()));
        stats.getWeaponDamageTakenMult().modifyMult(modifyKey, getModifyMultAmount(ignoreDefense, stats.getWeaponDamageTakenMult().getModifiedValue()));
        stats.getEnergyDamageTakenMult().modifyMult(modifyKey, getModifyMultAmount(ignoreDefense, stats.getEnergyDamageTakenMult().getModifiedValue()));
        stats.getEnergyShieldDamageTakenMult().modifyMult(modifyKey, getModifyMultAmount(ignoreDefense, stats.getEnergyShieldDamageTakenMult().getModifiedValue()));
        listenersToRemoveRef.addAll(ship.getListeners(DamageControl.DamageControlDamageTakenMod.class));
        ship.removeListenerOfClass(DamageControl.DamageControlDamageTakenMod.class);
        damageListener.setData(data);
        ship.addListener(damageListener);
    }

    private void resetShipAfterHit(ShipAPI ship, List<DamageTakenModifier> listenersToAddBack) {
        MutableShipStatsAPI stats = ship.getMutableStats();
        stats.getShieldDamageTakenMult().unmodify(modifyKey);
        stats.getArmorDamageTakenMult().unmodify(modifyKey);
        stats.getHullDamageTakenMult().unmodify(modifyKey);
        stats.getProjectileDamageTakenMult().unmodify(modifyKey);
        stats.getWeaponDamageTakenMult().unmodify(modifyKey);
        stats.getEnergyDamageTakenMult().unmodify(modifyKey);
        stats.getEnergyShieldDamageTakenMult().unmodify(modifyKey);
        ship.removeListener(damageListener);
        for (DamageTakenModifier listener : listenersToAddBack) {
            ship.addListener(listener);
        }
    }

    private void applyDamage(
            Object damageModifierParam,
            CombatEntityAPI target,
            Vector2f pt,
            float damage,
            ProjectileData data,
            CombatEngineAPI engine,
            boolean addHitParticle) {

        List<DamageTakenModifier> damageControlListeners = new ArrayList<>();

        if (target instanceof ShipAPI && !((ShipAPI) target).isFighter()) {
            prepareShipForHit((ShipAPI) target, data, damageControlListeners);
        }
        engine.applyDamage(
                damageModifierParam,
                target,
                pt,
                damage,
                data.proj.getDamageType(),
                data.proj.getEmpAmount(),
                false,
                data.proj.isFading(),
                data.proj.getSource(),
                true);
        if (target instanceof ShipAPI && !((ShipAPI) target).isFighter()) {
            resetShipAfterHit((ShipAPI) target, damageControlListeners);
        }

        if (addHitParticle) {
            engine.addHitParticle(
                    pt,
                    target.getVelocity().length() <= 50f ? target.getVelocity() : new Vector2f(),
                    data.proj.getProjectileSpec().getHitGlowRadius(),
                    1f,
                    data.proj.getProjectileSpec().getFringeColor());
        }
    }

    private void destroyProjectile(ProjectileData data, CollisionUtils.ClosestCollisionData collisionData, CombatEngineAPI engine) {

        DamagingProjectileAPI proj = data.proj;
        Global.getCombatEngine().removeEntity(proj);
        if (proj.isFading()) return;

        float explosionRadius =
                minExplosionRadius * Math.max(1f, (float) Math.sqrt(proj.getBaseDamageAmount() / proj.getProjectileSpec().getDamage().getBaseDamage()));

        Iterator<Object> itr = engine.getAllObjectGrid().getCheckIterator(proj.getLocation(), 2f*explosionRadius, 2f*explosionRadius);
        Set<CombatEntityAPI> alreadyDamaged = new HashSet<>();
        while (itr.hasNext()) {
            Object o = itr.next();
            if (!CollisionUtils.canCollide(o, null, proj.getSource(), true)) continue;
            if (collisionData != null && o.equals(collisionData.entity)) continue;
            CombatEntityAPI entity = (CombatEntityAPI) o;
            if (alreadyDamaged.contains(entity)) continue;

            alreadyDamaged.add(entity);
            Pair<Vector2f, Boolean> pair =
                    CollisionUtils.rayCollisionCheckEntity(
                            proj.getLocation(),
                            entity instanceof ShipAPI && !((ShipAPI) entity).isFighter() ? MathUtils.getVertexCenter(entity): entity.getLocation(),
                            entity);
            if (pair.one == null) continue;
            float dist = Misc.getDistance(pair.one, proj.getLocation());
            if (dist > explosionRadius) continue;
            float damage = proj.getDamageAmount() * 0.5f + proj.getDamageAmount() * 0.5f * (explosionRadius - dist) / explosionRadius;

            applyDamage(proj, entity, pair.one, damage, data, engine, false);
        }

        if (ModPlugin.particleEngineEnabled) {
            EnergyBallExplosion.makeExplosion(proj.getLocation(), explosionRadius);
        }

        float explosionRadiusRatio = explosionRadius / minExplosionRadius;
        Global.getSoundPlayer().playSound("wpnxt_energyball_hit", 1.5f - (explosionRadiusRatio - 1f) / 6f + MathUtils.randBetween(-0.03f, 0.03f), 0.6f + (explosionRadiusRatio - 1f) / 5f + MathUtils.randBetween(-0.03f, 0.03f), proj.getLocation(), new Vector2f());
    }

    @Override
    public void onFire(DamagingProjectileAPI proj, WeaponAPI weapon, CombatEngineAPI engine) {
        float damageMult = mapChargeLevelToDamageMult(chargeLevelOnAdvance);
        // Full charge shot
        if (proj != null) {
            Global.getCombatEngine().removeEntity(proj);
        }
        // Semi-charged shot, need to manually add the base flux cost
        else {
            FluxTrackerAPI flux = weapon.getShip().getFluxTracker();
            float costToFire = weapon.getFluxCostToFire();
            if (flux.getCurrFlux() + costToFire> flux.getMaxFlux()) {
                onFailToFire(weapon);
                return;
            }
            flux.setCurrFlux(flux.getCurrFlux() + costToFire);
            weapon.setRemainingCooldownTo(weapon.getCooldown());
        }

        float projectileSize = getProjectileSize();
        setProjectileSpeed(weapon, damageMult);

        proj = (DamagingProjectileAPI) engine.spawnProjectile(
                weapon.getShip(),
                weapon,
                weapon.getId(),
                weapon.getFirePoint(0),
                weapon.getCurrAngle(),
                weapon.getShip().getVelocity());
        proj.setCollisionClass(CollisionClass.NONE);
        proj.setCollisionRadius(projectileSize);

        Vector2f firePoint = weapon.getFirePoint(0);
        float maxDamage = proj.getBaseDamageAmount() * fullChargeDamageMultiplier;
        float actualDamage = proj.getBaseDamageAmount() * damageMult;
        proj.setDamageAmount(actualDamage);
        projectiles.add(new ProjectileData(proj, actualDamage, maxDamage));
        engine.addLayeredRenderingPlugin(new EnergyBallRenderer(proj, this, maxDamage, projRenderer.facing, projRenderer.modifiedTime));

        Global.getSoundPlayer().playSound("wpnxt_energyballlauncher_fire", 1.8f - damageMult / fullChargeDamageMultiplier, 1.5f * (float) Math.sqrt(damageMult / fullChargeDamageMultiplier), firePoint, new Vector2f());

        if (ModPlugin.particleEngineEnabled) {
            EnergyBallMuzzleFlash.makeMuzzleFlash(weapon, chargeLevelOnAdvance);
        }
    }

    @Override
    public void advanceAfter(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
        if (weapon.getChargeLevel() <= 0f && chargeLevelOnAdvance > 0f && !weapon.isDisabled()) {
            onFire(null, weapon, engine);
        }
    }

    @Override
    public void init(WeaponAPI weapon) {
        projRenderer = new EnergyBallRenderer(weapon, this);
        ((ProjectileWeaponSpecAPI) weapon.getSpec()).setChargeTime(chargeTime);
        weapon.ensureClonedSpec();

        if (Global.getCombatEngine() != null) {
            Global.getCombatEngine().addLayeredRenderingPlugin(projRenderer);
        }
    }

    /** Maps (0, 1) to (1, fullChargeDamageMultiplier) */
    public static float mapChargeLevelToDamageMult(float chargeLevel) {
        return 1f + (fullChargeDamageMultiplier - 1f) * (float) Math.pow(chargeLevel, 1.5);
    }

    public float getProjectileSize() {
        return chargeLevelOnAdvance <= 0f ? 0f : getProjectileSize(chargeLevelOnAdvance);
    }

    public static float getProjectileSize(float chargeLevel) {
        return  maxProjectileSize * (float) Math.sqrt(mapChargeLevelToDamageMult(chargeLevel) / fullChargeDamageMultiplier);
    }

    public static float getMoveSpeed(float damageMult) {
        return 900f - ((damageMult - 1f) / (fullChargeDamageMultiplier - 1f)) * 650f;
    }

    private void setProjectileSpeed(WeaponAPI weapon, float damageMult) {
        weapon.getSpec().setProjectileSpeed(getMoveSpeed(damageMult));
        ((ProjectileSpecAPI) weapon.getSpec().getProjectileSpec()).setMoveSpeed(getMoveSpeed(damageMult));
    }

    private void onFailToFire(WeaponAPI weapon) {
        weapon.setRemainingCooldownTo(weapon.getCooldown() * 0.5f);
    }

    private final DamageListener damageListener = new DamageListener();
    private static class DamageListener implements DamageTakenModifier {

        private ProjectileData data;

        private void setData(ProjectileData data) {
            this.data = data;
        }

        @Override
        public String modifyDamageTaken(Object param, CombatEntityAPI entity, DamageAPI damage, Vector2f pt, boolean shieldHit) {
            if (!(entity instanceof ShipAPI)) return null;

            float baseMultiplier = damage.getModifier().getModifiedValue();
            float chargeLevel = data.currDamage / data.maxDamage;
            float reductionFraction = ignoreDefenseMin + (ignoreDefenseMax - ignoreDefenseMin) * chargeLevel;
            float compensator = 1 + reductionFraction * (1f / MathUtils.clamp(baseMultiplier, 0.001f, 1f) - 1f);

            if (compensator > 1f || damage.getBaseDamage() < data.currDamage) {
                damage.setDamage(data.currDamage);
                damage.getModifier().modifyMult(modifyKey, compensator);
                return modifyKey;
            }

            return null;
        }
    }
}
