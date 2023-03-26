package weaponexpansion.combat.scripts;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import com.fs.starfarer.api.loading.DamagingExplosionSpec;
import com.fs.starfarer.api.loading.ProjectileSpecAPI;
import com.fs.starfarer.api.util.Misc;
import org.lwjgl.util.vector.Vector2f;
import weaponexpansion.combat.plugins.CombatPlugin;
import weaponexpansion.combat.plugins.EmpArcAction;
import weaponexpansion.combat.plugins.ExplosionAction;

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
    public void onHit(DamagingProjectileAPI proj, CombatEntityAPI target, Vector2f pt, boolean shieldHit, ApplyDamageResultAPI damageResult, CombatEngineAPI engine) {
        float offset = Misc.random.nextFloat() * 360f;

        DamagingExplosionSpec spec = ((MissileAPI) proj).getSpec().getExplosionSpec();
        List<CombatEntityAPI> thisAsList = Collections.singletonList((CombatEntityAPI) proj);

        CombatPlugin plugin = (CombatPlugin) engine.getCustomData().get(CombatPlugin.customDataKey);

        // Shouldn't be null, but just in case
        if (plugin == null) return;

        // Set the dummy spec to the appropriate values
        ProjectileSpecAPI dummyProjSpec = (ProjectileSpecAPI) Global.getSettings().getWeaponSpec(dummyWeapon).getProjectileSpec();
        dummyProjSpec.getDamage().setDamage(proj.getBaseDamageAmount());
        dummyProjSpec.setMaxRange(maxSpawnDistance);
        dummyProjSpec.setMoveSpeed(maxSpawnDistance / maxDelay);
        dummyProjSpec.getDamage().setType(proj.getDamageType());

        for (int i = 0; i < numSpawns; i++) {
            Vector2f spawnLoc = new Vector2f(pt);
            float angle = offset + angleDeviation * Misc.random.nextFloat() - angleDeviation / 2f;
            Vector2f dirVec = Misc.getUnitVectorAtDegreeAngle(angle);
            dirVec.scale(minSpawnDistance + Misc.random.nextFloat() * (maxSpawnDistance - minSpawnDistance));
            Vector2f.add(spawnLoc, dirVec, spawnLoc);
            float delay = minDelay + Misc.random.nextFloat() * (maxDelay - minDelay);
            plugin.queueAction(new ExplosionAction(spec, proj.getSource(), spawnLoc), delay);
            plugin.queueAction(new EmpArcAction(pt, proj, spawnLoc, proj, 5f, empFringe, empCore), delay / 2f);
            // Spawn projectile to telegraph the delayed explosions
            // so the AI doesn't just overload
            DamagingProjectileAPI dummyProj = (DamagingProjectileAPI) engine.spawnProjectile(proj.getSource(), proj.getWeapon(), dummyWeapon, pt, angle, new Vector2f());
            offset = (offset + 360f / numSpawns) % 360f;
        }
    }
}
