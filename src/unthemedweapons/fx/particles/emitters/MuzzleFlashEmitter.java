package unthemedweapons.fx.particles.emitters;

import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.util.Misc;
import org.lwjgl.util.vector.Vector2f;
import particleengine.BaseIEmitter;
import particleengine.ParticleData;
import unthemedweapons.util.MathUtils;

public class MuzzleFlashEmitter extends BaseIEmitter {

    private final Vector2f location;
    private float angle, arc, range, minLife, maxLife, minSize, maxSize, velocityScale;
    private final float[] color = new float[] {1f, 1f, 1f, 1f};
    private CombatEntityAPI anchor;

    public MuzzleFlashEmitter() {
        location = new Vector2f();
        angle = arc = range = 0f;
        minLife = maxLife = 0.5f;
        minSize = 20f;
        maxSize = 30f;
        velocityScale = 1f;
    }

    public MuzzleFlashEmitter anchor(CombatEntityAPI anchor) {
        this.anchor = anchor;
        return this;
    }

    public MuzzleFlashEmitter location(Vector2f location) {
        this.location.set(location);
        return this;
    }

    public MuzzleFlashEmitter angle(float angle) {
        this.angle = angle;
        return this;
    }

    public MuzzleFlashEmitter arc(float arc) {
        this.arc = arc;
        return this;
    }

    public MuzzleFlashEmitter range(float range) {
        this.range = range;
        return this;
    }

    public MuzzleFlashEmitter life(float minLife, float maxLife) {
        this.minLife = minLife;
        this.maxLife = maxLife;
        return this;
    }

    public MuzzleFlashEmitter size(float minSize, float maxSize) {
        this.minSize = minSize;
        this.maxSize = maxSize;
        return this;
    }

    public MuzzleFlashEmitter color(float r, float g, float b, float a) {
        color[0] = r;
        color[1] = g;
        color[2] = b;
        color[3] = a;
        return this;
    }

    public MuzzleFlashEmitter velocityScale(float velocityScale) {
        this.velocityScale = velocityScale;
        return this;
    }

    @Override
    public Vector2f getLocation() {
        return location;
    }

    @Override
    protected ParticleData initParticle(int i) {
        ParticleData data = new ParticleData();

        // Life uniformly random between minLife and maxLife
        float life = MathUtils.randBetween(minLife, maxLife);
        data.life(life).fadeTime(0f, life);

        float theta = angle + MathUtils.randBetween(-arc / 2f, arc / 2f);
        float r = range * (float) Math.sqrt(Misc.random.nextFloat());
        Vector2f pt = new Vector2f(r*(float)Math.cos(theta*Misc.RAD_PER_DEG), r*(float)Math.sin(theta*Misc.RAD_PER_DEG));
        // Velocity is proportional to distance from center
        Vector2f vel = Misc.getUnitVectorAtDegreeAngle(theta);
        vel.scale(velocityScale);
        vel.scale(r);
        // Add the anchor's velocity, if it exists
        if (anchor != null) {
            Vector2f.add(anchor.getVelocity(), vel, vel);
        }
        data.offset(pt).velocity(vel);

        // Size uniformly random between minSize and maxSize
        float size = MathUtils.randBetween(minSize, maxSize);
        data.size(size, size);

        // Color
        data.color(color);

        return data;
    }
}
