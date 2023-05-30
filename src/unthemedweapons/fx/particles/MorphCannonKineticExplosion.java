package unthemedweapons.fx.particles;

import org.lwjgl.util.vector.Vector2f;
import particleengine.IEmitter;
import particleengine.Particles;
import unthemedweapons.fx.particles.emitters.KineticBurstEmitter;

public abstract class MorphCannonKineticExplosion {
    public static void makeExplosion(Vector2f loc, float range, float size, float life, int numParticles) {
        IEmitter emitter =
                new KineticBurstEmitter(
                        loc,
                        range*0.7f,
                        range*1.3f,
                        size*0.7f,
                        size*1.3f,
                        life*0.7f,
                        life*1.3f
                );
        Particles.burst(emitter, numParticles);
    }
}
