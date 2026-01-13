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
    
    
    public LineVisualiser(String displayName, int numBands) {
        super(displayName, numBands);
        this.type = LineVisualiserType.MOUTH;
    }

    @Override
    public void update() {
        //analyser.getBands(bands);
    }

}
