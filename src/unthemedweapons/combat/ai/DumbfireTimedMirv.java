package unthemedweapons.combat.ai;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.DamagingProjectileAPI;
import com.fs.starfarer.api.combat.MissileAIPlugin;
import com.fs.starfarer.api.combat.MissileAPI;
import com.fs.starfarer.api.combat.ShipCommand;
import com.fs.starfarer.api.loading.MissileSpecAPI;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.combat.CombatEngine;
import org.lwjgl.util.vector.Vector2f;
import unthemedweapons.util.MathUtils;

import java.awt.*;

public class DumbfireTimedMirv implements MissileAIPlugin {

    private static final int smokeCount = 5;
    private static final Color smokeColor = new Color(100, 100, 100, 200);
    private static final float speedVariance = 0.25f;
    private final float initialFacing;
    private final String weaponName;
    private final MissileAPI missile;
    private final int numShots;
    private final float timeToSplit, splitArc;
    private final boolean evenSpread;
    private float elapsed = 0f;


    public DumbfireTimedMirv(
            MissileAPI missile,
            String weaponName,
            int numShots,
            float timeToSplit,
            float splitArc,
            boolean evenSpread) {
        this.missile = missile;
        initialFacing = missile.getFacing();
        this.weaponName = weaponName;
        this.numShots = numShots;
        this.timeToSplit = timeToSplit;
        this.splitArc = splitArc;
        this.evenSpread = evenSpread;
    }


    @Override
    public void advance(float amount) {
        elapsed += amount;
        missile.giveCommand(ShipCommand.ACCELERATE);

        if (elapsed >= timeToSplit) {
            CombatEngine engine = (CombatEngine) Global.getCombatEngine();
            engine.removeEntity(missile);

            float dir = initialFacing;
            float offset = numShots > 1 ? -splitArc / 2f : 0f;
            MissileSpecAPI spec = (MissileSpecAPI) Global.getSettings().getWeaponSpec(weaponName).getProjectileSpec();
            float temp = spec.getLaunchSpeed();
            for (int i = 0; i < numShots; i++) {
                float newDir = evenSpread ? dir + offset : dir + MathUtils.randBetween(-splitArc / 2f, splitArc / 2f);
                spec.setLaunchSpeed(temp * MathUtils.randBetween(1f-speedVariance, 1f+speedVariance));
                DamagingProjectileAPI proj = (DamagingProjectileAPI) engine.spawnProjectile(missile.getSource(), missile.getWeapon(), weaponName, missile.getLocation(), newDir, missile.getVelocity());
                proj.setFromMissile(true);
                offset += splitArc / (numShots - 1);
            }
            spec.setLaunchSpeed(temp);

            float missileSpeed = missile.getVelocity().length();
            for (int i = 0; i < smokeCount; i++) {
                Vector2f randomDir = Misc.getUnitVectorAtDegreeAngle(MathUtils.randBetween(0f, 360f));
                randomDir.scale(MathUtils.randBetween(0f, missileSpeed * 0.5f));
                Vector2f.add(randomDir, missile.getVelocity(), randomDir);
                engine.addSmokeParticle(missile.getLocation(), randomDir, missile.getCollisionRadius() * MathUtils.randBetween(0.6f, 1.5f), 1f, 1f, smokeColor);
            }
            // last argument: how much debris
            engine.getDebrisSystem().spawnDebris(missile.getLocation(), missile.getVelocity(), 150f, 10f, 60f, 200f);
        }
    }
}
