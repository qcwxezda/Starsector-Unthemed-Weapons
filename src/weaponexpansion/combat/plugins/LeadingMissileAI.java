package weaponexpansion.combat.plugins;

import com.fs.starfarer.api.combat.MissileAPI;
import com.fs.starfarer.api.combat.ShipCommand;
import com.fs.starfarer.api.util.Misc;
import org.lwjgl.util.vector.Vector2f;
import weaponexpansion.util.Utils;

public class LeadingMissileAI extends BaseMissileAI {

    private static class SmoothTurnData {
        ShipCommand command;
        float perpVelOffset;

        private SmoothTurnData(ShipCommand command, float perpVelOffset) {
            this.command = command;
            this.perpVelOffset = perpVelOffset;
        }
    }

    public LeadingMissileAI(MissileAPI missile, float maxSeekRange) {
        super(missile, maxSeekRange);
    }

    private SmoothTurnData smoothTurnExt(float targetAngle, boolean calculateResultingOffset) {
        // Observation: The missile should always be accelerating either clockwise or counterclockwise.
        // Instead of staying at angular velocity w for some angular distance d, we could travel the same distance in a
        // shorter amount of time by accelerating for the first d/2 stretch and decelerating for the second half,
        // ending up with the same angular velocity.
        // Therefore, we may either accelerate against the current angular velocity, or in the same direction
        // as the current angular velocity. If we accelerate in the -w direction, then the plan of motion is to bring
        // w to 0, and then correct the remaining error. If we accelerate in the w direction, then the plan of motion
        // is to accelerate until such a time that the stopping distance exactly equals the error, at which point we
        // start decelerating.
        // We should take whichever mechanism out of the two takes the shortest to accomplish.

        // Additionally, keep track of the resulting deviation in the actual velocity vector, which is its length
        // perpendicular to the target angle. Applying a linear acceleration for time dt when the signed facing error
        // is theta applies a deviation of a*sin(theta) dt. Since theta is quadratic in t, we may estimate sin theta
        // as theta to be able to integrate this analytically.

        float theta0 = Utils.angleDiff(missile.getFacing(), targetAngle);
        float w = missile.getAngularVelocity();
        if (w == 0f) {
            w = 0.0001f;
        }
        float W; // max turn rate
        float alpha; // turn acceleration

        // Case 1: accelerate against w
        float tA;
        float IA = 0f;
        float theta = theta0;
        alpha = -Math.signum(w) * missile.getTurnAcceleration();

        // First, zero out angular velocity. Obviously takes -w/alpha time to do this.
        float T1 = -w/alpha;

        // theta(t) = theta_0 + w t - 1/2 alpha t^2, so using approximation sin theta ~= theta, total perpendicular
        // velocity deviation for this time is theta_0 t + 1/2 w t^2 - 1/6 alpha t^3.
        IA += theta*T1 + 0.5f*w*T1*T1 - 1f/6f*alpha*T1*T1*T1;

        // During this time, facing angle increases by -w^2 / (2 alpha).
        theta -= w*w/(2f*alpha);
        theta = Utils.angleDiff(theta, 0f);
        // We may need to change acceleration direction here
        alpha = -missile.getTurnAcceleration() * Math.signum(theta);
        W = -missile.getMaxTurnRate() * Math.signum(theta);

        // Next, fix the remaining error by bringing theta to 0.
        // This is done by accelerating until theta is halved, and then decelerating until it is 0.
        // However, we may reach max speed before the half-way mark, so we need to account for that too.
        final float TW = W/alpha, TD = (float) Math.sqrt(-theta/alpha);
        if (TD <= TW) {
            final float T2 = T1 + TD;
            final float T3 = T2 + TD;
            tA = T3;

            // Integrate sin(theta + 1/2 alpha (t-T1)^2) ~= theta + 1/2 alpha t^2 from T1 to T2, then
            // integrate sin(theta/2 + alpha sqrt(-theta/alpha) (t-T2) - 1/2 alpha (t-T2)^2) from T2 to T3
            if (calculateResultingOffset) {
                final float fTheta = theta*Misc.RAD_PER_DEG, fAlpha = alpha*Misc.RAD_PER_DEG, fT1 = T1;
                IA += Utils.applyAtLimits(new Utils.Function() {
                    @Override
                    public float apply(float x) {
                        return fTheta * x + 1f / 6f * fAlpha * (x - fT1) * (x - fT1) * (x - fT1);
                    }
                }, T1, T2);
                IA += Utils.applyAtLimits(new Utils.Function() {
                    @Override
                    public float apply(float x) {
                        return 0.5f * fTheta * x + 0.5f * fAlpha * (float) Math.sqrt(-fTheta / fAlpha) * (x - T2) * (x - T2) - 1f / 6f * fAlpha * (x - T2) * (x - T2) * (x - T2);
                    }
                }, T2, T3);
            }
        }
        else {
            final float TH = (-0.5f*theta - 0.5f*alpha*TW*TW) / W; // How long it takes to go from max speed to the halfway mark
            final float T2 = T1 + TW;
            final float T3 = T2 + 2f*TH;
            final float T4 = T3 + TW;
            tA = T4;

            // sin(theta + 1/2 alpha (t-T1)^2) from T1 to T2
            // sin(theta + 1/2 alpha TW^2 + W*(t-T2)) from T2 to T3
            // sin(theta + 1/2 alpha TW^2 + 2*W*TH + W*(t-T3) - 1/2*alpha*(t-T3)^2) from T3 to T4
            if (calculateResultingOffset) {
                final float fTheta = theta*Misc.RAD_PER_DEG, fAlpha = alpha*Misc.RAD_PER_DEG, fw = w*Misc.RAD_PER_DEG, fT1 = T1;
                IA += Utils.applyAtLimits(new Utils.Function() {
                    @Override
                    public float apply(float x) {
                        return fTheta * x + 1f / 6f * fAlpha * (x - fT1) * (x - fT1) * (x - fT1);
                    }
                }, T1, T2);
                IA += Utils.applyAtLimits(new Utils.Function() {
                    @Override
                    public float apply(float x) {
                        return fTheta * x + 0.5f * fAlpha * TW * TW * x + 0.5f * fw * (x - T2) * (x - T2);
                    }
                }, T2, T3);
                IA += Utils.applyAtLimits(new Utils.Function() {
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

        // Find a time T such that the stopping distance after T plus the distance traveled to T is equal to 0 mod 360.
        // The simplified quadratic is alpha T^2 + 2wT + theta + w^2/(2 alpha) - 360k = 0 for some integer k.
        // Pick the smallest k such that the discriminant is positive and its square root is greater than 2w.
        float kThreshold = (w * w + 2f * alpha * theta) / (720f * alpha);
        int k = w < 0f ? (int) Math.floor(kThreshold) : (int) Math.ceil(kThreshold);
        final float T = (-2f*w + Math.signum(w) * (float) Math.sqrt(2f*w*w - 4f*alpha*theta + 1440f*alpha*k))/(2f*alpha);

        // Case 2.1: The angular velocity at T would be less than the maximum turn speed (everything is fine)
        if (Math.abs(w + alpha*T) <= W) {
            final float wT = w + alpha*T;
            final float T2 = T + wT/alpha;
            tW = T2;

            // sin(theta + w*t + 1/2*alpha*t^2) from 0 to T
            // sin(theta + w*T + 1/2*alpha*T^2 + wT*(t-T) - 1/2*alpha*(t-T)^2) from T to T2
            if (calculateResultingOffset && tW < tA) {
                final float fTheta = theta*Misc.RAD_PER_DEG, fAlpha = alpha*Misc.RAD_PER_DEG, fw = w;
                IW += Utils.applyAtLimits(new Utils.Function() {
                    @Override
                    public float apply(float x) {
                        return fTheta * x + 0.5f * fw * x * x + 1f / 6f * fAlpha * x * x * x;
                    }
                }, 0f, T);
                IW += Utils.applyAtLimits(new Utils.Function() {
                    @Override
                    public float apply(float x) {
                        return fTheta * x + fw * T * x + 0.5f * fAlpha * T * T * x + 0.5f * wT * (x - T) * (x - T) - 1f / 6f * fAlpha * (x - T) * (x - T) * (x - T);
                    }
                }, T, T2);
            }
        }
        // Case 2.2: The angular velocity at T would be greater than the maximum turn speed. In this case,
        // we only accelerate to maximum, then stay at a constant speed until we reach the stopping distance
        // criterion.
        else {
            W = Math.signum(w) * missile.getMaxTurnRate();
            final float fT1 = (W-w)/alpha; // Time to reach max angular velocity

            // Here we need to find T such that theta + w*fT1 + 1/2*alpha*fT1^2 + W*T + W^2/(2*alpha) = 360k
            // again with the smallest k so that T is positive.
            //k = (int) Math.ceil((theta + w*fT1 + 0.5f*alpha*fT1*fT1 + W*W/(2f*alpha))/360f);
            float T2part = Utils.modPositive((/*360f*k*/ - W*W/(2f*alpha) - 0.5f*alpha*fT1*fT1 - w*fT1 - theta)/W, Math.abs(360f / W));
            final float T2 = fT1 + T2part;
            final float T3 = T2 + W/alpha;
            tW = T3;

            // sin(theta + wt + 1/2 alpha t^2) from 0 to fT1
            // sin(theta + w fT1 + 1/2 alpha (fT1)^2 + W (t-fT1)) from fT1 to T2
            // sin(theta + w fT1 + 1/2 alpha (fT1)^2 + W (t-fT1) - 1/2 alpha (t-T2)^2) from T2 to T3
            if (calculateResultingOffset && tW < tA) {
                final float fTheta = theta*Misc.RAD_PER_DEG, fAlpha = alpha*Misc.RAD_PER_DEG, fw = w*Misc.RAD_PER_DEG, fW = W*Misc.RAD_PER_DEG;
                IW += Utils.applyAtLimits(new Utils.Function() {
                    @Override
                    public float apply(float x) {
                        return fTheta * x + 0.5f * fw * x * x + 1f / 6f * fAlpha * x * x * x;
                    }
                }, 0f, fT1);
                IW += Utils.applyAtLimits(new Utils.Function() {
                    @Override
                    public float apply(float x) {
                        return fTheta * x + fw * fT1 * x + 0.5f * fAlpha * fT1 * fT1 * x + 0.5f * fW * (x - fT1) * (x - fT1);
                    }
                }, fT1, T2);
                IW += Utils.applyAtLimits(new Utils.Function() {
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

        if (!preAdvance(amount)) {
            return;
        }

        Vector2f interceptor = getInterceptionLoS();
        float interceptAngle = Misc.getAngleInDegrees(interceptor);
        float velAngle = missile.getMoveSpeed() > 0f ? Misc.getAngleInDegrees(missile.getVelocity()) : missile.getFacing();

        float velError = Utils.angleDiff(interceptAngle, velAngle);

        if (Math.abs(velError) < 90f && Math.abs(velError) > 8f) {
            // To cancel out the perpendicular velocity.
            // Introduces some wobble -- to smooth out the motion would need to numerically integrate
            // the change in perpendicular error over time, etc.
            SmoothTurnData turnData = smoothTurnExt(interceptAngle, true);
            float curError = (float) Math.sin(velError * Misc.RAD_PER_DEG);
            if (Math.signum(turnData.perpVelOffset + curError) != Math.signum(curError)) {
                missile.giveCommand(turnData.command);
            }
            else {
                missile.giveCommand(smoothTurnExt(interceptAngle + velError * Math.min(1f, interceptor.length() / missile.getMoveSpeed()), false).command);
            }//turnTowardTarget(interceptAngle + velError);
        }
        else if (Math.abs(velError) > 3f) {
            missile.giveCommand(smoothTurnExt(interceptAngle, false).command);
            //turnTowardTarget(interceptAngle);
        }
    }
}
