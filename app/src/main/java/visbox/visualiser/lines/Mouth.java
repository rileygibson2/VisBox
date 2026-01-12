package visbox.visualiser.lines;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.MultipleGradientPaint.CycleMethod;
import java.awt.RadialGradientPaint;
import java.awt.RenderingHints;
import java.awt.geom.GeneralPath;
import java.util.ArrayList;
import java.util.List;

import visbox.Analyser;
import visbox.Analyser.AnalyserConfig;
import visbox.ColorManager;

public class Mouth extends LineVisualiser {
    
    private final List<HistSample> history = new ArrayList<HistSample>();
    private float bgAmp = 0f; // For background
    private long lastAmpTick;
    
    private float GAIN = 1.2f;
    private float GAMMA = 1.1f;
    
    private float maxOffset = -1f;
    private float offsetDelta = 15;
    private float tailLen = 50;
    
    private class HistSample {
        float v;
        float oV;
        float pos;
        
        HistSample(float v, float pos) {
            this.v = v;
            this.oV = v;
            this.pos = pos;
        }
    }
    
    public Mouth(Analyser analyser, ColorManager colorManager) {
        super(analyser, colorManager, -1);
    }
    
    @Override
    public void activate(AnalyserConfig a) {
        super.activate(a);
        colorManager.setHue(0f, -0.2f); // 0 0.8
    }
    
    @Override
    public void update() {
        super.update();
        
        float level;
        boolean update = false;
        synchronized (analyser) {
            long tick = analyser.getAmpTick();
            if (tick!=lastAmpTick) update = true;
            level = analyser.getCurrentLevel();
            lastAmpTick = tick;
            
            /*level = 0.5f;
            if (VBMain.isTimeIncrement(50)) update = true;
            else if (VBMain.isTimeIncrement(10)) {
                level = 0f;
                update = true;
            }*/
        }
        
        // Shape and clamp
        if (update) {
            level = (float) Math.pow(level*GAIN, GAMMA);
            level = Math.max(0, Math.min(1f, level));
            if (level<0.05f) level = 0f;

            if (level>=bgAmp) {
                bgAmp = level*4;
                if (bgAmp>1f) bgAmp = 1f;
            }
        }
        else {
            bgAmp -= 0.02f;
            if (bgAmp<0f) bgAmp = 0f;
        }
        
        // Adjust history
        synchronized (history) {
            // Move all and remove expired
            if (maxOffset!=-1f) {
                for (int i=history.size()-1; i>=0; i--) {
                    HistSample s = history.get(i);
                    s.pos += offsetDelta;
                    
                    // Decay if in tail zone
                    if (s.pos>=maxOffset-tailLen) {
                        s.v-=s.oV/(tailLen/offsetDelta);
                        if (s.v<0f) s.v = 0f;
                    }
                    if (s.v<0f) s.v = 0f;
                    
                    if (s.pos>=maxOffset) history.remove(i);
                }
            }
            
            // Add new
            if (update) history.add(0, new HistSample(level, 0));
        }
    }
    
    @Override
    public void render(Graphics2D g2, int width, int height) {
        // Set maxOffset
        maxOffset = (width/2)*0.96f;
        
        g2.setColor(Color.BLACK);
        g2.fillRect(0, 0, width, height);

        // Paint bg
        float[] c1 = colorManager.getColorAt(bgAmp);
        Color[] cols = new Color[] {
            new Color(c1[0], c1[1], c1[2], 0.6f*bgAmp),
            new Color(0f, 0f, 0f, 0f)
        };
        RadialGradientPaint rP = new RadialGradientPaint(width*0.5f, (float) (height*0.5f), width*0.5f, new float[] {0f, 1f}, cols, CycleMethod.NO_CYCLE);
        g2.setPaint(rP);;
        g2.fillRect(0, 0, width, (int) (height));
        //g2.setTransform(oldT);
        
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        
        float yScale = height * 0.45f;
        float xCent = width*0.5f;
        float yC = height*0.5f;
        float yIC = height*0.5f;
        
        int n = (history.size())*2+4;
        float[] xs = new float[n];
        float[] ys = new float[n];
        float[] ysI = new float[n];
        
        // Fill in edge points
        xs[0] = 0; ys[0] = yC; ysI[0] = yIC;
        xs[1] = width*0.02f; ys[1] = yC; ysI[1] = yIC;
        xs[n-2] = width-(width*0.02f); ys[n-2] = yC; ysI[n-2] = yIC;
        xs[n-1] = width; ys[n-1] = yC; ysI[n-1] = yIC;
        
        int l = history.size()+1;
        int r = history.size()+2;
        
        synchronized (history) {
            for (HistSample s : history) {
                float y = yC-(s.v*yScale);
                float yI = yIC+(s.v*yScale);
                
                xs[l] = xCent-s.pos;
                ys[l] = y; ysI[l] = yI;
                l--;
                
                xs[r] = xCent+s.pos;
                ys[r] = y; ysI[r] = yI;
                r++;
            }
        }
        
        // Build smooth path
        GeneralPath path1 = getCurvedPath(xs, ys);
        GeneralPath path2 = getCurvedPath(xs, ysI);
        
        // Draw lines
        float firstA = 1f;
        if (bgAmp<0.01f) firstA = 0.3f;
        float[] c2 = colorManager.getColorDif(c1, new float[]{1, 1, 1}, bgAmp*1.5f);
        cols = new Color[] {
            new Color(c2[0], c2[1], c2[2], firstA),
            //new Color(c1[0], c1[1], c1[2], 1f),
            new Color(c1[0], c1[1], c1[2], 0.3f),
            new Color(c1[0], c1[1], c1[2], 0f),
        };

        float sStart = 0.01f;
        float sEnd = 0.95f;
        float sSize = sEnd-sStart;

        float p2 = sStart+((sSize*0.99f)*bgAmp);
        float p3 = sEnd-((sSize*0.99f)*(1f-bgAmp));
        float[] fracs = new float[] {0f, p3, 1f};

        rP = new RadialGradientPaint(width*0.5f, height*0.5f, width*0.5f, fracs, cols, CycleMethod.NO_CYCLE);
        g2.setPaint(rP);
        g2.setStroke(new BasicStroke(5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

        g2.draw(path1);
        g2.draw(path2);
    }
    
    private GeneralPath getCurvedPath(float[] xs, float[] ys) {
        GeneralPath path = new GeneralPath();
        if (xs==null||xs.length==0||ys==null||ys.length==0) return path;
        path.moveTo(xs[0], ys[0]);
        float tension = 0.5f;
        
        for (int j=0; j<xs.length-1; j++) {
            int i0 = Math.max(0, j - 1);
            int i1 = j;
            int i2 = j+1;
            int i3 = Math.min(xs.length-1, j+2);
            
            float x0 = xs[i0], y0 = ys[i0];
            float x1 = xs[i1], y1 = ys[i1];
            float x2 = xs[i2], y2 = ys[i2];
            float x3 = xs[i3], y3 = ys[i3];
            
            float cx1 = x1 + (x2 - x0) * tension / 3f;
            float cy1 = y1 + (y2 - y0) * tension / 3f;
            
            float cx2 = x2 - (x3 - x1) * tension / 3f;
            float cy2 = y2 - (y3 - y1) * tension / 3f;
            
            // Stop same value neighbour artifact
            if (Math.abs(y2-y1)<0.0001f) {
                cy1 = y1;
                cy2 = y2;
            }
            
            path.curveTo(cx1, cy1, cx2, cy2, x2, y2);
        }
        return path;
    }
}