package weaponexpansion.combat.scripts;

import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.loading.ProjectileSpecAPI;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.Pair;
import com.fs.starfarer.ui.P;
import org.lwjgl.util.vector.Vector2f;

import java.util.*;

public class UnstableCannonEffect implements OnFireEffectPlugin, EveryFrameWeaponEffectPlugin {

    List<Pair<DamagingProjectileAPI, Vector2f>> projectiles = new LinkedList<>();

    boolean isFirstFrame = true;

    float damage = 167f, minDamage = 100f, maxDamage = 500f;

    @Override
    public void onFire(DamagingProjectileAPI proj, WeaponAPI weapon, CombatEngineAPI engine) {
        proj.setDamageAmount(damage);
        projectiles.add(new Pair<>(proj, Misc.getPerp(Misc.getUnitVectorAtDegreeAngle(weapon.getCurrAngle()))));

        // Change the projectile spec's stats every time the weapon fires
        randomizeDamage(proj.getProjectileSpec());
    }

    @Override
    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
        // So that the first shot isn't always the same
        if (isFirstFrame) {
            isFirstFrame = false;
            randomizeDamage((ProjectileSpecAPI) weapon.getSpec().getProjectileSpec());
        }

        Iterator<Pair<DamagingProjectileAPI, Vector2f>> itr = projectiles.iterator();
        while (itr.hasNext()) {
            Pair<DamagingProjectileAPI, Vector2f> proj_vec = itr.next();
            DamagingProjectileAPI proj = proj_vec.one;
            if (proj.isExpired() || !engine.isEntityInPlay(proj)) {
                itr.remove();
                continue;
            }
            if (proj.getElapsed() < 0.1f) {
                continue;
            }
            Vector2f jitterVec = new Vector2f(0f, 0f);
            Vector2f perp = new Vector2f(proj_vec.two);
            float maxJitter = (proj.getBaseDamageAmount() - minDamage) * 0.025f;
            if (perp.lengthSquared() > 0f) {
                jitterVec = (Vector2f) perp.scale(Misc.random.nextFloat() * maxJitter - maxJitter / 2);
            }
            Vector2f.add(proj.getLocation(), jitterVec, proj.getLocation());
        }
    }

    private void randomizeDamage(ProjectileSpecAPI spec) {
        float r1 = scaledValue(minDamage, maxDamage, Misc.random.nextFloat());
        float r2 = scaledValue(minDamage, maxDamage, Misc.random.nextFloat());
        float r3 = scaledValue(minDamage, maxDamage, Misc.random.nextFloat());
        float r4 = scaledValue(minDamage, maxDamage, Misc.random.nextFloat());
        float r5 = scaledValue(minDamage, maxDamage, Misc.random.nextFloat());

        // Minimum of 5 damage rolls
        damage = Math.min(r1, Math.min(r2,Math.min(r3, Math.min(r4, r5))));
    }

    private float scaledValue(float min, float max, float frac) {
        return min + frac * (max - min);
    }
}
