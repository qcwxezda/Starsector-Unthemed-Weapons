package weaponexpansion.combat.scripts;

import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.util.Misc;
import org.lwjgl.util.vector.Vector2f;

@SuppressWarnings("unused")
public class BoltStormEffect implements OnFireEffectPlugin, EveryFrameWeaponEffectPlugin, WeaponEffectPluginWithInit {

    float angleOffset = 0f;
    float maxAngleOffset = 10f;
    int shotsPerCycle = 42;
    float offsetDir = 1;
    float hasntFiredDuration = 0f;

    @Override
    public void onFire(DamagingProjectileAPI proj, WeaponAPI weapon, CombatEngineAPI engine) {

        Vector2f v = Misc.getUnitVectorAtDegreeAngle(weapon.getCurrAngle() + angleOffset + 180f);
        v.scale(proj.getProjectileSpec().getLength());
        Vector2f.add(v, proj.getLocation(), v);

        proj.getTailEnd().set(v);

        if (Math.abs(angleOffset) >= maxAngleOffset) {
            offsetDir *= -1;
        }
        angleOffset += offsetDir * 4 * (maxAngleOffset / shotsPerCycle);

        hasntFiredDuration = 0f;
    }

    @Override
    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
        if (hasntFiredDuration >= 3f * weapon.getCooldown())  {
            if (angleOffset > 0f) {
                angleOffset -= Math.min(angleOffset, 0.5f);
            }
            if (angleOffset < 0f) {
                angleOffset += Math.min(-angleOffset, 0.5f);
            }
        }

        hasntFiredDuration += amount;
    }

    @Override
    public void init(WeaponAPI weapon) {
        maxAngleOffset = weapon.getSpec().getMaxSpread() / 2f;
        if (weapon.getSlot().isHardpoint()) {
            maxAngleOffset /= 2f;
        }
    }
}
