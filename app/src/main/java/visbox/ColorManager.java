package visbox;

import java.awt.Color;

import visbox.logger.Logger;

public class ColorManager {
    
    public enum ColorMode {
        LOCAL,
        HUE,
    }
    
    private float hue;
    private float hueShift;
    
    private Color low;
    private Color high;
    
    public ColorManager() {
        setHue(0f, 0.8f);
    }
    
    public Color getLowColor() {return low;}
    public Color getHighColor() {return high;}
    
    public void setHue(float hue, float hueShift) {
        this.hue = hue;
        this.hueShift = hueShift;
        float hue2 = hue+hueShift;
        if (hue2>1f) hue2 -= 1f;
        else if (hue2<0f) hue2 = 1f+hue2;

        Logger.info("Setting Hue 1: "+hue+" Hue 2: "+hue2);
        low = hueToColor(hue);
        high = hueToColor(hue2);
    }
    
    public float[] getColorAt(float v) {return getColorDif(low, high, v);}
    
    public float[] getColorDif(Color low, Color high, float v) {
        float[] l = new float[]{low.getRed()/255, low.getGreen()/255, low.getBlue()/255};
        float[] h = new float[]{high.getRed()/255, high.getGreen()/255, high.getBlue()/255};
        return getColorDif(l, h, v);
    }
    
    public float[] getColorDif(float[] low, float[] high, float v) {
        if (v<0f) v = 0f;
        if (v>1f) v = 1f;
        
        float r = (low[0]+v*(high[0]-low[0]));
        float g = (low[1]+v*(high[1]-low[1]));
        float b = (low[2]+v*(high[2]-low[2]));
        return new float[]{r, g, b};
    }
    
    public static Color hueToColor(float hue) {
        int rgb = Color.HSBtoRGB(hue, 1f, 1f);
        return new Color(rgb | 0xFF000000, true);
    }
    
    public static Color hsvToColor(float h, float s, float v) {
        int rgb = Color.HSBtoRGB(h, s, v);
        return new Color(rgb | 0xFF000000, true);
    }

    public static Color hsvToColor(float h, float s, float v, float a) {
        int rgb = Color.HSBtoRGB(h, s, v);
        Color c = new Color(rgb | 0xFF000000, true);
        return new Color(c.getRed()/255, c.getGreen()/255, c.getBlue()/255, a);
    }
}
