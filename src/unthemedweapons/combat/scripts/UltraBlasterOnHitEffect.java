package unthemedweapons.combat.scripts;

import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import com.fs.starfarer.api.loading.DamagingExplosionSpec;
import com.fs.starfarer.api.util.Misc;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;

@SuppressWarnings("unused")
public class UltraBlasterOnHitEffect implements OnHitEffectPlugin {

    private static final float effectRadius = 60f;
    private static final float effectArc = 75f;
    private static final float explosionRadius = 80f;

    private static final int minExplosionCount = 5;
    private static final int maxExplosionCount = 5;
    private static final float explosionDamage = 150f;

    @Override
    public void onHit(DamagingProjectileAPI proj, CombatEntityAPI target, Vector2f pt, boolean shieldHit, ApplyDamageResultAPI damageResult, CombatEngineAPI engine) {

        // Doesn't trigger on missile impacts
        if (!(target instanceof ShipAPI)) {
            return;
        }

        float numExplosions = Misc.random.nextInt(maxExplosionCount - minExplosionCount + 1) + minExplosionCount;
        if (proj.isFading()) {
            numExplosions *= proj.getBrightness();
        }
        if (shieldHit) {
            numExplosions *= ((ShipAPI) target).getHardFluxLevel();
        }

        for (int i = 0; i < numExplosions; i++) {
            Vector2f explosionLocation = new Vector2f(proj.getLocation());
            // sqrt for uniform distribution on the circular segment
            float locOffset = (float) Math.sqrt(Misc.random.nextFloat()) * effectRadius;
            float angleOffset = (float) Math.PI / 180 * (proj.getFacing() + Misc.random.nextFloat() * effectArc - effectArc / 2);
            explosionLocation.x += locOffset * (float) Math.cos(angleOffset);
            explosionLocation.y += locOffset * (float) Math.sin(angleOffset);
            DamagingExplosionSpec spec = new DamagingExplosionSpec(
                    0.5f,
                    explosionRadius,
                    explosionRadius / 2,
                    explosionDamage,
                    explosionDamage / 2,
                    CollisionClass.PROJECTILE_FF,
                    CollisionClass.PROJECTILE_FIGHTER,
                    1f,
                    4f,
                    0.5f,
                    0,
                    new Color(225, 75, 75, 128),
                    new Color(155, 75, 75, 96)
            );

            spec.setUseDetailedExplosion(false);
            spec.setDamageType(DamageType.HIGH_EXPLOSIVE);

            engine.spawnDamagingExplosion(
                    spec,
                    proj.getSource(),
                    explosionLocation
            );
        }
    }
}
