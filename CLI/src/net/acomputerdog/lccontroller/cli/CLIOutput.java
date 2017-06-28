package net.acomputerdog.lccontroller.cli;

public interface CLIOutput {
    default void sendLine() {
        sendLine("");
    }

    default void sendLine(String line) {
        send(line.concat("\n"));
    }

    void send(String message);

    default void sendf(String format, Object... args) {
        send(String.format(format, args));
    }

    default void sendLine(int val) {
        sendLine(String.valueOf(val));
    }

    default void sendLine(long val) {
        sendLine(String.valueOf(val));
    }

    default void sendLine(Object obj) {
        sendLine(String.valueOf(obj));
    }
}
