package unthemedweapons.combat.scripts;

import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import com.fs.starfarer.api.loading.ProjectileSpecAPI;
import com.fs.starfarer.api.util.Misc;
import org.lwjgl.util.vector.Vector2f;
import unthemedweapons.util.MathUtils;

import java.util.HashMap;
import java.util.LinkedList;

public class NeedleDriverEffect implements OnHitEffectPlugin  {

    public static final String attachDataKey = "wpnxt_needleDriverData";
    public static final float attachedLength = 40f;

    @Override
    public void onHit(DamagingProjectileAPI proj, CombatEntityAPI target, Vector2f pt, boolean shieldHit, ApplyDamageResultAPI damageResult, CombatEngineAPI engine) {

        // Only care about ships, must be full damage shot
        if (proj.isFading() || !(target instanceof ShipAPI)) {
            return;
        }

        // Don't attach to fighters
        ShipAPI ship = (ShipAPI) target;
        if (ship.isFighter() || !ship.isAlive()) {
            return;
        }

        // Ignore shield hits
        if (damageResult.getDamageToHull() <= 0f && damageResult.getDamageToPrimaryArmorCell() <= 0) {
            return;
        }

        ProjectileSpecAPI spec = proj.getProjectileSpec();
        float maxRange = spec.getMaxRange();
        float glowRadius = spec.getGlowRadius();
        float length = spec.getLength();
        spec.setMaxRange(100000f);
        spec.setGlowRadius(0f);
        spec.setLength(attachedLength);
        Vector2f scaledVelocity = new Vector2f(proj.getVelocity());
        MathUtils.safeNormalize(scaledVelocity);
        scaledVelocity.scale(0.3f * attachedLength);
        float adjustAmount = attachedLength * 0.3f;
        Vector2f spawnLocation = Misc.getDiff(pt, scaledVelocity);
        DamagingProjectileAPI spawn = (DamagingProjectileAPI) engine.spawnProjectile(proj.getSource(), proj.getWeapon(), proj.getWeapon().getId(), spawnLocation, proj.getFacing(), new Vector2f());
        spec.setMaxRange(maxRange);
        spec.setGlowRadius(glowRadius);
        spec.setLength(length);
        spawn.setCollisionClass(CollisionClass.NONE);
        spawn.setDamageAmount(0f);
        spawn.setFromMissile(true);
        Vector2f offset = Misc.getDiff(spawnLocation, ship.getLocation());

        if (!engine.getCustomData().containsKey(attachDataKey)) {
            engine.getCustomData().put(attachDataKey, new HashMap<>());
        }

        //noinspection unchecked
        HashMap<ShipAPI, LinkedList<AttachData>> attachDataMap = (HashMap<ShipAPI, LinkedList<AttachData>>) engine.getCustomData().get(attachDataKey);
        if (!attachDataMap.containsKey(ship)) {
            attachDataMap.put(ship, new LinkedList<AttachData>());
        }

        attachDataMap.get(ship).add(new AttachData(spawn, offset, adjustAmount, proj.getFacing(), ship.getFacing()));
    }

    public static class AttachData {
        public DamagingProjectileAPI proj;
        public Vector2f offset;
        public float facing;
        public float initialShipAngle;

        // Move the projectile a bit in the opposite direction so that it doesn't appear
        // completely embedded in the ship; this amount is recorded so that the original
        // hit location (known to be in bounds of the target ship) can be recomputed
        public float adjustAmount;

        AttachData(DamagingProjectileAPI proj, Vector2f offset, float adjustAmount, float facing, float initialShipAngle) {
            this.proj = proj;
            this.offset = offset;
            this.facing = facing;
            this.initialShipAngle = initialShipAngle;
            this.adjustAmount = adjustAmount;
        }
    }
}
