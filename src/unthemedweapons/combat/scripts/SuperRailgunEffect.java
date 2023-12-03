package unthemedweapons.combat.scripts;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import org.lwjgl.util.vector.Vector2f;
import unthemedweapons.util.CollisionUtils;
import unthemedweapons.fx.render.GlowRenderer;

import java.awt.*;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

@SuppressWarnings("unused")
public class SuperRailgunEffect extends GlowOnFirePlugin {

    static final int maxPierce = 3;
    static final float minTimeBetweenHits = 0.06f;
    /** Fraction of current damage lost per pierce (not base damage) */
    static final float damageDecayPerHit = 0.3f;
    static final float speedLossDuringHit = 0.5f;
    static final Color hitGlowColor = new Color(255, 225, 128);
    static final Color chargeGlowColor = new Color(65, 130, 195);

    /** Whether hitting missiles counts toward the pierce limit */
    static final boolean passThroughMissiles = true;

    /** Whether hitting fighters counts toward the pierce limit */
    static final boolean passThroughFighters = true;

    GlowRenderer glowRenderer2;

    @Override
    public void init(WeaponAPI weapon) {
        glowRenderer2 = new GlowRenderer(weapon, true);
        glowRenderer2.setRenderColor(chargeGlowColor);
        Global.getCombatEngine().addLayeredRenderingPlugin(glowRenderer2);
        super.init(weapon);
    }

    private static class ProjectileData {
        private final DamagingProjectileAPI proj;
        private int timesPierced;
        private float timeSinceLastHit;
        private final Vector2f initialVelocity;
        //private float actualDamage, actualEmpDamage;

        private ProjectileData(DamagingProjectileAPI proj) {
            this.proj = proj;
            timesPierced = 0;
            timeSinceLastHit = 0f;
            initialVelocity = new Vector2f(proj.getVelocity());
//            float totalDamageRatio = getTotalBaseDamageMultiplier();
//            actualDamage = proj.getBaseDamageAmount();
//            actualEmpDamage = proj.getEmpAmount();
//            proj.setDamageAmount(totalDamageRatio * proj.getBaseDamageAmount());
//            proj.getDamage().setFluxComponent(totalDamageRatio * proj.getEmpAmount());
        }

        private void advance(float amount) {
            timeSinceLastHit += amount;
        }

        private void registerHit(CombatEntityAPI target) {
            timeSinceLastHit = 0f;
            boolean incrementPierced = !(target instanceof MissileAPI) || !passThroughMissiles;
            if (target instanceof ShipAPI && ((ShipAPI) target).isFighter() && passThroughFighters) {
                incrementPierced = false;
            }
            if (incrementPierced) {
                timesPierced++;
                proj.setDamageAmount(proj.getBaseDamageAmount() * (1 - damageDecayPerHit));
                proj.getDamage().setFluxComponent(proj.getEmpAmount() * (1 - damageDecayPerHit));
//                proj.setDamageAmount(proj.getBaseDamageAmount() - actualDamage);
//                proj.getDamage().setFluxComponent(proj.getEmpAmount() - actualEmpDamage);
//                actualDamage *= 1 - damageDecayPerHit;
//                actualEmpDamage *= 1 - damageDecayPerHit;
            }
        }

        private float getTotalBaseDamageMultiplier() {
            float sum = 0f, multiplier = 1f;
            for (int i = 0; i <= maxPierce; i++) {
                sum += multiplier;
                multiplier *= 1 - damageDecayPerHit;
            }
            return sum;
        }
    }

    private final List<ProjectileData> projectiles = new LinkedList<>();

    @Override
    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
        glowRenderer2.setAlpha(weapon.getChargeLevel());

        super.advance(amount, engine, weapon);

        Iterator<ProjectileData> itr = projectiles.iterator();
        while (itr.hasNext()) {
            ProjectileData data = itr.next();
            DamagingProjectileAPI proj = data.proj;

            // TIme since last hit should advance in world time, not ship time
            float realAmount = amount;
            if (weapon.getShip() != null) {
                MutableStat timeMult = weapon.getShip().getMutableStats().getTimeMult();
                if (timeMult != null) {
                    realAmount /= timeMult.getModifiedValue();
                }
            }
            data.advance(realAmount);

            if (data.timeSinceLastHit <= minTimeBetweenHits) {
                continue;
            } else {
                proj.getVelocity().set(data.initialVelocity);
            }

            if (proj.isExpired()) {
                itr.remove();
                continue;
            }

            Vector2f location = proj.getLocation();
            Vector2f prevLocation = new Vector2f();
            Vector2f scaledVelocity = new Vector2f(proj.getVelocity().x * amount, proj.getVelocity().y * amount);
            Vector2f.sub(location, scaledVelocity, prevLocation);

            CollisionUtils.ClosestCollisionData closest = CollisionUtils.rayCollisionCheck(prevLocation, location, proj.getSource(), true, engine);

            if (closest == null) {
                continue;
            }

            CombatEntityAPI target = closest.entity;

            proj.getVelocity().set(new Vector2f(data.initialVelocity.x * (1f - speedLossDuringHit), data.initialVelocity.y * (1f - speedLossDuringHit)));

            engine.applyDamage(
                    proj,
                    target,
                    closest.point,
                    proj.getDamageAmount(),
                    proj.getDamageType(),
                    proj.getEmpAmount(),
                    false,
                    proj.isFading(),
                    weapon.getShip(),
                    true);
            engine.addHitParticle(
                    closest.point,
                    target.getVelocity().length() <= 50f ? target.getVelocity() : new Vector2f(),
                    proj.getProjectileSpec().getHitGlowRadius(),
                    1f,
                    hitGlowColor);
            data.registerHit(target);

            if (data.timesPierced > maxPierce) {
                engine.removeEntity(proj);
                itr.remove();
            }
        }
    }

    @Override
    public void onFire(DamagingProjectileAPI proj, WeaponAPI weapon, CombatEngineAPI engine) {
        super.onFire(proj, weapon, engine);
        proj.setCollisionClass(CollisionClass.NONE);
        projectiles.add(new ProjectileData(proj));
    }
}
