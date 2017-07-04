package net.acomputerdog.lccontroller.gui.window;

import net.acomputerdog.lccontroller.LaserProperties;
import net.acomputerdog.lccontroller.gui.GUIMain;

import javax.swing.*;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class LaserPropWindow extends JDialog {
    private JPanel contentPane;
    private JButton buttonOK;
    private JButton buttonCancel;
    private JTextField heightField;
    private JTextField widthField;

    private final GUIMain main;
    private final JFrame owner;

    public LaserPropWindow(JFrame owner, GUIMain main) {
        super(owner);
        this.main = main;
        this.owner = owner;

        setContentPane(contentPane);
        setModal(true);
        getRootPane().setDefaultButton(buttonOK);

        buttonOK.addActionListener(e -> onOK());

        buttonCancel.addActionListener(e -> onCancel());

        // call onCancel() when X is clicked
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                onCancel();
            }
        });

        // call onCancel() on ESCAPE
        contentPane.registerKeyboardAction(e -> onCancel(), KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);

        super.setTitle("Laser cutter properties");
        super.setVisible(true);
    }

    private void onOK() {
        try {
            int width = Integer.parseInt(widthField.getText());
            int height = Integer.parseInt(heightField.getText());

            main.setLaserProperties(new LaserProperties(width, height));
            dispose();
        } catch (NumberFormatException e) {
            new PopupMessage(owner, "Invalid input", "Please enter only integers.");
        }
    }

    private void onCancel() {
        dispose();
    }
}
