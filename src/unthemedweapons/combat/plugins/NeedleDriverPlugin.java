package unthemedweapons.combat.plugins;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.util.Misc;
import org.lwjgl.util.vector.Vector2f;
import unthemedweapons.combat.scripts.NeedleDriverEffect;
import unthemedweapons.combat.scripts.NeedleDriverEffect.AttachData;

import java.awt.*;
import java.util.*;
import java.util.List;

@SuppressWarnings("unused")
public class NeedleDriverPlugin extends BaseEveryFrameCombatPlugin {
    static final float lingerTime = 15f;

    static final float maxExplosionRadius = 140f;
    static final float explosionRadiusPer = 7f;
    static final float maxExplosionDamage = 500;
    static final float explosionDamagePer = 25f;
    static final int maxAttachments = 20;

    @Override
    public void advance(float amount, List<InputEventAPI> events) {

        CombatEngineAPI engine = Global.getCombatEngine();
        if (engine == null || engine.isPaused()) return;

        //noinspection unchecked
        HashMap<ShipAPI, LinkedList<AttachData>> attachDataMap = (HashMap<ShipAPI, LinkedList<AttachData>>) engine.getCustomData().get(NeedleDriverEffect.attachDataKey);
        if (attachDataMap == null) return;

        Iterator<Map.Entry<ShipAPI, LinkedList<AttachData>>> itr = attachDataMap.entrySet().iterator();
        while (itr.hasNext()) {
            Map.Entry<ShipAPI, LinkedList<AttachData>> entry = itr.next();
            ShipAPI ship = entry.getKey();
            LinkedList<AttachData> attachments = entry.getValue();

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
                Vector2f newTailLoc = Misc.getDiff(newLoc, (Vector2f) Misc.getUnitVectorAtDegreeAngle(newFacing).scale(NeedleDriverEffect.attachedLength));
                data.proj.getTailEnd().set(newTailLoc);
            }

            if (!attachments.isEmpty() &&
                    (attachments.getFirst().proj.getElapsed() >= lingerTime ||
                            attachments.size() >= maxAttachments ||
                            ship.isPhased() ||
                            !ship.isAlive())) {

                int numAttached = attachments.size();

                for (NeedleDriverEffect.AttachData data : attachments) {
                    engine.removeEntity(data.proj);

                    float explosionDamage = Math.min(maxExplosionDamage, numAttached * explosionDamagePer);
                    float explosionRadius = Math.min(maxExplosionRadius, numAttached * explosionRadiusPer);

                    Vector2f lengthVec = Misc.getUnitVectorAtDegreeAngle(data.proj.getFacing());
                    lengthVec.scale(data.adjustAmount);
                    Vector2f explosionLoc = new Vector2f();
                    Vector2f.add(lengthVec, data.proj.getLocation(), explosionLoc);

                    engine.spawnExplosion(explosionLoc, ship.getVelocity(), new Color(255, 200, 255, 192), explosionRadius, 1f);
                    if (!ship.isPhased()) {
                        engine.applyDamage(data.proj, ship, explosionLoc, explosionDamage, DamageType.ENERGY, explosionDamage, true, false, data.proj.getSource(), true);
//                        Global.getSoundPlayer().playSound(
//                                explosionDamage < maxExplosionDamage ?
//                                        "hit_solid_energy" :
//                                        "hit_heavy_energy",
//                                0.9f + Misc.random.nextFloat() * 0.1f,
//                                1f,
//                                explosionLoc,
//                                ship.getVelocity()
//                        );
                    }
                }

                attachments.clear();
                itr.remove();
            }
        }
    }
}
