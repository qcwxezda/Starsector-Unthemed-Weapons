package unthemedweapons.combat.scripts;

import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import com.fs.starfarer.api.loading.DamagingExplosionSpec;
import org.lwjgl.util.vector.Vector2f;
import unthemedweapons.ModPlugin;
import unthemedweapons.fx.particles.Explosion;

import java.awt.*;

@SuppressWarnings("unused")
public class VoidCannonShotEffect implements OnHitEffectPlugin {

    float explosionRadius = 100f;

    @Override
    public void onHit(DamagingProjectileAPI proj, CombatEntityAPI target, Vector2f pt, boolean shieldHit, ApplyDamageResultAPI damageResult, CombatEngineAPI engine) {
        float explosionDamage = proj.getDamageAmount();
        DamagingExplosionSpec spec = new DamagingExplosionSpec(
                0.5f,
                explosionRadius,
                explosionRadius / 2,
                explosionDamage,
                explosionDamage / 2,
                CollisionClass.PROJECTILE_FF,
                CollisionClass.PROJECTILE_FIGHTER,
                2f,
                4f,
                1f,
                30,
                new Color(255,128,200,255),
                new Color(255,128,200,64)
        );

        spec.setUseDetailedExplosion(false);
        spec.setDamageType(DamageType.FRAGMENTATION);

        if (ModPlugin.particleEngineEnabled) {
            Explosion.makeExplosion(pt, explosionRadius*2f, 6, 1, 25, new float[] {1f, 0.5f, 0.8f, 0.15f}, new float[] {1f, 0.8f, 0.8f, 0.7f}, new float[] {1f, 0.6f, 0.8f, 0.3f}, new float[] {1f, 0.5f, 0.8f, 0.1f});
        }

        DamagingProjectileAPI explosion = engine.spawnDamagingExplosion(
                spec,
                proj.getSource(),
                proj.getLocation()
        );
        explosion.addDamagedAlready(target);
    }
}
