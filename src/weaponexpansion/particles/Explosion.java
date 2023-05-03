package weaponexpansion.particles;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;
import org.lwjgl.util.vector.Vector2f;
import particleengine.Emitter;
import particleengine.Particles;

import java.awt.*;

public class Explosion {
    public static Emitter core(Vector2f loc, float scale, float[] color) {
        Emitter emitter = Particles.initialize(loc, "graphics/fx/particlealpha_textured.png");
        emitter.circleOffset(0f, scale*0.15f);
        emitter.circleVelocity(scale*0.1f, scale*0.2f);
        emitter.color(color);
        emitter.facing(0f, 360f);
        emitter.fadeTime(0f, 0f, 1.75f, 2.25f);
        emitter.growthAcceleration(-scale, -scale*0.5f);
        emitter.growthRate(scale*0.75f, scale);
        emitter.life(1.75f, 2.25f);
        emitter.radialAcceleration(-scale, -scale*0.5f);
        emitter.randomHSVA(30f, 0.2f, 0f, 0f);
        emitter.revolutionRate(-20f, 20f);
        emitter.saturationShift(-0.3f, 0f);
        emitter.turnRate(-30f, 30f);
        emitter.turnAcceleration(-30f, 30f);
        emitter.size(scale*0.8f, scale*1.2f);
        return emitter;
    }

    public static Emitter ring(Vector2f loc, float scale, float[] color) {
        Emitter emitter = Particles.initialize(loc, "graphics/fx/wpnxt_explosion_ring.png");
        emitter.setSyncSize(true);
        emitter.life(0.75f, 1f);
        emitter.fadeTime(0.1f, 0.1f, 0.5f, 0.7f);
        emitter.size(scale*0.8f, scale*1.2f);
        emitter.growthRate(scale*1.5f, scale*2f);
        emitter.growthAcceleration(-scale*1.5f, -scale);
        emitter.color(color);
        emitter.hueShift(-50f, 50f);
        return emitter;
    }

    public static Emitter debris(Vector2f loc, float scale, float[] color) {
        Emitter emitter = Particles.initialize(loc, "graphics/fx/particlealpha32sq.png");
        emitter.setSyncSize(true);
        emitter.life(0.75f, 1.25f);
        emitter.fadeTime(0.1f, 0.1f, 0.5f, 0.7f);
        emitter.size(5f, 10f);
        emitter.circleOffset(0f, scale*0.25f);
        emitter.radialVelocity(scale*0.5f, scale);
        emitter.growthRate(-10f, -5f);
        emitter.color(color);
        emitter.hueShift(-20f, 20f);
        emitter.saturationShift(-0.2f, 0.2f);
        return emitter;
    }

    public static Emitter glow(Vector2f loc, float scale, float[] color) {
        Emitter emitter = Particles.initialize(loc, "graphics/fx/particlealpha64sq.png");
        emitter.setSyncSize(true);
        emitter.life(0.5f, 0.75f);
        emitter.fadeTime(0f, 0f, 0.5f, 0.75f);
        emitter.size(scale*1.5f, scale*1.5f);
        emitter.growthRate(-scale*0.4f, -scale*0.2f);
        emitter.color(color);
        return emitter;
    }

    public static void makeExplosion(Vector2f loc, float scale, int coreCount, int ringCount, int debrisCount) {
        makeExplosion(loc, scale, coreCount, ringCount, debrisCount, new float[] {1f, 0.75f, 0.5f, 0.2f}, new float[] {1f, 0.75f, 0.75f, 1f}, new float[] {1f, 0.5f, 0.2f, 1f}, new float[] {1f, 0.75f, 0.5f, 1f});
    }

    public static void makeExplosion(Vector2f loc, float scale, int coreCount, int ringCount, int debrisCount, float[] coreColor, float[] ringColor, float[] debrisColor, float[] glowColor) {
        Particles.burst(core(loc, scale, coreColor), coreCount);
        Particles.burst(ring(loc, scale, ringColor), ringCount);
        Particles.burst(debris(loc, scale, debrisColor), debrisCount);
        Particles.burst(glow(loc, scale, glowColor), 2);
    }
}
