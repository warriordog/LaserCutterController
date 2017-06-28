package net.acomputerdog.lccontroller.gui.message;

import java.io.File;

public class OpenGCodeMessage implements Message {
    public final File file;

    public OpenGCodeMessage(File file) {
        this.file = file;
    }
}
