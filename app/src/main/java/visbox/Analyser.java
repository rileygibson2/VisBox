package visbox;

import org.jtransforms.fft.FloatFFT_1D;

import visbox.logger.Logger;

public class Analyser {
    
    private static final int FFT_SIZE = 2048;
    private static final float WINDOW_COHERENT_GAIN = 0.5f;    // Hann approx
    private static final float FULL_SCALE_MAG = (FFT_SIZE * 0.5f) * WINDOW_COHERENT_GAIN;
    
    private final FloatFFT_1D fft = new FloatFFT_1D(FFT_SIZE);
    private final float[] window = new float[FFT_SIZE];
    private float sampleRate;
    
    // Time-domain ring buffer
    private final float[] sampleBuffer = new float[FFT_SIZE];
    private int writePos = 0;
    private boolean bufferFilled = false;
    
    // Frequency-domain magnitudes (current buckets)
    private final float[] fftMagnitudes = new float[FFT_SIZE / 2];
    
    //Amp output
    private float ampAcc = 0f;
    private int ampCount = 0;
    private final float levelSmooth = 0.2f;
    private float currentLevel;
    
    // FFT output
    private float[] rawBins;
    private float[] bandEdges;
    private float[] bands;
    private int[] bandBinCounts;
    
    // Bin-space band layout
    private int firstBinIndex;       // first FFT bin we use (>= 1)
    private int lastBinIndex;        // last FFT bin we use
    private int[] bandBinStart;      // per band: start bin index (inclusive)
    private int[] bandBinEnd;        // per band: end bin index (exclusive)
    
    // Controls
    private int NUM_BANDS;
    private int AMP_WINDOW; // Effective amp sample size for rms
    private float MIN_FQ;
    private float MAX_FQ;
    private float DB_MIN;  // anything quieter → 0
    private float DB_MAX;    // 0 dBFS → 1
    private float GAIN;
    private float GAMMA;
    
    // Ticks
    private long ampTick = 0L;
    private long fftTick = 0L;
    
    public class AnalyserConfig {
        public int NUM_BANDS = 24;
        public float MIN_FQ = 30f;
        public float MAX_FQ = 8000f;
        public float DB_MIN = -60f;
        public float DB_MAX = 0f;
        public float GAIN = 1.2f;
        public float GAMMA = 1.6f;
        public int AMP_WINDOW = 64;
    };
    
    public Analyser() {
        // Hann window
        for (int n = 0; n < FFT_SIZE; n++) {
            window[n] = 0.5f * (1f - (float) Math.cos(2.0 * Math.PI * n / (FFT_SIZE - 1)));
        }
    }
    
    public void setConfig(AnalyserConfig config) {
        NUM_BANDS = config.NUM_BANDS;
        MIN_FQ = config.MIN_FQ;
        MAX_FQ = config.MAX_FQ;
        AMP_WINDOW = config.AMP_WINDOW;
        DB_MIN = config.DB_MIN;
        DB_MAX = config.DB_MAX;
        GAIN = config.GAIN;
        GAMMA = config.GAMMA;
        
        buildBands();
    }
    
    public AnalyserConfig getNewConfig() {return new AnalyserConfig();}
    
    /**
    * Called from the audio thread with new mono samples (e.g. already converted from shorts).
    */
    public synchronized void addSamples(float[] samples, int count) {
        for (int i = 0; i < count; i++) {
            float s = samples[i];
            
            // FFT
            sampleBuffer[writePos++] = s;
            if (writePos >= FFT_SIZE) {
                writePos = 0;
                bufferFilled = true;
            }
            
            // Amplitude
            ampAcc += s*s;
            ampCount++;
            if (ampCount>AMP_WINDOW) {
                float rms = (float) Math.sqrt(ampAcc/ampCount);
                currentLevel = currentLevel*(1f-levelSmooth)+rms*levelSmooth;
                ampAcc = 0f;
                ampCount = 0;
                ampTick++;
            }
        }
        
        if (bufferFilled) {
            computeFft();
            fftTick++;
        }
    }
    
    public float getCurrentLevel() {return currentLevel;}
    
    public long getAmpTick() {return ampTick;}
    
    public long getFFTTick() {return fftTick;}
    
    private void computeFft() {
        float[] fftInput = new float[FFT_SIZE];
        
        // Copy current buffer snapshot into a linear frame and apply window
        int pos = writePos; // current write position is the "end"
        for (int n = 0; n < FFT_SIZE; n++) {
            int idx = (pos + n) % FFT_SIZE; // unwrap ring
            fftInput[n] = sampleBuffer[idx] * window[n];
        }
        
        // JTransforms real forward expects in-place real buffer
        fft.realForward(fftInput);
        
        // Convert from complex to real
        int bins = FFT_SIZE / 2;
        for (int i = 0; i < bins; i++) {
            float re = fftInput[2 * i];
            float im = fftInput[2 * i + 1];
            float mag = (float) Math.sqrt(re * re + im * im);
            fftMagnitudes[i] = mag;
        }
    }
    
    public synchronized void copyRawBuckets(float[] out) {
        int n = Math.min(out.length, fftMagnitudes.length);
        System.arraycopy(fftMagnitudes, 0, out, 0, n);
    }

    public void updateSampleRate(float sampleRate) {
        this.sampleRate = sampleRate;
        buildBands();
    }
    
    private void buildBands() {
        Logger.info("Building bands ("+NUM_BANDS+")");
        bands = new float[NUM_BANDS];
        rawBins = new float[FFT_SIZE/2];
        bandBinCounts = new int[NUM_BANDS]; 
        sampleRate = VBMain.getInstance().getAudioManager().getSampleRate();
        
        if (sampleRate<=0f) {
            Logger.error("Sample rate not set in buildBands");
            return;
        }
        computeBinBandsLog();
    }
    
    private void computeBinBandsLog() {
        float binWidth = sampleRate / FFT_SIZE;
        
        // Find first and last usable bins based on MIN_FQ / MAX_FQ
        int first = (int) Math.ceil(MIN_FQ / binWidth);
        int last  = (int) Math.floor(MAX_FQ / binWidth);
        
        // Skip DC and any weird cases
        if (first < 1) first = 1;
        int maxPossible = (FFT_SIZE / 2) - 1;
        if (last > maxPossible) last = maxPossible;
        
        if (last <= first) {
            Logger.error("Bad band config: no usable bins (first=" + first + ", last=" + last + ")");
            firstBinIndex = 1;
            lastBinIndex = 1;
            bandBinStart = new int[0];
            bandBinEnd   = new int[0];
            return;
        }
        
        firstBinIndex = first;
        lastBinIndex  = last;
        int usableBins = lastBinIndex - firstBinIndex + 1;
        
        if (usableBins < NUM_BANDS) {
            Logger.warn("Not enough bins (" + usableBins + ") for " + NUM_BANDS + " bands. Some bands will be merged.");
            // In extreme cases you could reduce NUM_BANDS here if you want.
        }
        
        bandBinStart = new int[NUM_BANDS];
        bandBinEnd   = new int[NUM_BANDS];
        
        // We do log spacing in BIN INDEX space from [firstBinIndex .. lastBinIndex+1)
        double minBinD = firstBinIndex;
        double maxBinD = lastBinIndex + 1; // treat as exclusive upper edge
        double logMin  = Math.log(minBinD);
        double logMax  = Math.log(maxBinD);
        
        int prevEnd = firstBinIndex;
        
        for (int b = 0; b < NUM_BANDS; b++) {
            double t0 = (double) b / NUM_BANDS;
            double t1 = (double) (b + 1) / NUM_BANDS;
            
            int start = (int) Math.round(Math.exp(logMin + (logMax - logMin) * t0));
            int end   = (int) Math.round(Math.exp(logMin + (logMax - logMin) * t1));
            
            // Enforce monotonic & at least 1 bin per band
            if (start < prevEnd) start = prevEnd;
            if (end <= start)    end   = start + 1;
            if (end > lastBinIndex + 1) end = lastBinIndex + 1;
            
            bandBinStart[b] = start;
            bandBinEnd[b]   = end;
            
            prevEnd = end;
        }
        
        // Optional: log the mapping for debugging
        Logger.info("Band bin ranges: ");
        for (int b = 0; b < NUM_BANDS; b++) {
            int s = bandBinStart[b];
            int e = bandBinEnd[b];
            float f0 = s * binWidth;
            float f1 = (e - 1) * binWidth;
            Logger.info("["+b+"]("+(e-s)+") "+s+"-"+(e-1)+" ("+(int) f0+"-"+(int) f1+" Hz)  ");
        }
    }
    
    public void getBands(float[] outBands) {
        if (outBands==null||bands == null) return;
        copyRawBuckets(rawBins);
        compressRawToBands();
        normalizeFullScale(bands, outBands);
    }
    
    public void compressRawToBands() {
        if (bands == null || bandBinStart == null || bandBinEnd == null) return;
        
        int numBands = bands.length;
        
        // Clear outputs
        for (int b = 0; b < numBands; b++) {
            bands[b] = 0f;
            bandBinCounts[b] = 0;
        }
        
        for (int b = 0; b < numBands; b++) {
            int start = bandBinStart[b];
            int end   = bandBinEnd[b]; // exclusive
            
            // Clamp safety
            if (start < 1) start = 1;
            if (end > rawBins.length) end = rawBins.length;
            if (start >= end) continue;
            
            float sum = 0f;
            int count = 0;
            
            for (int bin = start; bin < end; bin++) {
                sum += rawBins[bin];
                count++;
            }
            
            bands[b] = sum;
            bandBinCounts[b] = count;
        }
    }
    
    private void normalizeFullScale(float[] inBands, float[] outBands) {
        if (inBands == null || outBands == null) return;
        
        for (int b = 0; b < inBands.length; b++) {
            float sumMag = inBands[b];
            int count = bandBinCounts[b];
            
            if (count == 0) {
                outBands[b] = 0f;
                continue;
            }
            
            float avgMag = sumMag / (float) count; // Average magnitude per bin in this band
            float rel = avgMag / FULL_SCALE_MAG;   // Relative to theoretical full-scale per bin 0..1+
            if (rel < 1e-9f) rel = 1e-9f;          // avoid log(0)
            
            // Convert to dBFS
            float db = 20f * (float) Math.log10(rel);  // 0 dB at full-scale
            
            // Map [DB_MIN, DB_MAX] → [0, 1]
            float norm = (db - DB_MIN) / (DB_MAX - DB_MIN);
            norm = Math.max(0f, Math.min(norm, 1f));
            
            // Shaping
            norm = norm*GAIN;
            norm = (float) Math.pow(norm, GAMMA);

            // Eq - compress low end and increase high end
            float t = (float) b/(inBands.length-1); // Band pos 0..1
            float tilt = 0.6f+(1.2f)*t;
            norm = norm*tilt;
            
            outBands[b] = Math.max(0f, Math.min(norm, 1f));
        }
    }
    
    public int getFFTSize() {return FFT_SIZE;}
}