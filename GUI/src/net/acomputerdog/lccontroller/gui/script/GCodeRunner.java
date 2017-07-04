package net.acomputerdog.lccontroller.gui.script;

import net.acomputerdog.lccontroller.gui.GUIMain;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class GCodeRunner implements ScriptRunner {
    private final GUIMain main;
    private final BufferedReader gcodeReader;

    private String[] lines;

    ScriptState state = ScriptState.NOT_STARTED;

    String error = null;
    float progress = 0f;
    boolean ack = true; //true so first command can be sent
    String lastLine;
    int nextLine = 0;

    public GCodeRunner(GUIMain main, File file) throws FileNotFoundException {
        gcodeReader = new BufferedReader(new FileReader(file));
        this.main = main;
    }

    @Override
    public ScriptState getState() {
        return state;
    }

    @Override
    public void load() throws IOException {
        try {
            if (state == ScriptState.NOT_STARTED) {
                List<String> lineList = new ArrayList<>();
                while (gcodeReader.ready()) {
                    lineList.add(gcodeReader.readLine());
                }
                this.lines = lineList.toArray(new String[lineList.size()]);

                state = ScriptState.LOADED;
            }
        } catch (Exception e) {
            state = ScriptState.FINISHED;
            throw e;
        } finally {
            try {
                gcodeReader.close();
            } catch (IOException ignored) {
            }
        }
    }

    @Override
    public void stop() {
        if (state == ScriptState.RUNNING) {
            stopWithError("Stopped early by command.");
        }
    }

    @Override
    public void start() {
        state = ScriptState.RUNNING;
    }

    @Override
    public void tick() {
        try {
            if (state == ScriptState.RUNNING) {
                if (nextLine > lines.length) {
                    state = ScriptState.FINISHED;
                    progress = 1f;
                } else if (ack) {
                    String line = lines[nextLine];
                    nextLine++;

                    if (!line.isEmpty()) {
                        ack = false;
                        main.getLaser().getConnection().sendAsync(line);
                        lastLine = line;
                    }
                }
            }
        } catch (Exception e) {
            stopWithError("Internal exception: " + e.toString());
            main.logException("Exception running script", e);
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
    public String[] getLines() {
        return lines;
    }

    @Override
    public String getLastLine() {
        return lastLine;
    }

    private void stopWithError(String error) {
        state = ScriptState.FINISHED;
        this.error = error;
    }
}
