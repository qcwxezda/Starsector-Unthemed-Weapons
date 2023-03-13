package weaponexpansion.combat.scripts;

import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import com.fs.starfarer.api.loading.ProjectileSpecAPI;
import org.lwjgl.util.vector.Vector2f;

import java.util.ArrayList;
import java.util.List;

/** Limited ability to pierce through missiles */
public class SmallPlasmaOnHitEffect implements OnHitEffectPlugin {

    @Override
    public void onHit(DamagingProjectileAPI proj, CombatEntityAPI target, Vector2f pt, boolean shieldHit, ApplyDamageResultAPI damageResult, CombatEngineAPI engine) {
        // If the projectile is fading out (past max range), it can't pierce
        if (proj.isFading()) {
            return;
        }

        // Can only pierce missiles
        List<CollisionClass> canPierce = new ArrayList<>();
        canPierce.add(CollisionClass.MISSILE_FF);
        canPierce.add(CollisionClass.MISSILE_NO_FF);
        if (!canPierce.contains(target.getCollisionClass())) {
            return;
        }

        // Can only pierce missiles with less HP than its damage
        // target.hitpoints is the hitpoints after the damage has already been dealt
        if (target.getHitpoints() > 0) {
            return;
        }
        // TODO: Any way to use target's current hitpoints (before damage applied) instead?
        float newDamage = proj.getDamageAmount() - target.getMaxHitpoints();

        //If the damage of the new projectile would  be less than 25% of its original damage, destroy it
        if (newDamage < 0.25f * proj.getWeapon().getDamage().getDamage()) {
            return;
        }

        WeaponAPI weapon = proj.getWeapon();
        Float oldRange = (Float) proj.getCustomData().get("modifiedRange");
        if (oldRange == null) {
            oldRange = weapon.getRange();
        }
        float maxFlightTime = oldRange / weapon.getProjectileSpeed();
        float remainingRange = oldRange * (1 - proj.getElapsed() / maxFlightTime);

        StatBonus rangeBonus = proj.getSource().getMutableStats().getEnergyWeaponRangeBonus();
        float unmodifiedRemainingRange = ((remainingRange / rangeBonus.mult) - rangeBonus.flatBonus) / (1 + rangeBonus.percentMod / 100f);

        // Can't set projectile range or flight time directly -- have to temporarily modify the projectile spec
        ProjectileSpecAPI spec = proj.getProjectileSpec();
        float origRange = spec.getMaxRange();
        spec.setMaxRange(unmodifiedRemainingRange);
        float origWidth = spec.getWidth();
        spec.setWidth(origWidth * newDamage / proj.getWeapon().getDamage().getDamage());
        //TODO: test that the correct velocity is preserved
        Vector2f facingVector = new Vector2f();
        proj.getVelocity().normalise(facingVector);
        Vector2f velDiff = new Vector2f();
        Vector2f.sub(proj.getVelocity(), (Vector2f) facingVector.scale(proj.getWeapon().getProjectileSpeed()), velDiff);
        DamagingProjectileAPI newProj = (DamagingProjectileAPI)
                engine.spawnProjectile(proj.getSource(), proj.getWeapon(), proj.getWeapon().getId(), pt, proj.getFacing(), velDiff);
        newProj.setCustomData("modifiedRange", rangeBonus.computeEffective(unmodifiedRemainingRange));
        newProj.setDamageAmount(newDamage);
        spec.setMaxRange(origRange);
        spec.setWidth(origWidth);
    }
}