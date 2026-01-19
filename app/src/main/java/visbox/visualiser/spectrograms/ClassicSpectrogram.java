package visbox.visualiser.spectrograms;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.nio.FloatBuffer;

import org.lwjgl.BufferUtils;

import static org.lwjgl.opengl.GL11.GL_BLEND;
import static org.lwjgl.opengl.GL11.GL_FLOAT;
import static org.lwjgl.opengl.GL11.GL_LINEAR;
import static org.lwjgl.opengl.GL11.GL_RED;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_2D;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_MAG_FILTER;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_MIN_FILTER;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_WRAP_S;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_WRAP_T;
import static org.lwjgl.opengl.GL11.GL_TRIANGLES;
import static org.lwjgl.opengl.GL11.glBindTexture;
import static org.lwjgl.opengl.GL11.glDisable;
import static org.lwjgl.opengl.GL11.glDrawArrays;
import static org.lwjgl.opengl.GL11.glGenTextures;
import static org.lwjgl.opengl.GL11.glTexImage2D;
import static org.lwjgl.opengl.GL11.glTexParameteri;
import static org.lwjgl.opengl.GL11.glTexSubImage2D;
import static org.lwjgl.opengl.GL11.glViewport;
import static org.lwjgl.opengl.GL12.GL_CLAMP_TO_EDGE;
import static org.lwjgl.opengl.GL13.GL_TEXTURE0;
import static org.lwjgl.opengl.GL13.GL_TEXTURE1;
import static org.lwjgl.opengl.GL13.glActiveTexture;
import static org.lwjgl.opengl.GL20.glUniform1i;
import static org.lwjgl.opengl.GL20.glUseProgram;
import static org.lwjgl.opengl.GL30.GL_FRAMEBUFFER;
import static org.lwjgl.opengl.GL30.GL_R16F;
import static org.lwjgl.opengl.GL30.glBindFramebuffer;
import static org.lwjgl.opengl.GL30.glBindVertexArray;
import static org.lwjgl.opengl.GL33.*;

import visbox.Analyser;
import visbox.ColorManager;
import visbox.ShaderManager;
import visbox.VBMain;
import visbox.Analyser.AnalyserConfig;
import visbox.logger.Logger;
import visbox.ui.GLFWUI;

public class ClassicSpectrogram extends SpectrogramVisualiser {
    
    private float[] smoothBands;
    private boolean smoothInit;
    
    private int program;
    private int histTex;
    
    private FloatBuffer rowBuffer;
    private int writeRow;
    
    public ClassicSpectrogram() {
        super("Spectrogram", 35, 800);
        this.smoothBands = new float[numBands];
        this.smoothInit = false;
        this.rowBuffer = BufferUtils.createFloatBuffer(numBands);
    }
    
    @Override
    public void activate(AnalyserConfig a) {
        super.activate(a);
        ShaderManager sM = VBMain.getShaderManager();
        
        // Create texture
        histTex = glGenTextures();
        sM.bindTexture(histTex);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_R16F, numBands, historyLength, 0, GL_RED, GL_FLOAT, 0L);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);  // smooth between texels
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
        glBindTexture(GL_TEXTURE_2D, 0);
        
        // Setup program
        sM.bindVAO(sM.SCREEN_VAO);
        sM.bindVAO(0);
        program = sM.createProgram("screen/screenTex.vert", "histogram.frag");
        sM.setCurrentProgram(program);
        sM.useProgram(program);
        
        sM.setUniformInt("uHistTex", 0);
        sM.setUniformInt("uNumBands", numBands);
        sM.setUniformInt("uHistoryLen", historyLength);
        sM.setUniformInt("uWriteRow", 0);
        sM.setUniformFloat("uGamma", 1f);
        sM.setUniformFloat("uIntensity", 1f);
        sM.useProgram(0);
    }
    
    @Override
    public void update() {
        super.update();
        
        float a = 0.06f;
        if (!smoothInit) {
            System.arraycopy(bands, 0, smoothBands, 0, numBands);
            smoothInit = true;
        } else {
            for (int i=0; i<numBands; i++) {
                smoothBands[i] = smoothBands[i]+(bands[i]-smoothBands[i])*a;
            }
        }
        
        uploadBands(smoothBands);
    }
    
    @Override
    public void render(Graphics2D g, int w, int h) {
        int bands = numBands;
        int hist  = historyLength;
        
        // build blurred version of history for this frame
        rebuildBlurredHistory();
        
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        
        for (int py = 0; py < h; py++) {
            float vy = 1f - (py / (float)(h - 1));
            float y  = vy * (hist - 1);
            int   y0 = (int) y;
            float fy = y - y0;
            int   y1 = y0 + 1;
            if (y1 >= hist) y1 = hist - 1;
            
            for (int px = 0; px < w; px++) {
                float vx = px / (float)(w - 1);
                float x  = vx * (bands - 1);
                int   x0 = (int) x;
                float fx = x - x0;
                int   x1 = x0 + 1;
                if (x1 >= bands) x1 = bands - 1;
                
                // sample from blurred history
                float v00 = blurredHistory[y0][x0];
                float v10 = blurredHistory[y0][x1];
                float v01 = blurredHistory[y1][x0];
                float v11 = blurredHistory[y1][x1];
                
                float v0  = v00 + (v10 - v00) * fx;
                float v1  = v01 + (v11 - v01) * fx;
                float val = v0  + (v1  - v0)  * fy;
                
                val = Math.max(0f, Math.min(val, 1f));
                // optional extra gamma
                //val = (float)Math.pow(val, 1.1f);
                
                int a  = (int)(val * 255f);
                int r  = a, gn = a, b = a;
                int argb = (a << 24) | (r << 16) | (gn << 8) | b;
                img.setRGB(px, py, argb);
            }
        }
        
        g.drawImage(img, 0, 0, null);
    }
    
    @Override
    public void render() {
        ShaderManager sM = VBMain.getShaderManager();
        
        sM.bindFBO(0);
        sM.assertViewport();
        sM.setBlend(false);
        
        sM.useProgram(program);
        sM.setUniformInt("uWriteRow", writeRow);
        
        glActiveTexture(GL_TEXTURE0);
        sM.bindTexture(histTex);
        
        sM.bindVAO(sM.SCREEN_VAO);
        glDrawArrays(GL_TRIANGLES, 0, 3);
        sM.bindVAO(0);
        
        sM.bindTexture(0);
        sM.useProgram(0);
    }
    
    private void uploadBands(float[] bands) {
        if (bands == null) return;
        ShaderManager sM = VBMain.getShaderManager();
        
        rowBuffer.clear();
        for (int i = 0; i < numBands; i++) {
            float v = bands[i];
            if (v < 0f) v = 0f;
            if (v > 1f) v = 1f;
            rowBuffer.put(v);
        }
        rowBuffer.flip();
        
        sM.bindTexture(histTex);
        glTexSubImage2D(GL_TEXTURE_2D, 0, 0, writeRow, numBands, 1, GL_RED, GL_FLOAT, rowBuffer);
        sM.bindTexture(0);
        
        writeRow = (writeRow + 1) % historyLength;
    }
    
}
