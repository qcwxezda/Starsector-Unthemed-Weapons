package weaponexpansion.util.particles;

import com.fs.starfarer.api.combat.BaseCombatLayeredRenderingPlugin;
import com.fs.starfarer.api.combat.CombatEngineLayers;
import com.fs.starfarer.api.combat.ViewportAPI;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.util.Misc;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.*;
import weaponexpansion.shaders.ParticleShader;
import weaponexpansion.util.Utils;

import java.awt.*;
import java.nio.FloatBuffer;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public class ParticleSystem extends BaseCombatLayeredRenderingPlugin {
    private final static int vao, vbo;

    private final static float[] verts = new float[] {
            0f, 0f,
            0f, 1f,
            1f, 0f,
            1f, 0f,
            1f, 1f,
            0f, 1f
    };

    static {
        vao = GL30.glGenVertexArrays();
        GL30.glBindVertexArray(vao);

        FloatBuffer vertBuffer = BufferUtils.createFloatBuffer(verts.length);
        vertBuffer.put(verts).flip();

        vbo = GL15.glGenBuffers();
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vbo);
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, vertBuffer, GL15.GL_STATIC_DRAW);
        GL20.glEnableVertexAttribArray(0);
        GL20.glVertexAttribPointer(0, 2, GL11.GL_FLOAT, false, 0, 0);
        GL30.glBindVertexArray(0);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
    }

    private final List<Particle> particles = new LinkedList<>();
    private boolean expired = false;
    private final float dur;
    private float elapsed = 0f;

    public ParticleSystem(float lifetime) {
        dur = lifetime;
    }

    public void addParticles(int count, SpriteAPI sprite, ParticlePosition pos, ParticleAngle angle, ParticleSize size, float duration, float durationJitter, Color colorIn, Color colorOut) {
        for (int i = 0; i < count; i++) {
            particles.add(new Particle(sprite, pos.randomize(), angle.randomize(), size.randomize(), duration, durationJitter, colorIn, colorOut));
        }
    }

    @Override
    public float getRenderRadius() {
        return 500f;
    }

    @Override
    public boolean isExpired() {
        return expired;
    }

    @Override
    public void advance(float amount) {
        Iterator<Particle> itr = particles.listIterator();
        while (itr.hasNext()) {
            Particle particle = itr.next();
            particle.advance(amount);
            if (particle.isExpired()) {
                itr.remove();
            }
        }

        elapsed += amount;
        if (elapsed >= dur) {
            delete();
        }
    }

    public void delete() {
        expired = true;
        particles.clear();
    }

    @Override
    public void render(CombatEngineLayers layer, ViewportAPI viewport) {
        GL20.glUseProgram(ParticleShader.programId);

        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
        for (Particle particle : particles) {
            if (!particle.isExpired()) {
                particle.sprite.bindTexture();
                GL30.glBindVertexArray(vao);
                GL20.glUniform1f(ParticleShader.scaleLoc, particle.size.size);
                GL20.glUniform2f(ParticleShader.offsetLoc, entity.getLocation().x + particle.pos.x.x, entity.getLocation().y + particle.pos.x.y);
                float[] color = particle.getColor().getComponents(null);
                GL20.glUniform4f(ParticleShader.tintColorLoc, color[0], color[1], color[2], color[3]);
                GL20.glUniform1f(ParticleShader.angleLoc, particle.angle.theta * Misc.RAD_PER_DEG);
                GL20.glUniformMatrix4(ParticleShader.projectionLoc, true, Utils.getProjectionMatrix(viewport));
                GL11.glDrawArrays(GL11.GL_TRIANGLES, 0, 6);
                GL30.glBindVertexArray(0);
            }
        }
        GL11.glDisable(GL11.GL_BLEND);
        GL20.glUseProgram(0);
    }
}
