package net.acomputerdog.lccontroller.gui.message;

public class SetLaserPowerMessage implements Message {
    public final int power;

    public SetLaserPowerMessage(int power) {
        this.power = power;
    }
}
