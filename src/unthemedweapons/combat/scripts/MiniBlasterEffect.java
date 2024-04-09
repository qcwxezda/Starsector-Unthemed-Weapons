package unthemedweapons.combat.scripts;

import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import com.fs.starfarer.api.impl.combat.BreachOnHitEffect;
import org.lwjgl.util.vector.Vector2f;

@SuppressWarnings("unused")
public class MiniBlasterEffect implements OnHitEffectPlugin {
    public static final float ARMOR_DAMAGE_AMOUNT = 50f;

    @Override
    public void onHit(DamagingProjectileAPI proj, CombatEntityAPI target, Vector2f pt,
                      boolean shieldHit, ApplyDamageResultAPI result, CombatEngineAPI engine) {
        if (!(target instanceof ShipAPI) || shieldHit) return;
        BreachOnHitEffect.dealArmorDamage(proj, (ShipAPI) target, pt, ARMOR_DAMAGE_AMOUNT);
    }
}
