package weaponexpansion.util.particles;

import org.lwjgl.util.vector.Vector2f;
import weaponexpansion.util.Utils;

public class ParticlePosition {
    public Vector2f x, v, a;

    private Vector2f x0, v0, a0;
    private float xJitter, vJitter, aJitter;

    /** Jitter elements add vectors randomly chosen from inside a circle of the given radius */
    public ParticlePosition(Vector2f x, Vector2f v, Vector2f a, float xJitter, float vJitter, float aJitter) {
        x0 = x;
        v0 = v;
        a0 = a;
        this.xJitter = xJitter;
        this.vJitter = vJitter;
        this.aJitter = aJitter;
        randomize();
    }

    private ParticlePosition(Vector2f x, Vector2f v, Vector2f a) {
        this.x = x;
        this.v = v;
        this.a = a;
    }

    public ParticlePosition randomize() {
        x = new Vector2f(x0);
        Vector2f.add(x, Utils.randomPointInCircle(new Vector2f(), xJitter), x);
        v = new Vector2f(v0);
        Vector2f.add(v, Utils.randomPointInCircle(new Vector2f(), vJitter), v);
        a = new Vector2f(a0);
        Vector2f.add(a, Utils.randomPointInCircle(new Vector2f(), aJitter), a);
        return new ParticlePosition(x, v, a);
    }
}
