package weaponexpansion.combat.scripts;

import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import com.fs.starfarer.api.loading.DamagingExplosionSpec;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;

@SuppressWarnings("unused")
public class ExplosiveShellEffect implements OnHitEffectPlugin {

    static final float explosionRadius = 200f;

    static final Color color1 = new Color(255, 125, 25, 255);
    static final Color color2 = new Color(255, 192, 128, 255);

    @Override
    public void onHit(DamagingProjectileAPI proj, CombatEntityAPI target, Vector2f pt, boolean shieldHit, ApplyDamageResultAPI damageResult, CombatEngineAPI engine) {

        if (!proj.didDamage()) {
            return;
        }

        DamagingExplosionSpec spec = new DamagingExplosionSpec(
                0.2f,
                explosionRadius,
                explosionRadius / 2,
                proj.getDamageAmount(),
                proj.getDamageAmount() / 2,
                CollisionClass.PROJECTILE_FF,
                CollisionClass.PROJECTILE_FIGHTER,
                4f,
                4f,
                1.5f,
                250,
                color1,
                color2
        );

        spec.setUseDetailedExplosion(false);
        spec.setDamageType(DamageType.HIGH_EXPLOSIVE);

        engine.spawnDamagingExplosion(
                spec,
                proj.getSource(),
                pt
        ).addDamagedAlready(target);
    }
}
