package visbox.ui;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.KeyStroke;

import visbox.VBMain;
import visbox.logger.LogColor;
import visbox.logger.LogColorEnum;
import visbox.logger.Logger;

@LogColor(LogColorEnum.PURPLE)
public class UI extends JPanel {

    private BufferedImage backBuffer;
    private PickerOverlay pickerOverlay;

    private static final int TARGET_FPS = 60;

    public UI() {
        setDoubleBuffered(true);
    }

    public void setupKeyBindings() {
        InputMap im = this.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap am = this.getActionMap();

        im.put(KeyStroke.getKeyStroke("LEFT"), "leftPressed");
        am.put("leftPressed", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                //VBMain.getInstance().getUI().keyPressed(-1);
            }
        });

        im.put(KeyStroke.getKeyStroke("RIGHT"), "rightPressed");
        am.put("rightPressed", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                //VBMain.getInstance().getUI().keyPressed(1);
            }
        });
    }

    public void keyPressed(int dir) {
        Logger.info("Key pressed: "+(dir==-1 ? "Left" : "Right")+" Arrow");
        if (pickerOverlay==null) pickerOverlay = new PickerOverlay();
        pickerOverlay.slide(dir);
    }

    private void ensureBackBuffer() {
        int w = getWidth();
        int h = getHeight();

        if (w <= 0 || h <= 0) {
            backBuffer = null;
            return;
        }

        if (backBuffer == null || backBuffer.getWidth() != w || backBuffer.getHeight() != h) {
            backBuffer = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        }
    }

    public void renderAndPaint() {
        // Render to back buffer
        ensureBackBuffer();
        if (backBuffer == null) return;

        Graphics2D g = backBuffer.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = backBuffer.getWidth();
            int h = backBuffer.getHeight();

            g.setColor(Color.BLACK);
            g.fillRect(0, 0, w, h);

            VBMain.getInstance().getCurrentVisualiser().render(g, w, h);
            
            if (pickerOverlay!=null) {
                if (pickerOverlay.isAlive()) pickerOverlay.render(g, w, h);
                else pickerOverlay = null;
            }

        } finally {
            g.dispose();
        }

        // Frame repaint
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        if (backBuffer == null) {
            g.setColor(Color.BLACK);
            g.fillRect(0, 0, getWidth(), getHeight());
            return;
        }

        g.drawImage(backBuffer, 0, 0, getWidth(), getHeight(), null);
    }
}
