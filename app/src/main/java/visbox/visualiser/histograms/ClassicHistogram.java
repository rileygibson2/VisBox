package visbox.visualiser.histograms;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import visbox.Analyser;
import visbox.ColorManager;
import visbox.logger.Logger;

public class ClassicHistogram extends HistogramVisualiser {
    
    private float[] smoothBands;
    private boolean smoothInit;
    
    public ClassicHistogram() {
        super("Histogram", 24, 80);
        this.smoothBands = new float[numBands];
        this.smoothInit = false;
    }
    
    @Override
    public void update() {
        super.update();
        
        float a = 0.4f;
        if (!smoothInit) {
            System.arraycopy(bands, 0, smoothBands, 0, numBands);
            smoothInit = true;
        } else {
            for (int i=0; i<numBands; i++) {
                smoothBands[i] = smoothBands[i]*(1f-a)+bands[i]*a;
            }
        }
        
        pushBandsToHistoy(smoothBands);
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
}
