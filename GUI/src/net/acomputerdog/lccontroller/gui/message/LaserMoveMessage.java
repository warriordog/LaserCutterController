package net.acomputerdog.lccontroller.gui.message;

import net.acomputerdog.lccontroller.Location;

public class LaserMoveMessage implements Message {
    public final Location loc;

    public LaserMoveMessage(Location loc) {
        this.loc = loc;
    }
}
