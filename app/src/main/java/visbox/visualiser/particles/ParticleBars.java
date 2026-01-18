package visbox.visualiser.particles;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RadialGradientPaint;
import java.awt.image.BufferedImage;
import java.util.ArrayList;

import visbox.Analyser;
import visbox.ColorManager;

public class ParticleBars extends ParticleVisualiser {
    

    private BufferedImage glowBuffer;
    private ArrayList<Particle> particles;
    
    public ParticleBars() {
        super("ParticleBars", 16);
        this.bands = new float[numBands];
        this.particles = new ArrayList<Particle>();
    }

    public float rand(float low, float high) {
        return (float) (low+(Math.random()*(high-low)));
    }

    @Override
    public void update() {
        super.update();

        // Spawn particles
        for (int b=0; b<bands.length-1; b++) {
            float v = bands[b];
            if (v<0.01f) continue;

            int spawnCount = (int) (v*15);

            for (int i=0; i<spawnCount; i++) {
                Particle p = new Particle();

                p.band = b;
                p.x = rand(0, 1);
                p.y = rand(0, v);

                float bT = (float) b/(numBands-1);
                float speed = 1f+2f*v; // Middle div by bT if want
                double angle = (Math.random()*Math.PI*2.0);
                p.vx = (float) (Math.cos(angle)*speed);
                p.vy = (float) (Math.sin(angle)*speed);

                p.size = 2f+rand((1f*v), (4f*v));
                p.life = rand(0.8f, 1.2f);

                p.hue = (float) 0.9f+rand(-0.1f, 0.2f);
                if (p.hue>1f) p.hue -= 1f;
                else if (p.hue<0f) p.hue = 1f-p.hue;

                p.sat = 1f-rand(0.2f*v, 0.8f*v);
                p.brightness = 0.5f+(0.5f*v);
                particles.add(p);
            }
        }

        // Update existing particles
        for (int i=particles.size()-1; i>=0; i--) {
            Particle p = particles.get(i);
            p.x += p.vx*0.001;
            p.y += p.vy*0.001;

            p.life -= 0.08;

            if (p.life<=0f) particles.remove(i);
        }
    }
    
    @Override
    public void render(Graphics2D g, int w, int h) {
        if (glowBuffer==null) glowBuffer = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        float bandWidth = (float) (w/numBands);

        // Fade glow buffer
        Graphics2D gg = glowBuffer.createGraphics();
        gg.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.85f));
        gg.setColor(new Color(0f, 0f, 0f, 1f));
        gg.fillRect(0, 0, w, h);
        
        // Add to glow buffer
        for (Particle p : particles) {
            float a = p.brightness*p.life;
            if (a<=0f) continue;
            if (a>1f) a = 1f;
            Color c = ColorManager.hsvToColor(p.hue, p.sat, 1f);
            gg.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, a*0.3f));

            float x = (p.band*bandWidth)+(p.x*bandWidth);
            float y = h-(h*p.y);

            float r = p.size*3.2f;
            gg.setPaint(new RadialGradientPaint(
                x, y, r,
                new float[]{0f, 1f},
                new Color[] {new Color(c.getRed(), c.getGreen(), c.getBlue(), 255), new Color(c.getRed(), c.getGreen(), c.getBlue(), 0)}
            ));
            gg.fillOval((int) (x-r), (int) (y-r), (int) (2*r), (int) (2*r));
        }

        // Add to world
        g.drawImage(glowBuffer, 0, 0, null);

        // Draw particle centers
        for (Particle p : particles) {
            float a = p.brightness*p.life;
            if (a<=0f) continue;
            if (a>1f) a = 1f;
            float x = (p.band*bandWidth)+(p.x*bandWidth);
            float y = h-(h*p.y);
            float r = p.size/2;

            Color c = ColorManager.hsvToColor(p.hue, p.sat, 1f);
            g.setColor(c);
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, a));
            g.fillOval((int) (x-r), (int) (y-r), (int) p.size, (int) p.size);
        }

        g.setColor(Color.WHITE);
        g.drawString("C: "+particles.size(), 10, 15);
    }

    @Override
    public void render() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'render'");
    }
}
