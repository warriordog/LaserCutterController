package net.acomputerdog.lccontroller.gui.message;

public class SerialAsyncMessage implements Message {
    public final String command;

    public SerialAsyncMessage(String command) {
        this.command = command;
    }
}
