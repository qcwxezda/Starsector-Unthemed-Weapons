package weaponexpansion.util;

import com.fs.starfarer.api.combat.CombatEngineLayers;
import com.fs.starfarer.api.combat.ViewportAPI;
import com.fs.starfarer.api.impl.combat.CombatEntityPluginWithParticles;
import com.fs.starfarer.api.util.Misc;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;

public class ExplosionRenderer extends CombatEntityPluginWithParticles {

    private final float duration;
    private float elapsed;
    private final static Color explosionColor = new Color(100, 200, 200, 175);
    private final static Color smokeColor = new Color(38, 75, 75, 125);
    private final static float drag = 2f;
    private final static float initVel = 100f;

    public ExplosionRenderer(float duration) {
        super();
        this.duration = duration;
        setSpriteSheetKey("wpnxt_explosion_sheet");

        float rFrac = 0.2f;
        for (int i = 0; i < 20; i++) {
            addParticle(50f, 0f, duration, 1f, 12f, 30f, jitterColor(explosionColor, 0.15f));
            randomizePrevParticleLocation(10f);
            Vector2f vel = new Vector2f();
            prev.offset.normalise(vel);
            vel.scale(initVel * Utils.randBetween(1-rFrac, 1+rFrac));
            Vector2f.add(prev.vel, vel, prev.vel);
            prev.maxDur *= Utils.randBetween(1-rFrac, 1+rFrac);
            prev.scaleIncreaseRate = 2f;
            prev.baseSize *= Utils.randBetween(1-rFrac, 1+rFrac);
        }

        setSpriteSheetKey("nebula_particles");
        for (int i = 0; i < 12; i++) {
            addParticle(100f, 0f, 4f * duration, 1.1f, 50f, 45f, jitterColor(smokeColor, 0.1f));
            randomizePrevParticleLocation(10f);
            prev.maxDur *= Utils.randBetween(1-rFrac, 1+rFrac);
            prev.scaleIncreaseRate = 1.75f;
            prev.baseSize *= Utils.randBetween(1-rFrac, 1+rFrac);
        }
    }

    private Color jitterColor(Color orig, float frac) {
        int newRed = (int) Utils.clamp((orig.getRed() * Utils.randBetween(1-frac, 1+frac)), 0f, 255.99f);
        int newGreen = (int) Utils.clamp((orig.getGreen() * Utils.randBetween(1-frac, 1+frac)), 0f, 255.99f);
        int newBlue = (int) Utils.clamp((orig.getBlue() * Utils.randBetween(1-frac, 1+frac)), 0f, 255.99f);
        int newAlpha = (int) Utils.clamp((orig.getAlpha() * Utils.randBetween(1-frac, 1+frac)), 0f, 255.99f);
        return new Color(newRed, newGreen, newBlue, newAlpha);
    }

    @Override
    public void advance(float amount) {
        super.advance(amount);
        elapsed += amount;

        // particle drag
        for (ParticleData part : particles) {
            Vector2f velDrag = new Vector2f(part.vel);
            Utils.safeNormalize(velDrag);
            velDrag.scale(drag*amount*initVel);
            Vector2f.sub(part.vel, velDrag, part.vel);
        }
    }

    @Override
    public void render(CombatEngineLayers layer, ViewportAPI viewport) {
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        super.render(layer, viewport, null);
        GL14.glBlendEquation(GL14.GL_FUNC_ADD);
    }

    @Override
    public boolean isExpired() {
        return elapsed >= 4f * duration;
    }

    @Override
    public float getRenderRadius() {
        return 500f;
    }

    @Override
    protected float getGlobalAlphaMult() {
        return (float) Math.sqrt(elapsed / duration);
    }
}
