package weaponexpansion.combat.plugins;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;

public class EmpArcAction implements Action {

    private final Vector2f from, to;
    private final CombatEntityAPI fromAnchor, toAnchor;
    private final float thickness;
    private final Color fringe, core;

    public EmpArcAction(Vector2f from, CombatEntityAPI fromAnchor, Vector2f to, CombatEntityAPI toAnchor, float thickness, Color fringe, Color core) {
        this.from = from;
        this.to = to;
        this.fromAnchor = fromAnchor;
        this.toAnchor = toAnchor;
        this.thickness = thickness;
        this.fringe = fringe;
        this.core = core;
    }

    @Override
    public void perform() {
        Global.getCombatEngine().spawnEmpArcVisual(from, fromAnchor, to, toAnchor, thickness, fringe, core);
    }
}
