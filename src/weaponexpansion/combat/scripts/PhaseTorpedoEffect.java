package weaponexpansion.combat.scripts;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import com.fs.starfarer.api.loading.DamagingExplosionSpec;
import com.fs.starfarer.api.loading.MissileSpecAPI;
import com.fs.starfarer.api.util.Misc;
import org.lwjgl.util.vector.Vector2f;
import particleengine.Emitter;
import particleengine.Particles;
import weaponexpansion.ModPlugin;
import weaponexpansion.combat.plugins.Action;
import weaponexpansion.combat.plugins.ActionPlugin;
import weaponexpansion.particles.Explosion;
import weaponexpansion.particles.FlickerTrail;
import weaponexpansion.util.Utils;

import java.awt.*;

@SuppressWarnings("unused")
public class PhaseTorpedoEffect implements OnHitEffectPlugin, OnFireEffectPlugin {

    public static final float explosionSpeed = 400f, timeBetweenHits = 0.04f, ringDamage = 2500f;

    @Override
    public void onHit(final DamagingProjectileAPI proj, CombatEntityAPI target, final Vector2f pt, boolean shieldHit, ApplyDamageResultAPI damageResult, final CombatEngineAPI engine){
        final DamagingExplosionSpec spec = ((MissileAPI) proj).getSpec().getExplosionSpec();

        if (ModPlugin.particleEngineEnabled) {
            addExplosionVisual(pt, spec.getRadius()*1.2f);
        }

        Global.getSoundPlayer().playSound("wpnxt_phasetorpedo_explosion", 0.9f + Misc.random.nextFloat() * 0.1f, 1f, pt, new Vector2f());

        // Spawn mine to telegraph the delayed explosion
        Vector2f dummyPos = new Vector2f(pt);
        Vector2f scaledVelocity = new Vector2f(proj.getVelocity());
        scaledVelocity.scale(-0.1f); // So that it doesn't spawn inside the target's collision radius
        Vector2f.add(dummyPos, scaledVelocity, dummyPos);

        Utils.spawnFakeMine(dummyPos, spec.getRadius(), ringDamage, spec.getDamageType(), spec.getRadius() / explosionSpeed);

        for (float radius = explosionSpeed * timeBetweenHits, time = timeBetweenHits; radius <= spec.getRadius(); radius += explosionSpeed * timeBetweenHits, time += timeBetweenHits) {
            final float finalRadius = radius;
            ActionPlugin.queueAction(new Action() {
                @Override
                public void perform() {
                    Utils.applyDamageOnRing(
                            pt,
                            finalRadius,
                            proj.getOwner(),
                            false,
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
        String corePath = "graphics/fx/explosion0.png";
        float[] coreColor = new float[]{0.6f, 0.4f, 1f, 0.06f};
        Explosion.makeExplosion(loc, radius, 50, 1, 0, coreColor, new float[]{0.7f, 0.5f, 1f, 0.7f}, new float[]{0.7f, 0.3f, 1f, 0.5f}, new float[]{0.5f, 0.3f, 1f, 1f}, corePath);
        Emitter core = Explosion.core(loc, radius, coreColor, corePath);
        Particles.stream(core, 1, 75, 1.5f);
    }

    @Override
    public void onFire(DamagingProjectileAPI proj, WeaponAPI weapon, CombatEngineAPI engine) {
        if (ModPlugin.particleEngineEnabled) {
            FlickerTrail.makeTrail(proj);
            ((MissileAPI) proj).setEmpResistance(10000);
            ((MissileAPI) proj).setEccmChanceOverride(1f);
        }
    }
}
