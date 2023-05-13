package weaponexpansion.combat.scripts;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.util.Misc;
import org.lwjgl.util.vector.Vector2f;
import weaponexpansion.ModPlugin;
import weaponexpansion.particles.EnergyBallCharge;
import weaponexpansion.util.EnergyBallRenderer;
import weaponexpansion.util.Utils;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public class EnergyBallLauncherEffect implements EveryFrameWeaponEffectPluginWithAdvanceAfter, WeaponEffectPluginWithInit, OnFireEffectPlugin {

    public static final float maxSize = 1000f;
    public static final float maxProjectileSize = 100f;
    public static final float minDamageFraction = 0.05f;

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

        if (weapon.isFiring() && chargeLevelOnAdvance <= 0f) {
            EnergyBallCharge.makeChargeParticles(weapon.getFirePoint(0), this);
        }

        chargeLevelOnAdvance = weapon.getCooldownRemaining() > 0f ? 0f : weapon.getChargeLevel();

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
                    target.getVelocity(),
                    proj.getProjectileSpec().getHitGlowRadius(),
                    1f,
                    proj.getProjectileSpec().getFringeColor());
        }
    }

    @Override
    public void onFire(DamagingProjectileAPI proj, WeaponAPI weapon, CombatEngineAPI engine) {
        float chargeLevel = Math.max(chargeLevelOnAdvance, minDamageFraction);
        // Full charge shot
        if (proj != null) {
            Global.getCombatEngine().removeEntity(proj);
        }
        // Semi-charged shot, need to manually consider flux cost
        else {
            FluxTrackerAPI flux = weapon.getShip().getFluxTracker();
            float costToFire = weapon.getFluxCostToFire() * chargeLevel;
            if (flux.getCurrFlux() + costToFire> flux.getMaxFlux()) {
                return;
            }
            flux.setCurrFlux(flux.getCurrFlux() + costToFire);
            weapon.setRemainingCooldownTo(weapon.getCooldown());
        }


        float projectileSize = getProjectileSize();

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
        Vector2f adjust = Misc.getUnitVectorAtDegreeAngle(proj.getFacing());
        proj.getTailEnd().set(new Vector2f(firePoint.x + adjust.x * -projectileSize / 2f, firePoint.y + adjust.y * -projectileSize / 2f));
        proj.getLocation().set(firePoint);
        proj.getVelocity().scale(2.5f - 1.5f * chargeLevel);
        float maxDamage = proj.getDamageAmount();
        float actualDamage = maxDamage * chargeLevel;
        proj.setDamageAmount(actualDamage);
        projectiles.add(new ProjectileData(proj, actualDamage, maxDamage));
        engine.addLayeredRenderingPlugin(new EnergyBallRenderer(proj, this, maxDamage, projRenderer.facing));

        Global.getSoundPlayer().playSound("wpnxt_orb_fire", 1f, 1f, firePoint, new Vector2f());

        //projRenderer.render(CombatEngineLayers.BELOW_INDICATORS_LAYER, Global.getCombatEngine().getViewport());
        if (ModPlugin.particleEngineEnabled) {
//            Vector2f vel = Misc.getUnitVectorAtDegreeAngle(proj.getFacing());
//            vel.scale(proj.getProjectileSpec().getMoveSpeed(proj.getSource() == null ? null : proj.getSource().getMutableStats(), weapon));
//            EnergyBallTrail.makeTrail(proj, vel, maxSize);
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
        Global.getCombatEngine().addLayeredRenderingPlugin(projRenderer);
    }

    public float getProjectileSize() {
        return chargeLevelOnAdvance <= 0f ? 0f : maxProjectileSize * (float) Math.max(Math.sqrt(minDamageFraction), Math.sqrt(chargeLevelOnAdvance));
    }
}
