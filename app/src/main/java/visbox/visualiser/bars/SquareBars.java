package visbox.visualiser.bars;

import java.awt.Color;
import java.awt.Graphics2D;

import visbox.Analyser;
import visbox.ColorManager;
import visbox.logger.Logger;

public class SquareBars extends BarVisualiser {
    
    private float hue;
    private int numBlocks = 12;
    
    public SquareBars(Analyser analyzer, ColorManager colorManager) {
        super(analyzer, colorManager, 24);
        this.hue = 0.8f;
    }
    
    @Override
    public void update() {
        super.update();
        
        /*for (int i=0; i<numBands; i++) {
            if (VBMain.isTimeIncrement(50)) bands[i] = 1;
            else bands[i] = 0.1f;
        }*/
        
        // Decay
        for (int i = 0; i < numBands; i++) {
            bandReal[i] = bandReal[i]-0.03f;
            float v = bandReal[i];
            if (bands[i]>v) {
                v = bands[i];
                bandReal[i] = v;
            }
        }
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
            Logger.debugAt(50, numBands+" "+i+" "+x);
            
            float hB = hue+((i-(numBands/2))*0.01f);
            if (i<numBands/2) hB = hue-((i-(numBands/2))*0.01f);
            
            if (hB<0) hB = 1f-hB;
            else if (hB>1) hB = 1f-hB;
            
            for (int z=0; z<block; z++) {
                float aB = aI*(block-z);
                
                // Gamma shape alpha
                aB = (float) (Math.pow(aB/v, 0.4)*v);
                aB = Math.max(0.2f, Math.min(aB, 1f));
                
                
                Color c = Color.getHSBColor(hB, 1f-aB*0.8f, aB);
                
                g.setColor(c);
                int y = (int) (h-((z+1)*vSpacing+2));
                g.fillRoundRect((int) x, y, (int) (hSpacing-4), (int) (vSpacing-4), 4, 4);
                //g.fillRect((int) x, y, (int) (hSpacing-4), (int) (vSpacing-4));
                
            }
        }
    }
}
