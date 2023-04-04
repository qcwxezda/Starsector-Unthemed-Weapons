package weaponexpansion.combat.plugins;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.loading.DamagingExplosionSpec;
import com.fs.starfarer.api.util.Misc;
import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;
import weaponexpansion.util.Utils;

@SuppressWarnings("unused")
public class ProximityMineRandomDelay implements ProximityFuseAIAPI, MissileAIPlugin {

    private final MissileAPI missile;
    private boolean slowToMaxSpeed;
    private float range;
    private float vsMissileRange;
    private final float flightTimeSpread;
    private DamagingExplosionSpec explosionSpec;
    private static final Logger log = Logger.getLogger(ProximityMineRandomDelay.class);

    // On creation missile damage is 0, so wait a frame for the damage numbers to register
    private boolean updatedDamage = false;
    private final Utils.TargetChecker proxChecker;

    public ProximityMineRandomDelay(MissileAPI missile, float flightTimeSpread) {
        this.missile = missile;
        this.flightTimeSpread = flightTimeSpread;
        proxChecker = new Utils.CommonChecker(missile);
    }

    @Override
    public void advance(float amount) {
        if (!updatedDamage) {
            updateDamage();
            missile.setFacing(Misc.getAngleInDegrees(missile.getVelocity()));
            updatedDamage = true;
        }

        if (!missile.didDamage() && (missile.isFading()
                || Utils.isEntityNearby(missile.getLocation(), null, range, vsMissileRange, true, proxChecker))) {
            explode();
        }

        if (missile.getVelocity().length() > missile.getMaxSpeed() && slowToMaxSpeed) {
            missile.giveCommand(ShipCommand.DECELERATE);
        }
        else {
            missile.giveCommand(ShipCommand.ACCELERATE);
        }
    }

    @Override
    public void updateDamage() {
        JSONObject spec = missile.getBehaviorSpecParams();
        slowToMaxSpeed = spec.optBoolean("slowToMaxSpeed", false);
        range = (float)spec.optDouble("range", 0f);
        vsMissileRange = (float)spec.optDouble("vsMissileRange", range);
        missile.setMaxFlightTime((float)spec.optDouble("delay", 0f) * Utils.randBetween(1f - flightTimeSpread, 1f + flightTimeSpread));
        if (spec.has("explosionSpec")) {
            try {
                explosionSpec = DamagingExplosionSpec.loadFromJSON(spec.getJSONObject("explosionSpec"));
                explosionSpec.setDamageType(missile.getDamage().getType());
                explosionSpec.setMaxDamage(missile.getDamage().getBaseDamage());
                explosionSpec.setMinDamage(missile.getDamage().getBaseDamage() / 2.0F);
                explosionSpec.setEffect(missile.getSpec().getOnHitEffect());
            } catch (JSONException e) {
                log.error(e, e);
            }
        }
    }


    public void explode() {
        if (explosionSpec == null) return;

        CombatEngineAPI engine = Global.getCombatEngine();
        engine.removeEntity(missile);
        engine.spawnDamagingExplosion(explosionSpec, missile.getSource(), missile.getLocation());
    }
}
