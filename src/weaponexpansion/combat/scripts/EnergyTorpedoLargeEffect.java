package weaponexpansion.combat.scripts;

@SuppressWarnings("unused")
public class EnergyTorpedoLargeEffect extends EnergyTorpedoEffect {

    public EnergyTorpedoLargeEffect() {
        minSpawnDistance = 80f;
        maxSpawnDistance = 150f;
        minDelay = 1f;
        maxDelay = 2.5f;
        angleDeviation = 20f;
        empResistance = 100;
        particleScale = 24f;
        particlesPerSecond = 250;
    }
}
