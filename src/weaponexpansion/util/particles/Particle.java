package weaponexpansion.util.particles;

import com.fs.starfarer.api.graphics.SpriteAPI;
import org.lwjgl.util.vector.Vector2f;
import weaponexpansion.util.Utils;

import java.awt.*;

public class Particle {
    public ParticlePosition pos;
    public ParticleAngle angle;
    public ParticleSize size;
    public float dur;
    public Color colorIn, colorOut;
    public SpriteAPI sprite;

    private Color color;
    private float elapsed = 0f;

    public Particle(
            SpriteAPI sprite,
            ParticlePosition pos,
            ParticleAngle angle,
            ParticleSize size,
            float dur,
            float durJitter,
            Color colorIn,
            Color colorOut) {
        this.pos = pos;
        this.angle = angle;
        this.size = size;
        this.dur = dur + Utils.randBetween(-durJitter /2f, durJitter / 2f);
        this.colorIn = colorIn;
        this.colorOut = colorOut;
        this.sprite = sprite;
        color = colorIn;
    }

    public void advance(float amount) {

        // Apply acceleration and velocity
        Vector2f.add(pos.v, new Vector2f(pos.a.x * amount, pos.a.y * amount), pos.v);
        Vector2f.add(pos.x, new Vector2f(pos.v.x * amount, pos.v.y * amount), pos.x);

        // Apply angular acceleration and angular velocity
        angle.w += angle.alpha * amount;
        angle.theta += angle.w * amount;

        // Apply size growth and growth acceleration
        size.sizeV += size.sizeA * amount;
        size.size += size.sizeV * amount;

        // Interpolate color between colorIn and colorOut
        color = Utils.interpolateColor(colorIn, colorOut, Utils.clamp(elapsed / dur, 0f, 1f));

        elapsed += amount;
    }

    public boolean isExpired() {
        return elapsed > dur;
    }

    public Color getColor() {
        return color;
    }
}
