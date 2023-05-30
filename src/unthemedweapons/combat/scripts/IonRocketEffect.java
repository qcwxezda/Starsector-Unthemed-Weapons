package unthemedweapons.combat.scripts;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import org.lwjgl.util.vector.Vector2f;
import unthemedweapons.util.EngineUtils;
import unthemedweapons.util.TargetChecker;

import java.awt.*;
import java.util.Collection;

@SuppressWarnings("unused")
public class IonRocketEffect implements OnHitEffectPlugin {

    private static final int maxArcs = 3;
    private static final float arcRange = 150f;


    @Override
    public void onHit(final DamagingProjectileAPI proj, CombatEntityAPI target, Vector2f pt, boolean shieldHit, ApplyDamageResultAPI damageResult, CombatEngineAPI engine)  {
        Collection<CombatEntityAPI> nearest = EngineUtils.getKNearestEntities(
                maxArcs,
                proj.getLocation(),
                null,
                false,
                arcRange,
                true,
                new TargetChecker() {
                    @Override
                    public boolean check(CombatEntityAPI entity) {
                        // The EMP won't arc through shields so don't waste an arc trying
                        if (entity instanceof ShipAPI) {
                            ShipAPI ship = (ShipAPI) entity;
                            if (ship.getShield() != null && ship.getShield().isWithinArc(proj.getLocation())) {
                                return false;
                            }
                        }
                        return entity != null && Global.getCombatEngine().isEntityInPlay(entity) && entity.getHitpoints() > 0 && entity.getOwner() != proj.getOwner() && entity.getOwner() != 100;
                    }
            });

        for (CombatEntityAPI entity : nearest) {
            engine.spawnEmpArc(proj.getSource(), pt, proj, entity,
                    DamageType.ENERGY,
                    0f, // damage
                    proj.getEmpAmount(), // emp
                    100000f, // max range
                    "tachyon_lance_emp_impact",
                    20f, // thickness
                    new Color(125, 125, 100, 255),
                    new Color(255, 255, 255, 255));
        }
    }
}
