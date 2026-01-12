package visbox.visualiser;

import java.awt.Graphics2D;

import visbox.Analyser;
import visbox.Analyser.AnalyserConfig;
import visbox.ColorManager;
import visbox.logger.Logger;

public abstract class Visualiser {
    
    protected Analyser analyser;
    protected ColorManager colorManager;

    protected float[] bands;
    protected int numBands;

    public Visualiser(Analyser analyser, ColorManager colorManager, int numBands) {
        this.analyser = analyser;
        this.colorManager = colorManager;
        this.numBands = numBands;
        if (numBands>0) this.bands = new float[numBands];
    }

    public void activate(AnalyserConfig a) {
        if (numBands>0) a.NUM_BANDS = numBands;
        analyser.setConfig(a);
    }

    public void update() {
        if (numBands>0) analyser.getBands(bands);
    }

    public abstract void render(Graphics2D g, int w, int h);
}
