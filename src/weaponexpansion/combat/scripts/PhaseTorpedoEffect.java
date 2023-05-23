package weaponexpansion.combat.scripts;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import com.fs.starfarer.api.loading.DamagingExplosionSpec;
import com.fs.starfarer.api.util.Misc;
import org.lwjgl.util.vector.Vector2f;
import weaponexpansion.ModPlugin;
import weaponexpansion.combat.plugins.Action;
import weaponexpansion.combat.plugins.ActionPlugin;
import weaponexpansion.fx.particles.Explosion;
import weaponexpansion.fx.particles.PhaseTorpedoTrail;
import weaponexpansion.fx.particles.PhaseTorpedoSecondaryExplosion;
import weaponexpansion.util.EngineUtils;

@SuppressWarnings("unused")
public class PhaseTorpedoEffect implements OnHitEffectPlugin, OnFireEffectPlugin {

    public static final float explosionSpeed = 200f, timeBetweenHits = 0.1f, ringDamage = 1800f;

    @Override
    public void onHit(final DamagingProjectileAPI proj, CombatEntityAPI target, final Vector2f pt, boolean shieldHit, ApplyDamageResultAPI damageResult, final CombatEngineAPI engine){
        final DamagingExplosionSpec spec = ((MissileAPI) proj).getSpec().getExplosionSpec();

        if (ModPlugin.particleEngineEnabled) {
            addExplosionVisual(pt, spec.getRadius());
        }

        Global.getSoundPlayer().playSound("wpnxt_phasetorpedo_explosion", 0.9f + Misc.random.nextFloat() * 0.1f, 1f, pt, new Vector2f());

        // Spawn mine to telegraph the delayed explosion
        Vector2f dummyPos = new Vector2f(pt);
        Vector2f scaledVelocity = new Vector2f(proj.getVelocity());
        scaledVelocity.scale(-0.1f); // So that it doesn't spawn inside the target's collision radius
        Vector2f.add(dummyPos, scaledVelocity, dummyPos);

        EngineUtils.spawnFakeMine(dummyPos, spec.getRadius(), ringDamage, spec.getDamageType(), spec.getRadius() / explosionSpeed);

        for (float radius = explosionSpeed * timeBetweenHits, time = timeBetweenHits; radius <= spec.getRadius(); radius += explosionSpeed * timeBetweenHits, time += timeBetweenHits) {
            final float finalRadius = radius;
            ActionPlugin.queueAction(new Action() {
                @Override
                public void perform() {
                    EngineUtils.applyDamageOnRing(
                            pt,
                            finalRadius,
                            true,
                            true,
                            null,
                            ringDamage,
                            DamageType.HIGH_EXPLOSIVE,
                            0f,
                            false,
                            false,
                            proj.getSource(),
                            true
                    );
                }
            }, time);
        }
    }

    private void addExplosionVisual(Vector2f loc, float radius) {
        PhaseTorpedoSecondaryExplosion.makeStaticRing(loc);
        PhaseTorpedoSecondaryExplosion.makeRing(loc, 300);
        Explosion.makeExplosion(loc, 2f*radius, 2.4f,8, 5, 500);
    }

    @Override
    public void onFire(DamagingProjectileAPI proj, WeaponAPI weapon, CombatEngineAPI engine) {
        if (ModPlugin.particleEngineEnabled) {
            PhaseTorpedoTrail.makeTrail(proj);
            ((MissileAPI) proj).setEmpResistance(10000);
            ((MissileAPI) proj).setEccmChanceOverride(1f);
        }
    }
}
