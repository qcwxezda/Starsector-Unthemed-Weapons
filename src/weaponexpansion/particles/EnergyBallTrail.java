package weaponexpansion.particles;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import org.lwjgl.util.vector.Vector2f;
import particleengine.Emitter;
import particleengine.Particles;

public class EnergyBallTrail {

    public static final float growthRate = 500f;

    public static void makeTrail(final CombatEntityAPI follow, final Vector2f velocity, final float maxSize) {
        final float[] size = {250f};
        Emitter emitter = Particles.initialize(follow.getLocation(), "graphics/fx/wpnxt_starburst_glow.png");
        emitter.setSyncSize(true);
        emitter.life(0.5f, 0.5f);
        emitter.fadeTime(0f, 0f,0.5f, 0.5f);
        emitter.size(size[0], size[0]);
        emitter.growthRate(-400f, -600f);
        emitter.turnRate(-120f, 120f);
        emitter.facing(0f, 360f);
        emitter.color(0.7f, 1f, 0.7f, 0.09f);
        Particles.stream(emitter, 1, 35, 100f, new Particles.StreamAction<Emitter>() {
            float time = Global.getCombatEngine().getTotalElapsedTime(false);
            @Override
            public boolean apply(Emitter emitter) {
                float newTime = Global.getCombatEngine().getTotalElapsedTime(false);
                float advanced = newTime - time;
                size[0] = Math.min(maxSize, size[0] + advanced * growthRate);
                emitter.setLocation(follow.getLocation());
                emitter.velocity(velocity, velocity);
                emitter.size(size[0], size[0]);
                time = newTime;
                return Global.getCombatEngine().isEntityInPlay(follow);
            }
        });
    }
}
