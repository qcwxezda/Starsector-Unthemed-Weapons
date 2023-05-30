package unthemedweapons.util;

import com.fs.starfarer.api.combat.BoundsAPI;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.Pair;
import org.lwjgl.util.vector.Vector2f;

import java.util.List;
import java.util.Random;

public abstract class MathUtils {
    public static float sgnPos(float x) {
        return x < 0 ? -1f : 1f;
    }

    /** Misc.getAngleDiff is unsigned; this is signed */
    public static float angleDiff(float a, float b) {
        return ((a - b) % 360 + 540) % 360 - 180;
    }

    public static float randBetween(float a, float b) {
        return randBetween(a, b, Misc.random);
    }

    public static float randBetween(float a, float b, Random random) {
        return random.nextFloat() * (b - a) + a;
    }

    public static boolean isClockwise(Vector2f v1, Vector2f v2) {
        return v1.y * v2.x > v1.x * v2.y;
    }

    public static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    public static void safeNormalize(Vector2f v) {
        if (v.x*v.x + v.y*v.y > 0) {
            v.normalise();
        }
    }

    public static float interpolate(float a, float b, float t) {
        return a + (b - a) * t;
    }

    public static float modPositive(float x, float mod) {
        x = x % mod;
        return x < 0 ? x + mod : x;
    }

    /** returns f(b) - f(a) */
    public static float applyAtLimits(Function f, float a, float b) {
        return f.apply(b) - f.apply(a);
    }

    public static Vector2f randomPointInCircle(Vector2f center, float radius) {
        float theta = Misc.random.nextFloat() * 2f *  (float) Math.PI;
        float r = radius * (float) Math.sqrt(Misc.random.nextFloat());
        return new Vector2f(center.x + r*(float)Math.cos(theta), center.y + r*(float)Math.sin(theta));
    }

    public static Vector2f randomPointInRing(Vector2f center, float inRadius, float outRadius) {
        float theta = Misc.random.nextFloat() * 2f * (float) Math.PI;
        float r = (float) Math.sqrt(Misc.random.nextFloat() * (outRadius*outRadius - inRadius*inRadius) + inRadius*inRadius);
        return new Vector2f(center.x + r*(float)Math.cos(theta), center.y + r*(float)Math.sin(theta));
    }

    /** Assumes that the quadratic is concave.
     *  Input the value of the quadratic at T = 0 (start), T = maxTime (end), and the quadratic's peak.
     *  Returns the linear and quadratic coefficients. */
    public static Pair<Float, Float> getRateAndAcceleration(float start, float end, float peak, float maxTime) {
        float sqrtTerm = (float) Math.sqrt((peak - end) * (peak - start));
        float a = 2f * (-2f*sqrtTerm + end - 2f*peak + start) / (maxTime*maxTime);
        float r = 2f * (sqrtTerm + peak - start) / maxTime;
        return new Pair<>(r, a);
    }

    public static Vector2f getVertexCenter(CombatEntityAPI entity) {
        BoundsAPI bounds = entity.getExactBounds();
        if (bounds == null) return entity.getLocation();

        bounds.update(entity.getLocation(), entity.getFacing());
        List<BoundsAPI.SegmentAPI> segments = bounds.getSegments();
        Vector2f sum = new Vector2f();
        for (BoundsAPI.SegmentAPI segment : segments) {
            Vector2f.add(sum, segment.getP2(), sum);
        }

        sum.scale(1f / segments.size());
        return sum;
    }

    public static float[][] clone2DArray(float[][] arr) {
        float[][] res = new float[arr.length][];
        for (int i = 0; i < arr.length; i++) {
            res[i] = arr[i].clone();
        }
        return res;
    }

    public interface Function {
        float apply(float x);
    }
}
