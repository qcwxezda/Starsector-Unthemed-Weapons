package weaponexpansion.particles;

import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.util.Misc;
import org.lwjgl.util.vector.Vector2f;
import particleengine.BaseIEmitter;
import particleengine.ParticleData;
import weaponexpansion.util.Utils;

public class KineticBurstEmitter extends BaseIEmitter {

    private final Vector2f location;
    private final float minRange, maxRange, minSize, maxSize, minLife, maxLife;

    public KineticBurstEmitter(Vector2f location, float minRange, float maxRange, float minSize, float maxSize, float minLife, float maxLife) {
        this.location = new Vector2f(location);
        this.minRange = minRange;
        this.maxRange = maxRange;
        this.minSize = minSize;
        this.maxSize = maxSize;
        this.minLife = minLife;
        this.maxLife = maxLife;
    }

    @Override
    public Vector2f getLocation() {
        return location;
    }

    @Override
    public SpriteAPI getSprite() {
        return particleengine.Utils.getLoadedSprite("graphics/fx/owner_glow.png");
    }

    @Override
    protected ParticleData initParticle(int i) {
        ParticleData data = new ParticleData();

        float life = Utils.randBetween(minLife, maxLife);
        data.life(life).fadeTime(0f, life);

        float angle = Utils.randBetween(0f, 360f);
        Vector2f vel = Misc.getUnitVectorAtDegreeAngle(angle);
        vel.scale(Utils.randBetween(minRange, maxRange) / life);
        data.velocity(vel).facing(angle);

        float size = Utils.randBetween(minSize, maxSize);
        data.size(size, size / 6f);
        data.growthRate(-size/life, -size/(6f*life));

        data.color(0.9f, 0.9f, 0.7f, 1f);

        return data;
    }
}
