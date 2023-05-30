package unthemedweapons.fx.particles;

import com.fs.starfarer.api.util.Pair;
import org.lwjgl.util.vector.Vector2f;
import particleengine.Emitter;
import particleengine.Particles;
import unthemedweapons.util.MathUtils;

public abstract class Explosion {
    public static Emitter core(Vector2f loc, float scale, float dur, float[] color, String particlePath) {
        Emitter emitter = Particles.initialize(loc, particlePath);
        emitter.setSyncSize(true);
        emitter.circleOffset(0f, scale*0.1f);
        emitter.color(color);
        emitter.facing(0f, 360f);
        emitter.fadeTime(0f, 0f, dur*0.6f, dur*0.7f);

        float initialSize = scale*0.4f;
        Pair<Float, Float> growthRateAcceleration = MathUtils.getRateAndAcceleration(initialSize, scale*0.95f, scale, dur);
        emitter.size(initialSize*0.9f, initialSize*1.1f);
        emitter.growthAcceleration(growthRateAcceleration.two * 0.9f, growthRateAcceleration.two * 1.1f);
        emitter.growthRate(growthRateAcceleration.one * 0.9f, growthRateAcceleration.one * 1.1f);
        emitter.life(dur*0.9f, dur*1.1f);

        emitter.randomHSVA(35f, 1f, 0f, 0f);
        emitter.revolutionRate(-5f, 5f);
        emitter.saturationShift(-0.2f, -0.1f);
        emitter.turnRate(-20f, 20f);
        return emitter;
    }

    public static Emitter ring(Vector2f loc, float scale, float dur,  float[] color) {
        Emitter emitter = Particles.initialize(loc, "graphics/fx/wpnxt_explosion_ring.png");
        emitter.setSyncSize(true);
        emitter.life(dur * 0.5f, dur * 0.6f);
        emitter.fadeTime(0f, 0f, dur * 0.3f, dur * 0.4f);

        float initialSize = scale * 0.2f;
        Pair<Float, Float> growthRateAndAcceleration = MathUtils.getRateAndAcceleration(initialSize, scale*1.8f, scale*1.8f, dur);
        emitter.size(initialSize * 0.9f, initialSize * 1.1f);
        emitter.growthRate(growthRateAndAcceleration.one * 0.9f, growthRateAndAcceleration.one * 1.1f);
        emitter.growthAcceleration(growthRateAndAcceleration.two * 0.9f, growthRateAndAcceleration.two * 1.1f);

        emitter.color(color);
        emitter.randomHSVA(10f, 0.2f, 0f, 0f);
        return emitter;
    }

    public static Emitter debris(Vector2f loc, float scale, float dur, float[] color) {
        Emitter emitter = Particles.initialize(loc, "graphics/fx/particlealpha64sq.png");
        emitter.setSyncSize(true);
        emitter.life(dur * 0.6f, dur * 1.2f);
        emitter.fadeTime(0f, 0f, dur * 0.2f, dur * 0.3f);
        float debrisScale = (float) Math.sqrt(scale) * 1.2f;
        emitter.size(debrisScale*0.8f, debrisScale*1.2f);
        emitter.circleOffset(0f, scale*0.15f);
        emitter.radialVelocity(scale*0.2f / dur, scale*1.3f / dur);
        emitter.growthRate(-debrisScale / dur, -debrisScale / dur);
        emitter.color(color);
        emitter.randomHSVA(15f, 0.4f, 0f, 0f);
        emitter.saturationShift(-0.2f, 0.2f);
        return emitter;
    }

    public static Emitter glow(Vector2f loc, float scale, float dur, float[] color) {
        Emitter emitter = Particles.initialize(loc, "graphics/fx/particlealpha64sq.png");
        emitter.setSyncSize(true);
        emitter.life(dur, dur*1.2f);
        emitter.fadeTime(0f, 0f, dur * 0.7f, dur * 0.8f);

        float initialSize = scale*1.5f;
        Pair<Float, Float> growthRateAndAcceleration = MathUtils.getRateAndAcceleration(initialSize, initialSize, scale*1.8f, dur);
        emitter.size(initialSize * 0.9f, initialSize * 1.1f);
        emitter.growthRate(growthRateAndAcceleration.one * 0.9f, growthRateAndAcceleration.one * 1.1f);
        emitter.growthAcceleration(growthRateAndAcceleration.two * 0.9f, growthRateAndAcceleration.two * 1.1f);

        emitter.color(color);
        return emitter;
    }

    public static void makeExplosion(Vector2f loc, float scale, int coreCount, int ringCount, int debrisCount) {
        makeExplosion(loc, scale, coreCount, ringCount, debrisCount, new float[] {1f, 0.75f, 0.5f, 0.3f}, new float[] {1f, 0.75f, 0.5f, 1f}, new float[] {1f, 0.5f, 0.2f, 0.3f}, new float[] {1f, 0.75f, 0.5f, 1f});
    }

    public static void makeExplosion(Vector2f loc, float scale, float dur, int coreCount, int ringCount, int debrisCount) {
        makeExplosion(loc, scale, dur, coreCount, ringCount, debrisCount, new float[] {1f, 0.75f, 0.5f, 0.3f}, new float[] {1f, 0.75f, 0.5f, 1f}, new float[] {1f, 0.5f, 0.2f, 0.3f}, new float[] {1f, 0.75f, 0.5f, 1f});
    }

    public static void makeExplosion(Vector2f loc, float scale, int coreCount, int ringCount, int debrisCount, float[] coreColor, float[] ringColor, float[] debrisColor, float[] glowColor) {
        makeExplosion(loc, scale, coreCount, ringCount, debrisCount, coreColor, ringColor, debrisColor, glowColor, "graphics/fx/explosion3.png");
    }

    public static void makeExplosion(Vector2f loc, float scale, float dur, int coreCount, int ringCount, int debrisCount, float[] coreColor, float[] ringColor, float[] debrisColor, float[] glowColor) {
        makeExplosion(loc, scale, dur, coreCount, ringCount, debrisCount, coreColor, ringColor, debrisColor, glowColor, "graphics/fx/explosion3.png");
    }

    public static void makeExplosion(Vector2f loc, float scale, int coreCount, int ringCount, int debrisCount, float[] coreColor, float[] ringColor, float[] debrisColor, float[] glowColor, String particlePath) {
        makeExplosion(loc, scale, 1.5f, coreCount, ringCount, debrisCount, coreColor, ringColor, debrisColor, glowColor, particlePath);
    }

    public static void makeExplosion(Vector2f loc, float scale, float dur, int coreCount, int ringCount, int debrisCount, float[] coreColor, float[] ringColor, float[] debrisColor, float[] glowColor, String particlePath) {
        Particles.burst(core(loc, scale, dur, coreColor, particlePath), coreCount);
        Particles.burst(ring(loc, scale, dur, ringColor), ringCount);
        Particles.burst(debris(loc, scale, dur, debrisColor), debrisCount);
        Particles.burst(glow(loc, scale, dur, glowColor), 1);
    }

}
