package unthemedweapons.fx.particles;

import com.fs.starfarer.api.util.Pair;
import org.lwjgl.util.vector.Vector2f;
import particleengine.Emitter;
import particleengine.IEmitter;
import particleengine.Particles;
import unthemedweapons.fx.particles.emitters.MorphCannonDebrisEmitter;
import unthemedweapons.util.MathUtils;

public abstract class MorphCannonExplosion {
    public static Emitter core(Vector2f loc, float scale) {
        Emitter emitter = Particles.initialize(loc, "graphics/fx/owner_glow.png");
        float life = 1.2f;
        emitter.life(life*0.9f, life*1.1f);
        emitter.setSyncSize(true);
        emitter.fadeTime(life*0.1f, life*0.1f, life*0.9f, life*0.9f);

        float initialSize = scale * 0.6f;
        Pair<Float, Float> growthRateAcc = MathUtils.getRateAndAcceleration(initialSize, initialSize/2f, scale*0.8f, life);
        emitter.size(initialSize*0.9f, initialSize*1.1f);
        emitter.growthRate(growthRateAcc.one*0.9f, growthRateAcc.one*1.1f);
        emitter.growthAcceleration(growthRateAcc.two*0.9f, growthRateAcc.two*1.1f);
        emitter.color(1f, 0.75f, 0.5f, 0.25f);
        emitter.facing(0f, 360f);
        emitter.turnRate(-20f, 20f);
        emitter.circleOffset(0f, scale*0.1f);
        return emitter;
    }

    public static Emitter debris(Vector2f loc, float scale) {
        Emitter emitter = Particles.initialize(loc, "graphics/fx/particlealpha64sq.png");
        emitter.life(0.5f, 2f);
        emitter.setSyncSize(true);
        emitter.fadeTime(0f, 0f, 0.5f, 1.5f);
        emitter.size(10f, 30f);
        emitter.growthRate(-20f, -10f);
        emitter.color(1f, 0.5f, 0.25f, 1f);
        emitter.circleOffset(scale * 0.3f, scale * 0.5f);
        emitter.radialVelocity(-scale * 0.3f, - scale * 0.1f);
        emitter.revolutionRate(-20f, 20f);
        return emitter;
    }

    public static void makeExplosion(Vector2f loc, float scale) {
        Particles.stream(core(loc, scale), 1, 20f, 1.25f);
        IEmitter debrisEmitter =
                new MorphCannonDebrisEmitter(
                        scale / 2.4f,
                        scale / 16f,
                        scale / 8f,
                        -90f,
                        90f,
                        0.7f,
                        1f).location(loc);
        Particles.stream(debrisEmitter, 2, 400f, 1f);
    }
}
