package unthemedweapons.fx.render;

import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.graphics.SpriteAPI;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.EnumSet;

public class GlowRenderer extends BaseCombatLayeredRenderingPlugin {

    private final WeaponAPI weapon;
    private float alpha = 0f;
    private final float originalWidth;
    private final float originalWeaponWidth;
    private final boolean barrelRecoil;
    private Color renderColor;
    private SpriteAPI sprite;

    public GlowRenderer(WeaponAPI weapon, boolean barrelRecoil) {
        this.weapon = weapon;
        sprite = weapon.getGlowSpriteAPI();
        originalWidth = sprite.getWidth();
        this.barrelRecoil = barrelRecoil;
        originalWeaponWidth = weapon.getSprite().getWidth();
        renderColor = weapon.getGlowSpriteAPI().getColor();
    }

    @Override
    public float getRenderRadius() {
        return 500f;
    }

    @Override
    public EnumSet<CombatEngineLayers> getActiveLayers() {
        return EnumSet.of(CombatEngineLayers.BELOW_PLANETS, CombatEngineLayers.CONTRAILS_LAYER);
    }

    @Override
    public boolean isExpired() {
        return weapon.isPermanentlyDisabled() || weapon.getShip() == null || !weapon.getShip().isAlive();
    }

    @Override
    public void advance(float amount) {
        entity.getLocation().set(weapon.getLocation());
        entity.setFacing(0f);
    }

    public void setAlpha(float f) {
        alpha = f;
    }

    public void modifyAlpha(float f) {
        alpha += f;
        alpha = Math.max(0f, Math.min(1f, alpha));
    }

    public void setRenderColor(Color color) {
        renderColor = color;
    }

    public void setSprite(SpriteAPI sprite) {
        this.sprite = sprite;
    }

    /** Render order:
     * BELOW_PLANETS,
     * PLANET_LAYER,
     * ABOVE_PLANETS,
     * CLOUD_LAYER,
     * BELOW_SHIPS_LAYER,
     * UNDER_SHIPS_LAYER,
     * ASTEROIDS_LAYER,
     * CAPITAL_SHIPS_LAYER,
     * CRUISERS_LAYER,
     * DESTROYERS_LAYER,
     * FRIGATES_LAYER,
     * BELOW_PHASED_SHIPS_LAYER,
     * PHASED_SHIPS_LAYER,
     * STATION_WEAPONS_LAYER,
     * CONTRAILS_LAYER,
     * FIGHTERS_LAYER,
     * BELOW_INDICATORS_LAYER,
     * FF_INDICATORS_LAYER,
     * ABOVE_SHIPS_LAYER,
     * ABOVE_SHIPS_AND_MISSILES_LAYER,
     * ABOVE_PARTICLES_LOWER,
     * ABOVE_PARTICLES,
     * JUST_BELOW_WIDGETS
     */
    @Override
    public void render(CombatEngineLayers layer, ViewportAPI viewport) {
        if (layer.equals(CombatEngineLayers.BELOW_PLANETS)) {
            sprite.setWidth(0f);
        }

        if (layer.equals(CombatEngineLayers.CONTRAILS_LAYER)) {
            sprite.setWidth(originalWidth);
            Vector2f location = weapon.getLocation();
            sprite.setAlphaMult(alpha);
            sprite.setColor(renderColor);
            sprite.setAngle(weapon.getCurrAngle() - 90f);
            if (alpha > 0f) {
                if (barrelRecoil) {
                    weapon.renderBarrel(sprite, location, alpha);
//                    SpriteAPI barrelSprite = weapon.getBarrelSpriteAPI();
//                    float recoilAmount = (weapon.getSlot().isHardpoint() ? barrelSprite.getHeight() / 4f : barrelSprite.getHeight() / 2f) - barrelSprite.getCenterY();
//                    glowSprite.renderAtCenter(
//                            recoilAmount * (float) Math.cos(Misc.RAD_PER_DEG * weapon.getCurrAngle()) + location.x,
//                            recoilAmount * (float) Math.sin(Misc.RAD_PER_DEG * weapon.getCurrAngle()) + location.y);
                }
                else {
                    sprite.renderAtCenter(location.x, location.y);
                }
            }
        }

//        if (barrelRecoil) {
//            weapon.getSprite().setWidth(originalWeaponWidth);
//        }
//
//        Vector2f location = weapon.getLocation();
//        SpriteAPI glowSprite = sprite;
//        glowSprite.setAlphaMult(1f);
//        glowSprite.setColor(renderColor);
//        glowSprite.setAngle(weapon.getCurrAngle() - 90f);
//
//        if (barrelRecoil && weapon.getBarrelSpriteAPI() != null) {
//            if (alpha > 0f) {
//                SpriteAPI barrelSprite = weapon.getBarrelSpriteAPI();
//                float recoilAmount = (weapon.getSlot().isHardpoint() ? barrelSprite.getHeight() / 4f : barrelSprite.getHeight() / 2f) - barrelSprite.getCenterY();
//                glowSprite.renderAtCenter(
//                        recoilAmount * (float) Math.cos(Misc.RAD_PER_DEG * weapon.getCurrAngle()) + location.x,
//                        recoilAmount * (float) Math.sin(Misc.RAD_PER_DEG * weapon.getCurrAngle()) + location.y);
//            }
//        } else if (alpha > 0f && layer.equals(CombatEngineLayers.BELOW_INDICATORS_LAYER)) {
//            glowSprite.renderAtCenter(location.x, location.y);
//        }
////        if (shouldRerenderBaseThisFrame() && layer.equals(CombatEngineLayers.ABOVE_SHIPS_LAYER)) {
////            weapon.getSprite().renderAtCenter(location.x, location.y);
////        }
//
//        // Cheap hack to prevent glow on fire (setting MUZZLE_FLASH Only makes glow sprite null)
//        sprite.setWidth(0f);
////        if (shouldRerenderBaseThisFrame()) {
////            weapon.getSprite().setWidth(0f);
////        }
    }
}
