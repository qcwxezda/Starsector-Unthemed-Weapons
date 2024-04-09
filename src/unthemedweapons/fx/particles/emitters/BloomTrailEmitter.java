package unthemedweapons.fx.particles.emitters;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.MissileAPI;
import com.fs.starfarer.api.util.Misc;
import org.lwjgl.util.vector.Vector2f;
import particleengine.BaseIEmitter;
import particleengine.ParticleData;
import unthemedweapons.util.MathUtils;

public class BloomTrailEmitter extends BaseIEmitter {

    public final MissileAPI missile;
    public final float size;
    private float facingLastFrame = 0f;
    private float deltaFacing;
    private float lastTimestamp;

    public BloomTrailEmitter(MissileAPI missile, float size) {
        this.missile = missile;
        this.size = size;
    }

    @Override
    public Vector2f getLocation() {
        return missile.getLocation();
    }

    @Override
    public float getXDir() {
        return missile.getFacing() - 90f;
    }

    @Override
    protected boolean preInitParticles(int start, int count) {
        float timestamp = Global.getCombatEngine().getTotalElapsedTime(false);
        if (timestamp == lastTimestamp) return true;
        float facing = Misc.getAngleInDegrees(missile.getVelocity());
        deltaFacing = (facing - facingLastFrame) / (timestamp - lastTimestamp);
        facingLastFrame = facing;
        lastTimestamp = timestamp;
        return true;
    }

    @Override
    protected ParticleData initParticle(int i) {
        ParticleData data = new ParticleData();
        float offsetX = MathUtils.randBetween(-size /2f, size /2f);
        float life = MathUtils.randBetween(size * 0.1f, size * 0.15f);
        float velX = -offsetX / life, velY = 0f;
        data.offset(new Vector2f(offsetX, -size - 15f + MathUtils.randBetween(0, 15f)));
        data.velocity(new Vector2f(velX, velY));
        data.size(size * Math.min(4f, 1f+Math.abs(0.02f*deltaFacing)), 4f*size);
        data.life(life);
        data.facing(Misc.getAngleInDegrees(new Vector2f(missile.getMoveSpeed(), -offsetX)));
        data.fadeTime(0f, life);
        data.color(0.3f+MathUtils.randBetween(-0.1f, 0.1f), 0.3f+MathUtils.randBetween(-0.1f, 0.1f), 1f, 0.25f);
        data.saturationShift(-0.75f / life);
        return data;
    }
}
