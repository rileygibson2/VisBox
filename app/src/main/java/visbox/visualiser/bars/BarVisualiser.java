package visbox.visualiser.bars;

import visbox.Analyser;
import visbox.Analyser.AnalyserConfig;
import visbox.ColorManager;
import visbox.visualiser.Visualiser;

public abstract class BarVisualiser extends Visualiser {
    
    public enum BarVisualiserType {
        SQUARE_BARS,
        WHITE_TOPS
    }
    
    protected float[] bandReal;
    
    public BarVisualiser(Analyser analyzer, ColorManager colorManager, int numBands) {
        super(analyzer, colorManager, numBands);
        this.bandReal = new float[numBands];
    }
}
