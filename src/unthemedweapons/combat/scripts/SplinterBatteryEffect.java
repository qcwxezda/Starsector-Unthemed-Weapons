package unthemedweapons.combat.scripts;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import unthemedweapons.fx.render.GlowRenderer;

@SuppressWarnings("unused")
public class SplinterBatteryEffect implements EveryFrameWeaponEffectPlugin, OnFireEffectPlugin, WeaponEffectPluginWithInit {

    float bonusFireRate = 0f;
    static final float maxBonusFireRate = 1f;
    static final float timeToMaxBonus = 10f;
    float timeSinceLastFired = 0f;
    static final float timeUntilDecay = 2f;
    static final float decayRate = 0.4f;
    GlowRenderer heatGlowRenderer;

    @Override
    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
        timeSinceLastFired += amount;
        if (timeSinceLastFired > timeUntilDecay) {
            bonusFireRate = Math.max(0f, bonusFireRate - decayRate * amount);
        }
        heatGlowRenderer.setAlpha(bonusFireRate / maxBonusFireRate);
    }

    @Override
    public void onFire(DamagingProjectileAPI proj, WeaponAPI weapon, CombatEngineAPI engine) {
        float origCooldown = weapon.getCooldown();
        bonusFireRate += maxBonusFireRate * Math.min(timeSinceLastFired, origCooldown) / timeToMaxBonus;
        bonusFireRate = Math.min(bonusFireRate, maxBonusFireRate);
        weapon.setRemainingCooldownTo(origCooldown / (1 + bonusFireRate));
        weapon.setRefireDelay(origCooldown);
        timeSinceLastFired = 0f;
    }

    @Override
    public void init(WeaponAPI weapon) {
        heatGlowRenderer = new GlowRenderer(weapon, true);
        if (Global.getCombatEngine() != null) {
            Global.getCombatEngine().addLayeredRenderingPlugin(heatGlowRenderer);
        }
    }
}
