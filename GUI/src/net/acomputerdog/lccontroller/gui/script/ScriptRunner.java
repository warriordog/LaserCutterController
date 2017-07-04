package net.acomputerdog.lccontroller.gui.script;

import java.io.IOException;

public interface ScriptRunner {
    ScriptState getState();

    void load() throws IOException;
    void start();

    void stop();

    void tick();
    void onAck();

    float getEstimatedProgress();
    String getErrors();

    String[] getLines();
    String getLastLine();
}
