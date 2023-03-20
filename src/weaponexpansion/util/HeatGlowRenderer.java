package weaponexpansion.util;

import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.util.Misc;
import org.lwjgl.util.vector.Vector2f;

public class HeatGlowRenderer extends BaseCombatLayeredRenderingPlugin {

    private final WeaponAPI weapon;
    private float alpha = 0f;
    private final float originalWidth;
    private final float originalWeaponWidth;
    private final boolean barrelRecoil;

    public HeatGlowRenderer(WeaponAPI weapon, boolean barrelRecoil) {
        this.weapon = weapon;
        originalWidth = weapon.getGlowSpriteAPI().getWidth();
        this.barrelRecoil = barrelRecoil;
        originalWeaponWidth = weapon.getSprite().getWidth();
    }

    @Override
    public float getRenderRadius() {
        return 100f;
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

    @Override
    public void render(CombatEngineLayers layer, ViewportAPI viewport) {
        weapon.getGlowSpriteAPI().setWidth(originalWidth);
        if (barrelRecoil) {
            weapon.getSprite().setWidth(originalWeaponWidth);
        }

        Vector2f location = weapon.getLocation();
        SpriteAPI glowSprite = weapon.getGlowSpriteAPI();
        glowSprite.setAlphaMult(alpha);
        glowSprite.setAngle(weapon.getCurrAngle() - 90f);

        if (barrelRecoil && weapon.getBarrelSpriteAPI() != null) {
            if (alpha > 0f) {
                SpriteAPI barrelSprite = weapon.getBarrelSpriteAPI();
                float recoilAmount = (weapon.getSlot().isHardpoint() ? barrelSprite.getHeight() / 4f : barrelSprite.getHeight() / 2f) - barrelSprite.getCenterY();
                glowSprite.renderAtCenter(
                        recoilAmount * (float) Math.cos(Misc.RAD_PER_DEG * weapon.getCurrAngle()) + location.x,
                        recoilAmount * (float) Math.sin(Misc.RAD_PER_DEG * weapon.getCurrAngle()) + location.y);
            }
            weapon.getSprite().renderAtCenter(location.x, location.y);
        } else if (alpha > 0f) {
            glowSprite.renderAtCenter(location.x, location.y);
        }

        // Cheap hack to prevent glow on fire (setting MUZZLE_FLASH Only makes glow sprite null)
        weapon.getGlowSpriteAPI().setWidth(0f);
        if (barrelRecoil) {
            weapon.getSprite().setWidth(0f);
        }
    }
}
