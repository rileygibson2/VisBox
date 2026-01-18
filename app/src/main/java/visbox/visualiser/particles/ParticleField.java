package visbox.visualiser.particles;

import static org.lwjgl.opengl.GL11.GL_BLEND;
import static org.lwjgl.opengl.GL11.GL_COLOR_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.GL_FLOAT;
import static org.lwjgl.opengl.GL11.GL_LINEAR;
import static org.lwjgl.opengl.GL11.GL_ONE;
import static org.lwjgl.opengl.GL11.GL_ONE_MINUS_SRC_ALPHA;
import static org.lwjgl.opengl.GL11.GL_RGBA;
import static org.lwjgl.opengl.GL11.GL_SRC_ALPHA;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_2D;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_MAG_FILTER;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_MIN_FILTER;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_WRAP_S;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_WRAP_T;
import static org.lwjgl.opengl.GL11.GL_TRIANGLES;
import static org.lwjgl.opengl.GL11.GL_TRIANGLE_STRIP;
import static org.lwjgl.opengl.GL11.glBindTexture;
import static org.lwjgl.opengl.GL11.glBlendFunc;
import static org.lwjgl.opengl.GL11.glClear;
import static org.lwjgl.opengl.GL11.glClearColor;
import static org.lwjgl.opengl.GL11.glDisable;
import static org.lwjgl.opengl.GL11.glDrawArrays;
import static org.lwjgl.opengl.GL11.glEnable;
import static org.lwjgl.opengl.GL11.glGenTextures;
import static org.lwjgl.opengl.GL11.glTexImage2D;
import static org.lwjgl.opengl.GL11.glTexParameteri;
import static org.lwjgl.opengl.GL12.GL_CLAMP_TO_EDGE;
import static org.lwjgl.opengl.GL13.GL_TEXTURE0;
import static org.lwjgl.opengl.GL13.glActiveTexture;
import static org.lwjgl.opengl.GL15.GL_ARRAY_BUFFER;
import static org.lwjgl.opengl.GL15.GL_STATIC_DRAW;
import static org.lwjgl.opengl.GL15.GL_STREAM_DRAW;
import static org.lwjgl.opengl.GL15.glBindBuffer;
import static org.lwjgl.opengl.GL15.glBufferData;
import static org.lwjgl.opengl.GL15.glBufferSubData;
import static org.lwjgl.opengl.GL15.glGenBuffers;
import static org.lwjgl.opengl.GL20.glEnableVertexAttribArray;
import static org.lwjgl.opengl.GL20.glUseProgram;
import static org.lwjgl.opengl.GL20.glVertexAttribPointer;
import static org.lwjgl.opengl.GL30.GL_COLOR_ATTACHMENT0;
import static org.lwjgl.opengl.GL30.GL_FRAMEBUFFER;
import static org.lwjgl.opengl.GL30.GL_FRAMEBUFFER_COMPLETE;
import static org.lwjgl.opengl.GL30.GL_RGBA16F;
import static org.lwjgl.opengl.GL30.glBindFramebuffer;
import static org.lwjgl.opengl.GL30.glBindVertexArray;
import static org.lwjgl.opengl.GL30.glCheckFramebufferStatus;
import static org.lwjgl.opengl.GL30.glFramebufferTexture2D;
import static org.lwjgl.opengl.GL30.glGenFramebuffers;
import static org.lwjgl.opengl.GL30.glGenVertexArrays;
import static org.lwjgl.opengl.GL31.glDrawArraysInstanced;
import static org.lwjgl.opengl.GL33.*;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.nio.FloatBuffer;
import java.util.ArrayList;

import org.lwjgl.BufferUtils;

import visbox.Analyser;
import visbox.Analyser.AnalyserConfig;
import visbox.ShaderManager;
import visbox.VBMain;
import visbox.logger.Logger;
import visbox.ui.GLFWUI;

public class ParticleField extends ParticleVisualiser {
    
    
    private BufferedImage glowBuffer;
    private ArrayList<Particle> particles;
    private int MAX_PARTICLES = 10000;
    private long lastFFTTick;
    
    private int program;
    private String vertSrc = "particle1.vert";
    private String fragSrc = "particle1.frag";
    private int instanceVBO;
    private int vAO;
    
    private int glowFBO;
    private int glowTex;
    
    public ParticleField() {
        super("ParticleField", 16);
        this.bands = new float[numBands];
        this.particles = new ArrayList<Particle>();
    }
    
    public float rand(float low, float high) {
        return (float) (low+(Math.random()*(high-low)));
    }
    
    @Override
    public void activate(AnalyserConfig a) {
        ShaderManager sM = VBMain.getShaderManager();
        
        program = sM.createProgram(vertSrc, fragSrc);
        sM.setCurrentProgram(program);
        
        // Create VAO
        vAO = glGenVertexArrays();
        sM.bindVAO(vAO);
        
        // Create VBO
        float[] quad = {
            -0.5f, -0.5f,
            0.5f, -0.5f,
            -0.5f,  0.5f,
            0.5f,  0.5f
        };
        
        int quadVBO = glGenBuffers();
        sM.bindVBO(quadVBO);
        glBufferData(GL_ARRAY_BUFFER, quad, GL_STATIC_DRAW);
        
        // Add to VAO
        glEnableVertexAttribArray(0);
        glVertexAttribPointer(0, 2, GL_FLOAT, false, 2 * Float.BYTES, 0);
        
        // Create instance data VBO
        instanceVBO = glGenBuffers();
        sM.bindVBO(instanceVBO);
        glBufferData(
            GL_ARRAY_BUFFER,
            (long) MAX_PARTICLES * 7 * Float.BYTES,
            GL_STREAM_DRAW
        );
        
        int stride = 7*Float.BYTES;
        // Add center to VAO
        glEnableVertexAttribArray(1);
        glVertexAttribPointer(1, 2, GL_FLOAT, false, stride, 0L);
        glVertexAttribDivisor(1, 1);
        
        // Add size to VAO
        glEnableVertexAttribArray(2);
        glVertexAttribPointer(2, 1, GL_FLOAT, false, stride, 2L * Float.BYTES);
        glVertexAttribDivisor(2, 1);
        
        // Add HSB to VAO
        glEnableVertexAttribArray(3);
        glVertexAttribPointer(3, 3, GL_FLOAT, false, stride, 3L * Float.BYTES);
        glVertexAttribDivisor(3, 1);
        
        // Add Life to VAO
        glEnableVertexAttribArray(4);
        glVertexAttribPointer(4, 1, GL_FLOAT, false, stride, 6L * Float.BYTES);
        glVertexAttribDivisor(4, 1);
        
        sM.bindVAO(0);
        
        // Setup glow FBO
        GLFWUI ui = VBMain.getUI();
        glowFBO = glGenFramebuffers();
        sM.bindFBO(glowFBO);
        
        glowTex = glGenTextures();
        sM.bindTexture(glowTex);
        
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA16F, ui.getWidth(), ui.getHeight(), 0, GL_RGBA, GL_FLOAT, 0L);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
        glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, glowTex, 0);
        
        if (glCheckFramebufferStatus(GL_FRAMEBUFFER) != GL_FRAMEBUFFER_COMPLETE) {
            throw new RuntimeException("Glow FBO incomplete");
        }
        sM.bindFBO(0);
        
        sM.setOrthoProjection();
        super.activate(a);
    }
    
    public void resize() {
        GLFWUI ui = VBMain.getUI();
        ShaderManager sM = VBMain.getShaderManager();
        sM.bindTexture(glowTex);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA16F, ui.getWidth(), ui.getHeight(), 0, GL_RGBA, GL_FLOAT, 0L);
        sM.bindTexture(0);
        
        sM.setOrthoProjection();
    }
    
    @Override
    public void update() {
        super.update();
        GLFWUI ui = VBMain.getUI();
        
        // Check if new fft data
        Analyser analyser = VBMain.getInstance().getAnalyser();
        boolean newFFT = false;
        synchronized (analyser) {
            long tick = analyser.getFFTTick();
            if (tick!=lastFFTTick) newFFT = true;
            lastFFTTick = tick;
        }
        
        // Spawn particles
        if (newFFT) {
            for (int b=0; b<bands.length-1; b++) {
                float v = bands[b];
                if (v<0.01f) continue;
                
                int spawnCount = (int) (Math.pow(v, 1.9)*30);
                
                for (int i=0; i<spawnCount; i++) {
                    if (particles.size()>=MAX_PARTICLES) break;
                    Particle p = new Particle();
                    
                    p.band = b;
                    p.x = rand(0, ui.getWidth());
                    p.y = rand(0, ui.getHeight());
                    
                    float bT = (float) b/(numBands-1);
                    float speed = 1f+12f*v; // Middle div by bT if want
                    double angle = (Math.random()*Math.PI*2.0);
                    p.vx = (float) (Math.cos(angle)*speed);
                    p.vy = (float) (Math.sin(angle)*speed);
                    
                    p.size = 2f+rand(5*v, 30*v);
                    
                    p.life = 0.1f+rand(0.8f*(1-v), 1f*(1-v));
                    p.maxLife = p.life;

                    p.band = b;
                    
                    p.hue = (float) 0.9f+rand(-0.1f, 0.2f);
                    if (p.hue>1f) p.hue -= 1f;
                    else if (p.hue<0f) p.hue = 1f-p.hue;
                    
                    p.sat = 1f-rand(0.2f*v, 0.8f*v);
                    p.brightness = 0.5f+(0.5f*v);
                    particles.add(p);
                }

                bandLast[b] = bands[b];
            }
        }
        
        // Update existing particles
        for (int i=particles.size()-1; i>=0; i--) {
            Particle p = particles.get(i);
            p.x += p.vx*0.03;
            p.y += p.vy*0.03;
            
            p.life -= 0.003;
            
            if (newFFT) {
                float v = bands[p.band];
                if (v>=bandLast[p.band]) {
                    p.size += rand((0f*v), (10f*v));
                    //p.vx = p.vx*(1f+v);
                    //p.vy = p.vy*(1f+v);
                }
                else {
                    p.size -= rand((0f*v), (4f*v));
                    //p.vx = p.vx*(1f-v);
                    //p.vy = p.vy*(1f-v);
                }
            }
            
            if (p.life<=0f) particles.remove(i);
        }
    }
    
    @Override
    public void render() {
        ShaderManager sM = VBMain.getShaderManager();
        int c = particles.size();
        if (c>MAX_PARTICLES) c = MAX_PARTICLES;
        uploadInstanceData(c);
        
        // Set uniforms
        sM.useProgram(program);
        sM.setUniformFloat("uCoreRadius", 0.15f);
        sM.setUniformFloat("uCoreSoftness", 0.05f);
        sM.setUniformFloat("uHaloPower", 1.5f);
        
        // Fade glow buffer
        sM.bindFBO(glowFBO);
        sM.setBlend(true);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        sM.drawFade(0.08f);
        
        // Pass 1 - draw to glow buffer
        sM.useProgram(program);
        sM.setBlend(true);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE);
        sM.setUniformInt("uPass", 1); // Glow only
        sM.bindVAO(vAO);
        glDrawArraysInstanced(GL_TRIANGLE_STRIP, 0, 4, c);
        
        sM.bindVAO(0);
        sM.useProgram(0);
        sM.setBlend(false);
        
        // Pass 2 - draw glow texture
        sM.bindFBO(0);
        sM.clearToBlack();
        sM.drawScreenTexture(glowTex);
        
        // Pass 3 - core
        sM.setBlend(true);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE); // additive cores
        
        sM.useProgram(program);
        sM.setUniformInt("uPass", 2); // Core only
        sM.bindVAO(vAO);
        glDrawArraysInstanced(GL_TRIANGLE_STRIP, 0, 4, c);
        
        sM.bindVAO(0);
        sM.useProgram(0);
        sM.setBlend(false);

        VBMain.getUI().setTitleData("Count: "+particles.size());
    }
    
    private void uploadInstanceData(int c) {
        // 7 floats per particle: x, y, size, hue, sat, brightness, life
        FloatBuffer buffer = BufferUtils.createFloatBuffer(c*7);
        
        for (int i=0; i<c; i++) {
            Particle p = particles.get(i);
            
            buffer.put(p.x);
            buffer.put(p.y);
            buffer.put(p.size);
            buffer.put(p.hue);
            buffer.put(p.sat);
            buffer.put(p.brightness);

            float bright;
            float lim = 0.8f;
            float n = p.life/p.maxLife;
            if (n>=0.8f) bright = (1-n)/(1-lim);
            else bright = n/lim;
            buffer.put(bright);
        }
        buffer.flip();
        
        VBMain.getShaderManager().bindVBO(instanceVBO);
        glBufferSubData(GL_ARRAY_BUFFER, 0, buffer);
        VBMain.getShaderManager().bindVBO(0);
    }
    
    @Override
    public void render(Graphics2D g, int w, int h) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'render'");
    }
}
