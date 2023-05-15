package weaponexpansion.particles;

import com.fs.starfarer.api.combat.CombatEngineLayers;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.graphics.SpriteAPI;
import org.lwjgl.util.vector.Vector2f;
import particleengine.IEmitter;
import particleengine.ParticleData;

public class MuzzleFlashEmitter extends IEmitter {

    private final WeaponAPI weapon;

    public MuzzleFlashEmitter(WeaponAPI weapon) {
        this.weapon = weapon;
    }

    @Override
    public Vector2f getLocation() {
        return weapon.getFirePoint(0);
    }

    @Override
    public SpriteAPI getSprite() {
        return null;
    }

    @Override
    public float getXDir() {
        return 0;
    }

    @Override
    public int getBlendSourceFactor() {
        return 0;
    }

    @Override
    public int getBlendDestinationFactor() {
        return 0;
    }

    @Override
    public int getBlendFunc() {
        return 0;
    }

    @Override
    public CombatEngineLayers getLayer() {
        return null;
    }

    @Override
    public float getRenderRadius() {
        return 0;
    }

    @Override
    public ParticleData initParticle(int i) {
        return null;
    }
}
