package unthemedweapons.fx.render;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.loading.ProjectileSpecAPI;
import unthemedweapons.combat.scripts.EnergyBallLauncherEffect;
import unthemedweapons.util.MathUtils;

public class EnergyBallRenderer extends BaseCombatLayeredRenderingPlugin {

    private final WeaponAPI weaponSource;
    private final DamagingProjectileAPI projSource;
    private final float maxProjDamage;
    private final SpriteAPI sprite, glowSprite;
    private final EnergyBallLauncherEffect plugin;
    public float facing = MathUtils.randBetween(0f, 360f);
    public float modifiedTime = 0f;
    private final float[] glowSizeMults = new float[] {1f, 1f, 1f};
    private final float[] glowAlphaMults = new float[] {1f, 1f, 1f};

    // Attached to weapon
    public EnergyBallRenderer(WeaponAPI source, EnergyBallLauncherEffect plugin) {
        weaponSource = source;
        sprite = Global.getSettings().getSprite("misc", "wpnxt_energyball");
        glowSprite = Global.getSettings().getSprite("misc", "wpnxt_glow");
        ProjectileSpecAPI projSpec = (ProjectileSpecAPI) source.getSpec().getProjectileSpec();
        sprite.setColor(projSpec.getCoreColor());
        glowSprite.setColor(projSpec.getCoreColor());
        this.plugin = plugin;
        maxProjDamage = 0f;
        projSource = null;
    }

    // Attached to projectile
    public EnergyBallRenderer(DamagingProjectileAPI source, EnergyBallLauncherEffect plugin, float maxDamage, float initialFacing, float originalTime) {
        projSource = source;
        sprite = Global.getSettings().getSprite("misc", "wpnxt_energyball");
        glowSprite = Global.getSettings().getSprite("misc", "wpnxt_glow");
        ProjectileSpecAPI projSpec = source.getProjectileSpec();
        sprite.setColor(projSpec.getCoreColor());
        glowSprite.setColor(projSpec.getCoreColor());
        this.plugin = plugin;
        maxProjDamage = maxDamage;
        weaponSource = null;
        facing = initialFacing;
        modifiedTime = originalTime;
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
        return projSource.isExpired() || !Global.getCombatEngine().isEntityInPlay(projSource);
    }

    @Override
    public void advance(float amount) {
        if (weaponSource != null) {
            modifiedTime += amount * (plugin.getProjectileSize() / EnergyBallLauncherEffect.maxProjectileSize);
            entity.getLocation().set(weaponSource.getFirePoint(0));
        }
        else {
            modifiedTime += amount * 0.5f;
            entity.getLocation().set(projSource.getLocation());
        }
        facing += 180f * amount;

        for (int i = 0; i < glowSizeMults.length; i++) {
            float modTime = MathUtils.modPositive(-1.5f* modifiedTime + 0.33f * i, 1f);
            glowSizeMults[i] = 0.5f + modTime;
            glowAlphaMults[i] = (1f - 2f * Math.abs(modTime - 0.5f));
        }
    }

    @Override
    public void render(CombatEngineLayers layer, ViewportAPI viewport) {
        if (weaponSource != null) {
            render(plugin.getProjectileSize(), 1f);
        }
        else {
            render(
                    (float) Math.sqrt(projSource.getDamageAmount() / maxProjDamage) * EnergyBallLauncherEffect.maxProjectileSize,
                    projSource.getBrightness());
        }
    }

    private void render(float size, float alphaMult) {
        if (size <= 0f) return;
        sprite.setSize(size, size);
        sprite.setAlphaMult(alphaMult) ;
        sprite.setAdditiveBlend();
        sprite.setAngle(facing);
        sprite.renderAtCenter(entity.getLocation().x, entity.getLocation().y);

        glowSprite.setAdditiveBlend();
        for (int i = 0; i < glowSizeMults.length; i++) {
            glowSprite.setAlphaMult(alphaMult * glowAlphaMults[i]);
            glowSprite.setSize(size * 2f * glowSizeMults[i], size * 2f * glowSizeMults[i]);
            glowSprite.renderAtCenter(entity.getLocation().x, entity.getLocation().y);
        }
    }
}
