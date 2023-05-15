package weaponexpansion.particles;

import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.Pair;
import org.lwjgl.util.vector.Vector2f;
import particleengine.Emitter;
import particleengine.Particles;
import weaponexpansion.combat.scripts.EnergyBallLauncherEffect;
import weaponexpansion.util.Utils;

public class EnergyBallMuzzleFlash {

    public static void makeMuzzleFlash(WeaponAPI weapon, float chargeLevel) {
        int numParticles = 200 + (int) (chargeLevel * 300f);
        float life = 0.5f + chargeLevel;

        float ballSize = EnergyBallLauncherEffect.getProjectileSize(chargeLevel);
        Emitter emitter = Particles.initialize(weapon.getFirePoint(0), "graphics/fx/particlealpha64sq.png");
        emitter.setSyncSize(true);
        emitter.life(life*0.9f, life*1.1f);
        emitter.fadeTime(0f, 0f, life*0.8f, life*0.9f);

        emitter.circleOffset(0f, 3f);
//        emitter.radialVelocity(0f, ballSize / life);
//        emitter.radialAcceleration(ballSize / (life*life), 0f);
        emitter.velocity(weapon.getShip().getVelocity(), weapon.getShip().getVelocity());

        emitter.size(ballSize * 0.9f, ballSize * 1.1f);
        emitter.color(0.6f, 1f, 0.6f, 0.2f);
        emitter.randomHSVA(30f, 0.4f, 0f, 0.2f);

        Particles.burst(emitter, numParticles);
    }

}
