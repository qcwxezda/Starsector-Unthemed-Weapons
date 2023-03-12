package untitled.combat.scripts;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.impl.combat.CombatEntityPluginWithParticles;
import com.fs.starfarer.api.loading.ProjectileSpawnType;
import com.fs.starfarer.api.loading.ProjectileSpecAPI;
import com.fs.starfarer.api.util.FaderUtil;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.Misc;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;

public class ChromaticBlasterEffect extends CombatEntityPluginWithParticles implements EveryFrameWeaponEffectPlugin, OnFireEffectPlugin {

    @Override
    public void advance(float amt, CombatEngineAPI engine, WeaponAPI weapon) {
    }

    @Override
    public void onFire(DamagingProjectileAPI proj, WeaponAPI weapon, CombatEngineAPI engine) {
        ChromaticBlasterOverlay projEffect = new ChromaticBlasterOverlay(proj, engine);
        engine.addLayeredRenderingPlugin(projEffect);
    }

    private static class ChromaticBlasterOverlay extends CombatEntityPluginWithParticles {
        private final DamagingProjectileAPI proj;
        private final CombatEngineAPI engine;
        private final IntervalUtil interval = new IntervalUtil(0.4f, 0.4f);
        private float totalElapsed = 0f;
        private boolean firstFrame = true;
        private static final float angVel = (float) (Math.random() * 300f - 150f);

        private final Color kineticColor = new Color(255, 247, 154);
        private final Color energyColor = new Color(150, 226, 241);
        private final Color heColor = new Color(246, 173, 173);
        private static final float maxSize = 200f;
        private static final float growthRate = 100f;
        private float size = 40f;
        private static final int nProjectiles = 12;
        private final ProjectileSpecAPI spawnSpec = (ProjectileSpecAPI) Global.getSettings().getWeaponSpec("wip_chromaticblaster_spawn").getProjectileSpec();

        private ChromaticBlasterOverlay(DamagingProjectileAPI proj, CombatEngineAPI engine) {
            this.proj = proj;
            this.engine = engine;
            setSpriteSheetKey("wip_chromaticblaster_texture");
        }

        @Override
        public void advance(float amount) {
            if (engine.isPaused() || proj == null) {
                return;
            }

            entity.getLocation().set(proj.getLocation());

            super.advance(amount);
            interval.advance(amount);
            totalElapsed += amount / 2;

            if (size < maxSize) {
                size += growthRate * amount;
                size = Math.min(size, maxSize);

                for (ParticleData part : particles) {
                    part.baseSize = size;
                }
            }

            float modulo = totalElapsed % 3;
            Color color;
            if (modulo <= 1) {
                color = interpolate(energyColor, kineticColor, modulo);
            } else if (modulo <= 2) {
                color = interpolate(kineticColor, heColor, modulo - 1);
            } else {
                color = interpolate(heColor, energyColor, modulo - 2);
            }

            if (firstFrame || (interval.intervalElapsed() && !isProjectileExpired(proj))) {
                addParticle(size, 0.3f, 0.3f, 1f, 0f, 0f, color);
                prev.scaleIncreaseRate = 0f;
                prev.turnDir = angVel;
                firstFrame = false;
            }

            if (isProjectileExpired(proj)) {
                return;
            }

            // Check for nearby ships to activate the payload
            float radius = size / 2;
            for (ShipAPI ship : engine.getShips()) {
                if (ship.getOwner() == proj.getOwner() || !ship.isAlive()) {
                    continue;
                }
                if (Misc.getDistance(ship.getLocation(), proj.getLocation()) <= ship.getCollisionRadius() + radius) {
                    for (int i = 0; i < nProjectiles; i++) {
                        float dist = (float) Math.random() * radius;
                        float angle = (float) Math.random() * 2 * (float) Math.PI;
                        Vector2f offset = new Vector2f(dist * (float) Math.cos(angle), dist * (float) Math.sin(angle));
                        Vector2f spawnLocation = new Vector2f(proj.getLocation().x + offset.x, proj.getLocation().y + offset.y);
                        DamageType type = DamageType.ENERGY;
                        Color spawnColor = energyColor;
                        if (modulo < 1) {
                            if (modulo > Misc.random.nextFloat()) {
                                type = DamageType.KINETIC;
                                spawnColor = kineticColor;
                            }
                        } else if (modulo < 2) {
                            type = DamageType.KINETIC;
                            spawnColor = kineticColor;
                            if (modulo - 1 > Misc.random.nextFloat()) {
                                type = DamageType.HIGH_EXPLOSIVE;
                                spawnColor = heColor;
                            }
                        } else {
                            type = DamageType.HIGH_EXPLOSIVE;
                            spawnColor = heColor;
                            if (modulo - 2 > Misc.random.nextFloat()) {
                                type = DamageType.ENERGY;
                                spawnColor = kineticColor;
                            }
                        }
                        spawnSpec.setFringeColor(spawnColor);
                        spawnSpec.setGlowColor(new Color(spawnColor.getRed(), spawnColor.getGreen(), spawnColor.getBlue(), 75));
                        spawnSpec.getDamage().setType(type);
                        engine.spawnProjectile(
                                proj.getSource(),
                                proj.getWeapon(),
                                "wip_chromaticblaster_spawn",
                                spawnLocation,
                                Misc.getAngleInDegrees(proj.getLocation(), ship.getLocation()) + (float) (Math.random() * 30f - 15f),
                                new Vector2f(0f, 0f));
                    }
                    engine.removeEntity(proj);
                    break;
                }
            }
        }

        @Override
        public void render(CombatEngineLayers layer, ViewportAPI viewport) {
            super.render(layer, viewport, null);
        }

        @Override
        public float getRenderRadius() {
            return 500.0F;
        }

        @Override
        protected float getGlobalAlphaMult() {
            return this.proj != null && this.proj.isFading() ? this.proj.getBrightness() : super.getGlobalAlphaMult();
        }

        private boolean isProjectileExpired(DamagingProjectileAPI proj) {
            return proj.isExpired() || proj.didDamage() || !Global.getCombatEngine().isEntityInPlay(proj);
        }

        @Override
        public boolean isExpired() {
            return super.isExpired() && isProjectileExpired(proj);
        }

        private Color interpolate(Color c1, Color c2, float t) {
            int red = (int) ((1-t) * c1.getRed() + t * c2.getRed());
            int green = (int) ((1-t) * c1.getGreen() + t * c2.getGreen());
            int blue = (int) ((1-t) * c1.getBlue() + t * c2.getBlue());

            return new Color(red, green, blue);
        }
    }
}