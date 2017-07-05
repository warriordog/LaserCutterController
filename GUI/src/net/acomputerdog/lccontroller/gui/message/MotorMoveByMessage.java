package net.acomputerdog.lccontroller.gui.message;

public class MotorMoveByMessage implements Message {
    public final long xUm, yUm;

    public MotorMoveByMessage(long xUm, long yUm) {
        this.xUm = xUm;
        this.yUm = yUm;
    }
}
