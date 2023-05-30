package unthemedweapons.fx.particles;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import org.lwjgl.util.vector.Vector2f;
import particleengine.Emitter;
import particleengine.Particles;

public abstract class BloomTrail {
    public static Emitter trail(Vector2f loc, float scale) {
        Emitter emitter = Particles.initialize(loc, "graphics/fx/particlealpha64sq.png");
        emitter.setSyncSize(true);
        emitter.color(0.3f, 0.3f, 1f, 0.6f);
        emitter.offset(-30f, 0f, 0f, 0f);
        emitter.hueShift(-30f, 30f);
        emitter.saturationShift(-0.4f, 0.4f);
        emitter.velocity(-50f, -25f, -30f, 30f);
        emitter.size(scale, 2f*scale);
        emitter.growthRate(-2f*scale, -scale);
        emitter.fadeTime(0f, 0f, 0.5f, 0.5f);
        emitter.life(0.75f, 1.25f);
        emitter.facing(0f, 360f);
        emitter.sinusoidalMotionY(5f, 10f, 0.25f, 0.5f, 0f, 360f);
        return emitter;
    }

    public static void makeTrail(final CombatEntityAPI follow, float scale, int particlesPerSecond) {
        Emitter trailEmitter = trail(follow.getLocation(), scale);
        Particles.stream(trailEmitter, 1, particlesPerSecond, 100f, new Particles.StreamAction<Emitter>() {
            @Override
            public boolean apply(Emitter emitter) {
                emitter.setAxis(follow.getFacing());
                emitter.setLocation(follow.getLocation());
                return Global.getCombatEngine().isEntityInPlay(follow);
            }
        });
    }
}
