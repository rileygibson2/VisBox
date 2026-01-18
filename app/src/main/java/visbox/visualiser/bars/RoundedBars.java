package visbox.visualiser.bars;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import visbox.Analyser;
import visbox.ColorManager;
import visbox.VBMain;
import visbox.logger.Logger;

public class RoundedBars extends BarVisualiser {
    
    private float[] velocitys;
    private BufferedImage glowBuffer;
    
    public RoundedBars() {
        super("RoundedBars", 20);
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
        float gravity = -3f;
        
        for (int i = 0; i < numBands; i++) {
            float t = bands[i];
            float p = bandReal[i];
            float v = velocitys[i];
            
            float springF = (t-p)*stiffness;
            float accel = springF+gravity-(damping*v);
            
            v += accel*dt;
            p += v*dt;
            if (p<0f) {
                p = 0f;
                v = 0f;
            }
            if (p>1f) { // Ceiling clamp
                p = 1f;
                v = 0f;
            }
            
            bandReal[i] = p;
            velocitys[i] = v;
        }
    }
    
    @Override
    public void render(Graphics2D g, int w, int h) {
        float bandW = (float) w/(numBands*2);
        int round = 5;
        
        for (int i=0; i<numBands; i++) {
            float v = bandReal[numBands-1-i];
            float v1 = bandReal[i];
            if (v<0.01f) v = 0.01f;
            if (v1<0.01f) v1 = 0.01f;
            
            
            float x = (i*bandW)+(bandW*0.1f);
            float x1 = ((i+numBands)*bandW)+(bandW*0.1f);
            float y = (h/2)-((h/2)*v);
            float y1 = (h/2)-((h/2)*v1);
            
            //g.setColor(Color.WHITE);
            
            // 1st half Rounded bars and gap filling
            Color c = Color.WHITE;
            c = new Color(c.getRed(), c.getGreen(), c.getBlue(), (int) (Math.pow(v, 0.5f)*255));
            g.setColor(c);
            g.fillRoundRect((int) x, (int) y, (int) (bandW*0.8f), (int) (((h/2)*v)*2), round, round);
            
            // 2cnd half Rounded bars and gap filling
            c = Color.WHITE;
            c = new Color(c.getRed(), c.getGreen(), c.getBlue(), (int) (Math.pow(v1, 0.5f)*255));
            g.setColor(c);
            g.fillRoundRect((int) x1, (int) y1, (int) (bandW*0.8f), (int) (((h/2)*v1)*2), round, round);
            
        }
    }

    @Override
    public void render() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'render'");
    }
}
