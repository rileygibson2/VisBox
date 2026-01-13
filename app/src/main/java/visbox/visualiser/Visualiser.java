package visbox.visualiser;

import java.awt.Graphics2D;

import visbox.Analyser;
import visbox.Analyser.AnalyserConfig;
import visbox.ColorManager;
import visbox.VBMain;
import visbox.logger.Logger;

public abstract class Visualiser {
    
    protected String displayName;
    protected float[] bands;
    protected int numBands;

    public Visualiser(String displayName, int numBands) {
        this.displayName = displayName;
        this.numBands = numBands;
        if (numBands>0) this.bands = new float[numBands];
    }

    public String getDisplayName() {return displayName;}

    public void activate(AnalyserConfig a) {
        if (numBands>0) a.NUM_BANDS = numBands;
        VBMain.getInstance().getAnalyser().setConfig(a);
    }

    public void update() {
        if (numBands>0) VBMain.getInstance().getAnalyser().getBands(bands);
    }

    public abstract void render(Graphics2D g, int w, int h);
}
