package unthemedweapons.combat.ai;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.Misc;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.*;
import java.util.List;

public class PhaseTorpedoAI extends BaseGuidedMissileAI {

    private final IntervalUtil jumpInterval = new IntervalUtil(2.5f, 3f);
    private final IntervalUtil checkDamageInterval = new IntervalUtil(0.5f, 0.5f);
    private float hpCheckpoint, pendingDamage = 0f;
    private static final float minJumpRange = 150f, maxJumpRange = 300f;
    private static final float maxDamageTakenPerSecond = 1200f;
    private float fixedFacing = 0f;

    public PhaseTorpedoAI(MissileAPI missile, float maxSeekRangeFactor) {
        super(missile, maxSeekRangeFactor);
        hpCheckpoint = missile.getHitpoints();
        jumpInterval.setElapsed(1.5f);
        fixedFacing = missile.getFacing();
    }

    @Override
    public void advance(float amount) {
        missile.giveCommand(ShipCommand.ACCELERATE);
        jumpInterval.advance(amount);
        checkDamageInterval.advance(amount);

        boolean seek = preAdvance(amount);

        pendingDamage += Math.max(0f, hpCheckpoint - missile.getHitpoints());
        missile.setHitpoints(hpCheckpoint);

        if (checkDamageInterval.intervalElapsed()) {
            pendingDamage = Math.min(pendingDamage, maxDamageTakenPerSecond * checkDamageInterval.getIntervalDuration());

            hpCheckpoint = Math.max(1f, hpCheckpoint - pendingDamage);
            pendingDamage = 0f;
        }

        float tEnter = jumpInterval.getElapsed();
        float tExit = jumpInterval.getIntervalDuration() - jumpInterval.getElapsed();
        float tMin = Math.min(tEnter, tExit);
        if (tMin <= 0.5f) {
            missile.setJitter(missile, Color.WHITE, 1.6f * (0.5f - tMin), (int) (8f * (0.5f - tMin)), 0f, 100f * (0.5f - tMin));
        }

        missile.setFacing(fixedFacing);
        if (jumpInterval.intervalElapsed()) {
            List<Vector2f> jumpDestinations = new ArrayList<>();
            for (float t = 0f; t <= 1f; t += 0.1f) {
                float range = minJumpRange + t * (maxJumpRange - minJumpRange);
                Vector2f dest = Misc.getUnitVectorAtDegreeAngle(missile.getFacing());
                dest.scale(range);
                Vector2f.add(dest, missile.getLocation(), dest);
                jumpDestinations.add(dest);
            }

            if (seek) {
                Collections.sort(jumpDestinations, new Comparator<Vector2f>() {
                    @Override
                    public int compare(Vector2f v1, Vector2f v2) {
                        return Float.compare(
                                Misc.getDistance(v1, target.getLocation()),
                                Misc.getDistance(v2, target.getLocation())
                        );
                    }
                });
            }
            else {
                Collections.shuffle(jumpDestinations);
            }

            Iterator<Object> objItr = Global.getCombatEngine().getAllObjectGrid().getCheckIterator(missile.getLocation(), maxJumpRange, maxJumpRange);
            List<CombatEntityAPI> validCollisions = new ArrayList<>();
            while (objItr.hasNext()) {
                Object o = objItr.next();
                if (!(o instanceof CombatEntityAPI)) continue;

                CombatEntityAPI entity = (CombatEntityAPI) o;
                if (entity.getOwner() == missile.getOwner()) continue;
                if (entity instanceof DamagingProjectileAPI) continue;
                if (entity instanceof ShipAPI && ((ShipAPI) entity).isPhased()) continue;
                if (CollisionClass.NONE.equals(entity.getCollisionClass())) continue;

                validCollisions.add(entity);
            }

            outer:
            for (Vector2f dest : jumpDestinations) {
                for (CombatEntityAPI possibleCollision : validCollisions) {
                    if (Misc.getDistance(dest, possibleCollision.getLocation()) <= missile.getCollisionRadius() + possibleCollision.getCollisionRadius() + 25f) {
                        continue outer;
                    }
                }

                missile.getLocation().set(dest);
                missile.interruptContrail();
                if (seek) {
                    missile.getVelocity().set(new Vector2f());
                    missile.setFacing(Misc.getAngleInDegrees(dest, getInterceptionPoint(1f)));
                    fixedFacing = missile.getFacing();
                }
                break;
            }
        }
    }
}
