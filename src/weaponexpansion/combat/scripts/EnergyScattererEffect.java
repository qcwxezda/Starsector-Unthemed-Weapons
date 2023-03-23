package weaponexpansion.combat.scripts;

import weaponexpansion.util.ScatterPlugin;

@SuppressWarnings("unused")
public class EnergyScattererEffect extends ScatterPlugin {
    @Override
    public float getSpeedVariance() {
        return 0.15f;
    }

    @Override
    public float getDamageVariance() {
        return 0.15f;
    }

    @Override
    public boolean isUsingExtraBarrels() {
        return false;
    }
}
