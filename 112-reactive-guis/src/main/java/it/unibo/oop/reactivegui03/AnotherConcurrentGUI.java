package it.unibo.oop.reactivegui03;

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
 * Second example of reactive GUI.
 */
public final class AnotherConcurrentGUI extends JFrame {

    @Serial
    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = LoggerFactory.getLogger(AnotherConcurrentGUI.class);
    private static final long TIME_LIMIT_MS = 10_000;
    private final JLabel display = new JLabel("0");

    // Dichiarazione come campi di istanza
    private final JButton down = new JButton("down");
    private final JButton up = new JButton("up");
    private final JButton stop = new JButton("stop"); 
    private final transient Agent agent = new Agent();

    /**
     * Builds a new CGUI.
     */
    public AnotherConcurrentGUI() {
        super();
        JFrameUtil.dimensionJFrame(this);
        final JPanel mainPanel = new JPanel(new BorderLayout());
        final JPanel buttoPanel = new JPanel(new FlowLayout());
        buttoPanel.add(up);
        buttoPanel.add(down);
        buttoPanel.add(stop);
        mainPanel.add(display, BorderLayout.CENTER);
        mainPanel.add(buttoPanel, BorderLayout.SOUTH);
        this.getContentPane().add(mainPanel);

        /*
         * Create the counter agent and start it. This is actually not so good:
         * thread management should be left to
         * java.util.concurrent.ExecutorService
         */
        new Thread(agent).start();

        // 2. Avvia il thread dell'agente di stop automatico
        final StopperAgent stopper = new StopperAgent();
        new Thread(stopper).start();

        /*
         * Register a listener that stops it
         */
        up.addActionListener(e -> agent.setIncrement(true));
        down.addActionListener(e -> agent.setIncrement(false));
        stop.addActionListener(e -> disableAndStop());

        this.setVisible(true);
    }

    private void disableAndStop() {
         agent.stopCounting();
         up.setEnabled(false);
         down.setEnabled(false);
         stop.setEnabled(false);
    }

    private final class Agent implements Runnable {
        private volatile boolean stop;
        private volatile boolean increment = true;
        private int counter;

        @Override
        public void run() {
            while (!this.stop) {
                try {
                    SwingUtilities.invokeAndWait(() -> AnotherConcurrentGUI.this.display.setText(Integer.toString(counter)));

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

    private final class StopperAgent implements Runnable {

        @Override
        public void run() {
            try {
                // 1. Attende 10 secondi
                Thread.sleep(TIME_LIMIT_MS);

                // 2. Esegue le operazioni di stop e disabilitazione sull'EDT
                SwingUtilities.invokeAndWait(() -> {
                    // Chiama il metodo helper per fermare il contatore e disabilitare i pulsanti
                    disableAndStop();
                });

            } catch (InvocationTargetException | InterruptedException ex) {
                // Gestione degli errori/interruzioni
                Thread.currentThread().interrupt();
            }
        }
    }
}
