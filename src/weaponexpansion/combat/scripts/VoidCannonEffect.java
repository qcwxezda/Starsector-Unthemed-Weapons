package weaponexpansion.combat.scripts;

import com.fs.starfarer.api.combat.*;

/** A script is used instead of just setting extra hidden turret angles because:
 *    - setting the spread in the csv actually shows it in the weapon arc
 *    - Having too many hidden turrets makes glow effects like high energy focus look weird
 *    */
@SuppressWarnings("unused")
public class VoidCannonEffect implements OnFireEffectPlugin, EveryFrameWeaponEffectPlugin {
    static final int numProjectiles = 6;

    @Override
    public void onFire(DamagingProjectileAPI proj, WeaponAPI weapon, CombatEngineAPI engine) {
        engine.removeEntity(proj);

        float spread = weapon.getCurrSpread();
        float spreadIncrement = spread / (numProjectiles - 1);
        for (float x = -spread / 2f; x <= spread / 2f; x += spreadIncrement) {
            engine.spawnProjectile(
                    proj.getSource(),
                    weapon,
                    weapon.getId(),
                    weapon.getFirePoint(0),
                    weapon.getCurrAngle() + x,
                    proj.getSource().getVelocity()
            );
        }
        weapon.getAmmoTracker().setAmmo(Math.max(0, weapon.getAmmo() - numProjectiles + 1));
        weapon.setRemainingCooldownTo(weapon.getCooldown());
        weapon.setRefireDelay(weapon.getCooldown());
    }

    @Override
    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
        // Nothing here; this only exists because OnFireEffect only works on weapons if
        // they also have an EveryFrameEffect
    }
}
