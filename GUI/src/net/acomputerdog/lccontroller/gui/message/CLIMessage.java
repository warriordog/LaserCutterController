package net.acomputerdog.lccontroller.gui.message;

public class CLIMessage implements Message {
    public final String command;

    public CLIMessage(String command) {
        this.command = command;
    }
}
