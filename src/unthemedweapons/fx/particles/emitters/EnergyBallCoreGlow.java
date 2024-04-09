package unthemedweapons.fx.particles.emitters;

import com.fs.starfarer.api.graphics.SpriteAPI;
import org.lwjgl.util.vector.Vector2f;
import particleengine.BaseIEmitter;
import particleengine.ParticleData;
import unthemedweapons.util.MathUtils;

import java.awt.Color;

public class EnergyBallCoreGlow extends BaseIEmitter {

    public final Vector2f location;
    public float startScale, minEndScale, maxEndScale;
    public float life;
    public Color color = Color.WHITE;

    public EnergyBallCoreGlow(Vector2f location, float startScale, float minEndScale, float maxEndScale, float life) {
        this.location = location;
        this.startScale = startScale;
        this.minEndScale = minEndScale;
        this.maxEndScale = maxEndScale;
        this.life = life;
    }

    @Override
    public Vector2f getLocation() {
        return location;
    }

    @Override
    public SpriteAPI getSprite() {
        return particleengine.Utils.getLoadedSprite("graphics/fx/wpnxt_starburst_glow.png");
    }

    @Override
    protected ParticleData initParticle(int i) {
        ParticleData data = new ParticleData();
        data.life(life);
        data.fadeTime(0.1f * life, 0.9f * life);
        data.color(color.getRed() / 255f, color.getGreen() / 255f, color.getBlue() / 255f, color.getAlpha() / 255f * MathUtils.randBetween(1f, 2f));
        data.size(startScale, startScale);
        float endScale = MathUtils.randBetween(minEndScale, maxEndScale);
        float growthRate = (endScale - startScale) / life;
        data.growthRate(growthRate, growthRate);
        data.facing(MathUtils.randBetween(0f, 360f));
        data.turnRate((11f * i) % 30f - 15f);
        return data;
    }
}
