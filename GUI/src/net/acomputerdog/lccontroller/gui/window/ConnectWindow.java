package net.acomputerdog.lccontroller.gui.window;

import net.acomputerdog.lccontroller.gui.GUIMain;
import net.acomputerdog.lccontroller.gui.message.ConnectMessage;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class ConnectWindow extends JDialog {
    private JPanel contentPane;
    private JButton connectButton;
    private JButton cancelButton;
    private JTextField portField;
    private JTextField baudField;
    private JTextField dataField;
    private JTextField stopField;
    private JTextField parityField;
    private JTextField flowField;

    private final JFrame owner;
    private final GUIMain main;

    public ConnectWindow(JFrame owner, GUIMain main) {
        super(owner);
        super.setMinimumSize(new Dimension(400, 300));

        this.owner = owner;
        this.main = main;

        setContentPane(contentPane);
        setModal(true);
        getRootPane().setDefaultButton(connectButton);

        connectButton.addActionListener(e -> onOK());

        cancelButton.addActionListener(e -> onCancel());

        // call onCancel() when cross is clicked
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                onCancel();
            }
        });

        // call onCancel() on ESCAPE
        contentPane.registerKeyboardAction(e -> onCancel(), KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
    }

    private void onOK() {
        try {
            String port = portField.getText();
            int baud = Integer.parseInt(baudField.getText());
            int data = Integer.parseInt(dataField.getText());
            int stop = Integer.parseInt(stopField.getText());
            int parity = Integer.parseInt(parityField.getText());
            int flow = Integer.parseInt(flowField.getText());

            main.sendMessage(new ConnectMessage(port, baud, data, stop, parity, flow));
            dispose();
        } catch (NumberFormatException e) {
            new PopupMessage(owner, "Invalid input", "Please enter integers for all values except port name.");
        }
    }

    private void onCancel() {
        dispose();
    }
}
