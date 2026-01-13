package visbox;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.HashMap;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.UIManager;

import visbox.logger.Logger;
import visbox.ui.UI;
import visbox.visualiser.Visualiser;
import visbox.visualiser.bars.ClassicBars;
import visbox.visualiser.bars.ClipBounce;
import visbox.visualiser.bars.RoundedBars;
import visbox.visualiser.histograms.ClassicHistogram;
import visbox.visualiser.lines.Mouth;
import visbox.visualiser.particles.ParticleBars;

public class VBMain {
    
    private static VBMain instance;
    private JFrame frame;
    private AudioManager audioManager;
    private Analyser analyser;
    private ColorManager colorManager;
    private UI ui;

    private HashMap<String, Visualiser> visualisers;
    private ArrayList<String> displayNames;
    private Visualiser currVisualiser;
    
    private static final int TARGET_FPS = 60;
    private Timer timer;
    private static long globalTime;
    
    private VBMain() {}
    
    public static VBMain getInstance() {
        if (instance==null) instance = new VBMain();
        return instance;
    }
    
    public AudioManager getAudioManager() {return audioManager;}

    public Analyser getAnalyser() {return analyser;}

    public ColorManager getColorManager() {return colorManager;}

    public UI getUI() {return ui;}
    
    public Visualiser getCurrentVisualiser() {return currVisualiser;}

    public ArrayList<String> getDisplayNames() {return displayNames;}
    
    private void setup() {
        Logger.info("Setting up");
        
        // Setup JFrame
        ui = new UI();
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
        ui.setupKeyBindings();
        
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

        addVisualiser(new ClassicHistogram());
        addVisualiser(new Mouth());
        addVisualiser(new ParticleBars());

        setCurrentVisualiser("ClipBounce");
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
        
        Logger.info("Starting render thread");
        globalTime = 0L;
        timer = new Timer(1000/TARGET_FPS, e -> {
            if (currVisualiser!=null) currVisualiser.update();
            ui.renderAndPaint();
            
            globalTime++;
            if (globalTime>Integer.MAX_VALUE) globalTime = 1;
        });
        timer.start();
    }
    
    private void shutdown() {
        if (audioManager!=null) audioManager.stop();
        if (frame!=null) frame.dispose();
        if (timer!=null) timer.stop();
        Logger.info("Shutting down");
    }

    public static long getGlobalTime() {return globalTime;}

    public static boolean isTimeIncrement(int inc) {return globalTime%inc==0;}

    public static int getTargetFPS() {return TARGET_FPS;}
    
    public static void main(String[] args) {
        VBMain vb = VBMain.getInstance();
        vb.setup();
        vb.start();
    }
}

