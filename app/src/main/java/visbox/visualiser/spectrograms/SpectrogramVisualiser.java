package visbox.visualiser.spectrograms;

import visbox.Analyser;
import visbox.ColorManager;
import visbox.logger.Logger;
import visbox.visualiser.Visualiser;

public abstract class SpectrogramVisualiser extends Visualiser {
    
    public enum HistogramVisualiserType {
    }
    
    protected float[] bandsReal;
    protected int historyLength;
    private float[][] history;
    protected float[][] blurredHistory;
    private int head;
    
    public SpectrogramVisualiser(String displayName, int numBands, int historyLength) {
        super(displayName, numBands);
        this.bandsReal = new float[numBands];
        this.historyLength = historyLength;
        this.history = new float[historyLength][numBands];
        this.blurredHistory = new float[historyLength][numBands];
        this.head = 0;
    }
    
    public void pushBandsToHistoy(float[] bands) {
        head--;
        if (head<0) head = historyLength-1;
        System.arraycopy(bands, 0, history[head], 0, numBands);
    }
    
    protected void rebuildBlurredHistory() {
        // 1D vertical blur per band over age
        // kernel: [1, 4, 6, 4, 1]
        final float[] kernel = {1f, 4f, 6f, 4f, 1f};
        final float norm = 1f / (1f + 4f + 6f + 4f + 1f); // 1/16
        
        for (int age = 0; age < historyLength; age++) {
            for (int band = 0; band < numBands; band++) {
                float acc = 0f;
                float wsum = 0f;
                
                for (int k = -2; k <= 2; k++) {
                    int a = age + k;
                    if (a < 0 || a >= historyLength) continue;
                    float w = kernel[Math.abs(k)];
                    acc  += w * getHistoryValue(a, band);
                    wsum += w;
                }
                
                blurredHistory[age][band] = (wsum > 0f) ? (acc / wsum) : 0f;
            }
        }
    }
    
    public float getHistoryValue(int age, int band) {
        int idx = head+age;
        if (idx>=historyLength) idx -= historyLength;
        return history[idx][band];
        
        //Logger.debugAt(100, age/(float) (historyLength-1)+"");
        //return age/(float) (historyLength-1);
        //return band/(float) (numBands-1);
    } 
}
