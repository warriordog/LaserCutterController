package net.acomputerdog.lccontroller;

import net.acomputerdog.lccontroller.ex.IOTimeoutException;
import net.acomputerdog.lccontroller.ex.ResponseFormatException;
import net.acomputerdog.lccontroller.util.NumberUtils;

import java.util.ArrayList;
import java.util.List;

public class LaserCutter {
    public static final long DEFAULT_TIMEOUT = 4000L;

    private final IOConnection connection;
    private final String fwLine;

    private Location currLocation = new Location(0, 0);
    private long speed = 60;
    private boolean motorsOn = false;
    private boolean laserOn = false;
    private int laserPower = 0;

    public LaserCutter(IOConnection connection) {
        this.connection = connection;

        this.fwLine = connection.waitForLine(DEFAULT_TIMEOUT);
        if (fwLine == null) {
            throw new IOTimeoutException("Did not receive firmware ID line.");
        }

        updateLocation();
        enableMotors(motorsOn);
        enableLaser(laserOn);
        setLaserPower(laserPower);

        // listen in on gcode responses to keep state
        connection.addLineReceivedMonitor(line -> {
            try {
                // position update
                if (line.startsWith("M114")) {
                    readLocation(line);
                    // laser power update
                } else if (line.startsWith("M105")) {
                    readLaser(line);
                    // full update
                } else if (line.startsWith("I1")) {
                    readFullUpdate(line);
                }

            } catch (Exception ignore) {
            }
        });
    }

    public boolean sendLine(String line) {
        return connection.send(line, DEFAULT_TIMEOUT);
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

        String line = connection.waitForLine(DEFAULT_TIMEOUT);
        if (line != null) {
            if ("M145".equals(line.trim())) {
                List<String> lines = new ArrayList<>();
                while (true) {
                    String l = connection.waitForLine(DEFAULT_TIMEOUT);
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

    public Location getLocation() {
        return new Location(currLocation);
    }

    public void updateLocation() {
        if (!sendLine("M114")) {
            throw new IOTimeoutException("Laser did not respond to M114 in time.");
        }

        String line = connection.waitForLine(DEFAULT_TIMEOUT);
        if (line != null) {
            String response = line.trim();

            if (response.startsWith("M114")) {
                readLocation(response);
            } else {
                throw new ResponseFormatException("Response with incorrect gcode: '" + response + "'");
            }
        } else {
            throw new IOTimeoutException("Laser did not respond in time.");
        }
    }

    private void readLocation(String response) {
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
    }

    private void readFullUpdate(String line) {
        int spaceIdx = line.indexOf(' ');
        while (spaceIdx > -1) {
            // need at least two characters after space
            if (line.length() - spaceIdx < 3) {
                break;
            } else {
                int nextIdx = line.indexOf(' ', spaceIdx + 1);
                try {
                    char ch = line.charAt(spaceIdx + 1);
                    long num;
                    if (nextIdx > -1) {
                        num = Long.parseLong(line.substring(spaceIdx + 2, nextIdx));
                    } else {
                        num = Long.parseLong(line.substring(spaceIdx + 2));
                    }

                    switch (ch) {
                        case 'X':
                            currLocation.setXUM(num);
                            break;
                        case 'Y':
                            currLocation.setYUM(num);
                            break;
                        case 'F':
                            speed = num;
                            break;
                        case 'P':
                            laserOn = (num == 1);
                            break;
                        case 'S':
                            laserPower = (int) num;
                            break;
                        default:
                            //invalid letter, ignore
                            break;
                    }
                } catch (NumberFormatException e) {
                    // bad number, ignore
                }

                spaceIdx = nextIdx;
            }
        }
    }

    private void readLaser(String line) {
        int space = line.indexOf(' ');
        if (space > -1 && line.length() - space > 2) {
            this.laserPower = Integer.parseInt(line.substring(space + 1));
            this.laserOn = this.laserPower >= 0;
        }
    }

    public void disconnect() {
        connection.close();
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

    public boolean isLaserOn() {
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

    public void requestImmediateUpdate() {
        connection.sendAsync("I1");
    }
}
