package net.acomputerdog.lpcontroller.cli;

import com.fazecast.jSerialComm.SerialPort;
import net.acomputerdog.lpcontroller.IOConnection;
import net.acomputerdog.lpcontroller.LaserCutter;
import net.acomputerdog.lpcontroller.Location;
import net.acomputerdog.lpcontroller.ex.InternalIOException;
import net.acomputerdog.lpcontroller.ex.LaserException;
import net.acomputerdog.lpcontroller.ex.ResponseFormatException;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.InputMismatchException;
import java.util.Scanner;

public class CLIMain {
    private static final Scanner in = new Scanner(System.in);
    private static IOConnection connection;
    private static LaserCutter laser;
    private static boolean inShell;
    private static boolean inScript;

    public static void main(String[] args) {
        System.out.println("LaserController CLI 0.0.1");

        // read from STDIN
        SerialPort port;
        int baud;
        int dataBits;
        int stopBits;
        int parity;
        if (args.length != 5) {
            port = getPort();
            baud = getBaud();
            dataBits = getDataBits();
            stopBits = getStopBits();
            parity = getParity();

            // read from args
        } else {
            try {
                port = SerialPort.getCommPort(args[0]);
                baud = Integer.parseInt(args[1]);
                dataBits = Integer.parseInt(args[2]);
                stopBits = Integer.parseInt(args[3]);
                parity = Integer.parseInt(args[4]);
            } catch (NumberFormatException e) {
                System.out.println("Port parameters must be integers.");
                return; //exit
            }
        }

        try {
            connection = openConnection(port, baud, dataBits, stopBits, parity);
            if (connection == null) {
                System.err.println("Failed to open port.");
                return; //exit
            }
        } catch (LaserException e) {
            System.err.println("Error connecting to laser.");
            e.printStackTrace();
            return; //exit
        }

        try {
            System.out.print("Connecting to printer...");
            laser = new LaserCutter(connection);
            System.out.println("OK.");
        } catch (LaserException e) {
            System.out.println("failed.");
            e.printStackTrace();
            return; // exit
        }

        System.out.println("\nConnected to laser.  Use 'help' for commands or 'quit' to exit.");
        while (true) {
            try {
                System.out.print("> ");
                String line = in.nextLine().trim();
                parseCommand(line);
                System.out.println();
            } catch (LaserException e) {
                System.out.println("Laser error: " + e.toString());
            } catch (Exception e) {
                System.err.println("Unexpected error!");
                e.printStackTrace();
                shutdown();
                return; // exit
            }
        }
    }

    private static void parseCommand(String line) {
        if (!line.isEmpty()) {
            int split = line.indexOf(' ');

            String cmd;
            if (split > 0) {
                cmd = line.substring(0, split).toLowerCase();
            } else {
                cmd = line.toLowerCase();
            }

            switch (cmd) {
                case "help": {
                    System.out.println("Valid commands:");
                    System.out.println("  Axis/motor commands:");
                    System.out.println("    getposition - get position");
                    System.out.println("    setposition [X#] [Y#] - move to position");
                    System.out.println("    getspeed - get speed setting");
                    System.out.println("    setspeed <speed> - set movement speed");
                    System.out.println("  Power commands:");
                    System.out.println("    getmotorstate - check if motors are on");
                    System.out.println("    motorson - turn motors on");
                    System.out.println("    mototsoff - turn motors off");
                    System.out.println("    getlaserstate - check if laser is on");
                    System.out.println("    laserson - turn laser on");
                    System.out.println("    laseroff - turn laser off");
                    System.out.println("    getlaserpower - get laser power");
                    System.out.println("    laserpower <power> - set laser power");
                    System.out.println("  Debugging commands:");
                    System.out.println("    fwline - get firmware version line");
                    System.out.println("    debug - print debug info");
                    System.out.println("    gcode <line> - send raw gcode");
                    System.out.println("    shell - enter gcode shell");
                    System.out.println("  Scripting commands:");
                    System.out.println("    scriptcmd - Run a script of controller commands");
                    System.out.println("    scriptgcode - Run a script of gcodes");
                    System.out.println("  Other commands:");
                    System.out.println("    quit - disconnect and exit");
                    System.out.println("    help - show this help");
                    break;
                }
                case "quit": {
                    shutdown();
                    System.exit(0);
                    break;
                }
                case "gcode": {
                    if (split < 0 || line.length() - split < 2) {
                        System.out.println("You must enter a gcode to send.");
                    } else {
                        String gcode = line.substring(split + 1);
                        if (!laser.sendLine(gcode)) {
                            System.out.println("Printer did not acknowledge in time, is it connected?");
                        }
                    }
                    break;
                }
                case "shell": {
                    if (!inShell) {
                        if (!inScript) {
                            runShell();
                        }
                    } else {
                        System.out.println("$ You are already in shell mode.  If you want to exit, use '$EXIT'.");
                    }
                    break;
                }
                case "getposition": {
                    laser.updateLocation();
                    System.out.println(laser.getLocation());
                    break;
                }
                case "setposition": {
                    if (split < 0 || line.length() - split < 2) {
                        System.out.println("You must specify one or more axis to adjust.");
                    } else {
                        try {
                            Location l = laser.getCurrLocation();
                            l.setFromString(line.substring(split + 1));
                            laser.move(l);
                            System.out.println(laser.getLocation());
                        } catch (ResponseFormatException e) {
                            System.out.println("Locations must be an axis letter followed be an integer or decimal number.");
                        }
                    }
                    break;
                }
                case "getspeed": {
                    System.out.printf("%d mm/s", laser.getSpeed());
                    break;
                }
                case "setspeed": {
                    if (split < 0 || line.length() - split < 2) {
                        System.out.println("You must enter a speed to set.");
                    } else {
                        try {
                            int speed = Integer.parseInt(line.substring(split + 1));
                            laser.setSpeed(speed);
                        } catch (NumberFormatException e) {
                            System.out.println("Speed must be an integer.");
                        }
                    }
                    break;
                }
                case "debug": {
                    String[] lines = laser.getDebugInfo();
                    for (String debugLine : lines) {
                        System.out.println(debugLine);
                    }
                    break;
                }
                case "motorson": {
                    laser.enableMotors(true);
                    break;
                }
                case "motorsoff": {
                    laser.enableMotors(false);
                    break;
                }
                case "laseron": {
                    laser.enableLaser(true);
                    break;
                }
                case "laseroff": {
                    laser.enableLaser(false);
                    break;
                }
                case "laserpower": {
                    if (split < 0 || line.length() - split < 2) {
                        System.out.println("You must enter the laser power.");
                    } else {
                        try {
                            int power = Integer.parseInt(line.substring(split + 1));
                            if (power < 0 || power > 255) {
                                System.out.println("Power must be between 0 and 255 (inclusive).");
                            } else {
                                laser.setLaserPower(power);
                            }
                        } catch (NumberFormatException e) {
                            System.out.println("Laser power must be an integer.");
                        }
                    }
                    break;
                }
                case "getmotorstate": {
                    if (laser.getMotorState()) {
                        System.out.println("Motors are: ON");
                    } else {
                        System.out.println("Motors are: OFF");
                    }
                    break;
                }
                case "getlaserstate": {
                    if (laser.getLaserState()) {
                        System.out.println("Laser is: ON");
                    } else {
                        System.out.println("Laser is: OFF");
                    }
                    break;
                }
                case "getlaserpower": {
                    System.out.println(laser.getLaserPower());
                    break;
                }
                case "scriptcmd": {
                    if (!inScript) {
                        if (split < 0 || line.length() - split < 2) {
                            System.out.println("You must enter the path to a script.");
                        } else {
                            inScript = true;
                            runCMDScript(line.substring(split + 1));
                            inScript = false;
                        }
                    }
                    break;
                }
                case "scriptgcode": {
                    if (split < 0 || line.length() - split < 2) {
                        System.out.println("You must enter the path to a script.");
                    } else {
                        runGCodeScript(line.substring(split + 1));
                    }
                    break;
                }
                case "fwline": {
                    System.out.println(laser.getFwLine());
                    break;
                }
                default: {
                    System.out.println("Unknown command: " + cmd);
                    break;
                }
            }
        }
    }

    private static void runCMDScript(String path) {
        try {
            BufferedReader reader = new BufferedReader(new FileReader(path));
            System.out.println("CMD script running.");
            while (reader.ready()) {
                String line = reader.readLine();
                parseCommand(line);
            }
            System.out.println("CMD script done.");
        } catch (FileNotFoundException e) {
            System.out.println("File not found: '" + path + "'");
        } catch (IOException | InternalIOException e) {
            System.out.println("An IO error occurred.");
        } catch (LaserException e) {
            System.out.println("Unexpected laser error occurred: " + e.toString());
        } catch (Exception e) {
            System.err.println("Unexpected error occurred.");
            e.printStackTrace();
        }
    }

    private static void runGCodeScript(String path) {
        try {
            BufferedReader reader = new BufferedReader(new FileReader(path));
            System.out.println("GCode script running.");
            while (reader.ready()) {
                String line = reader.readLine();
                while (!laser.sendLine(line)) {
                    // loop until it there is space in machine buffer.
                }
            }
            System.out.println("GCode script done.");
        } catch (FileNotFoundException e) {
            System.out.println("File not found: '" + path + "'");
        } catch (IOException | InternalIOException e) {
            System.out.println("An IO error occurred.");
        } catch (LaserException e) {
            System.out.println("Unexpected laser error occurred: " + e.toString());
        } catch (Exception e) {
            System.err.println("Unexpected error occurred.");
            e.printStackTrace();
        }
    }

    private static SerialPort getPort() {
        while (true) {
            System.out.print("Enter serial port (case sensitive): ");
            String name = in.nextLine();
            for (SerialPort port : SerialPort.getCommPorts()) {
                // it only returns the name, not the path
                if (name.contains(port.getSystemPortName())) {
                    return port;
                }
            }
            System.out.println("That port could not be found.");
        }
    }

    private static int getBaud() {
        while (true) {
            try {
                System.out.print("Enter baud rate: ");
                int baud = in.nextInt();
                if (baud < 1) {
                    System.out.println("Baud must be at least 1.");
                } else {
                    return baud;
                }
            } catch (InputMismatchException e) {
                System.out.println("Please enter an integer.");
            }
        }
    }

    private static int getDataBits() {
        while (true) {
            try {
                System.out.print("Enter # of data bits: ");
                int db = in.nextInt();
                if (db < 5 || db > 8) {
                    System.out.println("Data bits must be between 5 and 8.");
                } else {
                    return db;
                }
            } catch (InputMismatchException e) {
                System.out.println("Please enter an integer.");
            }
        }
    }

    private static int getStopBits() {
        while (true) {
            try {
                System.out.print("Enter # of stop bits: ");
                int sb = in.nextInt();
                if (sb < 1 || sb > 3) {
                    System.out.println("Stop bits must be between 1 and 3.");
                } else {
                    return sb;
                }
            } catch (InputMismatchException e) {
                System.out.println("Please enter an integer.");
            }
        }
    }

    private static int getParity() {
        while (true) {
            try {
                System.out.print("Enter parity: ");
                int par = in.nextInt();
                if (par < 0 || par > 4) {
                    System.out.println("Parity must be between 0 and 4.");
                } else {
                    return par;
                }
            } catch (InputMismatchException e) {
                System.out.println("Please enter an integer.");
            }
        }
    }

    private static IOConnection openConnection(SerialPort port, int baud, int dataBits, int stopBits, int parity) {
        System.out.print("Opening port...");
        port.setBaudRate(baud);
        port.setNumDataBits(dataBits);
        port.setNumStopBits(stopBits);
        port.setParity(parity);
        port.setFlowControl(SerialPort.FLOW_CONTROL_DISABLED);
        port.setComPortTimeouts(SerialPort.TIMEOUT_READ_SEMI_BLOCKING | SerialPort.TIMEOUT_WRITE_BLOCKING, 0, 0);
        port.openPort();

        IOConnection connection = new IOConnection(port);
        System.out.println("OK");
        return connection;
    }

    private static void shutdown() {
        laser.disconnect();
    }

    private static void runShell() {
        inShell = true;

        Thread shellThread = new Thread(() -> {
            while (inShell) {
                try {
                    String line = connection.waitForLine();
                    if (line != null && !line.isEmpty()) {
                        if (line.charAt(0) == '$') {
                            line = "\\".concat(line);
                        }
                        System.out.println(line);
                    }
                } catch (Exception ignored) {
                }
            }
        });
        shellThread.setDaemon(true);
        shellThread.start();

        try {
            System.out.println("GCode shell opened.  You can enter controller commands by prefixing them with '$'.  '$EXIT' will return to normal mode.");
            while (true) {
                String line = in.nextLine();
                if (!line.isEmpty()) {
                    if (line.charAt(0) == '$') {
                        if (line.equalsIgnoreCase("$EXIT")) {
                            break;
                        } else {
                            parseCommand(line.substring(1));
                        }
                    } else {
                        laser.sendLine(line);
                    }
                }
            }
        } finally {
            inShell = false;
            shellThread.interrupt();
        }
    }
}
