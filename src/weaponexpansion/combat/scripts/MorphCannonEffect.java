package weaponexpansion.combat.scripts;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import com.fs.starfarer.api.combat.listeners.DamageDealtModifier;
import com.fs.starfarer.api.loading.DamagingExplosionSpec;
import com.fs.starfarer.api.util.Misc;
import org.lwjgl.util.vector.Vector2f;
import weaponexpansion.ModPlugin;
import weaponexpansion.particles.MorphCannonExplosion;
import weaponexpansion.particles.MorphCannonKineticExplosion;
import weaponexpansion.particles.MorphCannonTrail;
import weaponexpansion.util.Utils;

import java.awt.*;
import java.util.*;

@SuppressWarnings("unused")
public class MorphCannonEffect extends GlowOnFirePlugin implements DamageDealtModifier, OnHitEffectPlugin {
    private final Set<DamagingProjectileAPI> projectiles = new HashSet<>();
    public static final String modifyKey = "wpnxt_morphCannon";
    public static final String maxLifeKey = "wpnxt_maxLife";
    public static final float maxExplosionRadius = 200f;

    @Override
    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
        super.advance(amount, engine, weapon);
        Iterator<DamagingProjectileAPI> itr = projectiles.iterator();
        while (itr.hasNext()) {
            DamagingProjectileAPI proj = itr.next();

            if (proj.isExpired() || !engine.isEntityInPlay(proj)) {
                itr.remove();
            }
        }
    }

    @Override
    public void onFire(DamagingProjectileAPI proj, WeaponAPI weapon, CombatEngineAPI engine){
        super.onFire(proj, weapon, engine);
        // Only do this so that the AI sees a 1200 damage energy projectile
        // Will be a mix between high-explosive and kinetic when it actually hits.
        proj.getDamage().setType(DamageType.ENERGY);
        ShipAPI ship = weapon.getShip();
        if (!ship.hasListenerOfClass(MorphCannonEffect.class)) {
            ship.addListener(this);
        }

        float maxLife = proj.getWeapon().getRange() / proj.getMoveSpeed();
        proj.setCustomData(maxLifeKey, maxLife);
        projectiles.add(proj);

        if (ModPlugin.particleEngineEnabled) {
            MorphCannonTrail.makeTrail(proj);
        }
    }

    @Override
    public String modifyDamageDealt(Object param, CombatEntityAPI target, DamageAPI damage, Vector2f pt, boolean shieldHit) {
        if (param instanceof DamagingProjectileAPI && projectiles.contains(param)) {
            DamagingProjectileAPI proj = (DamagingProjectileAPI) param;
            // Deal partial kinetic damage; the remaining high-explosive damage will be dealt on the on-hit effect.
            float maxLife = (float) proj.getCustomData().get(maxLifeKey);
            float ratio = Math.min(1f, proj.getElapsed() / maxLife);
            proj.getDamage().setType(DamageType.KINETIC);
            proj.getDamage().getModifier().modifyMult(modifyKey, ratio);
            return modifyKey;
        }
        return null;
    }

    @Override
    public void onHit(DamagingProjectileAPI proj, CombatEntityAPI target, Vector2f pt, boolean shieldHit, ApplyDamageResultAPI result, CombatEngineAPI engine) {
        Float maxLife = (Float) proj.getCustomData().get(maxLifeKey);
        if (maxLife == null) {
            return;
        }

        float ratio = 1f - proj.getElapsed() / maxLife;
        float damage = proj.getDamageAmount() * ratio;

        engine.applyDamage(target,
                pt,
                damage,
                DamageType.HIGH_EXPLOSIVE,
                0f,
                false,
                proj.isFading(),
                proj.getSource(),
                true);

        if (ModPlugin.particleEngineEnabled) {
            float kineticRatioSqrt = (float)Math.sqrt((1f - ratio));
            MorphCannonKineticExplosion.makeExplosion(pt, 150f*kineticRatioSqrt, 60f*kineticRatioSqrt, 0.6f, (int) (100f * kineticRatioSqrt));
        }

        if (ratio <= 0.4f) {
            return;
        }
        float explosionRadius = maxExplosionRadius * (float) Math.sqrt(ratio - 0.4f) / (float) Math.sqrt(0.6f);
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
                damage,
                damage / 2,
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
                Utils.randBetween(0.95f, 1.05f),
                ratio,
                pt,
                new Vector2f());
    }
}
