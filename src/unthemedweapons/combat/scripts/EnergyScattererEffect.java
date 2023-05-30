package unthemedweapons.combat.scripts;

@SuppressWarnings("unused")
public class EnergyScattererEffect extends ScatterEffect {
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
