package weaponexpansion.combat.scripts;

import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import org.lwjgl.util.vector.Vector2f;
import weaponexpansion.combat.plugins.ProximityMineRandomDelay;

@SuppressWarnings("unused")
public class ClusterMineOnHitEffect implements OnHitEffectPlugin {
    @Override
    public void onHit(DamagingProjectileAPI proj, CombatEntityAPI entity, Vector2f pt, boolean shieldHit, ApplyDamageResultAPI result, CombatEngineAPI engine) {
        if (!(proj instanceof MissileAPI)) return;
        MissileAPI missile = (MissileAPI) proj;
        MissileAIPlugin ai = missile.getUnwrappedMissileAI();
        if (!(ai instanceof ProximityMineRandomDelay)) return;
        DamagingProjectileAPI explosion = ((ProximityMineRandomDelay) ai).explode();
        if (explosion != null) {
            explosion.addDamagedAlready(entity);
        }
    }
}
