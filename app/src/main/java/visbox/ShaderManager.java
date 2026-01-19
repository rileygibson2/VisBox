package visbox;

import static org.lwjgl.opengl.GL11.GL_BLEND;
import static org.lwjgl.opengl.GL11.GL_COLOR_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.GL_FALSE;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_2D;
import static org.lwjgl.opengl.GL11.GL_TRIANGLES;
import static org.lwjgl.opengl.GL11.glBindTexture;
import static org.lwjgl.opengl.GL11.glClear;
import static org.lwjgl.opengl.GL11.glClearColor;
import static org.lwjgl.opengl.GL11.glDisable;
import static org.lwjgl.opengl.GL11.glDrawArrays;
import static org.lwjgl.opengl.GL11.glEnable;
import static org.lwjgl.opengl.GL11.glViewport;
import static org.lwjgl.opengl.GL13.GL_TEXTURE0;
import static org.lwjgl.opengl.GL13.glActiveTexture;
import static org.lwjgl.opengl.GL15.GL_ARRAY_BUFFER;
import static org.lwjgl.opengl.GL15.glBindBuffer;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.opengl.GL33.*;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.FloatBuffer;
import java.util.HashMap;

import org.lwjgl.BufferUtils;

import visbox.ui.GLFWUI;

public class ShaderManager {
    
    private int currentProgram;
    private int screenTexProgram;
    private int screenFadeProgram;

    public int SCREEN_VAO;
    private int uScreenTex;
    private int uScreenFade;


    public ShaderManager() {
        clearCurrentProgram();
        createScreenTexProgram();
        createScreenFadeProgram();
    }

    private void createScreenTexProgram() {
        screenTexProgram = createProgram("screen/screenTex.vert", "screen/screenTex.frag");
        SCREEN_VAO = glGenVertexArrays();

        useProgram(screenTexProgram);
        uScreenTex = glGetUniformLocation(screenTexProgram, "uScreenTex");
        glUniform1i(uScreenTex, 0);
        useProgram(0);
    }

    private void createScreenFadeProgram() {
        screenFadeProgram = createProgram("screen/screenFade.vert", "screen/screenFade.frag");

        useProgram(screenFadeProgram);
        uScreenFade = glGetUniformLocation(screenFadeProgram, "uScreenFade");
        glUniform1f(uScreenFade, 0);
        useProgram(0);
    }

    public void setCurrentProgram(int program) {
        clearCurrentProgram();
        currentProgram = program;
    }

    public void clearCurrentProgram() {
        currentProgram = -1;
    }

    public int getScreenTexProgram() {return screenTexProgram;}

    public void drawScreenTexture(int tex) {
        bindFBO(0);
        assertViewport();

        useProgram(screenTexProgram);
        glActiveTexture(GL_TEXTURE0);
        bindTexture(tex);
        bindVAO(SCREEN_VAO);
        glDrawArrays(GL_TRIANGLES, 0, 3);
        
        bindTexture(0);
        bindVAO(0);
        useProgram(0);
    }

    public void drawFade(float fade) {
        useProgram(screenFadeProgram);
        glUniform1f(uScreenFade, fade);
        bindVAO(SCREEN_VAO);
        glDrawArrays(GL_TRIANGLES, 0, 3);
        bindVAO(0);
        useProgram(0);
    }

    public void clearToBlack() {
        glClearColor(0f, 0f, 0f, 1f);
        glClear(GL_COLOR_BUFFER_BIT);
    }

    public void useProgram(int prog) {
        glUseProgram(prog);
    }

    public void setBlend(boolean blend) {
        if (blend) glEnable(GL_BLEND);
        else glDisable(GL_BLEND);
    }

    public void bindFBO(int fBO) {glBindFramebuffer(GL_FRAMEBUFFER, fBO);}

    public void bindVBO(int vBO) {glBindBuffer(GL_ARRAY_BUFFER, vBO);}

    public void bindVAO(int vAO) {glBindVertexArray(vAO);}

    public void bindTexture(int tex) {glBindTexture(GL_TEXTURE_2D, tex);}

    public void assertViewport() {glViewport(0, 0, GLFWUI.WIDTH, GLFWUI.HEIGHT);}
    
    public int createProgram(String vertPath, String fragPath) {
        String vertSrc = loadResource("/shaders/"+vertPath);
        String fragSrc = loadResource("/shaders/"+fragPath);
        int vs = compileShader(vertSrc, GL_VERTEX_SHADER);
        int fs = compileShader(fragSrc, GL_FRAGMENT_SHADER);
        
        int program = glCreateProgram();
        glAttachShader(program, vs);
        glAttachShader(program, fs);
        glLinkProgram(program);
        
        int status = glGetProgrami(program, GL_LINK_STATUS);
        if (status == GL_FALSE) {
            String log = glGetProgramInfoLog(program);
            throw new RuntimeException("Program link error: " + log);
        }
        
        glDeleteShader(vs);
        glDeleteShader(fs);

        return program;
    }

    public void setUniformFloat(String n, float v) {
        if (currentProgram==-1) throw new RuntimeException("Cannot set uniform - no current program");
        int id = glGetUniformLocation(currentProgram, n);
        glUniform1f(id, v);
    }

    public void setUniformInt(String n, int v) {
        if (currentProgram==-1) throw new RuntimeException("Cannot set uniform - no current program");
        int id = glGetUniformLocation(currentProgram, n);
        glUniform1i(id, v);
    }

    public void setOrthoProjection() {
        if (currentProgram==-1) throw new RuntimeException("Cannot set ortho - no current program");

        float[] proj = makeOrtho(0f, GLFWUI.WIDTH, 0f, GLFWUI.HEIGHT, -1f, 1f);
        FloatBuffer fb = BufferUtils.createFloatBuffer(16);
        fb.put(proj).flip();

        useProgram(currentProgram);
        glUniformMatrix4fv(glGetUniformLocation(currentProgram, "uProj"), false, fb);
        useProgram(0);
    }
    
    private int compileShader(String src, int type) {
        int id = glCreateShader(type);
        glShaderSource(id, src);
        glCompileShader(id);
        
        int status = glGetShaderi(id, GL_COMPILE_STATUS);
        if (status == GL_FALSE) {
            String log = glGetShaderInfoLog(id);
            throw new RuntimeException("Shader compile error: " + log);
        }
        return id;
    }
    
    private String loadResource(String path) {
        try (InputStream in = getClass().getResourceAsStream(path);
        BufferedReader reader = new BufferedReader(new InputStreamReader(in))) {
            
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
            return sb.toString();
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to load resource: " + path, e);
        }
    }
    
    private float[] makeOrtho(float left, float right, float bottom, float top, float near, float far) {
        float[] m = new float[16];

        m[0]  =  2f / (right - left);
        m[5]  =  2f / (top - bottom);
        m[10] = -2f / (far - near);
        m[12] = -(right + left) / (right - left);
        m[13] = -(top + bottom) / (top - bottom);
        m[14] = -(far + near) / (far - near);
        m[15] = 1f;

        return m;
    }

    public float[] getGenericQuad() {
        float[] quad = {
            -0.5f, -0.5f,
            0.5f, -0.5f,
            -0.5f,  0.5f,
            0.5f,  0.5f
        };
        return quad;
    }
}
