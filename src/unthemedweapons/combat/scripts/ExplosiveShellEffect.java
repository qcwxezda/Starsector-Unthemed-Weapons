package unthemedweapons.combat.scripts;

import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import com.fs.starfarer.api.loading.DamagingExplosionSpec;
import org.lwjgl.util.vector.Vector2f;
import unthemedweapons.ModPlugin;
import unthemedweapons.fx.particles.Explosion;

import java.awt.*;

@SuppressWarnings("unused")
public class ExplosiveShellEffect implements OnHitEffectPlugin {

    static final float explosionRadius = 150f;

    static final Color color1 = new Color(255, 125, 25, 255);
    static final Color color2 = new Color(255, 192, 128, 255);

    @Override
    public void onHit(DamagingProjectileAPI proj, CombatEntityAPI target, Vector2f pt, boolean shieldHit, ApplyDamageResultAPI damageResult, CombatEngineAPI engine) {
        if (!proj.didDamage()) {
            return;
        }

        // Should use getDamageAmount instead of getBaseDamageAmount
        // as the explosionSpec damage does NOT get modified.
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
            Explosion.makeExplosion(pt, explosionRadius * 2f, 10, 1, 75);
        }
    }
}
