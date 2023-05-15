package weaponexpansion.combat.scripts;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.loading.DamagingExplosionSpec;
import com.fs.starfarer.api.loading.ProjectileSpecAPI;
import com.fs.starfarer.api.loading.ProjectileWeaponSpecAPI;
import org.lwjgl.util.vector.Vector2f;
import weaponexpansion.ModPlugin;
import weaponexpansion.particles.EnergyBallExplosion;
import weaponexpansion.particles.EnergyBallMuzzleFlash;
import weaponexpansion.particles.Explosion;
import weaponexpansion.util.EnergyBallRenderer;
import weaponexpansion.util.ExplosionRingRenderer;
import weaponexpansion.util.Utils;

import java.awt.*;
import java.util.*;
import java.util.List;

public class EnergyBallLauncherEffect implements EveryFrameWeaponEffectPluginWithAdvanceAfter, WeaponEffectPluginWithInit, OnFireEffectPlugin {

    public static final float maxProjectileSize = 80f;
    public static final float fullChargeDamageMultiplier = 15f;
    public static final float fullChargeFluxCostMultiplier = 20f;
    public static final float chargeTime = 24f;
    public static final float minExplosionRadius = 100f;

    private WeaponAPI weapon;
    private final List<ProjectileData> projectiles = new LinkedList<>();
    private EnergyBallRenderer projRenderer;
    private float chargeLevelOnAdvance = 0f;

    public static class ProjectileData {
        DamagingProjectileAPI proj;
        float initialDamage, maxDamage;

        private ProjectileData(DamagingProjectileAPI proj, float initialDamage, float maxDamage) {
            this.proj = proj;
            this.initialDamage = initialDamage;
            this.maxDamage = maxDamage;
        }

        public float getSize() {
            return maxProjectileSize * (float) Math.sqrt(proj.getDamageAmount() / maxDamage);
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

            List<CombatEntityAPI> ignoreList = new ArrayList<>();
            ignoreList.add(proj);
            ignoreList.add(proj.getSource());

            Utils.ClosestCollisionData collisionData = Utils.circleCollisionCheck(
                    proj.getLocation(),
                    (float) Math.sqrt(proj.getDamageAmount() / data.maxDamage) * maxProjectileSize * 0.5f,
                    ignoreList,
                    engine);

            if (collisionData == null) {
                continue;
            }

            CombatEntityAPI target = collisionData.entity;

            float targetPrevFlux = 0f;
            if (target instanceof ShipAPI) {
                targetPrevFlux = ((ShipAPI) target).getFluxTracker().getCurrFlux();
            }

            engine.applyDamage(
                    target,
                    collisionData.point,
                    proj.getDamageAmount(),
                    proj.getDamageType(),
                    proj.getEmpAmount(),
                    false,
                    proj.isFading(),
                    weapon.getShip(),
                    true);

            engine.addHitParticle(
                    collisionData.point,
                    target.getVelocity().length() <= 50f ? target.getVelocity() : new Vector2f(),
                    proj.getProjectileSpec().getHitGlowRadius(),
                    1f,
                    proj.getProjectileSpec().getFringeColor());


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
                        destroyProjectile(proj, collisionData, engine);
                        itr.remove();
                    }
                    else {
                        proj.setDamageAmount(newDamageAmount);
                    }
                    continue;
                }
            }

            // Otherwise, destroy this projectile
            destroyProjectile(proj, collisionData, engine);
            itr.remove();
        }
    }

    private void destroyProjectile(DamagingProjectileAPI proj, Utils.ClosestCollisionData collisionData, CombatEngineAPI engine) {

        Global.getCombatEngine().removeEntity(proj);
        if (proj.isFading()) return;

        float explosionRadius =
                minExplosionRadius * Math.max(1f, (float) Math.sqrt(proj.getBaseDamageAmount() / proj.getProjectileSpec().getDamage().getBaseDamage()));
        Color darkColor = new Color(122, 255, 122, 255), lightColor = new Color(200, 255, 200, 255);

        DamagingExplosionSpec spec = new DamagingExplosionSpec(
                0.1f,
                explosionRadius,
                explosionRadius / 2,
                proj.getDamageAmount(),
                proj.getDamageAmount() / 2,
                CollisionClass.PROJECTILE_FF,
                CollisionClass.PROJECTILE_FIGHTER,
                4f,
                3f,
                0.8f,
                100,
                darkColor,
                lightColor
        );

        spec.setUseDetailedExplosion(true);
        spec.setDetailedExplosionRadius(explosionRadius);
        spec.setDetailedExplosionFlashRadius(explosionRadius * 1.5f);
        spec.setDetailedExplosionFlashColorCore(darkColor);
        spec.setDetailedExplosionFlashColorFringe(lightColor);
        spec.setDamageType(DamageType.HIGH_EXPLOSIVE);


        if (ModPlugin.particleEngineEnabled) {
            spec.setParticleCount(0);
            spec.setUseDetailedExplosion(false);
            spec.setExplosionColor(new Color(0, 0, 0, 0));
        }

        engine.spawnDamagingExplosion(
                spec,
                proj.getSource(),
                collisionData.point
        ).addDamagedAlready(collisionData.entity);

        if (ModPlugin.particleEngineEnabled) {
            EnergyBallExplosion.makeExplosion(collisionData.point, explosionRadius);
        }

        float explosionRadiusRatio = explosionRadius / minExplosionRadius;
        Global.getSoundPlayer().playSound("wpnxt_energyball_hit", 2f - (explosionRadiusRatio - 1f) / 2.2f + Utils.randBetween(-0.03f, 0.03f), explosionRadiusRatio / 3f + Utils.randBetween(-0.03f, 0.03f), collisionData.point, new Vector2f());
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
        setProjectileSpeed(damageMult);

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
//        Vector2f adjust = Misc.getUnitVectorAtDegreeAngle(proj.getFacing());
//        proj.getTailEnd().set(new Vector2f(firePoint.x + adjust.x * -projectileSize / 2f, firePoint.y + adjust.y * -projectileSize / 2f));
//        proj.getLocation().set(firePoint);
//        proj.getVelocity().scale(2.5f - 1.5f * chargeLevel);
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

    public Vector2f getFirePoint() {
        return weapon == null ? new Vector2f() : weapon.getFirePoint(0);
    }

    @Override
    public void init(WeaponAPI weapon) {
        projRenderer = new EnergyBallRenderer(weapon, this);
        this.weapon = weapon;
        ((ProjectileWeaponSpecAPI) weapon.getSpec()).setChargeTime(chargeTime);
        weapon.ensureClonedSpec();
        Global.getCombatEngine().addLayeredRenderingPlugin(projRenderer);
    }

    /** Maps (0, 1) to (1, fullChargeDamageMultiplier) */
    public static float mapChargeLevelToDamageMult(float chargeLevel) {
        return 1f + (fullChargeDamageMultiplier - 1f) * chargeLevel;
    }

    public float getProjectileSize() {
        return chargeLevelOnAdvance <= 0f ? 0f : getProjectileSize(chargeLevelOnAdvance);
    }

    public static float getProjectileSize(float chargeLevel) {
        return  maxProjectileSize * (float) Math.sqrt(mapChargeLevelToDamageMult(chargeLevel) / fullChargeDamageMultiplier);
    }

    public static float getMoveSpeed(float damageMult) {
        return 750f - ((damageMult - 1f) / (fullChargeDamageMultiplier - 1f)) * 500f;
    }

    private void setProjectileSpeed(float damageMult) {
        if (weapon == null) return;
        weapon.getSpec().setProjectileSpeed(getMoveSpeed(damageMult));
        ((ProjectileSpecAPI) weapon.getSpec().getProjectileSpec()).setMoveSpeed(getMoveSpeed(damageMult));
    }

    private void onFailToFire(WeaponAPI weapon) {
        weapon.setRemainingCooldownTo(weapon.getCooldown() * 0.5f);
    }
}
