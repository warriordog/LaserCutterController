package net.acomputerdog.lpcontroller.ex;

public class LaserException extends RuntimeException {
    public LaserException() {
        super();
    }

    public LaserException(String message) {
        super(message);
    }

    public LaserException(String message, Throwable cause) {
        super(message, cause);
    }
}
