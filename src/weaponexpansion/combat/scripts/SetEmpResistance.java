package weaponexpansion.combat.scripts;

import com.fs.starfarer.api.combat.*;

@SuppressWarnings("unused")
public class SetEmpResistance implements OnFireEffectPlugin {

    private static final String energyTorpedo = "wpnxt_energytorpedo_shot";
    private static final String energyTorpedoLarge = "wpnxt_energytorpedolarge_shot";

    @Override
    public void onFire(DamagingProjectileAPI proj, WeaponAPI weapon, CombatEngineAPI engine) {
        if (energyTorpedo.equals(proj.getProjectileSpecId())) {
            ((MissileAPI) proj).setEmpResistance(5);
        }
        else if (energyTorpedoLarge.equals(proj.getProjectileSpecId())) {
            ((MissileAPI) proj).setEmpResistance(10);
        }
    }
}
