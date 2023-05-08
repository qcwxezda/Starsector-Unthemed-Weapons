package weaponexpansion.combat.scripts;

import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.Pair;
import org.lwjgl.util.vector.Vector2f;
import particleengine.Emitter;
import particleengine.Particles;
import weaponexpansion.ModPlugin;
import weaponexpansion.particles.IonTorpedoExplosion;
import weaponexpansion.util.Utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@SuppressWarnings("unused")
public class IonTorpedoEffect implements OnHitEffectPlugin {

    private static final float effectRadius = 250f;
    private static final int maxTargets = 100;
    private static final float effectChance = 0.5f;
    private static final float energyDamage = 750f;

    @Override
    public void onHit(final DamagingProjectileAPI proj, CombatEntityAPI target, final Vector2f pt, boolean shieldHit, ApplyDamageResultAPI damageResult, final CombatEngineAPI combatEngine)  {
        Collection<CombatEntityAPI> targets = Utils.getKNearestEntities(
                maxTargets,
                pt,
                null,
                true,
                effectRadius,
                true,
                new Utils.CommonChecker(proj)
        );

        if (ModPlugin.particleEngineEnabled) {
            Particles.burst(IonTorpedoExplosion.core(pt), 100);

            Emitter ringEmitter = IonTorpedoExplosion.ring(pt, proj.getFacing());
            Particles.burst(ringEmitter, 5);
            ringEmitter.facing(35f, 55f);
            Particles.burst(ringEmitter, 5);

            Particles.stream(IonTorpedoExplosion.empArcs(pt), 1, 15, 1f);
        }

        for (CombatEntityAPI tgt : targets) {
            // Boolean term is true if the weapon or engine is already disabled
            List<Pair<Vector2f, Boolean>> damageLocs = new ArrayList<>();

            if (tgt instanceof ShipAPI) {
                ShipAPI ship = (ShipAPI) tgt;
                ShipEngineControllerAPI engineController = ship.getEngineController();
                // Apply EMP damage to each engine if applicable
                if (engineController != null) {
                    for (ShipEngineControllerAPI.ShipEngineAPI engine : engineController.getShipEngines()) {
                        if (Misc.random.nextFloat() <= effectChance) {
                            damageLocs.add(new Pair<>(engine.getLocation(), engine.isDisabled()));
                        }
                    }
                }
                // Apply EMP damage to each weapon if applicable
                for (WeaponAPI weapon : ship.getAllWeapons()) {
                    if (Misc.random.nextFloat() <= effectChance) {
                        damageLocs.add(new Pair<>(weapon.getLocation(), weapon.isDisabled()));
                    }
                }
            }

            if (tgt instanceof MissileAPI) {
                MissileAPI missile = (MissileAPI) tgt;
                ShipEngineControllerAPI engineController = missile.getEngineController();
                if (engineController != null) {
                    damageLocs.add(new Pair<>(missile.getLocation(), engineController.isFlamedOut()));
                }
            }

            for (Pair<Vector2f, Boolean> pair : damageLocs) {
                Vector2f loc = pair.one;
                boolean wasDisabled = pair.two;
                // pt is inside shield, proj.getLocation() is outside
                if (Misc.getDistance(pt, loc) <= effectRadius && !Utils.doesSegmentHitShield(proj.getLocation(), loc, tgt.getShield())) {
                    combatEngine.applyDamage(
                            tgt,
                            loc,
                            wasDisabled ? energyDamage : 0f,
                            DamageType.ENERGY,
                            proj.getEmpAmount(),
                            false,
                            false,
                            proj.getSource(),
                            false
                    );
                }
            }
        }
    }
}
