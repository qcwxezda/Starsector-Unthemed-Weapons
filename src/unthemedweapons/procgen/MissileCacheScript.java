package unthemedweapons.procgen;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.PlanetAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.util.Misc;
import org.lwjgl.util.vector.Vector2f;
import unthemedweapons.util.MathUtils;

public class MissileCacheScript implements EveryFrameScript {

    private final SectorEntityToken cache;
    private final PlanetAPI star;
    private boolean revealed = false;
    private float time = 0f;
    private static final float timeToReveal = 20f;

    public MissileCacheScript(SectorEntityToken cache, PlanetAPI star) {
        this.cache = cache;
        this.star = star;
    }
    @Override
    public boolean isDone() {
        return revealed;
    }

    @Override
    public boolean runWhilePaused() {
        return false;
    }

    @Override
    public void advance(float amount) {
        CampaignFleetAPI playerFleet = Global.getSector().getPlayerFleet();
        if (playerFleet == null || playerFleet.getStarSystem() == null) return;
        // Early exit if player fleet isn't even in system
        if (!star.getStarSystem().equals(playerFleet.getStarSystem())) return;

        if (Misc.getDistance(playerFleet.getLocation(), star.getLocation()) <= star.getRadius()) {
            time += amount;
        }
        else {
            time = 0f;
        }

        if (time >= timeToReveal) {
            cache.setCircularOrbit(star, MathUtils.randBetween(0f, 360f), star.getRadius() * 0.75f, 2f);
            revealed = true;
        }
        else if (Misc.random.nextFloat() <= 0.5f*amount) {
            Vector2f randomLocation =  MathUtils.randomPointInRing(new Vector2f(), 1000000f, 1000000f);
            cache.setFixedLocation(randomLocation.x, randomLocation.y);
        }
    }
}
