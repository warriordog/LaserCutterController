package net.acomputerdog.lccontroller.gui.message;

public class SetLaserStateMessage implements Message {
    public final boolean state;

    public SetLaserStateMessage(boolean state) {
        this.state = state;
    }
}
