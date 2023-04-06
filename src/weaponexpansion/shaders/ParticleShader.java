package weaponexpansion.shaders;

import org.lwjgl.opengl.GL20;

public abstract class ParticleShader extends Shader {

    public static int projectionLoc, offsetLoc, scaleLoc, tintColorLoc, angleLoc;
    public static final String projectionName = "projection", offsetName = "offset", scaleName = "scale", tintColorName = "tintColor", angleLocName = "angle";

    public static void init(String vertShaderPath, String fragShaderPath) {
        Shader.init(vertShaderPath, fragShaderPath);
        projectionLoc = GL20.glGetUniformLocation(programId, projectionName);
        offsetLoc = GL20.glGetUniformLocation(programId, offsetName);
        scaleLoc = GL20.glGetUniformLocation(programId, scaleName);
        tintColorLoc = GL20.glGetUniformLocation(programId, tintColorName);
        angleLoc = GL20.glGetUniformLocation(programId, angleLocName);
    }
}
