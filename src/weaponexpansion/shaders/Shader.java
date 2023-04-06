package weaponexpansion.shaders;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;
import weaponexpansion.util.Utils;

import java.io.IOException;

public abstract class Shader {
    public static int programId = -1, vertShaderId, fragShaderId;

    public static void init(String vertShaderPath, String fragShaderPath) {

        if (programId > -1) {
            delete();
        }

        try {
            programId = GL20.glCreateProgram();
            vertShaderId = attachShader(GL20.GL_VERTEX_SHADER, programId, vertShaderPath);
            fragShaderId = attachShader(GL20.GL_FRAGMENT_SHADER, programId, fragShaderPath);
            GL20.glLinkProgram(programId);
            if (GL20.glGetProgrami(programId, GL20.GL_LINK_STATUS) == GL11.GL_FALSE) {
                throw new RuntimeException("Failure to link shader program");
            }
            GL20.glDetachShader(programId, vertShaderId);
            GL20.glDetachShader(programId, fragShaderId);
            GL20.glDeleteShader(vertShaderId);
            GL20.glDeleteShader(fragShaderId);
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void delete() {
        GL20.glDeleteProgram(programId);
    }

    public static int attachShader(int target, int program, String filePath) throws IOException {
        int id = GL20.glCreateShader(target);
        GL20.glShaderSource(id, Utils.readFile(filePath));
        GL20.glCompileShader(id);

        if (GL20.glGetShaderi(id, GL20.GL_COMPILE_STATUS) == GL11.GL_FALSE) {
            throw new RuntimeException("Failure to compile shader");
        }

        GL20.glAttachShader(program, id);
        return id;
    }
}
