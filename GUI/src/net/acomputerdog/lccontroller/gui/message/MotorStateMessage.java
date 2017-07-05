package net.acomputerdog.lccontroller.gui.message;

public class MotorStateMessage implements Message {
    public final boolean state;

    public MotorStateMessage(boolean state) {
        this.state = state;
    }
}
