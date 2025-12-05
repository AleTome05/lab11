package it.unibo.oop.reactivegui02;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.BorderLayout;
import java.awt.FlowLayout;

import it.unibo.oop.JFrameUtil;

import java.io.Serial;
import java.lang.reflect.InvocationTargetException;

/**
 * Second e xample of reactive GUI.
 */
public final class ConcurrentGUI extends JFrame {

    @Serial
    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = LoggerFactory.getLogger(ConcurrentGUI.class);
    private final JLabel display = new JLabel("0");

    /**
     * Builds a new CGUI.
     */
    public ConcurrentGUI() {
        super();
        JFrameUtil.dimensionJFrame(this);
        final JPanel mainPanel = new JPanel(new BorderLayout());
        final JPanel buttoPanel = new JPanel(new FlowLayout());
        final JButton down = new JButton("down");
        final JButton up = new JButton("up");
        final JButton stop = new JButton("stop");
        buttoPanel.add(up);
        buttoPanel.add(down);
        buttoPanel.add(stop);
        mainPanel.add(display, BorderLayout.CENTER);
        mainPanel.add(buttoPanel, BorderLayout.SOUTH);
        this.getContentPane().add(mainPanel);
        final Agent agent = new Agent();

        /*
         * Create the counter agent and start it. This is actually not so good:
         * thread management should be left to
         * java.util.concurrent.ExecutorService
         */
        new Thread(agent).start();
        /*
         * Register a listener that stops it
         */
        up.addActionListener(e -> agent.setIncrement(true));
        down.addActionListener(e -> agent.setIncrement(false));
        stop.addActionListener(e -> {
            agent.stopCounting();
            up.setEnabled(false);
            down.setEnabled(false);
            stop.setEnabled(false);
        });

        this.setVisible(true);
    }

    private final class Agent implements Runnable {
        private volatile boolean stop;
        private volatile boolean increment = true;
        private int counter;

        @Override
        public void run() {
            while (!this.stop) {
                try {
                    final var nextText = Integer.toString(this.counter);
                    SwingUtilities.invokeAndWait(() -> ConcurrentGUI.this.display.setText(nextText));

                    if (increment) {
                        counter++;
                    } else {
                        counter--;
                    }

                    Thread.sleep(100);
                } catch (InvocationTargetException | InterruptedException ex) {
                    LOGGER.error(ex.getMessage(), ex);
                    this.stop = true;
                    Thread.currentThread().interrupt();
                }
            }
        }

        /**
         * External command to stop counting.
         */
        public void stopCounting() {
            this.stop = true;
        }

        public void setIncrement(final boolean inc) {
            this.increment = inc;
        }
    }
}
