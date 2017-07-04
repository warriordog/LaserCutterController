package net.acomputerdog.lccontroller;

public class LaserProperties {
    private final int bedWidth;
    private final int bedHeight;

    public LaserProperties(int bedWidth, int bedHeight) {
        this.bedWidth = bedWidth;
        this.bedHeight = bedHeight;
    }

    public int getBedWidth() {
        return bedWidth;
    }

    public int getBedHeight() {
        return bedHeight;
    }
}
