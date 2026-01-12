package visbox;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

import javax.swing.JPanel;
import javax.swing.Timer;

import visbox.logger.LogColor;
import visbox.logger.LogColorEnum;
import visbox.logger.Logger;

@LogColor(LogColorEnum.PURPLE)
public class UI extends JPanel {

    private VBMain vbMain;
    private BufferedImage backBuffer;

    private static final int TARGET_FPS = 60;

    public UI(VBMain vbMain) {
        this.vbMain = vbMain;
        setDoubleBuffered(true);
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

            vbMain.getCurrentVisualiser().render(g, w, h);

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
