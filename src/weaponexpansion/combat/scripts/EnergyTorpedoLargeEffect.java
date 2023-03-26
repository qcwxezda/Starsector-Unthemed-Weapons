package weaponexpansion.combat.scripts;

@SuppressWarnings("unused")
public class EnergyTorpedoLargeEffect extends EnergyTorpedoEffect {

    public EnergyTorpedoLargeEffect() {
        numSpawns = 8;
        minSpawnDistance = 60f;
        maxSpawnDistance = 150f;
        minDelay = 1f;
        maxDelay = 1.5f;
        angleDeviation = 15f;
    }
}
