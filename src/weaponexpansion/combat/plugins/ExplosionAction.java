package weaponexpansion.combat.plugins;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.loading.DamagingExplosionSpec;
import org.lwjgl.util.vector.Vector2f;

public class ExplosionAction implements Action {

    private final DamagingExplosionSpec spec;
    private final Vector2f loc;
    private final ShipAPI owner;

    public ExplosionAction(DamagingExplosionSpec spec, ShipAPI owner, Vector2f loc) {
        this.spec = spec;
        this.loc = loc;
        this.owner = owner;
    }

    @Override
    public void perform() {
        Global.getCombatEngine().spawnDamagingExplosion(spec, owner, loc);
    }
}
