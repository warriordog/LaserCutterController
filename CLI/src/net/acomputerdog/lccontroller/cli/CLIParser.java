package net.acomputerdog.lccontroller.cli;

import net.acomputerdog.lccontroller.IOConnection;
import net.acomputerdog.lccontroller.LaserCutter;
import net.acomputerdog.lccontroller.Location;
import net.acomputerdog.lccontroller.ex.InternalIOException;
import net.acomputerdog.lccontroller.ex.LaserException;
import net.acomputerdog.lccontroller.ex.ResponseFormatException;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Scanner;

public class CLIParser {
    private final CLIMain main;
    private final LaserCutter laser;
    private final IOConnection connection;
    private final Scanner keyboard = new Scanner(System.in);

    private boolean inShell = false;
    private boolean inScript = false;

    public CLIParser(CLIMain main) {
        this.main = main;
        this.laser = main.getLaser();
        this.connection = main.getConnection();
    }


    public void parseCommand(String line) {
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
                    main.sendLine("Valid commands:");
                    main.sendLine("  Axis/motor commands:");
                    main.sendLine("    getposition - get position");
                    main.sendLine("    setposition [X#] [Y#] - move to position");
                    main.sendLine("    getspeed - get speed setting");
                    main.sendLine("    setspeed <speed> - set movement speed");
                    main.sendLine("  Power commands:");
                    main.sendLine("    getmotorstate - check if motors are on");
                    main.sendLine("    motorson - turn motors on");
                    main.sendLine("    mototsoff - turn motors off");
                    main.sendLine("    getlaserstate - check if laser is on");
                    main.sendLine("    laserson - turn laser on");
                    main.sendLine("    laseroff - turn laser off");
                    main.sendLine("    getlaserpower - get laser power");
                    main.sendLine("    laserpower <power> - set laser power");
                    main.sendLine("  Debugging commands:");
                    main.sendLine("    fwline - get firmware version line");
                    main.sendLine("    debug - print debug info");
                    main.sendLine("    gcode <line> - send raw gcode");
                    main.sendLine("    shell - enter gcode shell");
                    main.sendLine("  Scripting commands:");
                    main.sendLine("    scriptcmd - Run a script of controller commands");
                    main.sendLine("    scriptgcode - Run a script of gcodes");
                    main.sendLine("  Other commands:");
                    main.sendLine("    quit - disconnect and exit");
                    main.sendLine("    help - show this help");
                    break;
                }
                case "quit": {
                    main.shutdown();
                    System.exit(0);
                    break;
                }
                case "gcode": {
                    if (split < 0 || line.length() - split < 2) {
                        main.sendLine("You must enter a gcode to send.");
                    } else {
                        String gcode = line.substring(split + 1);
                        if (!laser.sendLine(gcode)) {
                            main.sendLine("Printer did not acknowledge in time, is it connected?");
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
                        main.sendLine("$ You are already in shell mode.  If you want to exit, use '$EXIT'.");
                    }
                    break;
                }
                case "getposition": {
                    laser.updateLocation();
                    main.sendLine(laser.getLocation());
                    break;
                }
                case "setposition": {
                    if (split < 0 || line.length() - split < 2) {
                        main.sendLine("You must specify one or more axis to adjust.");
                    } else {
                        try {
                            Location l = laser.getLocation();
                            l.setFromString(line.substring(split + 1));
                            laser.move(l);
                            main.sendLine(laser.getLocation());
                        } catch (ResponseFormatException e) {
                            main.sendLine("Locations must be an axis letter followed be an integer or decimal number.");
                        }
                    }
                    break;
                }
                case "getspeed": {
                    main.sendf("%d mm/s", laser.getSpeed());
                    break;
                }
                case "setspeed": {
                    if (split < 0 || line.length() - split < 2) {
                        main.sendLine("You must enter a speed to set.");
                    } else {
                        try {
                            int speed = Integer.parseInt(line.substring(split + 1));
                            laser.setSpeed(speed);
                        } catch (NumberFormatException e) {
                            main.sendLine("Speed must be an integer.");
                        }
                    }
                    break;
                }
                case "debug": {
                    String[] lines = laser.getDebugInfo();
                    for (String debugLine : lines) {
                        main.sendLine(debugLine);
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
                    laser.setLaserState(true);
                    break;
                }
                case "laseroff": {
                    laser.setLaserState(false);
                    break;
                }
                case "laserpower": {
                    if (split < 0 || line.length() - split < 2) {
                        main.sendLine("You must enter the laser power.");
                    } else {
                        try {
                            int power = Integer.parseInt(line.substring(split + 1));
                            if (power < 0 || power > 255) {
                                main.sendLine("Power must be between 0 and 255 (inclusive).");
                            } else {
                                laser.setLaserPower(power);
                            }
                        } catch (NumberFormatException e) {
                            main.sendLine("Laser power must be an integer.");
                        }
                    }
                    break;
                }
                case "getmotorstate": {
                    if (laser.getMotorState()) {
                        main.sendLine("Motors are: ON");
                    } else {
                        main.sendLine("Motors are: OFF");
                    }
                    break;
                }
                case "getlaserstate": {
                    if (laser.isLaserOn()) {
                        main.sendLine("Laser is: ON");
                    } else {
                        main.sendLine("Laser is: OFF");
                    }
                    break;
                }
                case "getlaserpower": {
                    main.sendLine(laser.getLaserPower());
                    break;
                }
                case "scriptcmd": {
                    if (!inScript) {
                        if (split < 0 || line.length() - split < 2) {
                            main.sendLine("You must enter the path to a script.");
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
                        main.sendLine("You must enter the path to a script.");
                    } else {
                        runGCodeScript(line.substring(split + 1));
                    }
                    break;
                }
                case "fwline": {
                    main.sendLine(laser.getFwLine());
                    break;
                }
                default: {
                    main.sendLine("Unknown command: " + cmd);
                    break;
                }
            }
        }
    }

    private void runShell() {
        inShell = true;

        Thread shellThread = new Thread(() -> {
            while (inShell) {
                try {
                    String line = connection.waitForLine();
                    if (line != null && !line.isEmpty()) {
                        if (line.charAt(0) == '$') {
                            line = "\\".concat(line);
                        }
                        main.sendLine(line);
                    }
                } catch (Exception ignored) {
                }
            }
        });
        shellThread.setDaemon(true);
        shellThread.start();

        try {
            main.sendLine("GCode shell opened.  You can enter controller commands by prefixing them with '$'.  '$EXIT' will return to normal mode.");
            while (true) {
                String line = keyboard.nextLine();
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

    private void runCMDScript(String path) {
        try {
            BufferedReader reader = new BufferedReader(new FileReader(path));
            main.sendLine("CMD script running.");
            while (reader.ready()) {
                String line = reader.readLine();
                parseCommand(line);
            }
            main.sendLine("CMD script done.");
        } catch (FileNotFoundException e) {
            main.sendLine("File not found: '" + path + "'");
        } catch (IOException | InternalIOException e) {
            main.sendLine("An IO error occurred.");
        } catch (LaserException e) {
            main.sendLine("Unexpected laser error occurred: " + e.toString());
        } catch (Exception e) {
            System.err.println("Unexpected error occurred.");
            e.printStackTrace();
        }
    }

    private void runGCodeScript(String path) {
        try {
            BufferedReader reader = new BufferedReader(new FileReader(path));
            main.sendLine("GCode script running.");
            while (reader.ready()) {
                String line = reader.readLine();
                while (!laser.sendLine(line)) {
                    // loop until it there is space in machine buffer.
                }
            }
            main.sendLine("GCode script done.");
        } catch (FileNotFoundException e) {
            main.sendLine("File not found: '" + path + "'");
        } catch (IOException | InternalIOException e) {
            main.sendLine("An IO error occurred.");
        } catch (LaserException e) {
            main.sendLine("Unexpected laser error occurred: " + e.toString());
        } catch (Exception e) {
            System.err.println("Unexpected error occurred.");
            e.printStackTrace();
        }
    }
}
