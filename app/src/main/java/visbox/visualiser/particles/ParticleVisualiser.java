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
        float hue, sat;
        float brightness;
        int band;
    }
    
    
    public ParticleVisualiser(Analyser analyzer, ColorManager colorManager, int numBands) {
        super(analyzer, colorManager, numBands);
    }
}
