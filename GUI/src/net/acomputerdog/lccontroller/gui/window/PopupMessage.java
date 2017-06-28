package net.acomputerdog.lccontroller.gui.window;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class PopupMessage extends JDialog {
    private JPanel contentPane;
    private JButton buttonOK;
    private JTextArea textArea;

    public PopupMessage(JFrame owner, String title, String message) {
        super(owner);
        super.setMinimumSize(new Dimension(300, 200));
        super.setTitle(title);

        setContentPane(contentPane);
        setModal(true);
        getRootPane().setDefaultButton(buttonOK);

        textArea.setText(message);

        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                onOK();
            }
        });

        contentPane.registerKeyboardAction(e -> onOK(), KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
        buttonOK.addActionListener(e -> onOK());

        this.pack();
        this.setVisible(true);
    }

    private void onOK() {
        dispose();
    }
}
