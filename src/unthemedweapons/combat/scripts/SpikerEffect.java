package unthemedweapons.combat.scripts;

import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import com.fs.starfarer.api.combat.listeners.DamageDealtModifier;
import org.lwjgl.util.vector.Vector2f;

@SuppressWarnings("unused")
public class SpikerEffect implements OnFireEffectPlugin, OnHitEffectPlugin, DamageDealtModifier {
    private String weaponId = null;
    private static final String modifyKey = "wpnxt_spiker";

    @Override
    public void onHit(DamagingProjectileAPI proj, CombatEntityAPI target, Vector2f pt, boolean shieldHit, ApplyDamageResultAPI damageResult, CombatEngineAPI engine) {
        if (shieldHit && target instanceof ShipAPI) {
            ShipAPI ship = (ShipAPI) target;
            // modifyDamageDealt happens before onHit, therefore
            // proj damage is the base damage and not twice the base damage
            engine.applyDamage(
                    proj,
                    target,
                    pt,
                    proj.getDamageAmount(),
                    DamageType.KINETIC,
                    0f,
                    false,
                    true,
                    proj.getSource(),
                    false
            );
        }
    }

    @Override
    public void onFire(DamagingProjectileAPI proj, WeaponAPI weapon, CombatEngineAPI engine) {
        // Double the damage so ships see the full damage and don't overload on the soft flux damage
        proj.setDamageAmount(proj.getBaseDamageAmount() * 2f);
        ShipAPI ship = weapon.getShip();
        weaponId = weapon.getId();
        if (!ship.hasListenerOfClass(SpikerEffect.class)) {
            ship.addListener(this);
        }
    }

    @Override
    public String modifyDamageDealt(Object param, CombatEntityAPI target, DamageAPI damage, Vector2f pt, boolean shieldHit) {
        if (shieldHit && param instanceof MissileAPI) {
            MissileAPI missile = (MissileAPI) param;
            if (missile.getWeaponSpec() != null && missile.getWeaponSpec().getWeaponId().equals(weaponId)) {
                // Halve the damage and apply the other half as soft flux
                damage.setDamage(0.5f * damage.getBaseDamage());
                return modifyKey;
            }
        }
        return null;
    }
}
