package net.acomputerdog.lccontroller.ex;

public class IOTimeoutException extends LaserException {
    public IOTimeoutException() {
        super();
    }

    public IOTimeoutException(String message) {
        super(message);
    }

    public IOTimeoutException(String message, Throwable cause) {
        super(message, cause);
    }
}
