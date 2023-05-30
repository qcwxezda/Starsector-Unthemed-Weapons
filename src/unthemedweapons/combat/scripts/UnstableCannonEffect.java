package unthemedweapons.combat.scripts;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.util.Misc;
import org.lwjgl.util.vector.Vector2f;

import java.util.*;
import java.util.List;

@SuppressWarnings("unused")
public class UnstableCannonEffect implements OnFireEffectPlugin, EveryFrameWeaponEffectPlugin, WeaponEffectPluginWithInit {

    List<DamagingProjectileAPI> projectiles = new LinkedList<>();

    float damage = 200f, minDamage = 100f, maxDamage = 500f;
    float lv2threshold = 230f, lv3threshold = 360f;

    // Maximum angle deviation per second
    float maxJitter = 125f;

    static final String lv2SpawnWeapon = "wpnxt_unstablecannon_lv2spawner";
    static final String lv3SpawnWeapon = "wpnxt_unstablecannon_lv3spawner";
    static final String lv2SpawnProjectile = "wpnxt_unstablecannon_lv2shot";
    static final String lv3SpawnProjectile = "wpnxt_unstablecannon_lv3shot";
    static final String lv1Sound = "wpnxt_unstablecannon_fire";
    static final String lv2Sound = "wpnxt_unstablecannon_fire2";
    static final String lv3Sound = "wpnxt_unstablecannon_fire3";

    @Override
    public void onFire(DamagingProjectileAPI proj, WeaponAPI weapon, CombatEngineAPI engine) {

        DamagingProjectileAPI newProj;
        Vector2f loc = proj.getWeapon().getFirePoint(0);
        Vector2f vel = proj.getSource().getVelocity();

        if (damage < lv2threshold) {
            newProj = proj;
            proj.setDamageAmount(damage);
            Global.getSoundPlayer().playSound(lv1Sound, 0.9f + Misc.random.nextFloat() * 0.1f, 1f, loc, vel);
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
            Global.getSoundPlayer().playSound(damage < lv3threshold ? lv2Sound : lv3Sound, 0.9f + Misc.random.nextFloat() * 0.1f, 1f, loc, vel);
        }

        projectiles.add(newProj);

        // Change the projectile spec's stats every time the weapon fires
        randomizeDamage();
    }

    @Override
    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
        Iterator<DamagingProjectileAPI> itr = projectiles.iterator();
        while (itr.hasNext()) {
            DamagingProjectileAPI proj = itr.next();
            if (proj.isExpired() || !engine.isEntityInPlay(proj)) {
                itr.remove();
                continue;
            }
            float jitter = Misc.random.nextFloat() * maxJitter - maxJitter / 2;
            jitter *= (proj.getBaseDamageAmount() - minDamage) / (maxDamage - minDamage);
            jitter *= amount;

            Vector2f newTail = Misc.rotateAroundOrigin(proj.getTailEnd(), jitter, proj.getLocation());

            proj.getTailEnd().set(newTail);
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
        if (weapon.getSlot().isHardpoint()) {
            maxJitter /= 2f;
        }
    }
}
