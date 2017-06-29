package net.acomputerdog.lccontroller;

import net.acomputerdog.lccontroller.ex.IOTimeoutException;
import net.acomputerdog.lccontroller.ex.ResponseFormatException;
import net.acomputerdog.lccontroller.util.NumberUtils;

import java.util.ArrayList;
import java.util.List;

public class LaserCutter {
    public static final long TIMEOUT = 3000L;

    private final IOConnection connection;
    private final String fwLine;

    private Location currLocation = new Location(0, 0);
    private long speed = 60;
    private boolean motorsOn = false;
    private boolean laserOn = false;
    private int laserPower = 0;

    public LaserCutter(IOConnection connection) {
        this.connection = connection;
        this.fwLine = connection.waitForLine(TIMEOUT);
        if (fwLine == null) {
            throw new IOTimeoutException("Did not receive firmware ID line.");
        }

        updateLocation();
        enableMotors(motorsOn);
        enableLaser(laserOn);
        setLaserPower(laserPower);
    }

    public boolean sendLine(String line) {
        return connection.send(line, TIMEOUT);
    }

    public void move(Location loc) {
        if (!sendLine(String.format("G0 X%d Y%d F%d\n", loc.getXUM(), loc.getYUM(), speed))) {
            throw new IOTimeoutException("Laser did not respond to G0 in time.");
        }

        currLocation.set(loc);
    }

    public long getSpeed() {
        return speed;
    }

    public void setSpeed(long speed) {
        this.speed = speed;
    }

    public String[] getDebugInfo() {
        if (!sendLine("M145\n")) {
            throw new IOTimeoutException("Laser did not respond to M145 in time.");
        }

        String line = connection.waitForLine(TIMEOUT);
        if (line != null) {
            if ("M145".equals(line.trim())) {
                List<String> lines = new ArrayList<>();
                while (true) {
                    String l = connection.waitForLine(TIMEOUT);
                    if (l != null) {
                        if ("EOL".equals(l.trim())) {
                            break;
                        } else {
                            lines.add(l);
                        }
                    } else {
                        throw new ResponseFormatException("Debug info did not end.");
                    }
                }
                return lines.toArray(new String[lines.size()]);
            } else {
                throw new ResponseFormatException("Received reply from wrong command: '" + line + "'");
            }
        } else {
            throw new IOTimeoutException("Laser did not respond in time.");
        }
    }

    public Location getCurrLocation() {
        return new Location(currLocation);
    }

    public void updateLocation() {
        if (!sendLine("M114")) {
            throw new IOTimeoutException("Laser did not respond to M114 in time.");
        }

        String line = connection.waitForLine(TIMEOUT);
        if (line != null) {
            String response = line.trim();

            if (response.startsWith("M114")) {
                String[] parts = response.split(" ");
                for (String part : parts) {
                    // needs at least 3 chars: 'X:N...'
                    if (part.length() > 2) {
                        int split = part.indexOf(':');
                        if (split == 1) {
                            // part with numbers
                            String numPart = part.substring(2);
                            long num = NumberUtils.parseAxisLoc(numPart);

                            if (part.charAt(0) == 'X') {
                                currLocation.setXUM(num);
                            } else if (part.charAt(0) == 'Y') {
                                currLocation.setYUM(num);
                            }
                        }
                    }
                }
            } else {
                throw new ResponseFormatException("Response with incorrect gcode: '" + response + "'");
            }
        } else {
            throw new IOTimeoutException("Laser did not respond in time.");
        }
    }

    public void disconnect() {
        connection.close();
    }

    public Location getLocation() {
        return currLocation;
    }

    public void enableMotors(boolean enable) {
        if (!sendLine(enable ? "M17" : "M18")) {
            throw new IOTimeoutException("Laser did not acknowledge in time.");
        }
        this.motorsOn = enable;
    }

    public void enableLaser(boolean enable) {
        String line;
        if (enable) {
            line = "M4 S" + String.valueOf(laserPower);
        } else {
            line = "M5";
        }
        if (!sendLine(line)) {
            throw new IOTimeoutException("Laser did not acknowledge in time.");
        }
        this.laserOn = enable;
    }

    public void setLaserPower(int power) {
        this.laserPower = power;
        if (laserOn) {
            // send to laser
            enableLaser(true);
        }
    }

    public String getFwLine() {
        return fwLine;
    }

    public boolean getMotorState() {
        return motorsOn;
    }

    public boolean getLaserState() {
        return laserOn;
    }

    public int getLaserPower() {
        return laserPower;
    }

    public IOConnection getConnection() {
        return connection;
    }

    public boolean isConnected() {
        return connection.isConnected();
    }

    public void moveBy(long xUm, long yUm) {
        move(new Location(currLocation.getXUM() + xUm, currLocation.getYUM() + yUm));
    }
}
