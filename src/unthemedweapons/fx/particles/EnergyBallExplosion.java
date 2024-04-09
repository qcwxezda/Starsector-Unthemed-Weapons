package unthemedweapons.fx.particles;

import org.lwjgl.util.vector.Vector2f;
import particleengine.Particles;
import unthemedweapons.fx.particles.emitters.EnergyBallCoreGlow;

import java.awt.Color;

public abstract class EnergyBallExplosion {

    public static void makeExplosion(Vector2f loc, float radius) {
        int coreCount = 5;
        float coreAlpha = 0.15f;
        float dur = 1.2f + radius / 200f;
        Explosion.makeExplosion(
                loc,
                radius*2.4f,
                dur,
                coreCount,
                10,
                0,
                new float[] {coreAlpha, 1f, coreAlpha, coreAlpha},
                new float[] {0.7f, 1f, 0.7f, 0.4f},
                new float[] {0.7f, 1f, 0.7f, 0.5f},
                new float[] {0.8f, 1f, 0.8f, 1f},
                "graphics/fx/explosion4.png");

        EnergyBallCoreGlow coreGlow = new EnergyBallCoreGlow(loc, radius*4f, radius*4f, radius*6f, dur);//coreGlow(loc, radius*1.6f, dur);
        coreGlow.color = new Color(0.5f, 1f, 0.5f, 0.5f);
        Particles.burst(coreGlow, 10);
    }
}
