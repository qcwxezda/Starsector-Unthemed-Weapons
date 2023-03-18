package weaponexpansion.combat.scripts;

import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.util.Misc;
import weaponexpansion.util.InstantaneousBurst;

import java.util.ArrayList;
import java.util.List;

public class BoltScramblerEffect extends InstantaneousBurst {
    @Override
    public String getSoundId() {
        return "wpnxt_boltshotgun_fire";
    }

    @Override
    public List<Float> getAngleOffsets(WeaponAPI weapon) {
        List<Float> offsets = new ArrayList<>();
        float spread = weapon.getCurrSpread();
        for (int i = 0; i < weapon.getSpec().getBurstSize(); i++) {
            offsets.add(Misc.random.nextFloat() * spread - spread / 2);
        }
        return offsets;
    }

    @Override
    public float getSpeedVariance() {
        return 0.15f;
    }

    @Override
    public float getDamageVariance() {
        return 0.15f;
    }
}
