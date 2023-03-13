package untitled.combat.scripts;

import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.DamagingProjectileAPI;
import com.fs.starfarer.api.combat.OnFireEffectPlugin;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.util.Misc;

@SuppressWarnings("unused")
public class ScatterEffect implements OnFireEffectPlugin {

    /** As a percentage of the original values */
    static final float speedVariance = 0.15f;
    static final float damageVariance = 0.15f;

    static final int numProjectiles = 20;

    @Override
    public void onFire(DamagingProjectileAPI proj, WeaponAPI weapon, CombatEngineAPI engine) {

        proj.setDamageAmount(proj.getBaseDamageAmount() / numProjectiles);

        float spread = weapon.getCurrSpread();
        float temp = proj.getProjectileSpec().getMoveSpeed(null, null);
        for (int i = 0; i < numProjectiles - 1; i++) {
            float angle = weapon.getCurrAngle() + Misc.random.nextFloat() * spread - spread / 2;

            proj.getProjectileSpec().setMoveSpeed(temp * (1 + Misc.random.nextFloat() * speedVariance - speedVariance / 2));
            DamagingProjectileAPI spawn = (DamagingProjectileAPI) engine.spawnProjectile(
                    proj.getSource(),
                    weapon,
                    weapon.getId(),
                    weapon.getFirePoint(0),
                    angle,
                    proj.getSource().getVelocity()
            );
            spawn.setDamageAmount(proj.getBaseDamageAmount() * (1 + Misc.random.nextFloat() * damageVariance - damageVariance / 2));
        }
        proj.getProjectileSpec().setMoveSpeed(temp);
    }
}
