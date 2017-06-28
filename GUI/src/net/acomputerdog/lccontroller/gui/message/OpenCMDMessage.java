package net.acomputerdog.lccontroller.gui.message;

import java.io.File;

public class OpenCMDMessage implements Message {
    public final File file;

    public OpenCMDMessage(File file) {
        this.file = file;
    }
}
