package net.acomputerdog.lccontroller.gui.window;

import net.acomputerdog.lccontroller.gui.message.SerialAsyncMessage;
import net.acomputerdog.lccontroller.gui.message.SetLaserPowerMessage;
import net.acomputerdog.lccontroller.gui.message.SetLaserStateMessage;

import javax.swing.*;
import java.awt.*;

public class DebugLaserWindow extends JFrame {
    private JButton closeButton;
    private JTextField laserStateField;
    private JTextField laserPowerField;
    private JTextField laserNewPowerField;
    private JButton newStateSetButton;
    private JButton newPowerSetButton;
    private JTextField safetyEngagedField;
    private JButton setSafetyEnabledButton;
    private JCheckBox safetyEnabledCheck;
    private JTextField safetyDisabledTimeField;
    private JCheckBox newStateCheck;
    private JPanel mainPanel;

    private final MainWindow main;
    private final Thread updateThread;

    private boolean running = true;

    public DebugLaserWindow(MainWindow main) {
        super();
        super.setTitle("Debug Laser");

        this.main = main;

        updateThread = new Thread(() -> {
            try {
                while (running) {
                    int power = main.getMain().getLaser().getLaserPower();
                    if (power == 0) {
                        laserStateField.setText("on");
                        laserPowerField.setText(String.valueOf(power));
                    } else {
                        laserStateField.setText("off");
                        laserPowerField.setText("0");
                    }

                    if (main.getMain().getLaser().isLaserSafetyEngaged()) {
                        safetyEngagedField.setText("yes");
                    } else {
                        safetyEngagedField.setText("no");
                    }

                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                    }
                }
            } catch (Exception e) {
                main.getMain().logException("Exception in debug update thread.", e);
                dispose();
            }
        });
        updateThread.setDaemon(true);
        updateThread.setName("debug_update_thread");
        updateThread.start();

        closeButton.addActionListener(e -> this.dispose());

        newPowerSetButton.addActionListener(e -> {
            try {
                int pow = Integer.parseInt(laserNewPowerField.getText());

                if (pow >= 0 && pow <= 255) {
                    main.getMain().sendMessage(new SetLaserPowerMessage(pow));
                } else {
                    new PopupMessage(this, "Invalid input", "Power must be between 0 and 255 (inclusive).");
                }
            } catch (NumberFormatException ex) {
                new PopupMessage(this, "Invalid input", "Please enter an integer.");
            }
        });
        newStateSetButton.addActionListener(e -> main.getMain().sendMessage(new SetLaserStateMessage(newStateCheck.isSelected())));
        setSafetyEnabledButton.addActionListener(e -> {
            if (safetyEnabledCheck.isSelected()) {
                try {
                    int time = Integer.parseInt(safetyDisabledTimeField.getText());

                    main.getMain().sendMessage(new SerialAsyncMessage("M888 S" + time));
                } catch (NumberFormatException ex) {
                    new PopupMessage(this, "Invalid input", "Time must be an integer.");
                }
            } else {
                main.getMain().sendMessage(new SerialAsyncMessage("M889"));
            }
        });

        super.setContentPane(mainPanel);
        super.setMaximumSize(new Dimension(480, 640));
        super.pack();
        super.setVisible(true);
    }

    @Override
    public void dispose() {
        running = false;
        updateThread.interrupt();
        super.dispose();
    }
}
