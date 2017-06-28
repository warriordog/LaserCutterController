package net.acomputerdog.lccontroller.gui.window;

import net.acomputerdog.lccontroller.gui.GUIMain;
import net.acomputerdog.lccontroller.gui.message.DisconnectMessage;
import net.acomputerdog.lccontroller.gui.message.OpenCMDMessage;
import net.acomputerdog.lccontroller.gui.message.OpenGCodeMessage;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;

public class MainWindow extends JFrame {
    private JPanel mainPanel;
    private JSplitPane vertSplit;
    private JSplitPane horizSplit;
    private JTabbedPane consoleTabs;
    private JPanel cliTab;
    private JPanel gcodeTab;
    public JTextArea serialTextArea;
    private JTextField serialSendField;
    private JButton serialSendButton;
    public JTextArea cliTextArea;
    public JTextField cliSendField;
    private JButton cliSendButton;
    private JPanel placeHolder2;
    private JButton exitButton;
    private JTextPane laserStatusPane;
    private JToolBar statusBar;
    public JLabel statusLabel;
    private JPanel logTab;
    public JTextArea logTextArea;
    private JScrollPane logScrollPane;
    private JScrollPane serialScrollPane;
    private JScrollPane cliScrollPane;
    private JButton serialClearButton;
    private JMenuBar menuBar;

    private JMenu fileMenu;
    private JMenuItem openGCodeItem;
    private JMenuItem openCMDItem;
    private JMenuItem exitMenuItem;
    private JMenu printerMenu;
    private JMenuItem connectItem;
    private JMenuItem disconnectItem;

    private JFileChooser gcodeChooser;
    private JFileChooser cmdChooser;

    private final GUIMain guiMain;

    public MainWindow(GUIMain guiMain) {
        super();
        super.setTitle("Laser Cutter Controller");
        super.setContentPane(mainPanel);
        super.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent windowEvent) {
                guiMain.shutdown();
            }
        });
        super.setMinimumSize(new Dimension(640, 480));
        super.pack();

        this.guiMain = guiMain;

        logTextArea.setText("Window created.\n");

        // add listeners
        exitMenuItem.addActionListener(e -> guiMain.shutdown());
        connectItem.addActionListener(e -> new ConnectWindow(this, guiMain).setVisible(true));
        disconnectItem.addActionListener(e -> guiMain.sendMessage(new DisconnectMessage()));
        cliSendButton.addActionListener(e -> {
            if (guiMain.getCLIInterface() != null) {
                cliTextArea.append(cliSendField.getText());
                cliTextArea.append("\n");
                guiMain.getCLIInterface().sendLineToCLI(cliSendField.getText());
            }
        });
        openGCodeItem.addActionListener(e -> {
            if (gcodeChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                File gcode = gcodeChooser.getSelectedFile();
                if (gcode.isFile()) {
                    guiMain.sendMessage(new OpenGCodeMessage(gcode));
                } else {
                    new PopupMessage(this, "Invalid input", "GCode file must be a file.");
                }
            }
        });
        openCMDItem.addActionListener(e -> {
            if (cmdChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                File gcode = cmdChooser.getSelectedFile();
                if (gcode.isFile()) {
                    guiMain.sendMessage(new OpenCMDMessage(gcode));
                } else {
                    new PopupMessage(this, "Invalid input", "CMD file must be a file.");
                }
            }
        });
        serialClearButton.addActionListener(e -> serialTextArea.setText(""));
    }

    private void createUIComponents() {
        // create menu
        menuBar = new JMenuBar();
        fileMenu = new JMenu("File");
        fileMenu.setMnemonic(KeyEvent.VK_F);
        openGCodeItem = new JMenuItem("Open GCode file");
        fileMenu.add(openGCodeItem);
        openCMDItem = new JMenuItem("Open CMD script file");
        fileMenu.add(openCMDItem);
        fileMenu.addSeparator();
        exitMenuItem = new JMenuItem("Exit");
        fileMenu.add(exitMenuItem);
        menuBar.add(fileMenu);
        printerMenu = new JMenu("Printer");
        printerMenu.setMnemonic(KeyEvent.VK_P);
        connectItem = new JMenuItem("Connect");
        printerMenu.add(connectItem);
        disconnectItem = new JMenuItem("Disconnect");
        printerMenu.add(disconnectItem);
        menuBar.add(printerMenu);

        // create choosers
        gcodeChooser = new JFileChooser();
        gcodeChooser.setDialogTitle("Open a GCode file");
        gcodeChooser.setMultiSelectionEnabled(false);
        gcodeChooser.addChoosableFileFilter(new FileNameExtensionFilter("GCode files", "gcode", "gc", "g"));

        cmdChooser = new JFileChooser();
        cmdChooser.setDialogTitle("Open a CMD script");
        cmdChooser.setMultiSelectionEnabled(false);
        cmdChooser.addChoosableFileFilter(new FileNameExtensionFilter("CMD scripts", "lcmd"));
    }
}
