package unthemedweapons.fx.particles.emitters;

import org.lwjgl.util.vector.Vector2f;
import particleengine.BaseIEmitter;
import particleengine.ParticleData;
import unthemedweapons.util.MathUtils;

public class MorphCannonDebrisEmitter extends BaseIEmitter {
    private final Vector2f location;
    private final float radius, minRevoRate, maxRevoRate, minLife, maxLife, minSize, maxSize;

    public MorphCannonDebrisEmitter(float radius, float minSize, float maxSize, float minRevoRate, float maxRevoRate, float minLife, float maxLife) {
        location = new Vector2f();
        this.radius = radius;
        this.minRevoRate = minRevoRate;
        this.maxRevoRate = maxRevoRate;
        this.minLife = minLife;
        this.maxLife = maxLife;
        this.minSize = minSize;
        this.maxSize = maxSize;
    }

    public MorphCannonDebrisEmitter location(Vector2f location) {
        this.location.set(location);
        return this;
    }

    @Override
    public Vector2f getLocation(){
        return location;
    }

    @Override
    protected ParticleData initParticle(int i) {
        ParticleData data = new ParticleData();

        float life = MathUtils.randBetween(minLife, maxLife);
        data.life(life).fadeTime(life, 0f);

        Vector2f offset = MathUtils.randomPointInRing(new Vector2f(), radius * 0.8f, radius * 1.2f);
        Vector2f velocity = new Vector2f(-offset.x, -offset.y);
        MathUtils.safeNormalize(velocity);
        velocity.scale((offset.length() - 1f) / life);
        data.offset(offset).velocity(velocity);

        float[] color = new float[] {
                1f,
                0.75f,
                0.5f,
                1f};
        data.color(color);
        data.revolutionRate(MathUtils.randBetween(minRevoRate, maxRevoRate));
        float size = MathUtils.randBetween(minSize, maxSize);
        data.size(size, size);
        data.growthRate(-size / life, -size / life);
        return data;
    }
}
