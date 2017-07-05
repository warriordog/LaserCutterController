package net.acomputerdog.lccontroller.gui.message;

import net.acomputerdog.lccontroller.Location;

public class MotorMoveMessage implements Message {
    public final Location loc;

    public MotorMoveMessage(Location loc) {
        this.loc = loc;
    }
}
