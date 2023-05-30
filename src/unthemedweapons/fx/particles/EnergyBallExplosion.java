package unthemedweapons.fx.particles;

import com.fs.starfarer.api.util.Pair;
import org.lwjgl.util.vector.Vector2f;
import particleengine.Emitter;
import particleengine.Particles;
import unthemedweapons.util.MathUtils;

public abstract class EnergyBallExplosion {

    public static Emitter coreGlow(Vector2f loc, float scale, float dur) {
        Emitter emitter = Particles.initialize(loc, "graphics/fx/wpnxt_starburst_glow.png");
        emitter.setSyncSize(true);
        emitter.life(dur, dur);
        emitter.circleOffset(0f, scale * 0.05f);
        emitter.fadeTime(0.25f, 0.25f, dur*0.7f-0.25f, dur*0.7f-0.25f);
        emitter.color(0.5f, 1f, 0.5f, 1f);
        emitter.randomHSVA(15f, 0.2f, 0f, 0f);
        Pair<Float, Float> rateAcceleration = MathUtils.getRateAndAcceleration(scale, scale * 2.5f, scale * 2.5f, dur);
        emitter.size(scale * 0.9f, scale * 1.1f);
        emitter.growthRate(rateAcceleration.one * 0.9f, rateAcceleration.one * 1.1f);
        emitter.growthAcceleration(rateAcceleration.two * 0.9f, rateAcceleration.two * 1.1f);
        emitter.facing(0f, 360f);
        return emitter;
    }

    public static void makeExplosion(Vector2f loc, float radius) {
        int coreCount = 20;
        float coreAlpha = 0.08f;
        float dur = 0.8f + radius / 300f;
        Explosion.makeExplosion(
                loc,
                radius*2f,
                dur,
                coreCount,
                10,
                0,
                new float[] {coreAlpha, 1f, coreAlpha, coreAlpha},
                new float[] {0.7f, 1f, 0.7f, 0.4f},
                new float[] {0f, 0f, 0f, 0f},
                new float[] {0.8f, 1f, 0.8f, 1f},
                "graphics/fx/explosion0.png");

        Emitter coreGlow = coreGlow(loc, radius*1.6f, dur);
        Particles.burst(coreGlow, 3);
    }
}
