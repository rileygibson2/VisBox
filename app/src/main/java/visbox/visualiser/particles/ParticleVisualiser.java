package visbox.visualiser.particles;

import visbox.Analyser;
import visbox.ColorManager;
import visbox.visualiser.Visualiser;

public abstract class ParticleVisualiser extends Visualiser {
    
    public enum ParticleVisualiserType {
        BarParticles
    }

    class Particle {
        float size;
        float x, y;
        float vx, vy;
        float life;
        float maxLife;
        float hue, sat;
        float brightness;
        int band;
    }
    
    protected float[] bandLast;
    
    public ParticleVisualiser(String displayName, int numBands) {
        super(displayName, numBands);
        this.bandLast = new float[numBands];
    }
}
