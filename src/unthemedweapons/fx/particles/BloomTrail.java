package unthemedweapons.fx.particles;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.MissileAPI;
import particleengine.Particles;
import unthemedweapons.fx.particles.emitters.BloomTrailEmitter;

public class BloomTrail {
    public static void makeTrail(final MissileAPI missile, float particleScale, float particlesPerSecond) {
        Particles.stream(
                new BloomTrailEmitter(missile, particleScale), 2, particlesPerSecond, -1f,
                new Particles.StreamAction<BloomTrailEmitter>() {
                    @Override
                    public boolean apply(BloomTrailEmitter emitter) {
                        return Global.getCombatEngine().isEntityInPlay(missile);
                    }
                });
    }
}
