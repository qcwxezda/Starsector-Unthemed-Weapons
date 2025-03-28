package unthemedweapons.combat.scripts;

import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.Pair;
import org.lwjgl.util.vector.Vector2f;
import unthemedweapons.ModPlugin;
import unthemedweapons.fx.particles.IonTorpedoExplosion;
import unthemedweapons.util.CollisionUtils;
import unthemedweapons.util.EngineUtils;
import unthemedweapons.util.TargetChecker;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

@SuppressWarnings("unused")
public class IonTorpedoEffect implements OnHitEffectPlugin {

    private static final float effectRadius = 250f;
    private static final int maxTargets = 100;
    private static final float effectChance = 0.5f;
    private static final float energyDamageSmall = 200f, energyDamageMedium = 400f, energyDamageLarge = 800f, energyDamageEngine = 400f;
    private static final float maxTotalDamage = 5000f;

    private enum DisabledType {
        NONE,
        SMALL,
        MEDIUM,
        LARGE,
        ENGINE;

        private float getDamage() {
            return switch (this) {
                case SMALL -> energyDamageSmall;
                case MEDIUM -> energyDamageMedium;
                case LARGE -> energyDamageLarge;
                case ENGINE -> energyDamageEngine;
                default -> 0f;
            };
        }
    }

    @Override
    public void onHit(final DamagingProjectileAPI proj, CombatEntityAPI target, final Vector2f pt, boolean shieldHit, ApplyDamageResultAPI damageResult, final CombatEngineAPI combatEngine)  {
        Collection<CombatEntityAPI> targets = EngineUtils.getKNearestEntities(
                maxTargets,
                pt,
                null,
                true,
                effectRadius,
                true,
                new TargetChecker.CommonChecker(proj)
        );

        if (ModPlugin.particleEngineEnabled) {
            IonTorpedoExplosion.makeExplosion(pt, proj.getFacing());
        }

        for (CombatEntityAPI tgt : targets) {
            // Boolean term is true if the weapon or engine is already disabled
            List<Pair<Vector2f, DisabledType>> damageLocs = new ArrayList<>();

            if (tgt instanceof ShipAPI ship) {
                ShipEngineControllerAPI engineController = ship.getEngineController();
                // Apply EMP damage to each engine if applicable
                if (engineController != null) {
                    for (ShipEngineControllerAPI.ShipEngineAPI engine : engineController.getShipEngines()) {
                        if (Misc.random.nextFloat() <= effectChance) {
                            damageLocs.add(new Pair<>(engine.getLocation(), engine.isDisabled() ? DisabledType.ENGINE : DisabledType.NONE));
                        }
                    }
                }

                // Apply EMP damage to each weapon if applicable
                for (WeaponAPI weapon : ship.getAllWeapons()) {
                    if (weapon.isDecorative() || weapon.getSlot() == null) continue;
                    DisabledType type = DisabledType.NONE;
                    if (Misc.random.nextFloat() <= effectChance) {
                        if (weapon.isDisabled()) {
                            type = switch (weapon.getSlot().getSlotSize()) {
                                case SMALL -> DisabledType.SMALL;
                                case MEDIUM -> DisabledType.MEDIUM;
                                case LARGE -> DisabledType.LARGE;
                            };
                        }
                    }
                    damageLocs.add(new Pair<>(weapon.getLocation(), type));
                }
            }

            Collections.shuffle(damageLocs);

            if (tgt instanceof MissileAPI missile) {
                ShipEngineControllerAPI engineController = missile.getEngineController();
                if (engineController != null) {
                    damageLocs.add(new Pair<>(missile.getLocation(), engineController.isFlamedOut() ? DisabledType.ENGINE : DisabledType.NONE));
                }
            }

            float totalDamage = 0f;
            for (Pair<Vector2f, DisabledType> pair : damageLocs) {
                Vector2f loc = pair.one;
                // pt is inside shield, proj.getLocation() is outside
                if (Misc.getDistance(pt, loc) <= effectRadius && CollisionUtils.rayCollisionCheckShield(proj.getLocation(), loc, tgt.getShield()) == null) {
                    combatEngine.applyDamage(
                            proj,
                            tgt,
                            loc,
                            pair.two.getDamage(),
                            DamageType.ENERGY,
                            proj.getEmpAmount(),
                            false,
                            false,
                            proj.getSource(),
                            false
                    );
                }
                totalDamage += pair.two.getDamage();
                if (totalDamage >= maxTotalDamage) break;
            }
        }
    }
}
