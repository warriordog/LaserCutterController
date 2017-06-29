package net.acomputerdog.lccontroller.gui.script;

import net.acomputerdog.lccontroller.gui.GUIMain;

import java.io.*;

public class GCodeRunner implements ScriptRunner {
    private final GUIMain main;
    private final BufferedReader gcodeReader;
    boolean started = false;
    boolean finished = false;
    String error = null;
    float progress = 0f;
    boolean ack = true; //true so first command can be sent
    String lastLine;

    public GCodeRunner(GUIMain main, File file) throws FileNotFoundException {
        gcodeReader = new BufferedReader(new FileReader(file));
        this.main = main;
    }

    public boolean isStarted() {
        return started;
    }

    @Override
    public boolean isFinished() {
        return finished;
    }

    @Override
    public void stop() {
        if (isRunning()) {
            try {
                gcodeReader.close();
            } catch (IOException ignored) {
            }
            stopWithError("Stopped early by command.");
        }
    }

    @Override
    public void start() {
        if (!started && !finished) {
            started = true;
        }
    }

    @Override
    public void tick() {
        if (isRunning()) {
            try {
                if (gcodeReader.ready()) {
                    if (ack) {
                        String line = gcodeReader.readLine().trim();
                        if (!line.isEmpty()) {
                            ack = false;
                            main.getLaser().getConnection().sendAsync(line);
                            lastLine = line;
                        }
                    }
                } else {
                    finished = true;
                    progress = 1f;
                }
            } catch (IOException e) {
                stopWithError("IO error");
            } catch (Exception e) {
                stopWithError("Internal exception: " + e.toString());
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onAck() {
        ack = true;
    }

    @Override
    public float getEstimatedProgress() {
        return progress;
    }

    @Override
    public String getErrors() {
        return error;
    }

    @Override
    public String getLastLine() {
        return lastLine;
    }

    private boolean isRunning() {
        return started && !finished;
    }

    private void stopWithError(String error) {
        finished = true;
        this.error = error;
    }
}
