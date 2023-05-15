package weaponexpansion.particles;

import com.fs.starfarer.api.util.Pair;
import org.lwjgl.util.vector.Vector2f;
import particleengine.Emitter;
import particleengine.Particles;
import weaponexpansion.util.Utils;

public class EnergyBallExplosion {

    public static Emitter coreGlow(Vector2f loc, float scale, float dur) {
        Emitter emitter = Particles.initialize(loc, "graphics/fx/starburst_glow1.png");
        emitter.setSyncSize(true);
        emitter.life(dur, dur);
        emitter.circleOffset(0f, scale * 0.05f);
        emitter.fadeTime(0.25f, 0.25f, dur*0.7f-0.25f, dur*0.7f-0.25f);
        emitter.color(0.5f, 1f, 0.5f, 1f);
        emitter.randomHSVA(20f, 0.6f, 0f, 0f);
        Pair<Float, Float> rateAcceleration = Utils.getRateAndAcceleration(scale, 0f, scale * 2f, dur);
        emitter.size(scale * 0.9f, scale * 1.1f);
        emitter.growthRate(rateAcceleration.one * 0.9f, rateAcceleration.one * 1.1f);
        emitter.growthAcceleration(rateAcceleration.two * 0.9f, rateAcceleration.two * 1.1f);
        emitter.facing(0f, 360f);
        emitter.turnRate(-30f, 30f);
        return emitter;
    }

    public static void makeExplosion(Vector2f loc, float radius) {
        int coreCount = 15;
        float coreAlpha = 0.3f;
        float dur = radius / 100f;
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

        Emitter coreGlow = coreGlow(loc, radius, dur);
        Particles.burst(coreGlow, 3);
    }
}
