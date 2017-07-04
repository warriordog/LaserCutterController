package net.acomputerdog.lccontroller.gui.message;

public class LaserStateMessage implements Message {
    public final boolean state;

    public LaserStateMessage(boolean state) {
        this.state = state;
    }
}
