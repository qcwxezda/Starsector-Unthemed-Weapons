package unthemedweapons.fx.particles;

import com.fs.starfarer.api.combat.CombatEngineLayers;
import com.fs.starfarer.api.util.Pair;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;
import org.lwjgl.util.vector.Vector2f;
import particleengine.Emitter;
import particleengine.Particles;
import unthemedweapons.util.MathUtils;

public abstract class PhaseTorpedoSecondaryExplosion {

    public static void makeStaticRing(Vector2f loc) {
        Emitter emitter = Particles.initialize(loc, "graphics/fx/explosion_ring0.png");
        emitter.setBlendMode(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL14.GL_FUNC_ADD);
        emitter.setLayer(CombatEngineLayers.ABOVE_SHIPS_AND_MISSILES_LAYER);
        emitter.setSyncSize(true);

        Pair<Float, Float> radVelAcc = MathUtils.getRateAndAcceleration(0f, 1500f, 1500f, 5.5f);
        emitter.size(30f, 50f);
        emitter.growthRate(radVelAcc.one * 0.9f, radVelAcc.one * 1.1f);
        emitter.growthAcceleration(radVelAcc.two * 0.9f, radVelAcc.two * 1.1f);

        emitter.facing(0f, 360f);
        emitter.color(1f, 0.75f, 0.5f,0.8f);
        emitter.turnRate(-10f, 10f);
        emitter.life(5f, 6f);
        emitter.fadeTime(0f, 0f, 4f, 5f);
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
        emitter.life(5f, 6f);
        emitter.fadeTime(0f, 0f, 4f, 5f);
        emitter.circleOffset(5f, 45f);

        Pair<Float, Float> radVelAcc = MathUtils.getRateAndAcceleration(0f, 500f, 500f, 5.5f);
        emitter.radialVelocity(radVelAcc.one * 0.9f, radVelAcc.one * 1.1f);
        emitter.radialAcceleration(radVelAcc.two * 0.9f, radVelAcc.two * 1.1f);

        emitter.facing(0f, 360f);
        emitter.size(120f, 150f);
        emitter.growthRate(40f, 60f);
        emitter.growthAcceleration(-8f, -12f);
        emitter.turnRate(-45f, 45f);
        emitter.color(1f, 0.75f, 0.5f,0.15f);
        emitter.randomHSVA(16f, 1.2f, 0f, 0f);
        Particles.burst(emitter, numParticles);
    }
}
