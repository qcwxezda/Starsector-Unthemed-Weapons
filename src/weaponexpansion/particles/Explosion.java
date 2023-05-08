package weaponexpansion.particles;

import com.fs.starfarer.api.combat.CombatEngineLayers;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;
import org.lwjgl.util.vector.Vector2f;
import particleengine.Emitter;
import particleengine.Particles;

public class Explosion {
    public static Emitter core(Vector2f loc, float scale, float[] color, String particlePath) {
        Emitter emitter = Particles.initialize(loc, particlePath);
        emitter.setSyncSize(true);
        emitter.circleOffset(0f, scale*0.15f);
        emitter.circleVelocity(0f, scale*0.05f);
        emitter.color(color);
        emitter.facing(0f, 360f);
        emitter.fadeTime(0.1f, 0.1f, 1.3f, 1.5f);
        emitter.size(scale*0.6f, scale*0.8f);
        emitter.growthAcceleration(-scale*0.7f, -scale*0.6f);
        emitter.growthRate(scale*0.8f, scale);
        emitter.life(1.75f, 2.25f);
        emitter.radialVelocity(scale*0.03f, scale*0.4f);
        emitter.radialAcceleration(-scale*0.2f, -scale*0.15f);
        emitter.randomHSVA(35f, 1f, 0f, 0f);
        emitter.revolutionRate(-10f, 10f);
        emitter.saturationShift(-0.3f, 0f);
        emitter.turnRate(-20f, 20f);
        emitter.turnAcceleration(-20f, 20f);
        return emitter;
    }

    public static Emitter ring(Vector2f loc, float scale, float[] color) {
        Emitter emitter = Particles.initialize(loc, "graphics/fx/wpnxt_explosion_ring.png");
        emitter.setSyncSize(true);
        emitter.life(0.75f, 1f);
        emitter.fadeTime(0.1f, 0.1f, 0.5f, 0.7f);
        emitter.size(scale*0.8f, scale*1.2f);
        emitter.growthRate(scale*2f, scale*2.5f);
        emitter.growthAcceleration(-scale*1.5f, -scale);
        emitter.color(color);
        emitter.hueShift(-50f, 50f);
        return emitter;
    }

    public static Emitter debris(Vector2f loc, float scale, float[] color) {
        Emitter emitter = Particles.initialize(loc, "graphics/fx/particlealpha32sq.png");
        emitter.setSyncSize(true);
        emitter.life(0.75f, 1.75f);
        emitter.fadeTime(0f, 0f, 0.5f, 0.7f);
        emitter.size(scale*0.025f, scale*0.05f);
        emitter.circleOffset(0f, scale*0.4f);
        emitter.radialVelocity(scale*0.5f, scale*0.75f);
        emitter.growthRate(-scale*0.025f, -scale*0.0125f);
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
        makeExplosion(loc, scale, coreCount, ringCount, debrisCount, new float[] {1f, 0.75f, 0.5f, 0.1f}, new float[] {1f, 0.75f, 0.75f, 1f}, new float[] {1f, 0.5f, 0.2f, 0.3f}, new float[] {1f, 0.75f, 0.5f, 1f});
    }

    public static void makeExplosion(Vector2f loc, float scale, int coreCount, int ringCount, int debrisCount, float[] coreColor, float[] ringColor, float[] debrisColor, float[] glowColor) {
        makeExplosion(loc, scale, coreCount, ringCount, debrisCount, coreColor, ringColor, debrisColor, glowColor, "graphics/fx/explosion4.png");
    }

    public static void makeExplosion(Vector2f loc, float scale, int coreCount, int ringCount, int debrisCount, float[] coreColor, float[] ringColor, float[] debrisColor, float[] glowColor, String particlePath) {
        Particles.burst(core(loc, scale, coreColor, particlePath), coreCount);
        Particles.burst(ring(loc, scale, ringColor), ringCount);
        Particles.burst(debris(loc, scale, debrisColor), debrisCount);
        Particles.burst(glow(loc, scale, glowColor), 2);
    }

}
