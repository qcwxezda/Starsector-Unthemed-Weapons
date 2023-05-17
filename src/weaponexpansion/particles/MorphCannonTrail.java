package weaponexpansion.particles;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.DamagingProjectileAPI;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.Pair;
import org.lwjgl.util.vector.Vector2f;
import particleengine.Emitter;
import particleengine.Particles;
import weaponexpansion.combat.scripts.MorphCannonEffect;
import weaponexpansion.util.Utils;

public class MorphCannonTrail {
    public static void makeTrail(final DamagingProjectileAPI source) {
        Emitter emitter = Particles.initialize(source.getLocation(), "graphics/fx/particlealpha_textured.png");
        float life = 1.5f;
        emitter.life(life, life);
        emitter.fadeTime(0.1f, 0.1f, life-0.3f, life-0.3f);

        float initialSize = 9f;
        Pair<Float, Float> growthRateAcc = Utils.getRateAndAcceleration(initialSize, initialSize, initialSize*1.5f, life);
        emitter.size(initialSize * 5f, initialSize * 6f, initialSize * 0.9f, initialSize * 1.1f);
        emitter.growthRate(0f, 0f, growthRateAcc.one * 0.9f, growthRateAcc.one * 1.1f);
        emitter.growthAcceleration(0f, 0f, growthRateAcc.two * 0.9f, growthRateAcc.two * 1.1f);

        emitter.circleOffset(0f, 3f);
        emitter.offset(-20f, 0f, 0f, 0f);
//                Vector2f perp = source.getVelocity();
//                Utils.safeNormalize(perp);
//                perp.scale(Vector2f.dot(perp, Misc.getUnitVectorAtDegreeAngle(source.getFacing())));
//                Vector2f.sub(source.getVelocity(), perp, perp);
//                emitter.velocity(perp, perp);

        Particles.stream(emitter, 1, 100f, 100f, new Particles.StreamAction<Emitter>() {
            @Override
            public boolean apply(Emitter emitter) {
                emitter.setLocation(source.getLocation());
                emitter.setAxis(source.getFacing());

                emitter.facing(-10f, 10f);

//                // Want the angle to match the projectile's facing rather than velocity
//                float angleDiff = Utils.angleDiff(source.getFacing(), Misc.getAngleInDegrees(source.getVelocity()));
//                emitter.facing(angleDiff - 10f, angleDiff + 10f);

                Float maxLife = (Float) source.getCustomData().get(MorphCannonEffect.maxLifeKey);
                if (maxLife == null) {
                    return false;
                }

                float ratio = Math.min(1f, source.getElapsed() /  maxLife);
                emitter.color(1f - ratio * 0.1f, 0.25f + ratio * 0.6f, 0.25f + ratio * 0.5f, 0.9f);

                float elapsed = Global.getCombatEngine().getElapsedInLastFrame();
                float dist = source.getVelocity().length() * elapsed;
                emitter.offset(-dist, 0f, 0f, 0f);

                return !source.didDamage() && Global.getCombatEngine().isEntityInPlay(source);
            }
        });
    }
}
