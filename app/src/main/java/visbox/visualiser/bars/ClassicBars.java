package visbox.visualiser.bars;

import static org.lwjgl.opengl.GL11.GL_FLOAT;
import static org.lwjgl.opengl.GL11.GL_INT;
import static org.lwjgl.opengl.GL11.GL_ONE;
import static org.lwjgl.opengl.GL11.GL_SRC_ALPHA;
import static org.lwjgl.opengl.GL11.GL_TRIANGLE_STRIP;
import static org.lwjgl.opengl.GL11.glBlendFunc;
import static org.lwjgl.opengl.GL11.glGenTextures;
import static org.lwjgl.opengl.GL11.glTexImage2D;
import static org.lwjgl.opengl.GL11.glTexParameteri;
import static org.lwjgl.opengl.GL15.GL_ARRAY_BUFFER;
import static org.lwjgl.opengl.GL15.GL_STATIC_DRAW;
import static org.lwjgl.opengl.GL15.GL_STREAM_DRAW;
import static org.lwjgl.opengl.GL15.glBufferData;
import static org.lwjgl.opengl.GL15.glBufferSubData;
import static org.lwjgl.opengl.GL15.glGenBuffers;
import static org.lwjgl.opengl.GL20.glEnableVertexAttribArray;
import static org.lwjgl.opengl.GL20.glVertexAttribPointer;
import static org.lwjgl.opengl.GL30.GL_FRAMEBUFFER_COMPLETE;
import static org.lwjgl.opengl.GL30.glCheckFramebufferStatus;
import static org.lwjgl.opengl.GL30.glFramebufferTexture2D;
import static org.lwjgl.opengl.GL30.glGenFramebuffers;
import static org.lwjgl.opengl.GL30.glGenVertexArrays;
import static org.lwjgl.opengl.GL31.glDrawArraysInstanced;
import static org.lwjgl.opengl.GL33.glVertexAttribDivisor;
import static org.lwjgl.opengl.GL33.*;

import java.awt.Color;
import java.awt.Graphics2D;
import java.nio.FloatBuffer;

import org.lwjgl.BufferUtils;

import visbox.Analyser;
import visbox.ColorManager;
import visbox.ShaderManager;
import visbox.VBMain;
import visbox.Analyser.AnalyserConfig;
import visbox.logger.Logger;
import visbox.ui.GLFWUI;

public class ClassicBars extends BarVisualiser {
    
    private float hue;
    private int numBlocks = 12;
    
    private int MAX_INSTANCES = 4000;
    private int program;
    private int vAO;
    private int instanceVBO;
    private FloatBuffer instanceBuffer;
    
    public ClassicBars() {
        super("ClassicBars", 24);
        this.hue = 0.8f;
    }
    
    @Override
    public void activate(AnalyserConfig a) {
        ShaderManager sM = VBMain.getShaderManager();
        
        program = sM.createProgram("classicbars.vert", "classicbars.frag");
        sM.setCurrentProgram(program);
        sM.useProgram(program);
        
        // Create VAO
        vAO = glGenVertexArrays();
        sM.bindVAO(vAO);
        
        // Create VBO
        int quadVBO = glGenBuffers();
        sM.bindVBO(quadVBO);
        glBufferData(GL_ARRAY_BUFFER, sM.getGenericQuad(), GL_STATIC_DRAW);
        
        // Add to VAO
        glEnableVertexAttribArray(0);
        glVertexAttribPointer(0, 2, GL_FLOAT, false, 2*Float.BYTES, 0L);
        
        // Create instance data VBO
        int stride = 5*Float.BYTES;
        instanceVBO = glGenBuffers();
        sM.bindVBO(instanceVBO);
        glBufferData(GL_ARRAY_BUFFER, (long) MAX_INSTANCES*stride, GL_STREAM_DRAW);
        
        // Add band/block to VAO
        glEnableVertexAttribArray(1);
        glVertexAttribPointer(1, 1, GL_FLOAT, false, stride, 0L);
        glVertexAttribDivisor(1, 1);
        
        glEnableVertexAttribArray(2);
        glVertexAttribPointer(2, 1, GL_FLOAT, false, stride, 1L*Float.BYTES);
        glVertexAttribDivisor(2, 1);
        
        // Add HSB to VAO
        glEnableVertexAttribArray(3);
        glVertexAttribPointer(3, 3, GL_FLOAT, false, stride, 2L*Float.BYTES);
        glVertexAttribDivisor(3, 1);
        
        //sM.setOrthoProjection();
        sM.bindVAO(0);
        sM.bindFBO(0);
        sM.bindVBO(0);
        sM.useProgram(0);
        
        instanceBuffer = BufferUtils.createFloatBuffer(MAX_INSTANCES*5);
        
        super.activate(a);
    }
    
    @Override
    public void update() {
        super.update();
        
        // Decay
        for (int i = 0; i < numBands; i++) {
            bandReal[i] = bandReal[i]-0.003f;
            float v = bandReal[i];
            if (bands[i]>v) {
                v = bands[i];
                bandReal[i] = v;
            }
        }
        
        uploadInstanceData();
    }
    
    @Override
    public void render(Graphics2D g, int w, int h) {
        float hSpacing = (float) w/numBands;
        float vSpacing = (float) h/numBlocks;
        float blockV = (float) 1/numBlocks;
        
        hue = hue-0.005f;
        if (hue<0f) hue = 1f-hue;
        
        for (int i=0; i<numBands; i++) {
            float v = bandReal[i];
            
            int block = (int) Math.ceil(v/blockV);
            block = Math.max(0, Math.min(block, numBlocks));
            
            float aI = 0;
            if (block>0) aI = v/block;
            
            float x = 2+(i*hSpacing);
            
            float hB = hue+((i-(numBands/2))*0.01f);
            if (i<numBands/2) hB = hue-((i-(numBands/2))*0.01f);
            
            if (hB<0) hB = 1f-hB;
            else if (hB>1) hB = 1f-hB;
            
            for (int z=0; z<block; z++) {
                int y = (int) (h-((z+1)*vSpacing+2));
                float aB = aI*(block-z);
                
                // Gamma shape alpha
                aB = (float) (Math.pow(aB/v, 0.4)*v);
                aB = Math.max(0.2f, Math.min(aB, 1f));
                Color c = Color.getHSBColor(hB, 1f-aB*0.8f, aB);
                g.setColor(c);
                
                g.fillRoundRect((int) x, y, (int) (hSpacing-4), (int) (vSpacing-4), 4, 4);
                //g.fillRect((int) x, y, (int) (hSpacing-4), (int) (vSpacing-4));
                
            }
        }
    }
    
    @Override
    public void render() {
        ShaderManager sM = VBMain.getShaderManager();
        
        sM.useProgram(program);
        sM.bindFBO(0);
        sM.setBlend(true);
        sM.clearToBlack();
        
        sM.setUniformInt("uNumBands", numBands);
        sM.setUniformInt("uNumBlocks", numBlocks);
        
        sM.bindVAO(vAO);
        glDrawArraysInstanced(GL_TRIANGLE_STRIP, 0, 4, numBlocks*numBands);
        
        sM.bindVAO(0);
        sM.useProgram(0);
        sM.setBlend(false);
    }
    
    private void uploadInstanceData() {
        instanceBuffer.clear();
        float blockV = (float) 1/numBlocks;
        
        //hue = hue-0.005f;
        //if (hue<0f) hue = 1f-hue;
        
        for (int i=0; i<numBands; i++) {
            float v = bandReal[i];
            
            int block = (int) Math.ceil(v/blockV);
            block = Math.max(0, Math.min(block, numBlocks));
            
            float aI = 0;
            if (block>0) aI = v/block;
            
            float hB = hue+((i-(numBands/2))*0.01f);
            if (i<numBands/2) hB = hue-((i-(numBands/2))*0.01f);
            
            for (int z=0; z<numBlocks; z++) {
                float bB;
                float sB;
                if (z>block) {
                    bB = 0;
                    sB = 0;
                } else {
                    bB = aI*(block-z);
                    bB = (float) (Math.pow(bB/v, 0.4)*v);
                    bB = Math.max(0.2f, Math.min(bB, 1f));
                }
                
                sB = 1f-bB*0.8f;
                
                instanceBuffer.put((float) i); // Band
                instanceBuffer.put((float) z); // Block
                instanceBuffer.put(hB);
                instanceBuffer.put(sB);
                instanceBuffer.put(bB);
            }
        }
        instanceBuffer.flip();
        
        VBMain.getShaderManager().bindVBO(instanceVBO);
        glBufferSubData(GL_ARRAY_BUFFER, 0, instanceBuffer);
        VBMain.getShaderManager().bindVBO(0);
    }
}
