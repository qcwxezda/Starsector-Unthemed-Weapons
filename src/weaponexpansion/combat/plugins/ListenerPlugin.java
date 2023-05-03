package weaponexpansion.combat.plugins;

import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.DamageTakenModifier;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.util.IntervalUtil;
import org.lwjgl.util.vector.Vector2f;

import java.util.List;

@SuppressWarnings("unused")
public class ListenerPlugin extends BaseEveryFrameCombatPlugin {

    public static String minispikerKey = "wpnxt_minispiker_damage_mod";
    private final IntervalUtil updateShipsInterval = new IntervalUtil(3f, 3f);
    private CombatEngineAPI engine;

    @Override
    public void advance(float amount, List<InputEventAPI> events) {
        if (engine == null) return;
        updateShipsInterval.advance(amount);
        if (updateShipsInterval.intervalElapsed()) {
            for (ShipAPI ship : engine.getShips()) {
                if (!(ship.hasListenerOfClass(TargetDamageModifier.class))) {
                    ship.addListener(new TargetDamageModifier());
                }
            }
        }
    }

    @Override
    public void init(CombatEngineAPI engine) {
        this.engine = engine;
        for (ShipAPI ship : engine.getShips()) {
            if (!(ship.hasListenerOfClass(TargetDamageModifier.class))) {
                ship.addListener(new TargetDamageModifier());
            }
        }
    }

    public static class TargetDamageModifier implements DamageTakenModifier {
        @Override
        public String modifyDamageTaken(Object o, CombatEntityAPI entity, DamageAPI damage, Vector2f pt, boolean shieldHit) {
            if (o instanceof MissileAPI) {
                MissileAPI missile = (MissileAPI) o;
                if ("wpnxt_minispiker_shot".equals(missile.getProjectileSpecId()) && shieldHit) {
                    // Minispiker effect: half of damage is soft flux. Reduce damage dealt by half; the other half
                    // is scripted into onHit. Note that this is better than setting projectile damage to half on fire
                    // as ships may overload thinking they can take the smaller damage on shields.
                    damage.getModifier().modifyMult(minispikerKey, 0.5f);
                    return minispikerKey;
                }
            }
            return null;
        }
    }
}
