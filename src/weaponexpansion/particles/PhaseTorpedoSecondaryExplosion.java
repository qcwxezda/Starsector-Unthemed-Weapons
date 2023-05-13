package weaponexpansion.particles;

import com.fs.starfarer.api.combat.CombatEngineLayers;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;
import org.lwjgl.util.vector.Vector2f;
import particleengine.Emitter;
import particleengine.Particles;

public class PhaseTorpedoSecondaryExplosion {

    public static void makeStaticRing(Vector2f loc) {
        Emitter emitter = Particles.initialize(loc, "graphics/fx/explosion_ring0.png");
        emitter.setBlendMode(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL14.GL_FUNC_ADD);
        emitter.setSyncSize(true);
        emitter.size(30f, 50f);
        emitter.growthRate(400f, 500f);
        emitter.facing(0f, 360f);
        emitter.color(1f, 0.75f, 0.5f,0.8f);
        emitter.turnRate(-10f, 10f);
        emitter.life(2.8f, 3.2f);
        emitter.fadeTime(0f, 0f, 2f, 2.5f);
        emitter.randomHSVA(20f, 0f, 0f, 0f);
        emitter.saturationShift(-0.1f, -0.2f);
        emitter.colorValueShift(-0.1f, -0.2f);
        emitter.hueShift(-10f, 10f);
        Particles.burst(emitter, 5);
    }

    public static void makeRing(Vector2f loc, int numParticles) {
        Emitter emitter = Particles.initialize(loc, "graphics/fx/smoke32.png");
        //emitter.setBlendMode(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL14.GL_FUNC_ADD);
        emitter.setLayer(CombatEngineLayers.ABOVE_PARTICLES);
        emitter.setSyncSize(true);
        emitter.life(2.6f, 3.4f);
        emitter.fadeTime(0f, 0f, 2f, 2.5f);
        emitter.circleOffset(5f, 45f);
        emitter.radialVelocity(140f, 160f);
        emitter.facing(0f, 360f);
        emitter.size(120f, 150f);
        emitter.growthRate(40f, 60f);
        emitter.turnRate(-45f, 45f);
        emitter.color(1f, 0.75f, 0.5f,0.15f);
        emitter.randomHSVA(16f, 1.2f, 0f, 0f);
        Particles.burst(emitter, numParticles);
    }
}
