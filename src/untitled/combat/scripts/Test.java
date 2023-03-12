package untitled.combat.scripts;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.impl.combat.CombatEntityPluginWithParticles;
import com.fs.starfarer.api.impl.combat.RealityDisruptorChargeGlow;
import com.fs.starfarer.api.impl.combat.RiftCascadeEffect;
import com.fs.starfarer.api.impl.combat.RiftLanceEffect;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.Misc;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.Iterator;

public class Test extends CombatEntityPluginWithParticles {
    public static int MAX_ARC_RANGE = 300;
    public static Color UNDERCOLOR;
    public static Color RIFT_COLOR;
    protected WeaponAPI weapon;
    protected DamagingProjectileAPI proj;
    protected IntervalUtil interval = new IntervalUtil(0.1F, 0.2F);
    protected IntervalUtil arcInterval = new IntervalUtil(0.17F, 0.23F);
    protected float delay = 1.0F;

    static {
        UNDERCOLOR = RiftCascadeEffect.EXPLOSION_UNDERCOLOR;
        RIFT_COLOR = RiftCascadeEffect.STANDARD_RIFT_COLOR;
    }

    public Test(WeaponAPI weapon, DamagingProjectileAPI proj) {
        this.weapon = weapon;
        this.proj = proj;
        this.arcInterval = new IntervalUtil(0.17F, 0.23F);
        this.delay = 0.5F;
        this.setSpriteSheetKey("fx_particles2");
    }

    public void advance(float amount) {
        if (!Global.getCombatEngine().isPaused()) {
            if (this.proj != null) {
                this.entity.getLocation().set(this.proj.getLocation());
            } else {
                this.entity.getLocation().set(this.weapon.getFirePoint(0));
            }

            super.advance(amount);
            boolean keepSpawningParticles = isWeaponCharging(this.weapon) || this.proj != null && !isProjectileExpired(this.proj) && !this.proj.isFading();
            if (keepSpawningParticles) {
                this.interval.advance(amount);
                if (this.interval.intervalElapsed()) {
                    this.addChargingParticles(this.weapon);
                }
            }

            if (this.proj != null && !isProjectileExpired(this.proj) && !this.proj.isFading()) {
                this.delay -= amount;
                if (this.delay <= 0.0F) {
                    this.arcInterval.advance(amount);
                    if (this.arcInterval.intervalElapsed()) {
                        this.spawnArc();
                    }
                }
            }

            if (this.proj != null) {
                Global.getSoundPlayer().playLoop("realitydisruptor_loop", this.proj, 1.0F, 1.0F * this.proj.getBrightness(), this.proj.getLocation(), this.proj.getVelocity());
            }

        }
    }

    public void render(CombatEngineLayers layer, ViewportAPI viewport) {
        super.render(layer, viewport, (DamagingProjectileAPI)null);
    }

    public boolean isExpired() {
        boolean keepSpawningParticles = isWeaponCharging(this.weapon) || this.proj != null && !isProjectileExpired(this.proj) && !this.proj.isFading();
        return super.isExpired() && (!keepSpawningParticles || !this.weapon.getShip().isAlive() && this.proj == null);
    }

    public float getRenderRadius() {
        return 500.0F;
    }

    protected float getGlobalAlphaMult() {
        return this.proj != null && this.proj.isFading() ? this.proj.getBrightness() : super.getGlobalAlphaMult();
    }

    public void spawnArc() {
        CombatEngineAPI engine = Global.getCombatEngine();
        float emp = this.proj.getEmpAmount();
        float dam = this.proj.getDamageAmount();
        CombatEntityAPI target = this.findTarget(this.proj, this.weapon, engine);
        float thickness = 20.0F;
        float coreWidthMult = 0.67F;
        Color color = this.weapon.getSpec().getGlowColor();
        if (target != null) {
            EmpArcEntityAPI arc = engine.spawnEmpArc(this.proj.getSource(), this.proj.getLocation(), (CombatEntityAPI)null, target, DamageType.ENERGY, dam, emp, 100000.0F, "realitydisruptor_emp_impact", thickness, color, new Color(255, 255, 255, 255));
            arc.setCoreWidthOverride(thickness * coreWidthMult);
            this.spawnEMPParticles(RealityDisruptorChargeGlow.EMPArcHitType.SOURCE, this.proj.getLocation(), (CombatEntityAPI)null);
            this.spawnEMPParticles(RealityDisruptorChargeGlow.EMPArcHitType.DEST, arc.getTargetLocation(), target);
        } else {
            Vector2f from = new Vector2f(this.proj.getLocation());
            Vector2f to = this.pickNoTargetDest(this.proj, this.weapon, engine);
            EmpArcEntityAPI arc = engine.spawnEmpArcVisual(from, (CombatEntityAPI)null, to, (CombatEntityAPI)null, thickness, color, Color.white);
            arc.setCoreWidthOverride(thickness * coreWidthMult);
            Global.getSoundPlayer().playSound("realitydisruptor_emp_impact", 1.0F, 1.0F, to, new Vector2f());
            this.spawnEMPParticles(RealityDisruptorChargeGlow.EMPArcHitType.SOURCE, from, (CombatEntityAPI)null);
            this.spawnEMPParticles(RealityDisruptorChargeGlow.EMPArcHitType.DEST_NO_TARGET, to, (CombatEntityAPI)null);
        }

    }

    public Vector2f pickNoTargetDest(DamagingProjectileAPI projectile, WeaponAPI weapon, CombatEngineAPI engine) {
        float range = 200.0F;
        Vector2f from = projectile.getLocation();
        Vector2f dir = Misc.getUnitVectorAtDegreeAngle((float)Math.random() * 360.0F);
        dir.scale(range);
        Vector2f.add(from, dir, dir);
        dir = Misc.getPointWithinRadius(dir, range * 0.25F);
        return dir;
    }

    public CombatEntityAPI findTarget(DamagingProjectileAPI projectile, WeaponAPI weapon, CombatEngineAPI engine) {
        float range = (float)MAX_ARC_RANGE;
        Vector2f from = projectile.getLocation();
        Iterator<Object> iter = Global.getCombatEngine().getAllObjectGrid().getCheckIterator(from, range * 2.0F, range * 2.0F);
        int owner = weapon.getShip().getOwner();
        CombatEntityAPI best = null;
        float minScore = Float.MAX_VALUE;

        while(true) {
            CombatEntityAPI other;
            ShipAPI otherShip;
            do {
                do {
                    Object o;
                    do {
                        if (!iter.hasNext()) {
                            return best;
                        }

                        o = iter.next();
                    } while(!(o instanceof MissileAPI) && !(o instanceof ShipAPI));

                    other = (CombatEntityAPI)o;
                } while(other.getOwner() == owner);

                if (!(other instanceof ShipAPI)) {
                    break;
                }

                otherShip = (ShipAPI)other;
            } while(otherShip.isHulk() || otherShip.isPhased());

            if (other.getCollisionClass() != CollisionClass.NONE) {
                float radius = Misc.getTargetingRadius(from, other, false);
                float dist = Misc.getDistance(from, other.getLocation()) - radius - 50.0F;
                if (!(dist > range) && dist < minScore) {
                    minScore = dist;
                    best = other;
                }
            }
        }
    }

    public void addChargingParticles(WeaponAPI weapon) {
        Color color = RiftLanceEffect.getColorForDarkening(RIFT_COLOR);
        float size = 50.0F;
        float underSize = 75.0F;
        float in = 0.25F;
        float out = 0.75F;
        out *= 3.0F;
        float velMult = 0.2F;
        if (isWeaponCharging(weapon)) {
            size *= 0.25F + weapon.getChargeLevel() * 0.75F;
        }

        this.addDarkParticle(size, in, out, 1.0F, size * 0.5F * velMult, 0.0F, color);
        this.randomizePrevParticleLocation(size * 0.33F);
        if (this.proj != null) {
            Vector2f dir = Misc.getUnitVectorAtDegreeAngle(this.proj.getFacing() + 180.0F);
            Vector2f offset;
            if (this.proj.getElapsed() > 0.2F) {
                this.addDarkParticle(size, in, out, 1.5F, size * 0.5F * velMult, 0.0F, color);
                offset = new Vector2f(dir);
                offset.scale(size * 0.6F + (float)Math.random() * 0.2F);
                Vector2f.add(this.prev.offset, offset, this.prev.offset);
            }

            if (this.proj.getElapsed() > 0.4F) {
                this.addDarkParticle(size * 1.0F, in, out, 1.3F, size * 0.5F * velMult, 0.0F, color);
                offset = new Vector2f(dir);
                offset.scale(size * 1.2F + (float)Math.random() * 0.2F);
                Vector2f.add(this.prev.offset, offset, this.prev.offset);
            }

            if (this.proj.getElapsed() > 0.6F) {
                this.addDarkParticle(size * 0.8F, in, out, 1.1F, size * 0.5F * velMult, 0.0F, color);
                offset = new Vector2f(dir);
                offset.scale(size * 1.6F + (float)Math.random() * 0.2F);
                Vector2f.add(this.prev.offset, offset, this.prev.offset);
            }

            if (this.proj.getElapsed() > 0.8F) {
                this.addDarkParticle(size * 0.8F, in, out, 1.1F, size * 0.5F * velMult, 0.0F, color);
                offset = new Vector2f(dir);
                offset.scale(size * 2.0F + (float)Math.random() * 0.2F);
                Vector2f.add(this.prev.offset, offset, this.prev.offset);
            }
        }

        this.addParticle(underSize * 0.5F, in, out, 4.5F, 0.0F, 0.0F, UNDERCOLOR);
        this.randomizePrevParticleLocation(underSize * 0.67F);
        this.addParticle(underSize * 0.5F, in, out, 4.5F, 0.0F, 0.0F, UNDERCOLOR);
        this.randomizePrevParticleLocation(underSize * 0.67F);
    }

    public void spawnEMPParticles(RealityDisruptorChargeGlow.EMPArcHitType type, Vector2f point, CombatEntityAPI target) {
        CombatEngineAPI engine = Global.getCombatEngine();
        Color color = RiftLanceEffect.getColorForDarkening(RIFT_COLOR);
        float size = 30.0F;
        float baseDuration = 1.5F;
        Vector2f vel = new Vector2f();
        int numNegative = 5;
        switch (type) {
            case SOURCE:
                size = 40.0F;
                numNegative = 10;
                break;
            case DEST:
                size = 50.0F;
                vel.set(target.getVelocity());
            case DEST_NO_TARGET:
        }

        Vector2f dir = Misc.getUnitVectorAtDegreeAngle(this.proj.getFacing() + 180.0F);

        float dur;
        Vector2f pt;
        for(int i = 0; i < numNegative; ++i) {
            dur = baseDuration + baseDuration * (float)Math.random();
            float nSize = size;
            if (type == RealityDisruptorChargeGlow.EMPArcHitType.SOURCE) {
                nSize = size * 1.5F;
            }

            pt = Misc.getPointWithinRadius(point, nSize * 0.5F);
            Vector2f v = Misc.getUnitVectorAtDegreeAngle((float)Math.random() * 360.0F);
            v.scale(nSize + nSize * (float)Math.random() * 0.5F);
            v.scale(0.2F);
            float endSizeMult = 2.0F;
            if (type == RealityDisruptorChargeGlow.EMPArcHitType.SOURCE) {
                pt = Misc.getPointWithinRadius(point, nSize * 0.0F);
                Vector2f offset = new Vector2f(dir);
                offset.scale(size * 0.2F * (float)i);
                Vector2f.add(pt, offset, pt);
                endSizeMult = 1.5F;
                v.scale(0.5F);
            }

            Vector2f.add(vel, v, v);
            float maxSpeed = nSize * 1.5F * 0.2F;
            float minSpeed = nSize * 1.0F * 0.2F;
            float overMin = v.length() - minSpeed;
            if (overMin > 0.0F) {
                float durMult = 1.0F - overMin / (maxSpeed - minSpeed);
                if (durMult < 0.1F) {
                    durMult = 0.1F;
                }

                dur *= 0.5F + 0.5F * durMult;
            }

            engine.addNegativeNebulaParticle(pt, v, nSize * 1.0F, endSizeMult, 0.25F / dur, 0.0F, dur, color);
        }

        float dur2 = baseDuration;
        dur2 = 0.5F / baseDuration;
        color = UNDERCOLOR;

        for(int i = 0; i < 7; ++i) {
            pt = new Vector2f(point);
            pt = Misc.getPointWithinRadius(pt, size * 1.0F);
            float s = size * 4.0F * (0.5F + (float)Math.random() * 0.5F);
            engine.addSwirlyNebulaParticle(pt, vel, s, 1.5F, dur2, 0.0F, dur2, color, false);
        }

    }

    public static boolean isProjectileExpired(DamagingProjectileAPI proj) {
        return proj.isExpired() || proj.didDamage() || !Global.getCombatEngine().isEntityInPlay(proj);
    }

    public static boolean isWeaponCharging(WeaponAPI weapon) {
        return weapon.getChargeLevel() > 0.0F && weapon.getCooldownRemaining() <= 0.0F;
    }

    public static enum EMPArcHitType {
        SOURCE,
        DEST,
        DEST_NO_TARGET;

        private EMPArcHitType() {
        }
    }
}
