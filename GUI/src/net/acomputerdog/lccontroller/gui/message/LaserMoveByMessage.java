package net.acomputerdog.lccontroller.gui.message;

public class LaserMoveByMessage implements Message {
    public final long xUm, yUm;

    public LaserMoveByMessage(long xUm, long yUm) {
        this.xUm = xUm;
        this.yUm = yUm;
    }
}
