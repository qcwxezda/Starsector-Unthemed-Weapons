package unthemedweapons.combat.scripts;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import com.fs.starfarer.api.combat.listeners.DamageDealtModifier;
import com.fs.starfarer.api.loading.DamagingExplosionSpec;
import org.lwjgl.util.vector.Vector2f;
import unthemedweapons.ModPlugin;
import unthemedweapons.fx.particles.MorphCannonExplosion;
import unthemedweapons.fx.particles.MorphCannonKineticExplosion;
import unthemedweapons.fx.particles.MorphCannonTrail;
import unthemedweapons.util.MathUtils;

import java.awt.*;
import java.util.*;

@SuppressWarnings("unused")
public class MorphCannonEffect extends GlowOnFirePlugin implements DamageDealtModifier, OnHitEffectPlugin {
    private final Set<DamagingProjectileAPI> projectiles = new HashSet<>();
    public static final String modifyKey = "wpnxt_morphCannon";
    public static final String morphDataKey = "wpnxt_morphData";
    public static final float maxExplosionRadius = 200f;

    private static class MorphProjDamageData {
        float kineticFraction;
        float heFraction;
        float totalDamage;
        final float maxLife;
        DamagingProjectileAPI kineticComponent;

        private MorphProjDamageData(float baseDamage, DamagingProjectileAPI kineticComponent, float maxLife) {
            totalDamage = baseDamage;
            kineticFraction = 0f;
            heFraction = 1f;
            this.kineticComponent = kineticComponent;
            this.maxLife = maxLife;
        }
    }

    @Override
    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
        super.advance(amount, engine, weapon);
        Iterator<DamagingProjectileAPI> itr = projectiles.iterator();
        while (itr.hasNext()) {
            DamagingProjectileAPI proj = itr.next();

            MorphProjDamageData data = (MorphProjDamageData) proj.getCustomData().get(morphDataKey);
            if (data == null) {
                itr.remove();
                continue;
            }

            // Copy projectile should always follow base projectile exactly
            data.kineticComponent.getLocation().set(proj.getLocation());
            data.kineticComponent.setFacing(proj.getFacing());
            data.kineticComponent.getVelocity().set(proj.getVelocity());

            // Set the damage values based on elapsed amount of time
            float ratio = Math.min(1f, proj.getElapsed() / data.maxLife);
            data.heFraction = 1f - ratio;
            data.kineticFraction = ratio;
            proj.setDamageAmount(data.heFraction * data.totalDamage);
            data.kineticComponent.setDamageAmount(data.kineticFraction * data.totalDamage);

            if (proj.isExpired() || !engine.isEntityInPlay(proj)) {
                engine.removeEntity(data.kineticComponent);
                itr.remove();
            }
        }
    }

    @Override
    public void onFire(DamagingProjectileAPI proj, WeaponAPI weapon, CombatEngineAPI engine){
        super.onFire(proj, weapon, engine);

        ShipAPI ship = weapon.getShip();
        if (!ship.hasListenerOfClass(MorphCannonEffect.class)) {
            ship.addListener(this);
        }

        float maxLife = proj.getWeapon().getRange() / proj.getMoveSpeed();

        // The base projectile will represent the high-explosive component
        proj.getDamage().setType(DamageType.HIGH_EXPLOSIVE);

        // Spawn a second projectile to represent the kinetic component of the damage
        // This projectile is invisible and solely serves to allow the AI to make an informed decision
        // about what to do when faced with it
        DamagingProjectileAPI kineticComponent =
                (DamagingProjectileAPI) engine.spawnProjectile(
                        null,
                        null,
                        ModPlugin.dummyProjWeapon,
                        proj.getLocation(),
                        0f,
                        new Vector2f());
        kineticComponent.setCollisionClass(CollisionClass.NONE);
        kineticComponent.getDamage().setType(DamageType.KINETIC);

        MorphProjDamageData data = new MorphProjDamageData(proj.getBaseDamageAmount(), kineticComponent, maxLife);
        projectiles.add(proj);
        proj.setCustomData(morphDataKey, data);

        if (ModPlugin.particleEngineEnabled) {
            MorphCannonTrail.makeTrail(proj, maxLife);
        }
    }

    @Override
    public String modifyDamageDealt(Object param, CombatEntityAPI target, DamageAPI damage, Vector2f pt, boolean shieldHit) {
        if (param instanceof DamagingProjectileAPI) {
            DamagingProjectileAPI proj = (DamagingProjectileAPI) param;
            if (proj.getCustomData() != null && proj.getCustomData().containsKey(morphDataKey)) {
                // Set the damage to a small number; it will be correctly applied in the onHit function
                // Don't set it to 0 because shieldHit doesn't register for hits that deal less than 1 damage!
                proj.setDamageAmount(5f);
                return modifyKey;
            }
        }
        return null;
    }

    @Override
    public void onHit(DamagingProjectileAPI proj, CombatEntityAPI target, Vector2f pt, boolean shieldHit, ApplyDamageResultAPI result, CombatEngineAPI engine) {
        if (proj.getCustomData() == null) {
            return;
        }
        MorphProjDamageData data = (MorphProjDamageData) proj.getCustomData().get(morphDataKey);
        if (data == null) {
            return;
        }

        proj.setDamageAmount(data.totalDamage);
        float kDamage = proj.getDamageAmount() * data.kineticFraction;
        float hDamage = proj.getDamageAmount() * data.heFraction;
        // If shield hit, apply kinetic damage equal to K + 0.25*HE
        if (shieldHit) {
            engine.applyDamage(
                    proj,
                    target,
                    pt,
                    kDamage + 0.25f*hDamage,
                    DamageType.KINETIC,
                    0f,
                    false,
                    proj.isFading(),
                    proj.getSource(),
                    true);
        }
        // Otherwise, apply high-explosive damage equal to HE + 0.25*K,
        // then apply an additional 0.75*K kinetic damage so that the total
        // hull damage is HE + KE
        else {
            engine.applyDamage(
                    proj,
                    target,
                    pt,
                    hDamage + 0.25f*kDamage,
                    DamageType.HIGH_EXPLOSIVE,
                    0f,
                    false,
                    proj.isFading(),
                    proj.getSource(),
                    true);
            engine.applyDamage(
                    proj,
                    target,
                    pt,
                    0.75f*kDamage,
                    DamageType.KINETIC,
                    0f,
                    false,
                    proj.isFading(),
                    proj.getSource(),
                    false);
        }


        if (ModPlugin.particleEngineEnabled) {
            float kineticRatioSqrt = (float)Math.sqrt(data.kineticFraction);
            if (kineticRatioSqrt > 0.3f) {
                MorphCannonKineticExplosion.makeExplosion(pt, 150f * kineticRatioSqrt, 60f * kineticRatioSqrt, 0.6f, (int) (100f * kineticRatioSqrt));
            }
        }

        if (data.heFraction <= 0.4f) {
            return;
        }
        float explosionRadius = maxExplosionRadius * (float) Math.sqrt(data.heFraction - 0.4f) / (float) Math.sqrt(0.6f);
        if (explosionRadius < 50f) {
            return;
        }

        Color color1 = new Color(255, 64, 0, 255);
        Color color2 = new Color(255, 160, 128, 255);


        // Spawn the rest of the damage as a damaging explosion
        DamagingExplosionSpec spec = new DamagingExplosionSpec(
                0.1f,
                explosionRadius,
                explosionRadius / 2,
                hDamage,
                hDamage,
                CollisionClass.PROJECTILE_FF,
                CollisionClass.PROJECTILE_FIGHTER,
                4f,
                3f,
                0.8f,
                100,
                color1,
                color2
        );

        spec.setUseDetailedExplosion(true);
        spec.setDetailedExplosionRadius(explosionRadius);
        spec.setDetailedExplosionFlashRadius(explosionRadius * 1.5f);
        spec.setDetailedExplosionFlashColorCore(color1);
        spec.setDetailedExplosionFlashColorFringe(color2);
        spec.setDamageType(DamageType.HIGH_EXPLOSIVE);


        if (ModPlugin.particleEngineEnabled) {
            spec.setParticleCount(0);
            spec.setUseDetailedExplosion(false);
            spec.setExplosionColor(new Color(0, 0, 0, 0));
        }

        engine.spawnDamagingExplosion(
                spec,
                proj.getSource(),
                pt
        ).addDamagedAlready(target);

        if (ModPlugin.particleEngineEnabled) {
            MorphCannonExplosion.makeExplosion(pt, explosionRadius * 2f);
        }
        Global.getSoundPlayer().playSound("wpnxt_morphcannon_hit",
                MathUtils.randBetween(0.95f, 1.05f),
                data.heFraction,
                pt,
                new Vector2f());
    }
}
