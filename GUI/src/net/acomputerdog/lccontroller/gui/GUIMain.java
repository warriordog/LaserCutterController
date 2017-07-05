package net.acomputerdog.lccontroller.gui;

import com.fazecast.jSerialComm.SerialPort;
import net.acomputerdog.lccontroller.IOConnection;
import net.acomputerdog.lccontroller.LaserCutter;
import net.acomputerdog.lccontroller.LaserProperties;
import net.acomputerdog.lccontroller.Location;
import net.acomputerdog.lccontroller.gui.message.*;
import net.acomputerdog.lccontroller.gui.script.GCodeRunner;
import net.acomputerdog.lccontroller.gui.script.ScriptRunner;
import net.acomputerdog.lccontroller.gui.script.ScriptState;
import net.acomputerdog.lccontroller.gui.window.MainWindow;
import net.acomputerdog.lccontroller.gui.window.PopupMessage;
import net.acomputerdog.lccontroller.util.MessagePipe;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.LinkedList;
import java.util.List;

public class GUIMain {

    private final MainWindow mainWindow;

    private LaserProperties properties;
    private LaserCutter laser;
    private CLIInterface cliInterface;

    private MessagePipe<Message> messagePipe = new MessagePipe<>();
    private List<Runnable> threadTasks = new LinkedList<>();
    private List<Runnable> addTasks = new LinkedList<>();
    private List<Runnable> removeTasks = new LinkedList<>();
    private Thread executorThread;
    private boolean isRunning = true;

    private ScriptRunner currentScript;
    private String scriptStatus = null;

    private final PrintWriter logWriter;

    // last state values to avoid refreshing UI unnecessarily.
    private boolean lastLaserState = false;
    private int lastLaserPower = -1;
    private Location lastLaserLocation = new Location(0, 0);
    private String lastStatus = null;
    private boolean lastMotorState = false;

    // duration between I1 update commands (ms)
    private long laserUpdateInterval = 1000;

    public GUIMain() {
        mainWindow = new MainWindow(this);

        this.logWriter = new PrintWriter(new Writer() {
            @Override
            public void write(char[] cbuf, int off, int len) throws IOException {
                addLogString(String.valueOf(cbuf, off, len));
            }

            @Override
            public void flush() throws IOException {
            }

            @Override
            public void close() throws IOException {
            }
        });

        this.executorThread = new Thread(this::runLoop);
        this.executorThread.setName("Executor_Thread");
    }

    private void runLoop() {
        // add a task to tick scripts
        threadTasks.add(() -> {
            if (currentScript != null) {
                // end script
                if (currentScript.getState() == ScriptState.FINISHED) {
                    String error = currentScript.getErrors();
                    if (error != null) {
                        scriptStatus = "Script failed: " + error;
                        addLogLine(error);
                    } else {
                        scriptStatus = "Script finished.";
                    }
                    addLogLine("Script finished.");
                    currentScript = null;
                    // load script
                } else if (currentScript.getState() == ScriptState.NOT_STARTED) {
                    try {
                        scriptStatus = "Script loading...";
                        currentScript.load();
                        mainWindow.scriptPreview.setScript(currentScript);
                        scriptStatus = "Script ready.";
                        addLogLine(String.format("Loaded script with %d lines.", currentScript.getLines().length));
                    } catch (IOException e) {
                        logException("Exception loading script.", e);
                    }
                    // tick script
                } else if (currentScript.getState() == ScriptState.RUNNING) {
                    scriptStatus = "Script running.";
                    currentScript.tick();
                    mainWindow.scriptLastInstruction.setText(currentScript.getLastLine());
                    mainWindow.scriptProgress.setValue((int) (currentScript.getEstimatedProgress() * 100.0f));
                } else {
                    // script has not started
                    scriptStatus = "Script ready.";
                }
            }
        });

        // add a task to set status bar message
        threadTasks.add(() -> {
            String state;
            if (isConnected()) {
                state = "Connected.";
            } else {
                state = "Disconnected.";
            }

            if (scriptStatus != null) {
                state = scriptStatus;
            }

            if (mainWindow.scriptPreview.isDrawing()) {
                state = "Drawing preview...";
            }

            if (!state.equals(lastStatus)) {
                lastStatus = state;
                setStatus(state);
            }
        });

        // add a task to refresh laser status
        threadTasks.add(() -> {
            if (isConnected()) {
                if (lastLaserState != laser.isLaserOn() || lastLaserPower != laser.getLaserPower()) {
                    lastLaserState = laser.isLaserOn();
                    lastLaserPower = laser.getLaserPower();
                    if (laser.isLaserOn()) {
                        mainWindow.laserPowerField.setText(String.format("%.2f%%", (((float) laser.getLaserPower()) / 255f) * 100f));
                    } else {
                        mainWindow.laserPowerField.setText("off");
                    }
                }

                Location loc = laser.getLocation();
                if (!loc.equals(lastLaserLocation)) {
                    lastLaserLocation = loc;
                    mainWindow.xLocField.setText(String.format("%d.%d", loc.getXMM(), loc.getXUM() % 1000));
                    mainWindow.yLocField.setText(String.format("%d.%d", loc.getYMM(), loc.getYUM() % 1000));
                }

                boolean motorState = laser.getMotorState();
                if (motorState != lastMotorState) {
                    lastMotorState = motorState;
                    mainWindow.motorStateField.setText(motorState ? "on" : "off");
                }
            }
        });

        // add a task to poll machine for status
        threadTasks.add(new Runnable() {
            private long lastRunTime;

            @Override
            public void run() {
                if (isConnected() && System.currentTimeMillis() - lastRunTime > laserUpdateInterval) {
                    lastRunTime = System.currentTimeMillis();
                    laser.requestImmediateUpdate();
                }
            }
        });

        // add a task to draw script preview
        threadTasks.add(() -> {
            if (mainWindow.scriptPreview.isDrawing()) {
                mainWindow.scriptPreview.updateDraw();
            }
        });

        while (isRunning) {
            try {

                // read messages
                readMessages();

                // run tasks
                for (Runnable r : threadTasks) {
                    r.run();
                }

                // add new tasks
                threadTasks.addAll(addTasks);

                // remove finished tasks
                while (!removeTasks.isEmpty()) {
                    threadTasks.remove(removeTasks.remove(0));
                }

                // sleep to rest CPU
                try {
                    Thread.sleep(1);
                } catch (InterruptedException ignored) {
                }
            } catch (Exception e) {
                logException("Exception in main loop", e);
            }
        }
    }

    private void readMessages() {
        for (Message m : messagePipe) {
            if (m instanceof ConnectMessage) {
                disconnect();
                ConnectMessage cm = (ConnectMessage) m;
                connect(cm.port, cm.baud, cm.dataBits, cm.stopBits, cm.parity, cm.flowMode);
            } else if (m instanceof DisconnectMessage) {
                disconnect();
            } else if (m instanceof OpenGCodeMessage) {
                if (currentScript != null) {
                    currentScript.stop();
                }
                try {
                    currentScript = new GCodeRunner(this, ((OpenGCodeMessage) m).file);
                } catch (FileNotFoundException e) {
                    addLogLine("Unable to open file: '" + ((OpenGCodeMessage) m).file + "'");
                    new PopupMessage(mainWindow, "File not found", String.format("The file %s could not be found.", ((OpenGCodeMessage) m).file));
                }
            } else if (m instanceof OpenCMDMessage) {
                //TODO implement
                new PopupMessage(mainWindow, "Not Implemented", "Sorry, that feature is not yet implemented.");
            } else if (m instanceof StartScriptMessage) {
                if (currentScript != null) {
                    if (currentScript.getState() == ScriptState.LOADED) {
                        currentScript.start();
                    } else if (currentScript.getState() == ScriptState.RUNNING) {
                        new PopupMessage(mainWindow, "Script already running", "The script is already running.");
                    } else if (currentScript.getState() == ScriptState.FINISHED) {
                        new PopupMessage(mainWindow, "Script already finished", "The script has already finished.  Please reload it to run it again.");
                    } else if (currentScript.getState() == ScriptState.NOT_STARTED) {
                        new PopupMessage(mainWindow, "Script not loaded", "Please wait for the script to finish loading.");
                    }
                } else {
                    new PopupMessage(mainWindow, "Script not selected", "Please load a script from the \"printer\" menu.");
                }
            } else if (m instanceof StopScriptMessage) {
                if (currentScript != null && currentScript.getState() != ScriptState.FINISHED) {
                    currentScript.stop();
                }
            } else if (m instanceof MotorStateMessage) {
                if (isConnected()) {
                    if (((MotorStateMessage) m).state) {
                        laser.enableMotors(true);
                        mainWindow.laserPowerField.setText("on");
                    } else {
                        laser.enableMotors(false);
                        mainWindow.laserPowerField.setText("off");
                    }
                }
            } else if (m instanceof CLIMessage) {
                if (isConnected()) {
                    cliInterface.sendLineToCLI(((CLIMessage) m).command);
                }
            } else if (m instanceof SerialAsyncMessage) {
                if (isConnected()) {
                    laser.getConnection().sendAsync(((SerialAsyncMessage) m).command);
                }
            } else if (m instanceof MotorMoveMessage) {
                if (isConnected()) {
                    laser.move(((MotorMoveMessage) m).loc);
                }
            } else if (m instanceof MotorMoveByMessage) {
                if (isConnected()) {
                    moveBy(((MotorMoveByMessage) m).xUm, ((MotorMoveByMessage) m).yUm);
                }
            } else if (m instanceof SetLaserPowerMessage) {
                if (isConnected()) {
                    laser.setLaserPower(((SetLaserPowerMessage) m).power);
                }
            } else if (m instanceof SetLaserStateMessage) {
                if (isConnected()) {
                    laser.setLaserState(((SetLaserStateMessage) m).state);
                }
            } else {
                addLogLine("Error: Unknown message type: " + (m == null ? "null" : m.getClass().getName()));
            }
        }
    }

    public void shutdown() {
        isRunning = false;
        if (executorThread.isAlive()) {
            executorThread.interrupt();
        }
        System.exit(0);
    }

    public void start() {
        mainWindow.setVisible(true);
        executorThread.start();
        addLogLine("Main thread started.");
    }

    public void receiveCLIMessage(String line) {
        mainWindow.cliTextArea.append(line);
    }

    private void connect(String name, int baud, int dataBits, int stopBits, int parity, int flowMode) {
        try {
            setStatus("Connecting...");
            for (SerialPort port : SerialPort.getCommPorts()) {
                if (name.contains(port.getSystemPortName())) {
                    // set up port
                    port.setComPortParameters(baud, dataBits, stopBits, parity);
                    port.setFlowControl(flowMode);
                    if (port.openPort()) {
                        // set up properties
                        if (properties == null) {
                            addLogLine("Setting default properties.");
                            this.properties = new LaserProperties(915, 610);
                        }

                        // connect to serial
                        IOConnection connection = new IOConnection(port);
                        connection.addLineReceivedMonitor(line -> {
                            mainWindow.serialTextArea.append("<--");
                            mainWindow.serialTextArea.append(line.replace('\n', '□'));
                            mainWindow.serialTextArea.append("\n");

                            // record ACks for script
                            if (currentScript != null && line.equals("OK")) {
                                currentScript.onAck();
                            }
                        });
                        connection.addLineSentMonitor(line -> {
                            mainWindow.serialTextArea.append("-->");
                            mainWindow.serialTextArea.append(line);
                            mainWindow.serialTextArea.append("\n");
                        });

                        // connect to printer
                        laser = new LaserCutter(connection, properties);

                        // set up CLI
                        cliInterface = new CLIInterface(connection, laser, this);

                        addLogLine("Connected to laser cutter.");
                        setStatus("Connected.");
                    } else {
                        setStatus("Unable to open port.");
                        addLogLine("Unable to open port.");
                    }
                    return;
                }
            }
            setStatus("Connect failed: port not found.");
        } catch (Exception e) {
            if (laser != null) {
                try {
                    laser.disconnect();
                } catch (Exception ignored) {
                }
                laser = null;
            }
            setStatus("Connect failed: " + e.toString());
            System.err.println("Exception connecting.");
            e.printStackTrace();
        }
    }

    private void disconnect() {
        setStatus("Disconnecting...");
        if (currentScript != null) {
            currentScript.stop();
            currentScript = null;
        }
        scriptStatus = null;
        if (cliInterface != null) {
            cliInterface = null;
        }
        if (laser != null) {
            laser.disconnect();
        }
        mainWindow.cliTextArea.setText("");
        mainWindow.serialTextArea.setText("");
        addLogLine("Disconnected.");
        setStatus("Disconnected.");
    }

    public void addLogString(String message) {
        if (mainWindow != null) {
            mainWindow.logTextArea.append(message);
        }
    }

    public void addLogLine(String line) {
        if (mainWindow != null) {
            addLogString(line);
            addLogString("\n");
        }
    }

    private void setStatus(String line) {
        if (mainWindow != null) {
            mainWindow.statusLabel.setText(line);
        }
    }

    public void sendMessage(Message m) {
        if (m != null) {
            messagePipe.send(m);
        }
    }


    public void logException(String message, Exception e) {
        System.err.println(message);
        e.printStackTrace();

        addLogLine(message);
        e.printStackTrace(logWriter);
    }

    public CLIInterface getCLIInterface() {
        return cliInterface;
    }

    public LaserCutter getLaser() {
        return laser;
    }

    public boolean isConnected() {
        return laser != null && laser.isConnected();
    }

    public void moveBy(long xUm, long yUm) {
        if (laser != null) {
            laser.moveBy(xUm, yUm);
        }
    }

    public LaserProperties getLaserProperties() {
        return properties;
    }

    public void setLaserProperties(LaserProperties properties) {
        this.properties = properties;
    }

    public static void main(String[] args) {
        new GUIMain().start();
    }
}
