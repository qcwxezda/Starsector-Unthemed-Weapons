package weaponexpansion.util.particles;

import weaponexpansion.util.Utils;

public class ParticleAngle {
    public float theta, w, alpha;
    private float thetaJitter, wJitter, alphaJitter, theta0, w0, alpha0;

    public ParticleAngle(float theta, float w, float alpha, float thetaJitter, float wJitter, float alphaJitter) {
        this.thetaJitter = thetaJitter;
        this.wJitter = wJitter;
        this.alphaJitter = alphaJitter;
        theta0 = theta;
        w0 = w;
        alpha0 = alpha;
        randomize();
    }

    private ParticleAngle(float theta, float w, float alpha) {
        this.theta = theta;
        this.w = w;
        this.alpha = alpha;
    }

    public ParticleAngle randomize() {
        theta = theta0 + Utils.randBetween(-thetaJitter / 2f, thetaJitter / 2f);
        w = w0 + Utils.randBetween(-wJitter / 2f, wJitter / 2f);
        alpha = alpha0 + Utils.randBetween(-alphaJitter / 2f, alphaJitter / 2f);
        return new ParticleAngle(theta, w, alpha);
    }
}