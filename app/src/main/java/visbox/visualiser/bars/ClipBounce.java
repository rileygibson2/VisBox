package visbox.visualiser.bars;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RadialGradientPaint;
import java.awt.image.BufferedImage;

import visbox.Analyser;
import visbox.ColorManager;
import visbox.VBMain;

public class ClipBounce extends BarVisualiser {
    
    private float[] velocitys;
    private BufferedImage glowBuffer;
    
    public ClipBounce() {
        super("ClipBounce", 30);
        this.velocitys = new float[numBands];
        for (int i=0; i<numBands; i++) velocitys[i] = 0f;
    }
    
    @Override
    public void update() {
        super.update();
        
        for (int i=0; i<numBands; i++) {
            //if (VBMain.isTimeIncrement(100)) bands[i] = 1f;
            //if (VBMain.isTimeIncrement(120)) bands[i] = 0f;
        }
        
        // Gravity
        float dt = 1f/VBMain.getTargetFPS();
        float stiffness = 80f;
        float damping = 10f;
        float gravity = -2f;
        float bounceThresh = 0.2f; // min speed to bounce
        float restitution = 0.9f; // 1 = perfect bounce <1 loses energy
        
        for (int i = 0; i < numBands; i++) {
            float t = bands[i];
            float h = bandReal[i];
            float v = velocitys[i];
            
            float springF = (t-h)*stiffness;
            float accel = springF+gravity-(damping*v);
            
            v += accel*dt;
            h += v*dt;
            if (h<0f) {
                if (v<-bounceThresh) { // Hit hard enough - bounce
                    v = -v*restitution;
                    h = 0f;
                } else {
                    h = 0f;
                    v = 0f;
                }
            }
            if (h>1f) { // Ceiling clamp
                h = 1f;
                v = 0f;
            }
            
            bandReal[i] = h;
            velocitys[i] = v;
        }
    }
    
    @Override
    public void render(Graphics2D g, int w, int h) {
        float bandW = (float) w/(numBands*2);
        float size = bandW*0.8f;
        float hue = 0.9f;
        
        // Fade glow buffer
        if (glowBuffer==null) glowBuffer = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D gg = glowBuffer.createGraphics();
        gg.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.8f));
        gg.setColor(new Color(0f, 0f, 0f, 1f));
        gg.fillRect(0, 0, w, h);
        
        // Add to glow buffer
        for (int i=0; i<numBands; i++) {
            float v = bandReal[i];
            float v1 = bandReal[numBands-1-i];
            
            float x = (i*bandW)+(bandW*0.5f);
            float x1 = ((i+numBands)*bandW)+(bandW*0.5f);
            float y = h-((h-size)*v)-size+(size*0.5f);
            float y1 = h-((h-size)*v1)-size+(size*0.5f);
            float r = bandW*0.8f;

            Color c = ColorManager.hsvToColor(hue, (float) (1f-Math.pow(v, 0.6f)), 1f);
            gg.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.2f+(0.4f*v)));
            gg.setPaint(new RadialGradientPaint(
                x, y, r,
                new float[]{0f, 1f},
                new Color[] {new Color(c.getRed(), c.getGreen(), c.getBlue(), 255), new Color(c.getRed(), c.getGreen(), c.getBlue(), 0)}
            ));
            gg.fillOval((int) (x-r), (int) (y-r), (int) (2*r), (int) (2*r));

            c = ColorManager.hsvToColor(hue, (float) (1f-Math.pow(v1, 0.6f)), 1f);
            gg.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.2f+(0.4f*v1)));
            gg.setPaint(new RadialGradientPaint(
                x1, y1, r,
                new float[]{0f, 1f},
                new Color[] {new Color(c.getRed(), c.getGreen(), c.getBlue(), 255), new Color(c.getRed(), c.getGreen(), c.getBlue(), 0)}
            ));
            gg.fillOval((int) (x1-r), (int) (y1-r), (int) (2*r), (int) (2*r));
        }

        // Add to world
        g.drawImage(glowBuffer, 0, 0, null);
        
        for (int i = 0; i < numBands; i++) {
            float v = bandReal[i];
            float v1 = bandReal[numBands-1-i];

            float x = (i*bandW)+(bandW*0.5f);
            float x1 = ((i+numBands)*bandW)+(bandW*0.5f);
            float y = h-((h-size)*v)-size+(size*0.5f);
            float y1 = h-((h-size)*v1)-size+(size*0.5f);
            float r = size/2;

            Color c = ColorManager.hsvToColor(hue, (float) (1f-Math.pow(v, 0.6f)), 1f);
            c = new Color(c.getRed(), c.getGreen(), c.getBlue(), 255);
            g.setColor(c);
            g.fillOval((int) (x-r), (int) (y-r), (int) (2*r), (int) (2*r));

            c = ColorManager.hsvToColor(hue, (float) (1f-Math.pow(v1, 0.6f)), 1f);
            c = new Color(c.getRed(), c.getGreen(), c.getBlue(), 255);
            g.setColor(c);
            g.fillOval((int) (x1-r), (int) (y1-r), (int) (2*r), (int) (2*r));
            
        }
    }
}
