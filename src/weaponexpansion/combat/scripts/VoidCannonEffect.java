package weaponexpansion.combat.scripts;

import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import com.fs.starfarer.api.loading.DamagingExplosionSpec;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;

@SuppressWarnings("unused")
public class VoidCannonEffect implements OnHitEffectPlugin {

    float explosionRadius = 100f;
    float explosionDamage = 300f;

    @Override
    public void onHit(DamagingProjectileAPI proj, CombatEntityAPI target, Vector2f pt, boolean shieldHit, ApplyDamageResultAPI damageResult, CombatEngineAPI engine) {
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

        engine.spawnDamagingExplosion(
                spec,
                proj.getSource(),
                proj.getLocation()
        );
    }
}
