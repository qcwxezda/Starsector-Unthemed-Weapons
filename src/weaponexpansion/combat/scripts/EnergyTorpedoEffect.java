package weaponexpansion.combat.scripts;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import com.fs.starfarer.api.loading.DamagingExplosionSpec;
import com.fs.starfarer.api.loading.MissileSpecAPI;
import com.fs.starfarer.api.util.Misc;
import org.lwjgl.util.vector.Vector2f;
import weaponexpansion.ModPlugin;
import weaponexpansion.combat.plugins.Action;
import weaponexpansion.combat.plugins.ActionPlugin;
import weaponexpansion.particles.BloomTrail;
import weaponexpansion.particles.Explosion;

import java.awt.*;
import java.util.Collections;
import java.util.List;

@SuppressWarnings("unused")
public class EnergyTorpedoEffect implements OnHitEffectPlugin, OnFireEffectPlugin {

    int numSpawns = 5, empResistance = 10, particlesPerSecond = 100;
    float minSpawnDistance = 60f, maxSpawnDistance = 120f, minDelay = 0.67f, maxDelay = 1.33f, angleDeviation = 20f, particleScale = 7f;
    static final Color empCore = new Color(180, 200, 255);
    static final Color empFringe = new Color(100, 120, 255);

    @Override
    public void onHit(final DamagingProjectileAPI proj, CombatEntityAPI target, final Vector2f pt, boolean shieldHit, ApplyDamageResultAPI damageResult, final CombatEngineAPI engine) {
        float offset = Misc.random.nextFloat() * 360f;

        final DamagingExplosionSpec spec = ((MissileAPI) proj).getSpec().getExplosionSpec();
        List<CombatEntityAPI> thisAsList = Collections.singletonList((CombatEntityAPI) proj);

        if (ModPlugin.particleEngineEnabled) {
            spec.setParticleCount(0);
            spec.setExplosionColor(new Color(0, 0, 0, 0));
            addExplosionVisual(pt, spec.getRadius()*1.5f);
        }

        //ActionPlugin plugin = (ActionPlugin) engine.getCustomData().get(ActionPlugin.customDataKey);

        String dummyWeapon = ModPlugin.dummyMissileWeapon;
        // Set the dummy spec to the appropriate values
        MissileSpecAPI dummyProjSpec = (MissileSpecAPI) Global.getSettings().getWeaponSpec(dummyWeapon).getProjectileSpec();
        dummyProjSpec.getDamage().setDamage(proj.getBaseDamageAmount());
        dummyProjSpec.setLaunchSpeed(0f);
        dummyProjSpec.getDamage().setType(proj.getDamageType());

        // Spawn mine to telegraph the delayed explosions
        // so the AI doesn't just overload
        Vector2f dummyPos = new Vector2f(pt);
        Vector2f scaledVelocity = new Vector2f(proj.getVelocity());
        scaledVelocity.scale(-0.1f); // So that it doesn't spawn inside the target's collision radius
        Vector2f.add(dummyPos, scaledVelocity, dummyPos);

        final MissileAPI dummyProj = (MissileAPI) engine.spawnProjectile(null, null, dummyWeapon, dummyPos, 0f, new Vector2f());
        dummyProj.setMine(true);
        dummyProj.setNoMineFFConcerns(true);
        dummyProj.setMinePrimed(true);
        dummyProj.setUntilMineExplosion(0f);
        dummyProj.setMineExplosionRange(((MissileAPI) proj).getSpec().getExplosionSpec().getRadius() + maxSpawnDistance);
        ActionPlugin.queueAction(new Action() {
            @Override
            public void perform() {
                engine.removeEntity(dummyProj);
            }
        }, maxDelay);

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
                        addExplosionVisual(spawnLoc, spec.getRadius()*1.5f);
                    }
                }
            }, delay);
            offset = (offset + 360f / numSpawns) % 360f;
        }
    }

    private void addExplosionVisual(Vector2f loc, float radius) {
        Explosion.makeExplosion(loc, radius, 50, 1, 100, new float[]{0.2f, 0.2f, 1f, 0.2f}, new float[]{0.4f, 0.4f, 1f, 0.7f}, new float[]{0.3f, 0.3f, 1f, 1f}, new float[]{0.2f, 0.2f, 1f, 1f});
    }

    @Override
    public void onFire(DamagingProjectileAPI proj, WeaponAPI weapon, CombatEngineAPI engine) {
        BloomTrail.makeTrail(proj, particleScale, particlesPerSecond);
        ((MissileAPI) proj).setEmpResistance(empResistance);
    }
}
