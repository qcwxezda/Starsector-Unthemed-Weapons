package weaponexpansion.combat.scripts;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import weaponexpansion.util.GlowRenderer;

public class GlowOnFirePlugin implements OnFireEffectPlugin, EveryFrameWeaponEffectPlugin, WeaponEffectPluginWithInit {

    GlowRenderer heatGlowRenderer;
    WeaponAPI weapon;

    @Override
    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
        heatGlowRenderer.modifyAlpha(-getDecayAmountPerSecond() * amount);
    }

    @Override
    public void onFire(DamagingProjectileAPI proj, WeaponAPI weapon, CombatEngineAPI engine){
        heatGlowRenderer.modifyAlpha(getGlowAmountPerShot());
    }

    @Override
    public void init(WeaponAPI weapon) {
        boolean hasBarrel = weapon.getBarrelSpriteAPI() != null;
        heatGlowRenderer = new GlowRenderer(weapon, hasBarrel);
        Global.getCombatEngine().addLayeredRenderingPlugin(heatGlowRenderer);
        this.weapon = weapon;
    }

    public float getDecayAmountPerSecond() {
        return 1f / (3f * weapon.getCooldown());
    }

    public float getGlowAmountPerShot() {
        return 0.5f / weapon.getSpec().getBurstSize();
    }
}
