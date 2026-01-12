package visbox;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.UIManager;

import visbox.logger.Logger;
import visbox.visualiser.Visualiser;
import visbox.visualiser.bars.RoundedBars;

public class VBMain {
    
    private static VBMain instance;
    private JFrame frame;
    private AudioManager audioManager;
    private Analyser analyser;
    private Visualiser currVisualiser;
    private ColorManager colorManager;
    private UI ui;
    
    private static final int TARGET_FPS = 60;
    private Timer timer;
    private static long globalTime;
    
    private VBMain() {}
    
    public static VBMain getInstance() {
        if (instance==null) instance = new VBMain();
        return instance;
    }
    
    public AudioManager getAudioManager() {return audioManager;}
    
    public Visualiser getCurrentVisualiser() {return currVisualiser;}

    public ColorManager getColorManager() {return colorManager;}
    
    private void setup() {
        Logger.info("Setting up");
        
        // Setup JFrame
        ui = new UI(this);
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
        
        analyser = new Analyser();
        audioManager = new AudioManager(analyser);
        colorManager = new ColorManager();
        currVisualiser = new RoundedBars(analyser, colorManager);
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

