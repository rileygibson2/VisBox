package visbox.ui;

import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.LinearGradientPaint;
import java.awt.RadialGradientPaint;
import java.awt.RenderingHints;
import java.util.ArrayList;

import visbox.VBMain;
import visbox.logger.Logger;

public class PickerOverlay {
    
    int nameI;
    float transition;
    float transDir;
    int lifetime;
    int maxLifetime = 50;
    
    public PickerOverlay() {
        VBMain vb = VBMain.getInstance();
        ArrayList<String> names = vb.getDisplayNames();
        String cur = vb.getCurrentVisualiser().getDisplayName();
        
        for (int i=0; i<names.size(); i++) {
            if (names.get(i).equals(cur)) {
                this.nameI = i;
                break;
            }
        };
        
        this.lifetime = maxLifetime;
        this.transition = 1f;
    }
    
    public boolean isAlive() {return lifetime>0;}
    
    public void slide(int dir) {
        if (transition<1f) return; // Still transitioning
        VBMain vb = VBMain.getInstance();
        int next = nameI+dir;
        if (next<0||next>vb.getDisplayNames().size()-1) return;
        
        lifetime = maxLifetime;
        nameI = next;
        transition = 0f;
        transDir = dir;
        
        vb.setCurrentVisualiser(vb.getDisplayNames().get(next));
    }
    
    public void render(Graphics2D g, int w, int h) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        
        VBMain vb = VBMain.getInstance();
        ArrayList<String> names = vb.getDisplayNames();
        
        int i = 0;
        for (int p=nameI-1; p<nameI+2; p++, i++) {
            String s = "";
            if (p>=0&&p<=names.size()-1) s = names.get(p);
            
            float fSLow = 20f;
            float fSHigh = 80f;
            float fSize = i==1?fSHigh:fSLow;
            int xAnc = 0;
            if (i==0) xAnc = (int) (w*0.1);
            if (i==1) xAnc = (int) (w*0.5);
            if (i==2) xAnc = (int) (w*0.9);
            if (transition<1f) {
                if (transDir==-1f) {
                    if (i==1) fSize = fSLow+(fSHigh-fSLow)*transition;
                    if (i==2) fSize = fSHigh-(fSHigh-fSLow)*transition;
                } else {
                    if (i==1) fSize = fSLow+(fSHigh-fSLow)*transition;
                    if (i==0) fSize = fSHigh-(fSHigh-fSLow)*transition;
                }
            }
            
            g.setFont(g.getFont().deriveFont(fSize));
            
            FontMetrics fm = g.getFontMetrics();
            int textW = fm.stringWidth(s);
            int textA = fm.getAscent();
            int textD = fm.getDescent();
            int y = h/2+(textA-textD)/2;
            
            if (transition<1f) {
                xAnc += transDir*((w*0.4)*(1f-transition));
            }
            int x = xAnc-textW/2;
            
            float a = 1f;
            if (lifetime<maxLifetime*0.2f) { // Fade out
                a = (float) (lifetime/(maxLifetime*0.2f));
            }
            
            g.setPaint(new LinearGradientPaint(
                0, 0, w, 0f,
                new float[]{0f, 0.5f, 1f},
                new Color[] {new Color(0f, 0f, 0f, 0.2f*a), new Color(1f, 1f, 1f, a), new Color(0f, 0f, 0f, 0.2f*a)}
            ));
            
            g.drawString(s, x, y);
        }
        
        if (transition<1f) {
            transition += 0.05;
        } else lifetime -= 1;
    }
}
