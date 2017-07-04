package net.acomputerdog.lccontroller.gui;

import net.acomputerdog.lccontroller.IOConnection;
import net.acomputerdog.lccontroller.LaserCutter;
import net.acomputerdog.lccontroller.cli.CLIMain;
import net.acomputerdog.lccontroller.util.LockedNotifier;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.Semaphore;

public class CLIInterface extends CLIMain implements Runnable {
    private final GUIMain guiMain;

    private final LockedNotifier lineNotifier = new LockedNotifier();
    private final Semaphore lineLock = new Semaphore(1);
    private Queue<String> lines = new LinkedList<>();

    private final Thread cliThread;

    public CLIInterface(IOConnection connection, LaserCutter laser, GUIMain guiMain) {
        super(connection, laser);
        this.guiMain = guiMain;
        this.cliThread = new Thread(this);
        this.cliThread.setDaemon(true);
        this.cliThread.setName("CLI_Interface_Thread");
        this.cliThread.start();
    }

    @Override
    public void shutdown() {
        guiMain.shutdown();
    }

    @Override
    public void send(String message) {
        guiMain.receiveCLIMessage(message);
    }

    @Override
    public void sendLine() {
        this.send("\n");
    }

    @Override
    public void sendLine(String line) {
        this.send(line);
        this.send("\n");
    }

    @Override
    public String getLine() {
        lineNotifier.waitForNotify();
        lineLock.acquireUninterruptibly();
        try {
            return lines.poll();
        } finally {
            lineLock.release();
        }
    }

    public void stop() {
        if (cliThread.isAlive()) {
            super.stop();
            sendLineToCLI("");
        }
    }

    public void sendLineToCLI(String line) {
        if (lineAllowed(line)) {
            lineLock.acquireUninterruptibly();
            lines.add(line);
            lineLock.release();
            lineNotifier.release();
        } else {
            guiMain.receiveCLIMessage("That command is disabled, please use the equivalent feature in the GUI interface.");
        }
    }

    private boolean lineAllowed(String line) {
        String formattedLine = line.trim().toLowerCase();
        if (formattedLine.startsWith("shell")) {
            return false;
        }
        if (formattedLine.startsWith("scriptcmd")) {
            return false;
        }
        if (formattedLine.startsWith("scriptgcode")) {
            return false;
        }
        return true;
    }
}
