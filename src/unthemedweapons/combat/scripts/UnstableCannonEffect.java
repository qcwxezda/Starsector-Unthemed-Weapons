package unthemedweapons.combat.scripts;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.util.Misc;
import org.lwjgl.util.vector.Vector2f;

import java.util.*;
import java.util.List;

@SuppressWarnings("unused")
public class UnstableCannonEffect implements OnFireEffectPlugin, EveryFrameWeaponEffectPlugin {

    List<DamagingProjectileAPI> projectiles = new LinkedList<>();

    public static final float BASE_DAMAGE = 190f;
    public static final float MIN_DAMAGE_RATIO = 120f/BASE_DAMAGE;
    public static final float MAX_DAMAGE_RATIO = 400f/BASE_DAMAGE;
    public static final float LV2_THRESHOLD = 200f/BASE_DAMAGE;
    public static final float LV3_THRESHOLD = 300f/BASE_DAMAGE;
    float damageRatio = 1f;

    // Maximum angle deviation per second
    float maxJitter = 60f;

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

        if (damageRatio < LV2_THRESHOLD) {
            newProj = proj;
            proj.setDamageAmount(damageRatio * proj.getBaseDamageAmount());
            Global.getSoundPlayer().playSound(lv1Sound, 0.9f + Misc.random.nextFloat() * 0.1f, 1f, loc, vel);
        } else {
            newProj = (DamagingProjectileAPI)
                    engine.spawnProjectile(
                            proj.getSource(),
                            proj.getWeapon(),
                            damageRatio < LV3_THRESHOLD ? lv2SpawnWeapon : lv3SpawnWeapon,
                            proj.getLocation(),
                            proj.getFacing(),
                            proj.getSource().getVelocity());
            newProj.setDamageAmount(damageRatio * proj.getBaseDamageAmount());
            engine.removeEntity(proj);
            Global.getSoundPlayer().playSound(damageRatio < LV3_THRESHOLD ? lv2Sound : lv3Sound, 0.9f + Misc.random.nextFloat() * 0.1f, 1f, loc, vel);
        }

        projectiles.add(newProj);

        // Change the projectile damage every time the weapon fires
        randomizeDamage();
    }

    @Override
    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
        if (weapon == null || weapon.getSlot() == null || weapon.getSlot().isHardpoint()) return;
        Iterator<DamagingProjectileAPI> itr = projectiles.iterator();
        while (itr.hasNext()) {
            DamagingProjectileAPI proj = itr.next();
            if (proj.isExpired() || !engine.isEntityInPlay(proj)) {
                itr.remove();
                continue;
            }

            float jitter = Misc.random.nextFloat() * maxJitter - maxJitter / 2;
            jitter *= (proj.getBaseDamageAmount() - BASE_DAMAGE*MIN_DAMAGE_RATIO) / (BASE_DAMAGE*MAX_DAMAGE_RATIO - BASE_DAMAGE*MIN_DAMAGE_RATIO);
            jitter *= amount;

            Vector2f newTail = Misc.rotateAroundOrigin(proj.getTailEnd(), jitter, proj.getLocation());

            proj.getTailEnd().set(newTail);
        }
    }

    private void randomizeDamage() {
        float r1 = scaledDamageRatio(Misc.random.nextFloat());
        float r2 = scaledDamageRatio(Misc.random.nextFloat());
        float r3 = scaledDamageRatio(Misc.random.nextFloat());

        // Minimum of 3 damage rolls
        damageRatio = Math.min(r1, Math.min(r2, r3));
    }

    private float scaledDamageRatio(float frac) {
        return UnstableCannonEffect.MIN_DAMAGE_RATIO + frac * (UnstableCannonEffect.MAX_DAMAGE_RATIO - UnstableCannonEffect.MIN_DAMAGE_RATIO);
    }
}
