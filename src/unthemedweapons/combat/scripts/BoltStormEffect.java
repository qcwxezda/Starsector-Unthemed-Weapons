package unthemedweapons.combat.scripts;

import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.util.Misc;
import org.lwjgl.util.vector.Vector2f;

@SuppressWarnings("unused")
public class BoltStormEffect implements OnFireEffectPlugin, EveryFrameWeaponEffectPlugin, WeaponEffectPluginWithInit {
    int shotsPerCycle = 42;
    float phase = 0f, amplitude = 0f;
    float hasntFiredDuration = 0f;

    @Override
    public void onFire(DamagingProjectileAPI proj, WeaponAPI weapon, CombatEngineAPI engine) {

        Vector2f v = Misc.getUnitVectorAtDegreeAngle(weapon.getCurrAngle() + amplitude * (float) Math.sin(phase) + 180f);
        v.scale(proj.getProjectileSpec().getLength());
        Vector2f.add(v, proj.getLocation(), v);

        proj.getTailEnd().set(v);

        phase += 2f * Math.PI / (shotsPerCycle);

        hasntFiredDuration = 0f;
    }

    @Override
    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
        if (hasntFiredDuration >= 3f * weapon.getCooldown())  {
            phase = 0f;
        }

        hasntFiredDuration += amount;
    }

    @Override
    public void init(WeaponAPI weapon) {
        amplitude = weapon.getSpec().getMaxSpread() / 2f;
        if (weapon.getSlot().isHardpoint()) {
            amplitude /= 2f;
        }
    }
}
