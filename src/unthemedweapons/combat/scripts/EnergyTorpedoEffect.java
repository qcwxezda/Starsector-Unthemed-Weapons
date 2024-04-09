package unthemedweapons.combat.scripts;

import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import com.fs.starfarer.api.loading.DamagingExplosionSpec;
import com.fs.starfarer.api.util.Misc;
import org.lwjgl.util.vector.Vector2f;
import unthemedweapons.ModPlugin;
import unthemedweapons.combat.plugins.Action;
import unthemedweapons.combat.plugins.ActionPlugin;
import unthemedweapons.fx.particles.BloomTrail;
import unthemedweapons.fx.particles.Explosion;
import unthemedweapons.util.EngineUtils;

import java.awt.*;
import java.util.Collections;
import java.util.List;

@SuppressWarnings("unused")
public class EnergyTorpedoEffect implements OnHitEffectPlugin, OnFireEffectPlugin {

    int numSpawns = 5, empResistance = 10, particlesPerSecond = 150;
    float minSpawnDistance = 60f, maxSpawnDistance = 120f, minDelay = 0.67f, maxDelay = 1.33f, angleDeviation = 20f, particleScale = 12f;
    static final Color empCore = new Color(180, 200, 255);
    static final Color empFringe = new Color(100, 120, 255);

    @Override
    public void onHit(final DamagingProjectileAPI proj, CombatEntityAPI target, final Vector2f pt, boolean shieldHit, ApplyDamageResultAPI damageResult, final CombatEngineAPI engine) {
        float offset = Misc.random.nextFloat() * 360f;

        final DamagingExplosionSpec spec = ((MissileAPI) proj).getSpec().getExplosionSpec();
        List<CombatEntityAPI> thisAsList = Collections.singletonList((CombatEntityAPI) proj);

        if (ModPlugin.particleEngineEnabled) {
            addExplosionVisual(pt, spec.getRadius());
        }

        // Spawn mine to telegraph the delayed explosions
        Vector2f dummyPos = new Vector2f(pt);
        Vector2f scaledVelocity = new Vector2f(proj.getVelocity());
        scaledVelocity.scale(-0.1f); // So that it doesn't spawn inside the target's collision radius
        Vector2f.add(dummyPos, scaledVelocity, dummyPos);

        EngineUtils.spawnFakeMine(dummyPos, spec.getRadius() + maxSpawnDistance, proj.getBaseDamageAmount(), proj.getDamageType(), maxDelay);

        for (int i = 0; i < numSpawns; i++) {
            final Vector2f spawnLoc = new Vector2f(pt);
            final float angle = offset + angleDeviation * Misc.random.nextFloat() - angleDeviation / 2f;
            Vector2f dirVec = Misc.getUnitVectorAtDegreeAngle(angle);
            // Should be uniformly distributed inside the ring
            float dist = (float) Math.sqrt(Misc.random.nextFloat() * (maxSpawnDistance*maxSpawnDistance - minSpawnDistance*minSpawnDistance) + minSpawnDistance*minSpawnDistance);
            dirVec.scale(dist);
            Vector2f.add(spawnLoc, dirVec, spawnLoc);
            final float delay = minDelay + Misc.random.nextFloat() * (maxDelay - minDelay);

            ActionPlugin.queueAction(new Action() {

                @Override
                public void perform() {
                    engine.spawnEmpArcVisual(pt, null, spawnLoc, null, 5f, empFringe, empCore);
                }
            }, delay / 2f);
            ActionPlugin.queueAction(new Action() {
                @Override
                public void perform() {
                    engine.spawnDamagingExplosion(spec, proj.getSource(), spawnLoc);
                    if (ModPlugin.particleEngineEnabled) {
                        addExplosionVisual(spawnLoc, spec.getRadius());
                    }
                }
            }, delay);
            offset = (offset + 360f / numSpawns) % 360f;
        }
    }

    private void addExplosionVisual(Vector2f loc, float radius) {
        Explosion.makeExplosion(loc, 2f*radius, radius / 100f,8, 1, 100, new float[]{0.6f, 0.6f, 1f, 0.35f}, new float[]{0.7f, 0.7f, 1f, 0.7f}, new float[]{0.4f, 0.4f, 1f, 0.3f}, new float[]{0.6f, 0.6f, 1f, 0.5f});
    }

    @Override
    public void onFire(DamagingProjectileAPI proj, WeaponAPI weapon, CombatEngineAPI engine) {
        if (ModPlugin.particleEngineEnabled) {
            BloomTrail.makeTrail((MissileAPI) proj, particleScale, particlesPerSecond);
        }
        ((MissileAPI) proj).setEmpResistance(empResistance);
    }
}
