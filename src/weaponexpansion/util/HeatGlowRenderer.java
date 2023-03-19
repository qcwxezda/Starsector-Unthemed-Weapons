package weaponexpansion.util;

import com.fs.starfarer.api.combat.BaseCombatLayeredRenderingPlugin;
import com.fs.starfarer.api.combat.CombatEngineLayers;
import com.fs.starfarer.api.combat.ViewportAPI;
import com.fs.starfarer.api.combat.WeaponAPI;

public class HeatGlowRenderer extends BaseCombatLayeredRenderingPlugin {

    private final WeaponAPI weapon;
    private float alpha = 1f;
    private final float originalWidth;

    public HeatGlowRenderer(WeaponAPI weapon) {
        this.weapon = weapon;
        originalWidth = weapon.getGlowSpriteAPI().getWidth();
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

    @Override
    public void render(CombatEngineLayers layer, ViewportAPI viewport) {
        weapon.getGlowSpriteAPI().setAlphaMult(alpha);
        weapon.getGlowSpriteAPI().setWidth(originalWidth);
        weapon.getGlowSpriteAPI().setAngle(weapon.getCurrAngle() - 90f);
        weapon.getGlowSpriteAPI().renderAtCenter(weapon.getLocation().x, weapon.getLocation().y);
        // Cheap hack to prevent glow on fire (setting MUZZLE_FLASH Only makes glow sprite null)
        weapon.getGlowSpriteAPI().setWidth(0f);
    }
}
