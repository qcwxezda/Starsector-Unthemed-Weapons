package weaponexpansion.combat.scripts;

import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import com.fs.starfarer.api.loading.ProjectileSpecAPI;
import com.fs.starfarer.api.util.Misc;
import org.lwjgl.util.vector.Vector2f;

import java.util.LinkedList;

public class NeedleDriverEffect implements OnHitEffectPlugin  {

    public static final String attachDataKey = "wpnxt_needleDriverData";

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
        spec.setMaxRange(100000f);
        Vector2f scaledVelocity = new Vector2f(proj.getVelocity());
        scaledVelocity.scale(0.8f * engine.getElapsedInLastFrame());
        Vector2f spawnLocation = Misc.getDiff(pt, scaledVelocity);
        CombatEntityAPI spawn = engine.spawnProjectile(proj.getSource(), proj.getWeapon(), proj.getWeapon().getId(), spawnLocation, proj.getFacing(), new Vector2f());
        spec.setMaxRange(maxRange);
        spawn.setCollisionClass(CollisionClass.NONE);
        ((DamagingProjectileAPI) spawn).setFromMissile(true);
        Vector2f offset = Misc.getDiff(spawnLocation, ship.getLocation());

        if (!ship.getCustomData().containsKey(attachDataKey)) {
            ship.setCustomData(attachDataKey, new LinkedList<AttachData>());
        }

        //noinspection unchecked
        ((LinkedList<AttachData>) (ship.getCustomData().get(attachDataKey)))
                .add(new AttachData((DamagingProjectileAPI) spawn, offset, proj.getFacing(), ship.getFacing()));
    }

    public static class AttachData {
        public DamagingProjectileAPI proj;
        public Vector2f offset;
        public float facing;
        public float initialShipAngle;

        AttachData(DamagingProjectileAPI proj, Vector2f offset, float facing, float initialShipAngle) {
            this.proj = proj;
            this.offset = offset;
            this.facing = facing;
            this.initialShipAngle = initialShipAngle;
        }
    }
}
