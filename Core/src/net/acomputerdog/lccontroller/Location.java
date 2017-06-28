package net.acomputerdog.lccontroller;

import net.acomputerdog.lccontroller.util.NumberUtils;

public class Location {
    private long xUM;
    private long yUM;

    public Location(long xUM, long yUM) {
        this.xUM = xUM;
        this.yUM = yUM;
    }

    public Location(Location old) {
        this(old.xUM, old.yUM);
    }

    public long getXUM() {
        return xUM;
    }

    public long getYUM() {
        return yUM;
    }

    public long getXMM() {
        return xUM / 1000L;
    }

    public long getYMM() {
        return yUM / 1000L;
    }

    public void setXUM(long xUM) {
        this.xUM = xUM;
    }

    public void setYUM(long yUM) {
        this.yUM = yUM;
    }

    public void setXMM(long xMM) {
        this.xUM = xMM * 1000L;
    }

    public void setYMM(long yMM) {
        this.yUM = yMM * 1000L;
    }

    public void set(Location o) {
        if (o != null) {
            this.xUM = o.xUM;
            this.yUM = o.yUM;
        }
    }

    public void setFromString(String pos) {
        if (pos != null) {
            int split = pos.indexOf(' ');

            if (split >= 0 && pos.length() - split > 2) {
                setFromSingleString(pos.substring(0, split));
                setFromSingleString(pos.substring(split + 1));
            } else {
                setFromSingleString(pos);
            }
        }
    }

    private void setFromSingleString(String pos) {
        if (!pos.isEmpty()) {
            if (pos.charAt(0) == 'X') {
                xUM = NumberUtils.parseAxisLoc(pos.substring(1));
            } else if (pos.charAt(0) == 'Y') {
                yUM = NumberUtils.parseAxisLoc(pos.substring(1));
            }
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Location location = (Location) o;

        return xUM == location.xUM && yUM == location.yUM;
    }

    @Override
    public int hashCode() {
        int result = (int) (xUM ^ (xUM >>> 32));
        result = 31 * result + (int) (yUM ^ (yUM >>> 32));
        return result;
    }

    @Override
    public String toString() {
        return String.format("%d.%d, %d.%d (X%d Y%d)", (xUM / 1000L), (xUM % 1000L), (yUM / 1000L), (yUM % 1000L), xUM, yUM);
    }
}
