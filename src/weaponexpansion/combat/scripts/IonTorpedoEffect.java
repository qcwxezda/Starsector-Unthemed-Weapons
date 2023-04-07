package weaponexpansion.combat.scripts;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.Pair;
import org.lwjgl.util.vector.Vector2f;
import weaponexpansion.combat.plugins.Action;
import weaponexpansion.combat.plugins.ActionPlugin;
import weaponexpansion.combat.plugins.ParticleEngine;
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


    private static final float[] coreColorIn = new float[] {1f, 1f, 1f, 0.5f};
    private static final float[] coreColorOut = new float[] {-1f, -1f, -1f, -0.3f};
    private static final float[] colorIn = new float[] {0.588f, 1f, 0.902f, 0.098f};
    private static final float[] colorOut = new float[] {0f, -0.2f, -0.2f, 0f};
    private static final float[] ringColorIn = new float[] {0.784f, 1f, 0.902f, 1f};
    private static final float[] ringColorOut = new float[] {0.784f, 1f, 0.902f, 0f};

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
//        ParticleSystem part = new ParticleSystem(4f);
//        ParticlePosition pos = new ParticlePosition(new Vector2f(), new Vector2f(), new Vector2f(), 25f, 90f, 60f);
//        ParticleAngle angle = new ParticleAngle(0f, 0f, 0f, 360f, 30f, 45f);
//        ParticleSize size = new ParticleSize(250f, 250f, -250f, 150f, 100f, 100f);
//        part.addParticles(30, Global.getSettings().getSprite("misc", "__explosion1"), pos, angle, size, 3f, 1f,  colorIn, colorOut);
//        ParticlePosition ringPos = new ParticlePosition(new Vector2f(), new Vector2f(), new Vector2f(), 0f, 0f, 0f);
//        ParticleAngle ringAngle = new ParticleAngle(0f, 0f, 0f, 0f, 0f, 0f);
//        ParticleSize ringSize = new ParticleSize(200f, 550f, -370f, 250f, 50f, 50f);
//        part.addParticles(7, Global.getSettings().getSprite("misc", "wpnxt_explosion_ring"), ringPos, ringAngle, ringSize, 1.5f, 0.5f, ringColorIn, ringColorOut);
//        combatEngine.addLayeredRenderingPlugin(part).getLocation().set(pt);
        ParticleEngine.Cluster coreParticles = ParticleEngine.makeParticleCluster(50, Global.getSettings().getSprite("systemMap", "radar_entity"), 1f, 2f);
        coreParticles.setColorData(coreColorIn, coreColorOut, 0.01f);
        coreParticles.setPositionData(pt, new Vector2f(), new Vector2f());
        coreParticles.setSizeData(30f, 60f, 50f, 100f, -50f, -100f);
        coreParticles.setPositionSpreadData(30f, 20f, 20f);
        coreParticles.setRadialVelocity(-20f, -10f);
        coreParticles.setRadialAcceleration(10f, 20f);
        coreParticles.setAngleData(0f, 360f, -30f, 30f, -30f, 30f);
        coreParticles.generate();
        ParticleEngine.Cluster explosionParticles = ParticleEngine.makeParticleCluster(150, Global.getSettings().getSprite("systemMap", "radar_entity"), 2f, 3f);
        explosionParticles.setColorData(colorIn, colorOut, 0.01f);
        explosionParticles.setPositionData(pt, new Vector2f(), new Vector2f());
        explosionParticles.setSizeData(150f, 250f, 120f, 180f, -90f, -120f);
        explosionParticles.setPositionSpreadData(50f, 30f, 30f);
        explosionParticles.setRadialVelocity(-25f, 25f);
        explosionParticles.setRadialAcceleration(-25f, -25f);
        explosionParticles.setAngleData(0f, 360f, -50f, 50f, -50f, 50f);
        //explosionParticles.generate();
        ParticleEngine.Cluster ringParticles =  ParticleEngine.makeParticleCluster(10, Global.getSettings().getSprite("misc", "wpnxt_explosion_ring"), 1.5f, 2f);
        ringParticles.setColorData(ringColorIn, ringColorOut, 0.15f);
        ringParticles.setPositionData(pt, new Vector2f(), new Vector2f());
        ringParticles.setSizeData(100f, 200f, 400f, 500f, -350f, -250f);
        ringParticles.generate();
        ParticleEngine.Cluster test = ParticleEngine.makeParticleCluster(1000, Global.getSettings().getSprite("misc", "__explosion1"), 2f, 2.5f);
        test.setPositionData(pt, new Vector2f(), new Vector2f());
        test.setPositionSpreadData(100f, 20f, 20f);
        test.setSizeData(150f, 300f, 0, 0, 0, 0);
        test.setSinusoidalMotionX(0f, 200f, 1f, 2f, 0f, 360f);
        test.setSinusoidalMotionY(0f, 200f, 1f, 2f, 0f, 360f);
        test.setRadialRevolution(0f, 360f, -10f, 10f, 0f, 0f);
        test.setColorData(colorIn, colorOut, 0.1f);
        test.generate();


        for (int i = 0; i < 15; i++) {
            final Vector2f rand = Utils.randomPointInCircle(new Vector2f(), effectRadius);
            Vector2f.add(rand, pt, rand);
            ActionPlugin.queueAction(new Action() {
                @Override
                public void perform() {
                    combatEngine.spawnEmpArcVisual(
                            pt, proj, rand, null, 20f,
                            new Color(100, 200, 150, 255),
                            new Color(200, 255, 220, 255));
                }
            }, 0.05f * (i+1));
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
