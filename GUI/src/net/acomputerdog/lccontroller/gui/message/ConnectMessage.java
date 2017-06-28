package net.acomputerdog.lccontroller.gui.message;

public class ConnectMessage implements Message {
    public final String port;
    public final int baud;
    public final int dataBits;
    public final int stopBits;
    public final int parity;
    public final int flowMode;

    public ConnectMessage(String port, int baud, int dataBits, int stopBits, int parity, int flowMode) {
        this.port = port;
        this.baud = baud;
        this.dataBits = dataBits;
        this.stopBits = stopBits;
        this.parity = parity;
        this.flowMode = flowMode;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ConnectMessage that = (ConnectMessage) o;

        if (baud != that.baud) return false;
        if (dataBits != that.dataBits) return false;
        if (stopBits != that.stopBits) return false;
        if (parity != that.parity) return false;
        if (flowMode != that.flowMode) return false;
        return port != null ? port.equals(that.port) : that.port == null;
    }

    @Override
    public int hashCode() {
        int result = port != null ? port.hashCode() : 0;
        result = 31 * result + baud;
        result = 31 * result + dataBits;
        result = 31 * result + stopBits;
        result = 31 * result + parity;
        result = 31 * result + flowMode;
        return result;
    }
}
