package net.acomputerdog.lccontroller.cli;

import com.fazecast.jSerialComm.SerialPort;
import net.acomputerdog.lccontroller.IOConnection;
import net.acomputerdog.lccontroller.LaserCutter;
import net.acomputerdog.lccontroller.ex.LaserException;

import java.util.InputMismatchException;
import java.util.Scanner;

public class CLIMain implements CLIInput, CLIOutput {
    private static final Scanner keyboard = new Scanner(System.in);

    private final CLIParser parser;

    private IOConnection connection;
    private LaserCutter laser;

    private boolean isRunning = true;

    public CLIMain(IOConnection connection, LaserCutter laser) {
        this.connection = connection;
        this.laser = laser;
        this.parser = new CLIParser(this);
    }

    public void shutdown() {
        laser.disconnect();
    }

    public IOConnection getConnection() {
        return connection;
    }

    public LaserCutter getLaser() {
        return laser;
    }

    @Override
    public void sendLine() {
        System.out.println();
    }

    @Override
    public void sendLine(String line) {
        System.out.println(line);
    }

    @Override
    public void send(String message) {
        System.out.print(message);
    }

    @Override
    public String getLine() {
        return keyboard.nextLine();
    }

    public void run() {
        sendLine("\nConnected to laser.  Use 'help' for commands or 'quit' to exit.");
        while (isRunning) {
            try {
                send("> ");
                String line = getLine();
                parser.parseCommand(line);
                sendLine();
            } catch (LaserException e) {
                sendLine("Laser error: " + e.toString());
            } catch (Exception e) {
                System.err.println("Unexpected error!");
                e.printStackTrace();
                shutdown();
                return; // exit
            }
        }
    }

    public void stop() {
        isRunning = false;
    }

    public static void main(String[] args) {
        System.out.println("LaserController CLI 0.1.0");

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

        IOConnection connection;
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

        LaserCutter laser;
        try {
            System.out.print("Connecting to printer...");
            laser = new LaserCutter(connection);
            System.out.println("OK.");
        } catch (LaserException e) {
            System.out.println("failed.");
            e.printStackTrace();
            return; // exit
        }

        //actually run CLI
        new CLIMain(connection, laser).run();
    }

    private static SerialPort getPort() {
        while (true) {
            System.out.print("Enter serial port (case sensitive): ");
            String name = keyboard.nextLine();
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
                int baud = keyboard.nextInt();
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
                int db = keyboard.nextInt();
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
                int sb = keyboard.nextInt();
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
                int par = keyboard.nextInt();
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
}
