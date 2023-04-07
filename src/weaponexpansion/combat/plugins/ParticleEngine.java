package weaponexpansion.combat.plugins;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.ViewportAPI;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.util.Misc;
import org.apache.log4j.Logger;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.*;
import org.lwjgl.util.vector.Vector2f;
import weaponexpansion.shaders.ParticleShader2;
import weaponexpansion.util.Utils;

import java.nio.FloatBuffer;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public class ParticleEngine extends BaseEveryFrameCombatPlugin {

    private CombatEngineAPI engine;
    private float currentTime;
    public static final String customDataKey = "wpnxt_ParticlesPlugin";

    private final List<Cluster> particleClusters = new LinkedList<>();
    private static final Logger log = Global.getLogger(ParticleEngine.class);

    @Override
    public void init(CombatEngineAPI engine) {
        this.engine = engine;
        currentTime = 0f;

        engine.getCustomData().put(customDataKey, this);
    }

    @Override
    public void advance(float amount, List<InputEventAPI> events) {
        if (engine == null || engine.isPaused()) {
            return;
        }

        currentTime += amount;
    }

    @Override
    public void renderInWorldCoords(ViewportAPI viewport) {
        GL20.glUseProgram(ParticleShader2.programId);
        GL11.glEnable(GL11.GL_BLEND);
        Iterator<Cluster> itr = particleClusters.listIterator();
        while (itr.hasNext()) {
            Cluster cluster = itr.next();

            cluster.sprite.bindTexture();
            GL11.glBlendFunc(cluster.sfactor, cluster.dfactor);
            GL30.glBindVertexArray(cluster.vao);
            GL20.glUniformMatrix4(ParticleShader2.projectionLoc, true, Utils.getProjectionMatrix(viewport));
            GL20.glUniform1f(ParticleShader2.timeLoc, currentTime - cluster.startTime);
            GL31.glDrawArraysInstanced(GL11.GL_TRIANGLE_STRIP, 0, 4, cluster.count);
            GL30.glBindVertexArray(0);

            if (cluster.startTime + cluster.maxLife <= currentTime) {
                // Delete this cluster and free up the space used
                GL15.glDeleteBuffers(cluster.vbo);
                GL30.glDeleteVertexArrays(cluster.vao);
                itr.remove();
            }
        }
        GL11.glDisable(GL11.GL_BLEND);
        GL20.glUseProgram(0);
    }

    public static Cluster makeParticleCluster(int count, SpriteAPI sprite, float minLife, float maxLife) {
        return new Cluster(count, GL11.GL_SRC_ALPHA, GL11.GL_ONE, sprite, minLife, maxLife);
    }

    public static ParticleEngine getInstance() {
        CombatEngineAPI engine = Global.getCombatEngine();
        if (engine == null) {
            return null;
        }

        return (ParticleEngine) engine.getCustomData().get(customDataKey);
    }

    /** A cluster of particles. Each cluster is rendered with a single draw call, so large clusters of particles
     *  generated periodically are favored. */
    public static class Cluster {
        private final int sfactor, dfactor, count;
        private int vao = -1, vbo = -1;
        private final float minLife, maxLife;
        private final SpriteAPI sprite;
        private boolean isActive = false;
        private float startTime = 0f;

        private Vector2f position = new Vector2f(), velocity = new Vector2f(), acceleration = new Vector2f();

        /** Random point in circle of radius positionSpread is added to position. Same for velocity and acceleration. */
        private float positionSpread = 0f, velocitySpread = 0f, accelerationSpread = 0f;
        /** Alpha is angular acceleration, NOT image alpha */
        private float minTheta = 0f, maxTheta = 0f, minW = 0f, maxW = 0f, minAlpha = 0f, maxAlpha = 0f;
        private float minSize = 25f, maxSize = 25f, minSizeV = 0f, maxSizeV = 0f, minSizeA = 0f, maxSizeA = 0f;
        private float[] inColor = new float[] {1f, 1f, 1f, 1f}, outColor = new float[] {0f, 0f, 0f, 0f};
        private float colorSpread = 0f;
        /** Velocity and acceleration amounts pointed outwards. No effect if positionSpread is 0. */
        private float minRadialVelocity = 0f, maxRadialVelocity = 0f, minRadialAcceleration = 0f, maxRadialAcceleration = 0f;
        private float minRadialTheta = 0f, maxRadialTheta = 0f, minRadialW = 0f, maxRadialW = 0f, minRadialAlpha = 0f, maxRadialAlpha = 0f;
        private float minSinXAmplitude = 0f, maxSinXAmplitude = 0f, minSinXFrequency = 0f, maxSinXFrequency = 0f, minSinXPhase = 0f, maxSinXPhase = 0f;
        private float minSinYAmplitude = 0f, maxSinYAmplitude = 0f, minSinYFrequency = 0f, maxSinYFrequency = 0f, minSinYPhase = 0f, maxSinYPhase = 0f;

        private Cluster(
                int count,
                int sfactor,
                int dfactor,
                SpriteAPI sprite,
                float minLife,
                float maxLife) {
            this.count = count;
            this.sfactor = sfactor; // blend mode
            this.dfactor = dfactor; // blend mode
            this.sprite = sprite;
            this.minLife = minLife;
            this.maxLife = maxLife;
        }

        public boolean wasGenerated() {
            return isActive;
        }

        public void setPositionData(Vector2f position, Vector2f velocity, Vector2f acceleration) {
            this.position = position;
            this.velocity = velocity;
            this.acceleration = acceleration;
        }

        public void setPositionSpreadData(float positionSpread, float velocitySpread, float accelerationSpread) {
            this.positionSpread = positionSpread;
            this.velocitySpread = velocitySpread;
            this.accelerationSpread = accelerationSpread;
        }

        public void setAngleData(float minTheta, float maxTheta, float minW, float maxW, float minAlpha, float maxAlpha) {
            this.minTheta = minTheta;
            this.maxTheta = maxTheta;
            this.minW = minW;
            this.maxW = maxW;
            this.minAlpha = minAlpha;
            this.maxAlpha = maxAlpha;
        }

        public void setSizeData(float minSize, float maxSize, float minSizeV, float maxSizeV, float minSizeA, float maxSizeA) {
            this.minSize = minSize;
            this.maxSize = maxSize;
            this.minSizeV = minSizeV;
            this.maxSizeV = maxSizeV;
            this.minSizeA = minSizeA;
            this.maxSizeA = maxSizeA;
        }

        /**
         * @param inColor 4-element float array with RGBA components between 0 and 1. Particles' color at start of life.
         * @param outColor 4-element float array with RGBA components between 0 and 1. Particles' color at end of life.
         * @param colorSpread Perturbs each channel by a random number in [-colorSpread/2, colorSpread/2]. Resulting
         *                    components outside [0,1] are clamped by the vertex shader.
         */
        public void setColorData(float[] inColor, float[] outColor, float colorSpread) {
            this.inColor = inColor;
            this.outColor = outColor;
            this.colorSpread = colorSpread;
        }

        public void setRadialVelocity(float minRadialVelocity, float maxRadialVelocity) {
            this.minRadialVelocity = minRadialVelocity;
            this.maxRadialVelocity = maxRadialVelocity;
        }

        public void setRadialAcceleration(float minRadialAcceleration, float maxRadialAcceleration) {
            this.minRadialAcceleration = minRadialAcceleration;
            this.maxRadialAcceleration = maxRadialAcceleration;
        }

        public void setRadialRevolution(float minRadialTheta, float maxRadialTheta, float minRadialW, float maxRadialW, float minRadialAlpha, float maxRadialAlpha) {
            this.minRadialTheta = minRadialTheta;
            this.maxRadialTheta = maxRadialTheta;
            this.minRadialW = minRadialW;
            this.maxRadialW = maxRadialW;
            this.minRadialAlpha = minRadialAlpha;
            this.maxRadialAlpha = maxRadialAlpha;
        }

        /**
         * @param minSinXAmplitude Minimum amplitude of periodic motion along global x-axis, in world-coord units.
         * @param maxSinXAmplitude Maximum amplitude of periodic motion along global x-axis, in world-coord units.
         * @param minSinXFrequency Minimum number of complete cycles per second along global x-axis.
         * @param maxSinXFrequency Maximum of complete cycles per second along global x-axis.
         * @param minSinXPhase Minimum initial phase of periodic motion along global x-axis, in degrees.
         * @param maxSinXPhase Maximum initial phase of periodic motion along global x-axis, in degrees.
         */
        public void setSinusoidalMotionX(float minSinXAmplitude, float maxSinXAmplitude, float minSinXFrequency, float maxSinXFrequency, float minSinXPhase, float maxSinXPhase) {
            this.minSinXAmplitude = minSinXAmplitude;
            this.maxSinXAmplitude = maxSinXAmplitude;
            this.minSinXFrequency = minSinXFrequency;
            this.maxSinXFrequency = maxSinXFrequency;
            this.minSinXPhase = minSinXPhase;
            this.maxSinXPhase = maxSinXPhase;
        }

        /**
         * @param minSinYAmplitude Minimum amplitude of periodic motion along global y-axis, in world-coord units.
         * @param maxSinYAmplitude Maximum amplitude of periodic motion along global y-axis, in world-coord units.
         * @param minSinYFrequency Minimum number of complete cycles per second along global y-axis.
         * @param maxSinYFrequency Maximum of complete cycles per second along global y-axis.
         * @param minSinYPhase Minimum initial phase of periodic motion along global y-axis, in degrees.
         * @param maxSinYPhase Maximum initial phase of periodic motion along global y-axis, in degrees.
         */
        public void setSinusoidalMotionY(float minSinYAmplitude, float maxSinYAmplitude, float minSinYFrequency, float maxSinYFrequency, float minSinYPhase, float maxSinYPhase) {
            this.minSinYAmplitude = minSinYAmplitude;
            this.maxSinYAmplitude = maxSinYAmplitude;
            this.minSinYFrequency = minSinYFrequency;
            this.maxSinYFrequency = maxSinYFrequency;
            this.minSinYPhase = minSinYPhase;
            this.maxSinYPhase = maxSinYPhase;
        }

        /** Actually allocates the memory for the particles and allows them to be rendered.
         *
         * @return whether the particles were successfully generated
         */
        public boolean generate() {

            if (isActive) {
                throw new RuntimeException("Attempted to allocate an already active particle cluster");
            }

            ParticleEngine particleEngine = ParticleEngine.getInstance();
            if (particleEngine == null) {
                log.warn("Couldn't generate particles because the ParticleEngine couldn't be found.");
                return false;
            }

            particleEngine.particleClusters.add(this);
            startTime = particleEngine.currentTime;
            isActive = true;

            vao = GL30.glGenVertexArrays();
            GL30.glBindVertexArray(vao);

            vbo = GL15.glGenBuffers();
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vbo);

            GL20.glEnableVertexAttribArray(0); // position
            GL20.glEnableVertexAttribArray(1); // velocity and acceleration
            GL20.glEnableVertexAttribArray(2); // sinusoidal motion in x
            GL20.glEnableVertexAttribArray(3); // sinusoidal motion in y
            GL20.glEnableVertexAttribArray(4); // angular data
            GL20.glEnableVertexAttribArray(5); // radial turning data
            GL20.glEnableVertexAttribArray(6); // size data
            GL20.glEnableVertexAttribArray(7); // starting color
            GL20.glEnableVertexAttribArray(8); // ending color
            GL20.glEnableVertexAttribArray(9); // lifetime

            int elems = 30, stride = elems*4;
            GL20.glVertexAttribPointer(0, 2, GL11.GL_FLOAT, false, stride, 0);
            GL20.glVertexAttribPointer(1, 4, GL11.GL_FLOAT, false, stride, 8);
            GL20.glVertexAttribPointer(2, 3, GL11.GL_FLOAT, false, stride, 24);
            GL20.glVertexAttribPointer(3, 3, GL11.GL_FLOAT, false, stride, 36);
            GL20.glVertexAttribPointer(4, 3, GL11.GL_FLOAT, false, stride, 48);
            GL20.glVertexAttribPointer(5, 3, GL11.GL_FLOAT, false, stride, 60);
            GL20.glVertexAttribPointer(6, 3, GL11.GL_FLOAT, false, stride, 72);
            GL20.glVertexAttribPointer(7, 4, GL11.GL_FLOAT, false, stride, 84);
            GL20.glVertexAttribPointer(8, 4, GL11.GL_FLOAT, false, stride, 100);
            GL20.glVertexAttribPointer(9, 1, GL11.GL_FLOAT, false, stride, 116);

            GL33.glVertexAttribDivisor(0, 1);
            GL33.glVertexAttribDivisor(1, 1);
            GL33.glVertexAttribDivisor(2, 1);
            GL33.glVertexAttribDivisor(3, 1);
            GL33.glVertexAttribDivisor(4, 1);
            GL33.glVertexAttribDivisor(5, 1);
            GL33.glVertexAttribDivisor(6, 1);
            GL33.glVertexAttribDivisor(7, 1);
            GL33.glVertexAttribDivisor(8, 1);
            GL33.glVertexAttribDivisor(9, 1);

            FloatBuffer buffer = BufferUtils.createFloatBuffer(count*elems);

            //float[] bufferData = new float[count*elems];
            float twoPi = 2f * (float)Math.PI;
            for (int i = 0; i < count; i++) {
                Vector2f newPos = new Vector2f();
                Vector2f.add(position, Utils.randomPointInCircle(newPos, positionSpread), newPos);
                Vector2f radialDir = Misc.getDiff(newPos, position);

                Vector2f newVel = new Vector2f();
                Vector2f.add(velocity, Utils.randomPointInCircle(newVel, velocitySpread), newVel);
                if (radialDir.lengthSquared() > 0f) {
                    radialDir.normalise();
                    radialDir.scale(Utils.randBetween(minRadialVelocity, maxRadialVelocity));
                    Vector2f.add(newVel, radialDir, newVel);
                }

                Vector2f newAcc = new Vector2f();
                Vector2f.add(acceleration, Utils.randomPointInCircle(newAcc, accelerationSpread), newAcc);
                if (radialDir.lengthSquared() > 0f) {
                    radialDir.normalise();
                    radialDir.scale(Utils.randBetween(minRadialAcceleration, maxRadialAcceleration));
                    Vector2f.add(newAcc, radialDir, newAcc);
                }

                float newSinXAmplitude = Utils.randBetween(minSinXAmplitude, maxSinXAmplitude);
                float newSinXFrequency = Utils.randBetween(minSinXFrequency, maxSinXFrequency) * twoPi;
                float newSinXPhase = Utils.randBetween(minSinXPhase, maxSinXPhase) * Misc.RAD_PER_DEG;
                float newSinYAmplitude = Utils.randBetween(minSinYAmplitude, maxSinYAmplitude);
                float newSinYFrequency = Utils.randBetween(minSinYFrequency, maxSinYFrequency) * twoPi;
                float newSinYPhase = Utils.randBetween(minSinYPhase, maxSinYPhase) * Misc.RAD_PER_DEG;

                float newTheta = Utils.randBetween(minTheta, maxTheta) * Misc.RAD_PER_DEG;
                float newW = Utils.randBetween(minW, maxW) * Misc.RAD_PER_DEG;
                float newAlpha = Utils.randBetween(minAlpha, maxAlpha) * Misc.RAD_PER_DEG;

                float newRadialTheta = Utils.randBetween(minRadialTheta, maxRadialTheta) * Misc.RAD_PER_DEG;
                float newRadialW = Utils.randBetween(minRadialW, maxRadialW) * Misc.RAD_PER_DEG;
                float newRadialAlpha = Utils.randBetween(minRadialAlpha, maxRadialAlpha) * Misc.RAD_PER_DEG;

                float newSize = Utils.randBetween(minSize, maxSize);
                float newSizeV = Utils.randBetween(minSizeV, maxSizeV);
                float newSizeA = Utils.randBetween(minSizeA, maxSizeA);

                float[] newInColor = new float[] {
                        inColor[0] + Utils.randBetween(-colorSpread/2f, colorSpread/2f),
                        inColor[1] + Utils.randBetween(-colorSpread/2f, colorSpread/2f),
                        inColor[2] + Utils.randBetween(-colorSpread/2f, colorSpread/2f),
                        inColor[3] + Utils.randBetween(-colorSpread/2f, colorSpread/2f),
                };

                float[] newOutColor = new float[] {
                        outColor[0] + Utils.randBetween(-colorSpread/2f, colorSpread/2f),
                        outColor[1] + Utils.randBetween(-colorSpread/2f, colorSpread/2f),
                        outColor[2] + Utils.randBetween(-colorSpread/2f, colorSpread/2f),
                        outColor[3] + Utils.randBetween(-colorSpread/2f, colorSpread/2f),
                };

                float newLife = Utils.randBetween(minLife, maxLife);

                buffer.put(
                        new float[] {
                                newPos.x,
                                newPos.y,
                                newVel.x,
                                newVel.y,
                                newAcc.x,
                                newAcc.y,
                                newSinXAmplitude,
                                newSinXFrequency,
                                newSinXPhase,
                                newSinYAmplitude,
                                newSinYFrequency,
                                newSinYPhase,
                                newTheta,
                                newW,
                                newAlpha,
                                newRadialTheta,
                                newRadialW,
                                newRadialAlpha,
                                newSize,
                                newSizeV,
                                newSizeA,
                                newInColor[0],
                                newInColor[1],
                                newInColor[2],
                                newInColor[3],
                                newOutColor[0],
                                newOutColor[1],
                                newOutColor[2],
                                newOutColor[3],
                                newLife}
                );
            }
            buffer.flip();
            GL15.glBufferData(GL15.GL_ARRAY_BUFFER, buffer, GL15.GL_STATIC_DRAW);

            GL30.glBindVertexArray(0);
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);

            return true;
        }
    }
}
