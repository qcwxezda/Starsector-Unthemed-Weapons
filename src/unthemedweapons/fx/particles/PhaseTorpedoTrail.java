package unthemedweapons.fx.particles;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import org.lwjgl.util.vector.Vector2f;
import particleengine.Emitter;
import particleengine.Particles;

public abstract class PhaseTorpedoTrail {

    public static Emitter trail(Vector2f loc, float amp) {
        Emitter emitter = Particles.initialize(loc, "graphics/fx/smoke32.png");
        emitter.color(0.98f, 0.53f, 0.69f, 0.12f);
        emitter.randomHSVA(30f, 0f, 0f, 0f);
        emitter.saturationShift(-0.12f, -0.15f);
        emitter.velocity(-90f, -80f, 0f, 0f);
        emitter.size(20f, 30f, 10f, 15f);
        emitter.growthRate(8f, 12f, 8f, 12f);
        emitter.fadeTime(0f, 0f, 2f, 3f);
        emitter.life(3f, 3.5f);
        emitter.facing(0, 360f);
        emitter.turnRate(-5f, 5f);
        emitter.sinusoidalMotionY(amp*0.5f, amp*0.75f, 0.5f, 0.5f, 0f, 0f);
        return emitter;
    }

    public static void makeTrail(final CombatEntityAPI follow) {
        for (float f = -15f; f <= 15f; f += 30f) {
            Emitter trailEmitter = trail(follow.getLocation(), f);
            Particles.stream(trailEmitter, 1, 250, 100f, new Particles.StreamAction<Emitter>() {
                @Override
                public boolean apply(Emitter emitter) {
                    emitter.setAxis(follow.getFacing());
                    emitter.setLocation(follow.getLocation());
                    return Global.getCombatEngine().isEntityInPlay(follow);
                }
            });
        }
    }
}
