package weaponexpansion.combat.scripts;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import com.fs.starfarer.api.loading.DamagingExplosionSpec;
import com.fs.starfarer.api.loading.MissileSpecAPI;
import com.fs.starfarer.api.loading.ProjectileSpecAPI;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.combat.entities.Missile;
import org.lwjgl.util.vector.Vector2f;
import weaponexpansion.combat.plugins.Action;
import weaponexpansion.combat.plugins.CombatPlugin;

import java.awt.*;
import java.util.Collections;
import java.util.List;

@SuppressWarnings("unused")
public class EnergyTorpedoEffect implements OnHitEffectPlugin {

    int numSpawns;
    float minSpawnDistance = 60f, maxSpawnDistance = 120f, minDelay = 0.33f, maxDelay = 0.67f, angleDeviation = 20f;
    final String dummyWeapon = "wpnxt_dummy";
    static final Color empCore = new Color(180, 200, 255);
    static final Color empFringe = new Color(100, 120, 255);

    public EnergyTorpedoEffect() {
        numSpawns = 5;
    }

    @Override
    public void onHit(final DamagingProjectileAPI proj, CombatEntityAPI target, final Vector2f pt, boolean shieldHit, ApplyDamageResultAPI damageResult, final CombatEngineAPI engine) {
        float offset = Misc.random.nextFloat() * 360f;

        final DamagingExplosionSpec spec = ((MissileAPI) proj).getSpec().getExplosionSpec();
        List<CombatEntityAPI> thisAsList = Collections.singletonList((CombatEntityAPI) proj);

        CombatPlugin plugin = (CombatPlugin) engine.getCustomData().get(CombatPlugin.customDataKey);

        // Shouldn't be null, but just in case
        if (plugin == null) return;

        // Set the dummy spec to the appropriate values
        MissileSpecAPI dummyProjSpec = (MissileSpecAPI) Global.getSettings().getWeaponSpec(dummyWeapon).getProjectileSpec();
        dummyProjSpec.getDamage().setDamage(proj.getBaseDamageAmount());
        dummyProjSpec.setLaunchSpeed(0f);
        dummyProjSpec.getDamage().setType(proj.getDamageType());

        // Spawn mine to telegraph the delayed explosions
        // so the AI doesn't just overload
        Vector2f dummyPos = new Vector2f(pt);
        Vector2f scaledVelocity = new Vector2f(proj.getVelocity());
        scaledVelocity.scale(-0.1f);
        Vector2f.add(dummyPos, scaledVelocity, dummyPos);

        final MissileAPI dummyProj = (MissileAPI) engine.spawnProjectile(null, null, dummyWeapon, dummyPos, 0f, new Vector2f());
        dummyProj.setMine(true);
        dummyProj.setNoMineFFConcerns(true);
        dummyProj.setMinePrimed(true);
        dummyProj.setUntilMineExplosion(0f);
        dummyProj.setMineExplosionRange(((MissileAPI) proj).getSpec().getExplosionSpec().getRadius());
        plugin.queueAction(new Action() {
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

            plugin.queueAction(new Action() {

                @Override
                public void perform() {
                    engine.spawnEmpArcVisual(pt, null, spawnLoc, null, 5f, empFringe, empCore);
                }
            }, delay / 2f);
            plugin.queueAction(new Action() {
                @Override
                public void perform() {
                    engine.spawnDamagingExplosion(spec, proj.getSource(), spawnLoc);
                }
            }, delay);
            offset = (offset + 360f / numSpawns) % 360f;
        }
    }
}
