package weaponexpansion.combat.scripts;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.Pair;
import org.lwjgl.util.vector.Vector2f;
import weaponexpansion.combat.plugins.Action;
import weaponexpansion.combat.plugins.ActionPlugin;
import weaponexpansion.util.particles.ParticleAngle;
import weaponexpansion.util.particles.ParticlePosition;
import weaponexpansion.util.particles.ParticleSize;
import weaponexpansion.util.particles.ParticleSystem;
import weaponexpansion.util.Utils;

import java.awt.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@SuppressWarnings("unused")
public class IonTorpedoEffect implements OnHitEffectPlugin {

    private static final float effectRadius = 250f;
    private static final int maxTargets = 100;
    private static final float effectChance = 0.5f;
    private static final float energyDamage = 750f;

    private static final Color colorIn = new Color(150, 255, 230, 35);
    private static final Color colorOut = new Color(0, 0, 0, 0);
    private static final Color ringColorIn = new Color(200, 255, 230, 255);
    private static final Color ringColorOut = new Color(200, 255, 230, 0);

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

//        combatEngine.addLayeredRenderingPlugin(new ExplosionRenderer(0.5f)).getLocation().set(pt);
        ParticleSystem part = new ParticleSystem(4f);
        ParticlePosition pos = new ParticlePosition(new Vector2f(), new Vector2f(), new Vector2f(), 25f, 90f, 60f);
        ParticleAngle angle = new ParticleAngle(0f, 0f, 0f, 360f, 30f, 45f);
        ParticleSize size = new ParticleSize(250f, 250f, -250f, 150f, 100f, 100f);
        part.addParticles(30, Global.getSettings().getSprite("misc", "__explosion1"), pos, angle, size, 3f, 1f,  colorIn, colorOut);
        ParticlePosition ringPos = new ParticlePosition(new Vector2f(), new Vector2f(), new Vector2f(), 0f, 0f, 0f);
        ParticleAngle ringAngle = new ParticleAngle(0f, 0f, 0f, 0f, 0f, 0f);
        ParticleSize ringSize = new ParticleSize(200f, 550f, -370f, 250f, 50f, 50f);
        part.addParticles(7, Global.getSettings().getSprite("misc", "wpnxt_explosion_ring"), ringPos, ringAngle, ringSize, 1.5f, 0.5f, ringColorIn, ringColorOut);
        combatEngine.addLayeredRenderingPlugin(part).getLocation().set(pt);

        ActionPlugin plugin = (ActionPlugin) combatEngine.getCustomData().get(ActionPlugin.customDataKey);
        for (int i = 0; i < 15; i++) {
            final Vector2f rand = Utils.randomPointInCircle(new Vector2f(), effectRadius);
            Vector2f.add(rand, pt, rand);
            plugin.queueAction(new Action() {
                @Override
                public void perform() {
                    combatEngine.spawnEmpArcVisual(
                            pt, proj, rand, null, 20f,
                            new Color(100, 200, 150, 255),
                            new Color(200, 255, 220, 255));
                }
            }, 0.1f * (i+1));
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
