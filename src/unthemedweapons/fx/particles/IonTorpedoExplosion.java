package unthemedweapons.fx.particles;

import org.lwjgl.util.vector.Vector2f;
import particleengine.Emitter;
import particleengine.Particles;

public abstract class IonTorpedoExplosion {
    public static Emitter core(Vector2f loc) {
        Emitter emitter = Particles.initialize(loc, "graphics/fx/explosion1.png");
        emitter.circleOffset(0f, 50f);
        emitter.life(1.25f, 1.75f);
        emitter.fadeTime(0.1f, 0.1f, 1f, 1.5f);
        emitter.facing(0f, 360f);
        emitter.turnRate(-50f, 50f);
        emitter.turnAcceleration(-50f, 50f);
        emitter.radialVelocity(70f, 100f);
        emitter.radialAcceleration(-100f, -70f);
        emitter.revolutionRate(-20f, 20f);
        emitter.color(0.588f, 1f, 0.902f, 0.05f);
        emitter.randomHSVA(20f, 0.1f, 0f, 0f);
        emitter.colorShiftHSVA(0f, -0.3f, 0f, 0f);
        emitter.size(200f, 300f);
        emitter.growthRate(100f, 150f);
        emitter.growthAcceleration(-50f, -75f);
        return emitter;
    }

    public static Emitter ring(Vector2f loc, float angle) {
        Emitter emitter = Particles.initialize(loc, "graphics/fx/wpnxt_explosion_ring.png");
        emitter.setAxis(angle);
        emitter.life(0.75f, 1f);
        emitter.fadeTime(0.1f, 0.1f, 0.5f, 0.7f);
        emitter.size(300f, 400f, 40f, 60f);
        emitter.growthRate(400f, 500f);
        emitter.growthAcceleration(-50f, -60f);
        emitter.color(0.5f, 1f, 0.902f, 0.75f);
        emitter.hueShift(-50f, 50f);
        emitter.saturationShift(-0.2f, -0.2f);
        emitter.facing(-55f, -35f);
        return emitter;
    }

    public static Emitter empArcs(Vector2f loc) {
        Emitter emitter = Particles.initialize(loc, "graphics/fx/wpnxt_emp_arcs_modified.png");
        emitter.life(0.25f, 0.3f);
        emitter.fadeTime(0f, 0f, 0.15f, 0.25f);
        emitter.size(550f, 550f, 500f, 500f);
        emitter.growthRate(-40f, -80f);
        emitter.turnRate(-10f, 10f);
        emitter.facing(0f, 360f);
        emitter.color(0.7f, 1f, 1f, 0.7f);
        emitter.facing(0f, 360f);
        emitter.alphaShift(-0.5f, -0.5f);
        return emitter;
    }

    public static void makeExplosion(Vector2f loc, float angle) {
        Particles.burst(core(loc), 100);

        Emitter ringEmitter = IonTorpedoExplosion.ring(loc, angle);
        Particles.burst(ringEmitter, 5);
        ringEmitter.facing(35f, 55f);
        Particles.burst(ringEmitter, 5);

        Particles.stream(empArcs(loc), 1, 15, 1f);
    }
}
