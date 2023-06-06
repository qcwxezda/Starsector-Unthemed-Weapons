package unthemedweapons.combat.ai;

import com.fs.starfarer.api.combat.MissileAPI;
import com.fs.starfarer.api.combat.ShipCommand;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.Misc;
import org.lwjgl.util.vector.Vector2f;
import unthemedweapons.util.MathUtils;

public class LeadingMissileAI extends BaseGuidedMissileAI {

    private final IntervalUtil seekInterval = new IntervalUtil(0.1f, 0.1f);
    private float targetAngle = 0f;
    private static final float maxRandomOffset = 500f;
    private final float randomOffset;

    private static class SmoothTurnData {
        ShipCommand command;
        float perpVelOffset;

        private SmoothTurnData(ShipCommand command, float perpVelOffset) {
            this.command = command;
            this.perpVelOffset = perpVelOffset;
        }
    }

    public LeadingMissileAI(MissileAPI missile, float maxSeekRangeFactor) {
        super(missile, maxSeekRangeFactor);
        randomOffset = MathUtils.randBetween(-maxRandomOffset, maxRandomOffset);
        seekInterval.forceIntervalElapsed();
    }

    private SmoothTurnData smoothTurnExt(float targetAngle, boolean calculateResultingOffset) {
        float theta0 = MathUtils.angleDiff(missile.getFacing(), targetAngle);
        float w = missile.getAngularVelocity();
        if (w == 0f) {
            w = 0.0001f;
        }
        float W;
        float alpha;

        // Case 1: accelerate against w
        float tA;
        float IA = 0f;
        float theta = theta0;
        alpha = -Math.signum(w) * missile.getTurnAcceleration();
        float T1 = -w/alpha;
        IA += theta*T1 + 0.5f*w*T1*T1 - 1f/6f*alpha*T1*T1*T1;
        theta -= w*w/(2f*alpha);
        theta = MathUtils.angleDiff(theta, 0f);
        alpha = -missile.getTurnAcceleration() * Math.signum(theta);
        W = -missile.getMaxTurnRate() * Math.signum(theta);
        final float TW = W/alpha, TD = (float) Math.sqrt(-theta/alpha);
        if (TD <= TW) {
            final float T2 = T1 + TD;
            final float T3 = T2 + TD;
            tA = T3;
            if (calculateResultingOffset) {
                final float fTheta = theta*Misc.RAD_PER_DEG, fAlpha = alpha*Misc.RAD_PER_DEG, fT1 = T1;
                IA += MathUtils.applyAtLimits(new MathUtils.Function() {
                    @Override
                    public float apply(float x) {
                        return fTheta * x + 1f / 6f * fAlpha * (x - fT1) * (x - fT1) * (x - fT1);
                    }
                }, T1, T2);
                IA += MathUtils.applyAtLimits(new MathUtils.Function() {
                    @Override
                    public float apply(float x) {
                        return 0.5f * fTheta * x + 0.5f * fAlpha * (float) Math.sqrt(-fTheta / fAlpha) * (x - T2) * (x - T2) - 1f / 6f * fAlpha * (x - T2) * (x - T2) * (x - T2);
                    }
                }, T2, T3);
            }
        }
        else {
            final float TH = (-0.5f*theta - 0.5f*alpha*TW*TW) / W;
            final float T2 = T1 + TW;
            final float T3 = T2 + 2f*TH;
            final float T4 = T3 + TW;
            tA = T4;
            if (calculateResultingOffset) {
                final float fTheta = theta*Misc.RAD_PER_DEG, fAlpha = alpha*Misc.RAD_PER_DEG, fw = w*Misc.RAD_PER_DEG, fT1 = T1;
                IA += MathUtils.applyAtLimits(new MathUtils.Function() {
                    @Override
                    public float apply(float x) {
                        return fTheta * x + 1f / 6f * fAlpha * (x - fT1) * (x - fT1) * (x - fT1);
                    }
                }, T1, T2);
                IA += MathUtils.applyAtLimits(new MathUtils.Function() {
                    @Override
                    public float apply(float x) {
                        return fTheta * x + 0.5f * fAlpha * TW * TW * x + 0.5f * fw * (x - T2) * (x - T2);
                    }
                }, T2, T3);
                IA += MathUtils.applyAtLimits(new MathUtils.Function() {
                    @Override
                    public float apply(float x) {
                        return fTheta * x + 0.5f * fAlpha * TW * TW * x + 2f * fw * TH * x + 0.5f * fw * (x - T3) * (x - T3) + 1f / 6f * fAlpha * (x - T3) * (x - T3) * (x - T3);
                    }
                }, T3, T4);
            }
        }

        // Case 2: accelerate with w
        float tW;
        float IW = 0f;
        alpha = Math.signum(w) * missile.getTurnAcceleration();
        theta = theta0;
        W = missile.getMaxTurnRate();
        float kThreshold = (w * w + 2f * alpha * theta) / (720f * alpha);
        int k = w < 0f ? (int) Math.floor(kThreshold) : (int) Math.ceil(kThreshold);
        final float T = (-2f*w + Math.signum(w) * (float) Math.sqrt(2f*w*w - 4f*alpha*theta + 1440f*alpha*k))/(2f*alpha);
        if (Math.abs(w + alpha*T) <= W) {
            final float wT = w + alpha*T;
            final float T2 = T + wT/alpha;
            tW = T2;
            if (calculateResultingOffset && tW < tA) {
                final float fTheta = theta*Misc.RAD_PER_DEG, fAlpha = alpha*Misc.RAD_PER_DEG, fw = w;
                IW += MathUtils.applyAtLimits(new MathUtils.Function() {
                    @Override
                    public float apply(float x) {
                        return fTheta * x + 0.5f * fw * x * x + 1f / 6f * fAlpha * x * x * x;
                    }
                }, 0f, T);
                IW += MathUtils.applyAtLimits(new MathUtils.Function() {
                    @Override
                    public float apply(float x) {
                        return fTheta * x + fw * T * x + 0.5f * fAlpha * T * T * x + 0.5f * wT * (x - T) * (x - T) - 1f / 6f * fAlpha * (x - T) * (x - T) * (x - T);
                    }
                }, T, T2);
            }
        }
        else {
            W = Math.signum(w) * missile.getMaxTurnRate();
            final float fT1 = (W-w)/alpha;

            float T2part = MathUtils.modPositive((/*360f*k*/ - W*W/(2f*alpha) - 0.5f*alpha*fT1*fT1 - w*fT1 - theta)/W, Math.abs(360f / W));
            final float T2 = fT1 + T2part;
            final float T3 = T2 + W/alpha;
            tW = T3;

            if (calculateResultingOffset && tW < tA) {
                final float fTheta = theta*Misc.RAD_PER_DEG, fAlpha = alpha*Misc.RAD_PER_DEG, fw = w*Misc.RAD_PER_DEG, fW = W*Misc.RAD_PER_DEG;
                IW += MathUtils.applyAtLimits(new MathUtils.Function() {
                    @Override
                    public float apply(float x) {
                        return fTheta * x + 0.5f * fw * x * x + 1f / 6f * fAlpha * x * x * x;
                    }
                }, 0f, fT1);
                IW += MathUtils.applyAtLimits(new MathUtils.Function() {
                    @Override
                    public float apply(float x) {
                        return fTheta * x + fw * fT1 * x + 0.5f * fAlpha * fT1 * fT1 * x + 0.5f * fW * (x - fT1) * (x - fT1);
                    }
                }, fT1, T2);
                IW += MathUtils.applyAtLimits(new MathUtils.Function() {
                    @Override
                    public float apply(float x) {
                        return fTheta * x + fw * fT1 * x + 0.5f * fAlpha * fT1 * fT1 * x + 0.5f * fW * (x - fT1) * (x - fT1) - 1f / 6f * fAlpha * (x - T2) * (x - T2) * (x - T2);
                    }
                }, T2, T3);
            }
        }

        if (tA <= tW) {
            return new SmoothTurnData(w < 0f ? ShipCommand.TURN_LEFT : ShipCommand.TURN_RIGHT, IA);
        }
        else {
            return new SmoothTurnData(w < 0f ? ShipCommand.TURN_RIGHT : ShipCommand.TURN_LEFT, IW);
        }
    }

    @Override
    public void advance(float amount) {

        missile.giveCommand(ShipCommand.ACCELERATE);
        seekInterval.advance(amount);

        if (!preAdvance(amount)) {
            return;
        }

        if (seekInterval.intervalElapsed()) {
            Vector2f interceptPoint = getInterceptionPoint(1f);
            Vector2f interceptLoS = Misc.getDiff(interceptPoint, missile.getLocation());
            MathUtils.safeNormalize(interceptLoS);
            Vector2f perp = Misc.getPerp(interceptLoS);
            float dist = Misc.getDistance(interceptPoint, missile.getLocation());
            float distRatio = Math.min(1f, dist / missile.getMaxRange());
            perp.scale(distRatio * randomOffset);

            Vector2f modifiedInterceptPoint = new Vector2f();
            Vector2f.add(interceptPoint, perp, modifiedInterceptPoint);
            Vector2f modifiedInterceptLoS = Misc.getDiff(modifiedInterceptPoint, missile.getLocation());
            targetAngle = Misc.getAngleInDegrees(modifiedInterceptLoS);
        }

        float moveSpeed = missile.getVelocity().length() + 0.01f;
        float velAngle = moveSpeed > 0f ? Misc.getAngleInDegrees(missile.getVelocity()) : missile.getFacing();
        float velError = MathUtils.angleDiff(targetAngle, velAngle);

        if (Math.abs(velError) < 90f && Math.abs(velError) > 8f) {
            // To cancel out the perpendicular velocity.
            // Introduces some wobble -- to smooth out the motion would need to numerically integrate
            // the change in perpendicular error over time, etc.
            SmoothTurnData turnData = smoothTurnExt(targetAngle, true);
            float curError = (float) Math.sin(velError * Misc.RAD_PER_DEG);
            if (Math.signum(turnData.perpVelOffset + curError) != Math.signum(curError)) {
                missile.giveCommand(turnData.command);
            }
            else {
                missile.giveCommand(smoothTurnExt(targetAngle + Math.signum(velError) * Math.min(Math.abs(velError), 10f)/* + velError * Math.min(1f, Misc.getDistance(target.getLocation(), missile.getLocation()) / moveSpeed)*/, false).command);
            }//turnTowardTarget(interceptAngle + velError);
        }
        else if (Math.abs(velError) > 3f) {
            missile.giveCommand(smoothTurnExt(targetAngle, false).command);
            //turnTowardTarget(interceptAngle);
        }
    }
}
