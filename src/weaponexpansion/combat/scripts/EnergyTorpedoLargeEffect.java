package weaponexpansion.combat.scripts;

@SuppressWarnings("unused")
public class EnergyTorpedoLargeEffect extends EnergyTorpedoEffect {

    public EnergyTorpedoLargeEffect() {
        numSpawns = 5;
        minSpawnDistance = 80f;
        maxSpawnDistance = 150f;
        minDelay = 1f;
        maxDelay = 2.5f;
        angleDeviation = 20f;
        empResistance = 100;
        particleScale = 10f;
        particlesPerSecond = 200;
    }
}
