package weaponexpansion.combat.scripts;

import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.util.Misc;

@SuppressWarnings("unused")
public class ScatterEffect implements OnFireEffectPlugin, EveryFrameWeaponEffectPlugin {

    /** As a percentage of the original values */
    static final float speedVariance = 0.15f;
    static final float damageVariance = 0.15f;

    static final int numProjectiles = 20;

    @Override
    public void onFire(DamagingProjectileAPI proj, WeaponAPI weapon, CombatEngineAPI engine) {
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

        weapon.setRemainingCooldownTo(weapon.getCooldown());
        weapon.setRefireDelay(weapon.getCooldown());
    }

    @Override
    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
        // Nothing here; this only exists because OnFireEffect only works on weapons if
        // they also have an EveryFrameEffect
    }
}
