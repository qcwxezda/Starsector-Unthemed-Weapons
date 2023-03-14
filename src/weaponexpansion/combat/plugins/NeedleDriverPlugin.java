package weaponexpansion.combat.plugins;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.loading.DamagingExplosionSpec;
import com.fs.starfarer.api.util.Misc;
import org.lwjgl.util.vector.Vector2f;
import weaponexpansion.combat.scripts.NeedleDriverEffect;
import weaponexpansion.combat.scripts.NeedleDriverEffect.AttachData;

import java.awt.*;
import java.util.LinkedList;
import java.util.List;

@SuppressWarnings("unused")
public class NeedleDriverPlugin extends BaseEveryFrameCombatPlugin {
    static final float lingerTime = 12f;

    static final float maxExplosionRadius = 100f;
    static final float explosionRadiusPer = 10f;
    static final float maxExplosionDamage = 500f;
    static final float explosionDamagePer = 50f;
    @Override
    public void advance(float amount, List<InputEventAPI> events) {

        CombatEngineAPI engine = Global.getCombatEngine();
        if (engine.isPaused()) return;

        for (ShipAPI ship : engine.getShips()) {
            //noinspection unchecked
            LinkedList<AttachData> attachments = (LinkedList<AttachData>) ship.getCustomData().get(NeedleDriverEffect.attachDataKey);

            if (attachments == null) {
                continue;
            }

            for (NeedleDriverEffect.AttachData data : attachments) {
                Vector2f newLoc = new Vector2f();
                Vector2f.add(data.offset, ship.getLocation(), newLoc);
                newLoc = Misc.rotateAroundOrigin(newLoc, ship.getFacing() - data.initialShipAngle, ship.getLocation());
                data.proj.getLocation().set(newLoc);
                float newFacing = data.facing + ship.getFacing() - data.initialShipAngle;
                data.proj.setFacing(newFacing);
                Vector2f newTailLoc = Misc.getDiff(newLoc, (Vector2f) Misc.getUnitVectorAtDegreeAngle(newFacing).scale(data.proj.getProjectileSpec().getLength()));
                data.proj.getTailEnd().set(newTailLoc);
            }

            if (!attachments.isEmpty() && (attachments.getFirst().proj.getElapsed() >= lingerTime || ship.isPhased() || !ship.isAlive())) {

                int numAttached = attachments.size();

                for (NeedleDriverEffect.AttachData data : attachments) {
                    engine.removeEntity(data.proj);

                    float explosionDamage = Math.min(maxExplosionDamage, numAttached * explosionDamagePer);
                    float explosionRadius = Math.min(maxExplosionRadius, numAttached * explosionRadiusPer);
                    DamagingExplosionSpec spec = new DamagingExplosionSpec(
                            0.5f,
                            explosionRadius,
                            explosionRadius / 2,
                            explosionDamage,
                            explosionDamage / 2,
                            CollisionClass.PROJECTILE_FF,
                            CollisionClass.PROJECTILE_FIGHTER,
                            1f,
                            4f,
                            1f,
                            50,
                            new Color(255, 200, 255, 192),
                            new Color(255, 75, 255, 64)
                    );

                    spec.setDamageType(DamageType.ENERGY);
                    spec.setDetailedExplosionRadius(explosionRadius * 1.5f);
                    spec.setDetailedExplosionFlashRadius(explosionRadius * 1.5f);

                    Vector2f lengthVec = Misc.getUnitVectorAtDegreeAngle(data.proj.getFacing());
                    lengthVec.scale(data.proj.getProjectileSpec().getLength() / 2);
                    Vector2f explosionLoc = new Vector2f();
                    Vector2f.add(lengthVec, data.proj.getLocation(), explosionLoc);
                    engine.spawnDamagingExplosion(
                            spec,
                            data.proj.getSource(),
                            explosionLoc
                    );
                }

                attachments.clear();
            }
        }
    }
}
