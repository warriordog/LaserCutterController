package net.acomputerdog.lccontroller.gui;

import com.fazecast.jSerialComm.SerialPort;
import net.acomputerdog.lccontroller.IOConnection;
import net.acomputerdog.lccontroller.LaserCutter;
import net.acomputerdog.lccontroller.gui.message.ConnectMessage;
import net.acomputerdog.lccontroller.gui.message.DisconnectMessage;
import net.acomputerdog.lccontroller.gui.message.Message;
import net.acomputerdog.lccontroller.gui.message.OpenGCodeMessage;
import net.acomputerdog.lccontroller.gui.script.GCodeRunner;
import net.acomputerdog.lccontroller.gui.script.ScriptRunner;
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
                if (currentScript.isFinished()) {
                    String error = currentScript.getErrors();
                    if (error != null) {
                        scriptStatus = "Script failed: " + error;
                        addLogLine(error);
                    } else {
                        scriptStatus = "Script finished.";
                    }
                    addLogLine("Script finished.");
                    currentScript = null;
                } else if (!currentScript.isStarted()) {
                    scriptStatus = "Script Starting...";
                    currentScript.start();
                } else {
                    scriptStatus = "Script running.";
                    currentScript.tick();
                }
            }
        });

        // add a task
        threadTasks.add(() -> {
            String state = "Error: Unknown state";
            if (isConnected()) {
                state = "Connected.";
            } else {
                state = "Disconnected.";
            }

            if (scriptStatus != null) {
                state = scriptStatus;
            }
            setStatus(state);
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
                continue;
            }
            if (m instanceof DisconnectMessage) {
                disconnect();
                continue;
            }
            if (m instanceof OpenGCodeMessage) {
                if (currentScript != null) {
                    currentScript.stop();
                }
                try {
                    currentScript = new GCodeRunner(this, ((OpenGCodeMessage) m).file);
                    currentScript.start();
                } catch (FileNotFoundException e) {
                    addLogLine("Unable to open file: '" + ((OpenGCodeMessage) m).file + "'");
                    new PopupMessage(mainWindow, "File not found", String.format("The file %s could not be found.", ((OpenGCodeMessage) m).file));
                }
                continue;
            }

            addLogLine("Error: Unknown message type: " + (m == null ? "null" : m.getClass().getName()));
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

    public void receiveCLImessage(String line) {
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
                    port.openPort();

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
                    laser = new LaserCutter(connection);

                    // set up CLI
                    cliInterface = new CLIInterface(connection, laser, this);

                    addLogLine("Connected to laser cutter.");
                    setStatus("Connected.");
                    return;
                }
            }
            setStatus("Connect failed: port not found.");
        } catch (Exception e) {
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

    public static void main(String[] args) {
        new GUIMain().start();
    }
}
