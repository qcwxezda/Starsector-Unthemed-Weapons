package weaponexpansion.combat.plugins;

import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.loading.DamagingExplosionSpec;
import org.lwjgl.util.vector.Vector2f;

import java.util.List;
import java.util.PriorityQueue;
import java.util.Queue;

public class CombatPlugin extends BaseEveryFrameCombatPlugin {

    private float currentTime;
    private CombatEngineAPI engine;
    public final static String customDataKey = "wpnxt_PluginKey";
    private final Queue<ActionItem> actionList = new PriorityQueue<>();

    @Override
    public void init(CombatEngineAPI engine) {
        this.engine = engine;
        currentTime = 0f;

        engine.getCustomData().put(customDataKey, this);
    }

    @Override
    public void advance(float amount, List<InputEventAPI> events) {
        if (engine == null || engine.isPaused()) return;

        ActionItem firstItem;
        while ((firstItem = actionList.peek()) != null && firstItem.timeToPerform <= currentTime) {
            firstItem.action.perform();
            actionList.poll();
        }

        currentTime += amount;
    }

    public void queueAction(Action action, float delay) {
        actionList.add(new ActionItem(action, currentTime + delay));
    }

    public static class ActionItem implements Comparable<ActionItem> {
        Action action;
        float timeToPerform;

        public ActionItem(Action action, float timeToPerform) {
            this.action = action;
            this.timeToPerform = timeToPerform;
        }

        @Override
        public int compareTo(ActionItem other) {
            return Float.compare(timeToPerform, other.timeToPerform);
        }
    }
}
