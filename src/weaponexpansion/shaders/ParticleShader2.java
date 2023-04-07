package weaponexpansion.shaders;

import org.lwjgl.opengl.GL20;

public abstract class ParticleShader2 extends Shader {
    public static int programId = -1;
    public static int projectionLoc, timeLoc;
    public static String projectionName = "projection", timeName = "time";

    public static void init(String vertShaderPath, String fragShaderPath) {
        programId = Shader.createProgram(vertShaderPath, fragShaderPath, programId);
        projectionLoc = GL20.glGetUniformLocation(programId, projectionName);
        timeLoc = GL20.glGetUniformLocation(programId, timeName);
    }
}