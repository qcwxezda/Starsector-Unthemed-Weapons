package weaponexpansion.combat.scripts;

import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import org.lwjgl.util.vector.Vector2f;

@SuppressWarnings("unused")
public class SpikerEffect implements OnHitEffectPlugin {
    @Override
    public void onHit(DamagingProjectileAPI proj, CombatEntityAPI target, Vector2f pt, boolean shieldHit, ApplyDamageResultAPI damageResult, CombatEngineAPI engine) {
        if (shieldHit && target instanceof ShipAPI) {
            ShipAPI ship = (ShipAPI) target;
            engine.applyDamage(
                    target,
                    pt,
                    proj.getDamageAmount() * 0.5f,
                    DamageType.KINETIC,
                    0f,
                    false,
                    true,
                    proj.getSource()
            );
        }
    }
}
