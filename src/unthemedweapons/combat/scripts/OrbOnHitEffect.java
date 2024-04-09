package unthemedweapons.combat.scripts;

import java.awt.Color;

import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.util.Misc;
import org.lwjgl.util.vector.Vector2f;

import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import unthemedweapons.combat.plugins.Action;
import unthemedweapons.combat.plugins.ActionPlugin;

@SuppressWarnings("unused")
public class OrbOnHitEffect implements OnHitEffectPlugin {

    public static float empDamage = 1000f;
    public static float chancePerHit = 0.1f;
    public static float hitTime = 3f;
    public static String COUNT_KEY = "wpnxt_OrbHitCount";



    public void onHit(DamagingProjectileAPI projectile, CombatEntityAPI target,
                      Vector2f point, boolean shieldHit, ApplyDamageResultAPI damageResult, CombatEngineAPI engine) {

        if (!(target instanceof ShipAPI)) return;
        if (!shieldHit) {
            StatBonus hits = ((ShipAPI) target).getMutableStats().getDynamic().getMod(COUNT_KEY);
            // Force recompute, since we're modifying the values directly
            hits.modifyFlat("wpnxt_ForceRecompute", Misc.random.nextFloat() * 0.01f);
            int numHits = (int) hits.computeEffective(0f);
            if (Misc.random.nextFloat() <= numHits * chancePerHit) {
                float emp = empDamage;
                float dam = 0;
                engine.spawnEmpArcPierceShields(
                        projectile.getSource(), point, target, target,
                        DamageType.ENERGY,
                        dam, // damage
                        emp, // emp
                        100000f, // max range
                        "tachyon_lance_emp_impact",
                        20f, // thickness
                        new Color(125, 125, 100, 255),
                        new Color(255, 255, 255, 255)
                                               );
            }

            MutableStat.StatMod bonus = hits.getFlatBonus(COUNT_KEY);
            if (bonus == null) {
                hits.modifyFlat(COUNT_KEY, 1);
                bonus = hits.getFlatBonus(COUNT_KEY);
            }
            else {
                bonus.value++;
            }
            final MutableStat.StatMod finalBonus = bonus;
            ActionPlugin.queueAction(new Action() {
                @Override
                public void perform() {
                    if (finalBonus != null) {
                        finalBonus.value--;
                    }
                }
            }, hitTime);
        }
    }
}
