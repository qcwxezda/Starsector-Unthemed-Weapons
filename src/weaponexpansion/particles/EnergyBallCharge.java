package weaponexpansion.particles;

import org.lwjgl.util.vector.Vector2f;
import particleengine.Emitter;
import particleengine.Particles;
import weaponexpansion.combat.scripts.EnergyBallLauncherEffect;

public class EnergyBallCharge {
    public static void makeChargeParticles(Vector2f loc, final EnergyBallLauncherEffect plugin) {
        Emitter emitter = Particles.initialize(loc);
        emitter.setSyncSize(true);
        emitter.life(0.2f, 0.2f);
        emitter.fadeTime(0.3f, 0.3f, 0.2f, 0.2f);
        emitter.circleOffset(30f, 60f);
        emitter.radialVelocity(-80f, -120f);
        emitter.size(5f, 10f);
        emitter.color(0.75f, 1f, 0.75f, 1f);
        emitter.revolutionRate(45f, 60f);
        Particles.stream(emitter, 1, 100f, 10f, new Particles.StreamAction() {
            @Override
            public boolean apply(Emitter emitter) {
                emitter.setLocation(plugin.getFirePoint());
                return plugin.getProjectileSize() > 0f;
            }
        });
    }
}
