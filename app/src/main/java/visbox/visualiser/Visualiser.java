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

    private long lastAmpTick;
    private long lastFFTTick;
    private boolean newAmpData;
    private boolean newFFTData;

    public Visualiser(String displayName, int numBands) {
        this.displayName = displayName;
        this.numBands = numBands;
        if (numBands>0) this.bands = new float[numBands];
        this.newAmpData = false;
        this.newFFTData = false;
    }

    public String getDisplayName() {return displayName;}

    public void activate(AnalyserConfig a) {
        if (numBands>0) a.NUM_BANDS = numBands;
        VBMain.getAnalyser().setConfig(a);
    }

    public void resize(int w, int h) {}

    public void update() {
        if (numBands>0) {VBMain.getAnalyser().getBands(bands);}

        Analyser analyser = VBMain.getAnalyser();
        newAmpData = false;
        newFFTData = false;
        synchronized (analyser) {
            long tick = analyser.getAmpTick();
            if (tick!=lastAmpTick) newAmpData = true;
            lastAmpTick = tick;

            tick = analyser.getFFTTick();
            if (tick!=lastFFTTick) newFFTData = true;
            lastFFTTick = tick;
        }
    }

    public boolean newAmpData() {return newAmpData;}

    public boolean newFFTData() {return newFFTData;}

    public abstract void render(Graphics2D g, int w, int h);

    public abstract void render();
}
