package weaponexpansion.util;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseCombatLayeredRenderingPlugin;
import com.fs.starfarer.api.combat.CombatEngineLayers;
import com.fs.starfarer.api.combat.ViewportAPI;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.util.FaderUtil;
import org.lwjgl.opengl.GL14;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;

public class ExplosionRingRenderer extends BaseCombatLayeredRenderingPlugin {

    private final float minRadius, maxRadius;
    private final SpriteAPI sprite;
    private final FaderUtil fader;
    private final Color color;

    private static final String spriteName = "wpnxt_explosion_ring";

    public ExplosionRingRenderer(float minRadius, float maxRadius, float duration, Color color) {
        this.minRadius = minRadius;
        this.maxRadius = maxRadius;
        this.color = color;
        sprite = Global.getSettings().getSprite("misc", spriteName);
        fader = new FaderUtil(1f, duration);
        fader.fadeOut();
    }

    @Override
    public float getRenderRadius() {
        return 500f + maxRadius;
    }

    @Override
    public boolean isExpired() {
        return fader.isFadedOut();
    }

    @Override
    public void advance(float amount) {
        fader.advance(amount);
    }

    @Override
    public void render(CombatEngineLayers layer, ViewportAPI viewport) {
        float radius = (minRadius + (maxRadius - minRadius) * (1f - fader.getBrightness()));
        sprite.setWidth(2 * radius);
        sprite.setHeight(2 * radius);
        sprite.setColor(color);
        sprite.setAlphaMult(fader.getBrightness());
        sprite.renderAtCenter(entity.getLocation().x, entity.getLocation().y);
    }
}
