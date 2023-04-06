package weaponexpansion.util.particles;

import weaponexpansion.util.Utils;

public class ParticleSize {
    public float size, sizeV, sizeA;
    private float size0, sizeV0, sizeA0, sizeJitter, sizeVJitter, sizeAJitter;
    public ParticleSize(float size, float sizeV, float sizeA, float sizeJitter, float sizeVJitter, float sizeAJitter) {
        size0 = size;
        sizeV0 = sizeV;
        sizeA0 = sizeA;
        this.sizeJitter = sizeJitter;
        this.sizeVJitter = sizeVJitter;
        this.sizeAJitter = sizeAJitter;
        randomize();
    }

    private ParticleSize(float size, float sizeV, float sizeA) {
        this.size = size;
        this.sizeV = sizeV;
        this.sizeA = sizeA;
    }

    public ParticleSize randomize() {
        size = size0 + Utils.randBetween(-sizeJitter / 2f, sizeJitter / 2f);
        sizeV = sizeV0 + Utils.randBetween(-sizeVJitter / 2f, sizeVJitter / 2f);
        sizeA = sizeA0 + Utils.randBetween(-sizeAJitter / 2f, sizeAJitter / 2f);
        return new ParticleSize(size, sizeV, sizeA);
    }
}
