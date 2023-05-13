package weaponexpansion.util;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.loading.ProjectileSpecAPI;
import weaponexpansion.combat.scripts.EnergyBallLauncherEffect;

public class EnergyBallRenderer extends BaseCombatLayeredRenderingPlugin {

    private final WeaponAPI weaponSource;
    private final DamagingProjectileAPI projSource;
    private final float maxProjDamage;
    private final SpriteAPI sprite;
    private final EnergyBallLauncherEffect plugin;
    public float facing = Utils.randBetween(0f, 360f);

    // Attached to weapon
    public EnergyBallRenderer(WeaponAPI source, EnergyBallLauncherEffect plugin) {
        weaponSource = source;
        sprite = Global.getSettings().getSprite("misc", "wpnxt_energyball");
        ProjectileSpecAPI projSpec = (ProjectileSpecAPI) source.getSpec().getProjectileSpec();
        sprite.setColor(projSpec.getCoreColor());
        this.plugin = plugin;
        maxProjDamage = 0f;
        projSource = null;
    }

    // Attached to projectile
    public EnergyBallRenderer(DamagingProjectileAPI source, EnergyBallLauncherEffect plugin, float maxDamage, float initialFacing) {
        projSource = source;
        sprite = Global.getSettings().getSprite("misc", "wpnxt_energyball");
        ProjectileSpecAPI projSpec = source.getProjectileSpec();
        sprite.setColor(projSpec.getCoreColor());
        this.plugin = plugin;
        maxProjDamage = maxDamage;
        weaponSource = null;
        facing = initialFacing;
    }

    @Override
    public float getRenderRadius() {
        return 500f;
    }

    @Override
    public boolean isExpired() {
        if (weaponSource != null) {
            return weaponSource.getShip() == null || !weaponSource.getShip().isAlive();
        }
        return projSource.isExpired();
    }

    @Override
    public void advance(float amount) {
        if (weaponSource != null) {
            entity.getLocation().set(weaponSource.getFirePoint(0));
        }
        else {
            entity.getLocation().set(projSource.getLocation());
        }
        facing += 240f * amount;
    }

    @Override
    public void render(CombatEngineLayers layer, ViewportAPI viewport) {
        if (weaponSource != null) {
            float size = plugin.getProjectileSize();
            if (size <= 0f) return;
            sprite.setWidth(size);
            sprite.setHeight(size);
            sprite.setAlphaMult(0.25f);
            render();
        }
        else {
            sprite.setAlphaMult(projSource.getBrightness() * 0.25f);
            float size = (float) Math.sqrt(projSource.getDamageAmount() / maxProjDamage) * EnergyBallLauncherEffect.maxProjectileSize;
            sprite.setWidth(size);
            sprite.setHeight(size);
            render();
        }
    }

    private void render() {
        sprite.setAdditiveBlend();
        for (int i = 0; i < 6; i++) {
            sprite.setAngle(facing + i * 60f);
            sprite.renderAtCenter(entity.getLocation().x, entity.getLocation().y);
        }
    }
}
