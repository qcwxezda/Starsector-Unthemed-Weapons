package unthemedweapons.combat.scripts;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import unthemedweapons.fx.render.GlowRenderer;

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
        this.weapon = weapon;
        boolean hasBarrel = weapon.getBarrelSpriteAPI() != null;
        heatGlowRenderer = new GlowRenderer(weapon, hasBarrel);

        if (Global.getCombatEngine() != null) {
            Global.getCombatEngine().addLayeredRenderingPlugin(heatGlowRenderer);
        }
    }

    public float getDecayAmountPerSecond() {
        return 1f / (3f * weapon.getCooldown());
    }

    public float getGlowAmountPerShot() {
        return 0.5f / weapon.getSpec().getBurstSize();
    }
}
