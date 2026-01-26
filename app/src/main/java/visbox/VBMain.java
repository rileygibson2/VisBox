package visbox;

import java.util.ArrayList;
import java.util.HashMap;

import javax.swing.Timer;

import visbox.logger.Logger;
import visbox.ui.GLFWUI;
import visbox.ui.IO;
import visbox.visualiser.Visualiser;
import visbox.visualiser.bars.ClassicBars;
import visbox.visualiser.bars.ClipBounce;
import visbox.visualiser.bars.RoundedBars;
import visbox.visualiser.lines.Mouth;
import visbox.visualiser.particles.FluidDynamics;
import visbox.visualiser.particles.ParticleBars;
import visbox.visualiser.particles.ParticleField;
import visbox.visualiser.spectrograms.ClassicSpectrogram;

public class VBMain {
    
    private static VBMain instance;
    private static AudioManager audioManager;
    private static Analyser analyser;
    private static ColorManager colorManager;
    private static GLFWUI ui;
    private static ShaderManager shaderManager;
    
    private HashMap<String, Visualiser> visualisers;
    private ArrayList<String> displayNames;
    private Visualiser currVisualiser;
    
    private static final int TARGET_FPS = 60;
    private Timer timer;
    private static long globalTick;
    private static long lastTime;
    private static float deltaTime;
    private static boolean running;
    
    private VBMain() {}
    
    public static VBMain getInstance() {
        if (instance==null) instance = new VBMain();
        return instance;
    }
    
    public static ShaderManager getShaderManager() {return shaderManager;}

    public static AudioManager getAudioManager() {return audioManager;}
    
    public static Analyser getAnalyser() {return analyser;}
    
    public static ColorManager getColorManager() {return colorManager;}
    
    public static GLFWUI getUI() {return ui;}
    
    public Visualiser getCurrentVisualiser() {return currVisualiser;}
    
    public ArrayList<String> getDisplayNames() {return displayNames;}
    
    private void setup() {
        Logger.info("Setting up");
        
        // Setup JFrame
        /*ui = new UI();
        try {
        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {}
        SwingUtilities.invokeLater(() -> {
        frame = new JFrame("VisBox");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setContentPane(ui);
        frame.setSize(960, 540);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
        
        frame.addWindowListener(new WindowAdapter() {
        @Override
        public void windowClosing(WindowEvent e) {
        VBMain.getInstance().shutdown();
        }
        });
        });
        ui.setupKeyBindings();*/
        
        ui = new GLFWUI();
        ui.init();
        
        shaderManager = new ShaderManager();
        analyser = new Analyser();
        audioManager = new AudioManager(analyser);
        colorManager = new ColorManager();
        initialiseVisualisers();
    }
    
    private void initialiseVisualisers() {
        visualisers = new HashMap<String, Visualiser>();
        displayNames = new ArrayList<String>();

        addVisualiser(new ClassicBars());
        addVisualiser(new ClipBounce());
        addVisualiser(new RoundedBars());
        addVisualiser(new ClassicSpectrogram());
        addVisualiser(new Mouth());
        addVisualiser(new ParticleBars());
        addVisualiser(new ParticleField());
        addVisualiser(new FluidDynamics());
        
        setCurrentVisualiser("FluidDynamics");
    }
    
    private void addVisualiser(Visualiser v) {
        visualisers.put(v.getDisplayName(), v);
        displayNames.add(v.getDisplayName());
    }
    
    public void setCurrentVisualiser(String displayName) {
        if (displayName==null) return;
        Visualiser v = visualisers.get(displayName);
        if (v==null) return;
        
        Logger.info("Switching visualiser: "+displayName);
        currVisualiser = v;
        currVisualiser.activate(analyser.getNewConfig());
    }
    
    private void start() {
        audioManager.start();
        lastTime = System.nanoTime();
        globalTick = 0L;
        running = true;
        
        Logger.info("Starting render thread");
        while (running) {
            long now = System.nanoTime();
            deltaTime = (now-lastTime) * 1e-9f;
            lastTime = now;
            globalTick++;

            IO.update();
            
            // Exit condition
            if (!ui.windowAlive()) running = false;
            else {
                if (currVisualiser!=null) currVisualiser.update();
                ui.iterate();
            }
        }
        Logger.info("Main loop exited");
        shutdown();
    }
    
    private void shutdown() {
        if (ui!=null) ui.cleanup();
        if (audioManager!=null) audioManager.stop();
        if (timer!=null) timer.stop();
        Logger.info("Shutting down");
    }
    
    public static float getDeltaTime() {return deltaTime;}
    
    public static long getGlobalTick() {return globalTick;}
    
    public static boolean isTickIncrement(int inc) {return globalTick%inc==0;}
    
    public static int getTargetFPS() {return TARGET_FPS;}
    
    public static void main(String[] args) {
        VBMain vb = VBMain.getInstance();
        vb.setup();
        vb.start();
    }
}