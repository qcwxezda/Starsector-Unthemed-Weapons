package unthemedweapons.combat.scripts;

import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.DamagingProjectileAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.combat.WeaponEffectPluginWithInit;

@SuppressWarnings("unused")
public class ShotgunEffect extends ScatterEffect implements WeaponEffectPluginWithInit {

    private float spread = 0f, maxSpread = 0f, minSpread = 0f;
    private static final float spreadIncreasePerSecond = 4f;
    private static final float spreadDecreasePerShot = 5f;

    @Override
    public float getSpeedVariance() {
        return 0.25f;
    }

    @Override
    public float getDamageVariance() {
        return 0.25f;
    }

    /** Use extra barrels instead of a fake burst because the barrel recoil doesn't work otherwise */
    @Override
    public boolean isUsingExtraBarrels() {
        return true;
    }

    @Override
    public void onFire(DamagingProjectileAPI proj, WeaponAPI weapon, CombatEngineAPI engine) {
        super.onFire(proj, weapon, engine);

        spread -= spreadDecreasePerShot;
        spread = Math.max(minSpread, spread);
    }

    @Override
    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
        if (!weapon.isFiring()) {
            spread += spreadIncreasePerSecond * amount;
            spread = Math.min(spread, maxSpread);
        }
    }

    @Override
    public void init(WeaponAPI weapon) {
        maxSpread = weapon.getSpec().getMaxSpread();
        spread = maxSpread;
        minSpread = maxSpread / 2f;
    }

    @Override
    public float getSpread(WeaponAPI weapon) {
        return spread;
    }
}
