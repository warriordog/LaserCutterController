package net.acomputerdog.lccontroller.gui.window;

import net.acomputerdog.lccontroller.Location;
import net.acomputerdog.lccontroller.ex.ResponseFormatException;
import net.acomputerdog.lccontroller.gui.ComponentScriptPath;
import net.acomputerdog.lccontroller.gui.GUIMain;
import net.acomputerdog.lccontroller.gui.message.*;
import net.acomputerdog.lccontroller.util.NumberUtils;

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
    private JPanel scriptPanel;
    private JButton exitButton;
    private JToolBar statusBar;
    public JLabel statusLabel;
    private JPanel logTab;
    public JTextArea logTextArea;
    private JScrollPane logScrollPane;
    private JScrollPane serialScrollPane;
    private JScrollPane cliScrollPane;
    private JButton serialClearButton;
    private JMenuBar menuBar;
    private JToolBar scriptToolbar;
    private JButton startScriptButton;
    private JButton stopScriptButton;
    public JProgressBar scriptProgress;
    public JTextField scriptLastInstruction;
    private JRadioButton y10Radio;
    private JRadioButton y1Radio;
    private JRadioButton y01Radio;
    private JButton yDownButton;
    private JButton yUpButton;
    private JRadioButton x10Radio;
    private JRadioButton x1Radio;
    private JRadioButton x01Radio;
    private JButton xUpButton;
    private JButton xDownButton;
    private JTextField newXLocField;
    private JTextField newYLocField;
    private JButton locGoButton;
    private JCheckBox relativeCheck;
    public JTextField laserPowerField;
    public JTextField xLocField;
    public JTextField yLocField;
    public ComponentScriptPath scriptPreview;
    public JTextField motorStateField;
    private JButton motorOnButton;
    private JButton motorOffButton;

    private JMenu fileMenu;
    private JMenuItem openGCodeItem;
    private JMenuItem openCMDItem;
    private JMenuItem exitMenuItem;
    private JMenu printerMenu;
    private JMenuItem connectItem;
    private JMenuItem disconnectItem;
    private JMenuItem propertiesItem;
    private JMenu debugMenu;
    private JMenuItem debugLaserItem;

    private JFileChooser gcodeChooser;
    private JFileChooser cmdChooser;

    private final GUIMain main;

    public MainWindow(GUIMain main) {
        super();
        super.setTitle("Laser Cutter Controller");
        super.setContentPane(mainPanel);
        super.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent windowEvent) {
                main.shutdown();
            }
        });
        super.setMinimumSize(new Dimension(640, 480));
        super.pack();

        this.main = main;
        scriptPreview.setMain(main);

        logTextArea.setText("Window created.\n");

        // add listeners
        exitMenuItem.addActionListener(e -> main.shutdown());
        connectItem.addActionListener(e -> new ConnectWindow(this, main).setVisible(true));
        disconnectItem.addActionListener(e -> main.sendMessage(new DisconnectMessage()));
        cliSendButton.addActionListener(e -> {
            if (main.getCLIInterface() != null) {
                cliTextArea.append(cliSendField.getText());
                cliTextArea.append("\n");

                main.sendMessage(new CLIMessage(cliSendField.getText()));
            }
        });
        openGCodeItem.addActionListener(e -> {
            if (gcodeChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                File gcode = gcodeChooser.getSelectedFile();
                if (gcode.isFile()) {
                    main.sendMessage(new OpenGCodeMessage(gcode));
                } else {
                    new PopupMessage(this, "Invalid input", "GCode file must be a file.");
                }
            }
        });
        openCMDItem.addActionListener(e -> {
            if (cmdChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                File gcode = cmdChooser.getSelectedFile();
                if (gcode.isFile()) {
                    main.sendMessage(new OpenCMDMessage(gcode));
                } else {
                    new PopupMessage(this, "Invalid input", "CMD file must be a file.");
                }
            }
        });
        serialClearButton.addActionListener(e -> serialTextArea.setText(""));
        serialSendButton.addActionListener(e -> main.sendMessage(new SerialAsyncMessage(serialSendField.getText())));
        startScriptButton.addActionListener(e -> main.sendMessage(new StartScriptMessage()));
        stopScriptButton.addActionListener(e -> main.sendMessage(new StopScriptMessage()));
        locGoButton.addActionListener(e -> {
            try {
                long xUm = NumberUtils.parseAxisLoc(newXLocField.getText());
                long yUm = NumberUtils.parseAxisLoc(newYLocField.getText());

                if (!relativeCheck.isSelected()) {
                    main.sendMessage(new MotorMoveMessage(new Location(xUm, yUm)));
                } else {
                    main.sendMessage(new MotorMoveByMessage(xUm, yUm));
                }
            } catch (ResponseFormatException ex) {
                new PopupMessage(this, "Invalid input", "X and Y must be integer or decimal numbers.");
            }
        });
        yUpButton.addActionListener(e -> moveAxis(false, true));
        yDownButton.addActionListener(e -> moveAxis(false, false));
        xUpButton.addActionListener(e -> moveAxis(true, true));
        xDownButton.addActionListener(e -> moveAxis(true, false));
        propertiesItem.addActionListener(e -> new LaserPropWindow(this, main));
        motorOnButton.addActionListener(e -> main.sendMessage(new MotorStateMessage(true)));
        motorOffButton.addActionListener(e -> main.sendMessage(new MotorStateMessage(false)));
        debugLaserItem.addActionListener(e -> new DebugLaserWindow(this));
    }

    private void moveAxis(boolean axis, boolean direction) {
        long xStep = 0;
        long yStep = 0;
        if (axis) {
            if (x10Radio.isSelected()) {
                xStep = 10000;
            } else if (x1Radio.isSelected()) {
                xStep = 1000;
            } else if (x01Radio.isSelected()) {
                xStep = 100;
            }
        } else {
            if (y10Radio.isSelected()) {
                yStep = 10000;
            } else if (y1Radio.isSelected()) {
                yStep = 1000;
            } else if (y01Radio.isSelected()) {
                yStep = 100;
            }
        }
        if (!direction) {
            xStep *= -1;
            yStep *= -1;
        }

        if (main.isConnected()) {
            Location loc = main.getLaser().getLocation();
            xLocField.setText(String.format("%d.%d", loc.getXMM(), (loc.getXUM() % 1000)));
            yLocField.setText(String.format("%d.%d", loc.getYMM(), (loc.getYUM() % 1000)));

            main.sendMessage(new MotorMoveByMessage(xStep, yStep));
            //main.moveBy(xStep, yStep);
        }
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

        printerMenu = new JMenu("Machine");
        printerMenu.setMnemonic(KeyEvent.VK_M);
        connectItem = new JMenuItem("Connect");
        printerMenu.add(connectItem);
        disconnectItem = new JMenuItem("Disconnect");
        printerMenu.add(disconnectItem);
        printerMenu.addSeparator();
        propertiesItem = new JMenuItem("Properties");
        printerMenu.add(propertiesItem);
        menuBar.add(printerMenu);

        debugMenu = new JMenu("Debug");
        debugMenu.setMnemonic(KeyEvent.VK_D);
        debugLaserItem = new JMenuItem("Laser");
        debugMenu.add(debugLaserItem);
        menuBar.add(debugMenu);

        // create choosers
        gcodeChooser = new JFileChooser();
        gcodeChooser.setDialogTitle("Open a GCode file");
        gcodeChooser.setMultiSelectionEnabled(false);
        gcodeChooser.addChoosableFileFilter(new FileNameExtensionFilter("GCode files", "gcode", "gc", "g"));

        cmdChooser = new JFileChooser();
        cmdChooser.setDialogTitle("Open a CMD script");
        cmdChooser.setMultiSelectionEnabled(false);
        cmdChooser.addChoosableFileFilter(new FileNameExtensionFilter("CMD scripts", "lcmd"));

        // create preview
        scriptPreview = new ComponentScriptPath();
    }

    public GUIMain getMain() {
        return main;
    }
}
