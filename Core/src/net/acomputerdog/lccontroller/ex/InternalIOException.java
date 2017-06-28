package net.acomputerdog.lccontroller.ex;

import java.io.IOException;

public class InternalIOException extends LaserException {
    public InternalIOException(IOException cause) {
        this(null, cause);
    }

    public InternalIOException(String message, IOException cause) {
        super(message, cause);
    }
}
