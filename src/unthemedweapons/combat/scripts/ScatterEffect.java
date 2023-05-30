package unthemedweapons.combat.scripts;

import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.util.Misc;

public abstract class ScatterEffect implements OnFireEffectPlugin, EveryFrameWeaponEffectPlugin {
    @Override
    public void onFire(DamagingProjectileAPI proj, WeaponAPI weapon, CombatEngineAPI engine) {
        float spread = getSpread(weapon);
        int numProjectiles = isUsingExtraBarrels() ? 1 : weapon.getSpec().getBurstSize();
        float temp = proj.getProjectileSpec().getMoveSpeed(null, null);
        engine.removeEntity(proj);
        for (int i = 0; i < numProjectiles; i++) {
            float angle = weapon.getCurrAngle() + Misc.random.nextFloat() * spread - spread / 2;

            proj.getProjectileSpec().setMoveSpeed(temp * (1 + Misc.random.nextFloat() * getSpeedVariance() - getSpeedVariance() / 2));
            DamagingProjectileAPI spawn = (DamagingProjectileAPI) engine.spawnProjectile(
                    proj.getSource(),
                    weapon,
                    weapon.getId(),
                    weapon.getFirePoint(0),
                    angle,
                    proj.getSource().getVelocity()
            );
            spawn.setDamageAmount(proj.getBaseDamageAmount() * (1 + Misc.random.nextFloat() * getDamageVariance() - getDamageVariance() / 2));
        }
        proj.getProjectileSpec().setMoveSpeed(temp);

        if (weapon.usesAmmo()) {
            weapon.getAmmoTracker().setAmmo(Math.max(0, weapon.getAmmo() - numProjectiles + 1));
        }
        weapon.setRemainingCooldownTo(weapon.getCooldown());
        weapon.setRefireDelay(weapon.getCooldown());
    }

    @Override
    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
        // Nothing here; this only exists because OnFireEffect only works on weapons if
        // they also have an EveryFrameEffect

        // Don't want to put the onFireEffect in the proj file as multiple weapons might want to use the
        // same proj
    }

    /** As percentage of original value */
    public abstract float getSpeedVariance();

    /** As percentage of original value */
    public abstract float getDamageVariance();

    /** If true, the weapon has extra barrels. If false, it has a burst size greater than one. */
    public abstract boolean isUsingExtraBarrels();

    public float getSpread(WeaponAPI weapon) {
        return weapon.getCurrSpread();
    }
}
