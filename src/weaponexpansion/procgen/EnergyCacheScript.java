package weaponexpansion.procgen;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.impl.campaign.terrain.PulsarBeamTerrainPlugin;
import org.lwjgl.util.vector.Vector2f;

import java.util.List;

public class EnergyCacheScript implements EveryFrameScript {

    private final SectorEntityToken cache;
    private final List<PulsarBeamTerrainPlugin> pulsarPlugins;
    private final Vector2f originalLocation;
    private boolean isInOriginalLocation = true;

    public EnergyCacheScript(SectorEntityToken cache, List<PulsarBeamTerrainPlugin> pulsarPlugins) {
        this.cache = cache;
        this.pulsarPlugins = pulsarPlugins;
        originalLocation = new Vector2f(cache.getLocation());
    }

    @Override
    public boolean isDone() {
        return false;
    }

    @Override
    public boolean runWhilePaused() {
        return false;
    }

    @Override
    public void advance(float amount) {
        // Don't need to do anything if player fleet is not in the system
        CampaignFleetAPI playerFleet = Global.getSector().getPlayerFleet();
        if (playerFleet == null || playerFleet.getStarSystem() == null) return;
        if (!playerFleet.getStarSystem().equals(cache.getStarSystem())) return;

        boolean isInBeam = false;
        for (PulsarBeamTerrainPlugin plugin : pulsarPlugins) {
            if (plugin.getIntensityAtPoint(originalLocation) >= 0.5f) {
                isInBeam = true;
                break;
            }
        }
        if (isInBeam && !isInOriginalLocation) {
            cache.setFixedLocation(originalLocation.x, originalLocation.y);
            isInOriginalLocation = true;
        }
        else if (!isInBeam && isInOriginalLocation) {
            cache.setFixedLocation(1000000f, 1000000f);
            // Clear course if target is the (now "invisible") cache
            SectorEntityToken courseTarget = Global.getSector().getUIData().getCourseTarget();
            if (cache.equals(courseTarget)) {
                Global.getSector().getCampaignUI().clearLaidInCourse();
            }
            isInOriginalLocation = false;
        }
    }
}
