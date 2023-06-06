package unthemedweapons.combat.scripts;

import com.fs.starfarer.api.combat.*;

/** A script is used instead of just setting extra hidden turret angles because:
 *    - setting the spread in the csv actually shows it in the weapon arc
 *    - Having too many hidden turrets makes glow effects like high energy focus look weird
 *    */
@SuppressWarnings("unused")
public class VoidCannonEffect implements OnFireEffectPlugin {

    @Override
    public void onFire(DamagingProjectileAPI proj, WeaponAPI weapon, CombatEngineAPI engine) {
        engine.removeEntity(proj);

        float spread = weapon.getCurrSpread();
        int numProjectiles = weapon.getSpec().getBurstSize();
        float spreadIncrement = spread / (numProjectiles - 1);
        weapon.getAmmoTracker().addOneAmmo(); // counteract the automatic deduction
        for (float x = -spread / 2f; x <= spread / 2f; x += spreadIncrement) {
            engine.spawnProjectile(
                    proj.getSource(),
                    weapon,
                    weapon.getId(),
                    weapon.getFirePoint(0),
                    weapon.getCurrAngle() + x,
                    proj.getSource().getVelocity()
            );
            weapon.getAmmoTracker().deductOneAmmo();
            if (weapon.getAmmo() <= 0) {
                break;
            }
        }
        weapon.setRemainingCooldownTo(weapon.getCooldown());
        weapon.setRefireDelay(weapon.getCooldown());
    }
}
