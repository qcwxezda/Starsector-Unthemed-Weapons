package weaponexpansion.combat.scripts;

import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.Misc;
import org.lwjgl.util.vector.Vector2f;

import java.util.*;
import java.util.List;

@SuppressWarnings("unused")
public class UnstableCannonEffect implements OnFireEffectPlugin, EveryFrameWeaponEffectPlugin, WeaponEffectPluginWithInit {

    List<DamagingProjectileAPI> projectiles = new LinkedList<>();

    float damage = 200f, minDamage = 100f, maxDamage = 600f;
    float lv2threshold = 300f, lv3threshold = 500f;

    // Maximum angle deviation per second
    float maxJitter = 120f;
    IntervalUtil jitterInterval = new IntervalUtil(0.05f, 0.05f);

    static final String lv2SpawnWeapon = "wpnxt_unstablecannon_lv2spawner";
    static final String lv3SpawnWeapon = "wpnxt_unstablecannon_lv3spawner";
    static final String lv2SpawnProjectile = "wpnxt_unstablecannon_lv2shot";
    static final String lv3SpawnProjectile = "wpnxt_unstablecannon_lv3shot";

    @Override
    public void onFire(DamagingProjectileAPI proj, WeaponAPI weapon, CombatEngineAPI engine) {

        DamagingProjectileAPI newProj;

        if (damage < lv2threshold) {
            newProj = proj;
            proj.setDamageAmount(damage);
        } else {
            newProj = (DamagingProjectileAPI)
                    engine.spawnProjectile(
                            proj.getSource(),
                            proj.getWeapon(),
                            damage < lv3threshold ? lv2SpawnWeapon : lv3SpawnWeapon,
                            proj.getLocation(),
                            proj.getFacing(),
                            proj.getSource().getVelocity());
            newProj.setDamageAmount(damage);
            engine.removeEntity(proj);
        }

        projectiles.add(newProj);

        // Change the projectile spec's stats every time the weapon fires
        randomizeDamage();
    }

    @Override
    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {

        jitterInterval.advance(amount);

        if (!jitterInterval.intervalElapsed()) {
            return;
        }

        Iterator<DamagingProjectileAPI> itr = projectiles.iterator();
        while (itr.hasNext()) {
            DamagingProjectileAPI proj = itr.next();
            if (proj.isExpired() || !engine.isEntityInPlay(proj)) {
                itr.remove();
                continue;
            }
            float jitter = Misc.random.nextFloat() * maxJitter - maxJitter / 2;
            jitter *= (proj.getBaseDamageAmount() - minDamage) / (maxDamage - minDamage);
            jitter *= jitterInterval.getIntervalDuration();

            Vector2f newLoc = Misc.rotateAroundOrigin(proj.getLocation(), jitter, proj.getTailEnd());
            proj.getLocation().set(newLoc);
        }
    }

    private void randomizeDamage() {
        float r1 = scaledValue(minDamage, maxDamage, Misc.random.nextFloat());
        float r2 = scaledValue(minDamage, maxDamage, Misc.random.nextFloat());

        // Minimum of 2 damage rolls
        damage = Math.min(r1, r2);
    }

    private float scaledValue(float min, float max, float frac) {
        return min + frac * (max - min);
    }

    @Override
    public void init(WeaponAPI weapon) {
        randomizeDamage();
    }
}
