package net.acomputerdog.lasertest;

import com.fazecast.jSerialComm.SerialPort;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Scanner;

public class SerialTest {
    public static void main(String[] args) {
        SerialPort port = SerialPort.getCommPort("/dev/ttyUSB0");
        port.setBaudRate(115200);
        port.setNumDataBits(8);
        port.setNumStopBits(1);
        port.setParity(SerialPort.NO_PARITY);
        port.setFlowControl(SerialPort.FLOW_CONTROL_DISABLED);
        port.setComPortTimeouts(SerialPort.TIMEOUT_READ_SEMI_BLOCKING | SerialPort.TIMEOUT_WRITE_BLOCKING, 0, 0);

        if (port.openPort()) {
            //System.out.println("Port descriptor: " + port.getSystemPortName());
            //System.out.println("Port name: " + port.getDescriptivePortName());

            BufferedReader in = new BufferedReader(new InputStreamReader(port.getInputStream()));
            //InputStream in = port.getInputStream();
            Writer writer = new OutputStreamWriter(port.getOutputStream());
            Scanner keyboard = new Scanner(System.in);

            Thread readThread = new Thread(() -> {
                try {
                    while (true) {
                        //System.out.write(in.read());
                        System.out.println(in.readLine());
                    }
                } catch (Exception e) {
                    System.err.println("Exception in reader.");
                    e.printStackTrace();
                }
            });
            readThread.setDaemon(true);
            readThread.start();

            try {
                while (true) {
                    String line = keyboard.nextLine();
                    writer.write(line);
                    writer.write('\n');
                    writer.flush();
                }
            } catch (Exception e) {
                System.err.println("Exception in writer.");
                e.printStackTrace();
            }
        } else {
            System.out.println("Port did not open.");
        }

    }
}
