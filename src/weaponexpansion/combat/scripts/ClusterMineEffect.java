package weaponexpansion.combat.scripts;

import com.fs.starfarer.api.combat.*;
import org.lwjgl.util.vector.Vector2f;
import weaponexpansion.ModPlugin;
import weaponexpansion.combat.plugins.Action;
import weaponexpansion.combat.plugins.ActionPlugin;

@SuppressWarnings("unused")
public class ClusterMineEffect implements OnFireEffectPlugin {

    @Override
    public void onFire(final DamagingProjectileAPI proj, WeaponAPI weapon, final CombatEngineAPI engine) {
        if (!(proj instanceof MissileAPI)) {
            return;
        }

        final MissileAIPlugin missileAI = ((MissileAPI) proj).getUnwrappedMissileAI();
        if (!(missileAI instanceof GuidedMissileAI)) {
            return;
        }

        // In order to force the MIRV to split after two seconds no matter what, spawn a dummy projectile
        // on it and set that dummy projectile as the target. This will cause the MIRV AI to make it split.
        ActionPlugin plugin = (ActionPlugin) engine.getCustomData().get(ActionPlugin.customDataKey);
        plugin.queueAction(new Action() {
            @Override
            public void perform() {
                CombatEntityAPI dummyProj = engine.spawnProjectile(null, null, ModPlugin.dummyProjWeapon, proj.getLocation(), proj.getFacing(), new Vector2f());
                // missileAI.getTarget returns null if target.getOwner() == missile.getOwner()
                dummyProj.setOwner(100);
                ((GuidedMissileAI) missileAI).setTarget(dummyProj);
            }
        }, 1f); // delay should match with the min split time in clustermine_shot.proj
    }
}
