package weaponexpansion.particles;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import org.lwjgl.util.vector.Vector2f;
import particleengine.Emitter;
import particleengine.Particles;

public class FlickerTrail {

    public static Emitter trail(Vector2f loc, float amp) {
        Emitter emitter = Particles.initialize(loc, "graphics/fx/smoke32.png");
        emitter.color(0.6f, 0.4f, 1f, 0.15f);
        emitter.randomHSVA(30f, 0f, 0f, 0f);
        emitter.offset(-30f, 0f, 0f, 0f);
        emitter.saturationShift(-0.24f, -0.3f);
        emitter.velocity(-90f, -80f, 0f, 0f);
        emitter.size(50f, 60f, 20f, 20f);
        emitter.growthRate(-5f, -5f, 0f, 0f);
        emitter.fadeTime(0f, 0f, 1.5f, 2f);
        emitter.life(3f, 3.5f);
        emitter.facing(-10f, 10f);
        emitter.turnRate(-5f, 5f);
        emitter.sinusoidalMotionY(amp, amp*1.5f, 0.1f, 0.1f, 0f, 0f);
        return emitter;
    }

    public static void makeTrail(final CombatEntityAPI follow) {
        final float angleAmp = 5f;
        final float[] anglePhase = { 0f };
        for (float f = -20f; f <= 20f; f += 20f) {
            Emitter trailEmitter = trail(follow.getLocation(), f);
            Particles.stream(trailEmitter, 5, 100, 100f, new Particles.StreamAction() {
                @Override
                public boolean apply(Emitter emitter) {
                    anglePhase[0] += 0.04f;
                    emitter.setAxis(follow.getFacing() + angleAmp * (float)Math.sin(anglePhase[0]));
                    emitter.setLocation(follow.getLocation());
                    return Global.getCombatEngine().isEntityInPlay(follow);
                }
            });
        }
    }
}
