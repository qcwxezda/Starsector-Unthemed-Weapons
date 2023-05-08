package weaponexpansion.particles;

import com.fs.starfarer.api.combat.CombatEngineLayers;
import com.fs.starfarer.api.util.Misc;
import org.lwjgl.util.vector.Vector2f;
import particleengine.Emitter;
import particleengine.Particles;

public class FlickerJumpTrail {
    public static Emitter jumpTrail(Vector2f loc, float dist) {
        Emitter emitter = Particles.initialize(loc, "graphics/fx/particlealpha_textured.png");
        emitter.setLayer(CombatEngineLayers.CONTRAILS_LAYER);
        emitter.fadeTime(0f, 0f, 0.5f, 0.5f);
        emitter.size(32f, 32f);
        emitter.growthRate(-10f, -20f);
        emitter.velocity(0f, 0f, -5f, 5f);
        emitter.facing(0f, 360f);
        emitter.turnRate(-60f, 60f);
        emitter.color(0.7f, 0.5f, 1f, 0.2f);
        emitter.offset(0f, dist, 0f, 0f);
        return emitter;
    }

    public static void makeJumpTrail(Vector2f from, Vector2f to) {
        float dist = Misc.getDistance(from, to);
        Emitter emitter = jumpTrail(from, dist);
        emitter.setAxis(Misc.getDiff(to, from));
        Particles.burst(emitter, (int) (dist / 2f));
    }
}
