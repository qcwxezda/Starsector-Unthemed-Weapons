package unthemedweapons.combat.scripts;

@SuppressWarnings("unused")
public class EnergyTorpedoLargeEffect extends EnergyTorpedoEffect {

    public EnergyTorpedoLargeEffect() {
        minSpawnDistance = 80f;
        maxSpawnDistance = 150f;
        minDelay = 0.8f;
        maxDelay = 1.6f;
        numSpawns = 6;
        angleDeviation = 20f;
        empResistance = 100;
        particleScale = 16f;
        particlesPerSecond = 150;
    }
}
