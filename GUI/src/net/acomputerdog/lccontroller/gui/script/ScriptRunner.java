package net.acomputerdog.lccontroller.gui.script;

public interface ScriptRunner {
    boolean isStarted();

    boolean isFinished();

    void stop();

    void start();


    void tick();

    void onAck();

    float getEstimatedProgress();

    String getErrors();
}
