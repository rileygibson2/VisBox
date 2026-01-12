package visbox.visualiser.lines;

import visbox.Analyser;
import visbox.Analyser.AnalyserConfig;
import visbox.ColorManager;
import visbox.visualiser.Visualiser;

public abstract class LineVisualiser extends Visualiser {
    
    public enum LineVisualiserType {
        MOUTH
    }
    
    protected LineVisualiserType type;
    
    
    public LineVisualiser(Analyser analyzer, ColorManager colorManager, int numBands) {
        super(analyzer, colorManager, numBands);
        this.type = LineVisualiserType.MOUTH;
    }
    
    @Override
    public void activate(AnalyserConfig a) {
        if (a==null) a = analyser.getNewConfig();
        analyser.setConfig(a);
    }

    @Override
    public void update() {
        //analyser.getBands(bands);
    }

}
