package weaponexpansion.util;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.impl.combat.CombatEntityPluginWithParticles;
import com.fs.starfarer.api.util.Misc;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.EnumSet;
import java.util.List;


/** Weapons that intend to use an effect that extend this should not have any on fire sound set; instead, they should
 *  put it in getSoundId.
 *  The glow sprite name should be the same as the weapon's sprite name with a _glow, i.e.
 *  if the weapon sprite name is weapon.png, the glow sprite should be weapon_glow.png.
 *  */
public abstract class InstantaneousBurst implements OnFireEffectPlugin, EveryFrameWeaponEffectPlugin, WeaponEffectPluginWithInit, EveryFrameWeaponEffectPluginWithAdvanceAfter {

    private boolean isInFakeBurst = false;
    private int ammoAtFakeBurstStart = 0;
    private float glowSpriteWidth = 0;
    private int muzzleFlashCount = 0;

    @Override
    public void init(WeaponAPI weapon) {
        glowSpriteWidth = weapon.getGlowSpriteAPI().getWidth();
        muzzleFlashCount = weapon.getMuzzleFlashSpec().getParticleCount();
//        Global.getCombatEngine().addLayeredRenderingPlugin(new GlowRenderer(weapon))
//                .getLocation()
//                .set(weapon.getLocation());
    }

    @Override
    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon)  {
        weapon.getGlowSpriteAPI().renderAtCenter(weapon.getLocation().x, weapon.getLocation().y);
        weapon.getGlowSpriteAPI().setAlphaMult(1f);
        if (weapon.getChargeLevel() < 1f) {
            isInFakeBurst = false;
            ammoAtFakeBurstStart = weapon.getAmmo();
//            weapon.getGlowSpriteAPI().setWidth(glowSpriteWidth);
            weapon.getMuzzleFlashSpec().setParticleCount(muzzleFlashCount);
        }
    }

    @Override
    public void onFire(DamagingProjectileAPI proj, WeaponAPI weapon, CombatEngineAPI engine) {
        engine.removeEntity(proj);
        if (isInFakeBurst) {
            return;
        }
        weapon.getGlowSpriteAPI().setAlphaMult(1f);
        float temp = proj.getProjectileSpec().getMoveSpeed(null, null);
        for (float angleOffset : getAngleOffsets(weapon)) {
            proj.getProjectileSpec().setMoveSpeed(temp * (1 + Misc.random.nextFloat() * getSpeedVariance() - getSpeedVariance() / 2));
            DamagingProjectileAPI spawn = (DamagingProjectileAPI) engine.spawnProjectile(
                    proj.getSource(),
                    weapon,
                    weapon.getId(),
                    weapon.getFirePoint(0),
                    weapon.getCurrAngle() + angleOffset,
                    proj.getSource().getVelocity()
            );
            spawn.setDamageAmount(proj.getBaseDamageAmount() * (1 + Misc.random.nextFloat() * getDamageVariance() - getDamageVariance() / 2));
        }
        proj.getProjectileSpec().setMoveSpeed(temp);

        isInFakeBurst = true;
        //weapon.getGlowSpriteAPI().setWidth(0f);
        //weapon.getMuzzleFlashSpec().setParticleCount(0);
        //weapon.getMuzzleFlashSpec().setParticleDuration(0f);
        weapon.getMuzzleFlashSpec().setParticleCount(0);
        Global.getSoundPlayer().playSound(
                getSoundId(),
                0.95f + Misc.random.nextFloat() * 0.05f,
                1f,
                weapon.getFirePoint(0),
                proj.getSource() == null ? new Vector2f() : proj.getSource().getVelocity());
    }

    @Override
    public void advanceAfter(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
        if (isInFakeBurst) {
            weapon.getGlowSpriteAPI().setAlphaMult(1f);
            weapon.getGlowSpriteAPI().renderAtCenter(weapon.getLocation().x, weapon.getLocation().y);
            //weapon.getGlowSpriteAPI().renderAtCenter(weapon.getLocation().x, weapon.getLocation().y);
        }
    }

    public abstract String getSoundId();

    public abstract List<Float> getAngleOffsets(WeaponAPI weapon);
    public abstract float getSpeedVariance();
    public abstract float getDamageVariance();

    public static class GlowRenderer extends BaseCombatLayeredRenderingPlugin {
        WeaponAPI weapon;
        SpriteAPI sprite;
        Color originalColor;

        public GlowRenderer(WeaponAPI weapon) {
            this.weapon = weapon;
//            String weaponSpriteName =
//                    weapon.getSlot().isHardpoint() ?
//                            weapon.getSpec().getHardpointSpriteName() : weapon.getSpec().getTurretSpriteName();
//            int extensionIndex = weaponSpriteName.lastIndexOf('.');
//            String glowSpriteName = weaponSpriteName.substring(0, extensionIndex)
//                    + "_glow"
//                    + weaponSpriteName.substring(extensionIndex);
//            sprite = Global.getSettings().getSprite(glowSpriteName);
            sprite = weapon.getGlowSpriteAPI();
            originalColor = sprite.getColor();
        }

        @Override
        public float getRenderRadius() {return 500f;}

        @Override
        public EnumSet<CombatEngineLayers> getActiveLayers() {return EnumSet.of(CombatEngineLayers.ABOVE_PARTICLES);}

        @Override
        public void init(CombatEntityAPI entity) {
            super.init(entity);
        }

        @Override
        public boolean isExpired() {
            return weapon.isPermanentlyDisabled() || weapon.getShip() == null || !weapon.getShip().isAlive();
        }

        @Override
        public void advance(float amount) {
            entity.getLocation().set(weapon.getLocation());
        }

        @Override
        public void render(CombatEngineLayers layer, ViewportAPI viewport) {
//            sprite.setColor(originalColor);
//            sprite.renderAtCenter(entity.getLocation().x, entity.getLocation().y);
//            sprite.setColor(Color.black);
        }
    }
}
