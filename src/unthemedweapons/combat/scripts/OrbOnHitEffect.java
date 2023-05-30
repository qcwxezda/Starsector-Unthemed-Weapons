package unthemedweapons.combat.scripts;

import java.awt.Color;

import com.fs.starfarer.api.util.Misc;
import org.lwjgl.util.vector.Vector2f;

import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.DamageType;
import com.fs.starfarer.api.combat.DamagingProjectileAPI;
import com.fs.starfarer.api.combat.OnHitEffectPlugin;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;

@SuppressWarnings("unused")
public class OrbOnHitEffect implements OnHitEffectPlugin {

    public void onHit(DamagingProjectileAPI projectile, CombatEntityAPI target,
                      Vector2f point, boolean shieldHit, ApplyDamageResultAPI damageResult, CombatEngineAPI engine) {

        if (!(target instanceof ShipAPI)) return;
        if (!shieldHit && Misc.random.nextFloat() < 0.5f) {
            float emp = 2f * projectile.getEmpAmount();
            float dam = 0;
            engine.spawnEmpArcPierceShields(
                    projectile.getSource(), point, projectile, target,
                    DamageType.ENERGY,
                    dam, // damage
                    emp, // emp
                    100000f, // max range
                    "tachyon_lance_emp_impact",
                    20f, // thickness
                    new Color(125, 125, 100, 255),
                    new Color(255, 255, 255, 255)
            );
        }
    }
}
