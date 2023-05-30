package unthemedweapons.fx.particles;

import com.fs.starfarer.api.combat.WeaponAPI;
import particleengine.Particles;
import unthemedweapons.fx.particles.emitters.MuzzleFlashEmitter;

public abstract class EnergyBallMuzzleFlash {

    public static void makeMuzzleFlash(WeaponAPI weapon, float chargeLevel) {
        MuzzleFlashEmitter emitter = new MuzzleFlashEmitter()
                .arc(12f + chargeLevel * 8f)
                .angle(weapon.getCurrAngle())
                .range(30f + chargeLevel * 30f)
                .life(0.3f + chargeLevel * 0.5f, 0.4f + chargeLevel * 0.5f)
                .size(20f + chargeLevel * 15f, 40f + chargeLevel * 30f)
                .color(0.6f, 1f, 0.6f, 1f)
                .location(weapon.getFirePoint(0))
                .anchor(weapon.getShip());

        Particles.burst(emitter, 30 + (int) (chargeLevel * 40f));
    }

}
