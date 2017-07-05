package net.acomputerdog.lccontroller;

import com.fazecast.jSerialComm.SerialPort;
import net.acomputerdog.lccontroller.ex.InternalIOException;
import net.acomputerdog.lccontroller.util.LockedNotifier;

import java.io.*;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.Semaphore;
import java.util.function.Consumer;

public class IOConnection {
    private static final int INPUT_BUFFER_SIZE = 2048;

    private final SerialPort serialPort;
    private final InputStream serialIn;
    private final Writer serialOut;

    private final Semaphore inputQueueLock = new Semaphore(1);
    private final Queue<String> inputQueue = new LinkedList<>();

    private final Thread serialReader;

    private boolean isOpen = true;

    private final LockedNotifier ackLock = new LockedNotifier();
    private final LockedNotifier lineLock = new LockedNotifier();

    private final List<Consumer<String>> lineReceivedMonitors = new LinkedList<>();
    private final List<Consumer<String>> lineSentMonitors = new LinkedList<>();

    public IOConnection(SerialPort serialPort) {
        this.serialPort = serialPort;

        this.serialIn = serialPort.getInputStream();
        this.serialOut = new OutputStreamWriter(serialPort.getOutputStream());

        // flush buffer in case there is already data
        try {
            while (serialIn.available() > 0) {
                serialIn.read();
            }
        } catch (IOException e) {
            throw new RuntimeException("Exception flushing read buffer.", e);
        }

        this.serialReader = new Thread(new Runnable() {
            private char[] buffer = new char[INPUT_BUFFER_SIZE];
            private int bufferPos = 0;

            @Override
            public void run() {
                try {
                    while (isOpen) {
                        // read a char.  it will either be added, ignored as /n, or ignored because line is too long
                        char chr = (char) serialIn.read();

                        // if we get a newline, then store line
                        if (chr == '\n') {
                            String line = String.valueOf(buffer, 0, bufferPos);

                            for (Consumer<String> receiver : lineReceivedMonitors) {
                                receiver.accept(line);
                            }

                            if ("OK".equals(line)) {
                                ackLock.release();
                            } else {
                                inputQueueLock.acquireUninterruptibly();
                                try {
                                    inputQueue.add(line);
                                } finally {
                                    inputQueueLock.release();
                                }

                                lineLock.release();
                            }
                            buffer = new char[INPUT_BUFFER_SIZE];
                            bufferPos = 0;
                            // if buffer has room, then add character
                        } else if (bufferPos < INPUT_BUFFER_SIZE) {
                            buffer[bufferPos] = chr;
                            bufferPos++;
                        }
                    }
                } catch (Exception e) {
                    close();
                }
            }
        });
        serialReader.setDaemon(true);
        serialReader.setName("Serial_Read_Thread");
        serialReader.start();
    }

    public boolean linesAvailable() {
        inputQueueLock.acquireUninterruptibly();
        try {
            return !inputQueue.isEmpty();
        } finally {
            inputQueueLock.release();
        }
    }

    public String nextLine() {
        inputQueueLock.acquireUninterruptibly();
        try {
            return inputQueue.poll();
        } finally {
            inputQueueLock.release();
        }
    }

    public String waitForLine() {
        return waitForLine(-1);
    }

    public String waitForLine(long timeout) {
        if (lineLock.waitForNotify(timeout)) {
            if (linesAvailable()) {
                return nextLine();
            } else {
                return null;
            }
        } else {
            return null;
        }
    }

    public void sendAsync(String line) {
        try {
            serialOut.write(line);
            serialOut.write('\n');
            serialOut.flush();

            for (Consumer<String> receiver : lineSentMonitors) {
                receiver.accept(line);
            }
        } catch (IOException e) {
            throw new InternalIOException("Exception writing line.", e);
        }
    }

    public boolean send(String line, long timeout) {
        sendAsync(line);

        return ackLock.waitForNotify(timeout);
    }

    public void close() {
        isOpen = false;
        serialReader.interrupt();
        closeSafe(serialIn);
        closeSafe(serialOut);
        serialPort.closePort();
    }

    public void addLineSentMonitor(Consumer<String> monitor) {
        if (monitor != null) {
            lineSentMonitors.add(monitor);
        }
    }

    public void addLineReceivedMonitor(Consumer<String> monitor) {
        if (monitor != null) {
            lineReceivedMonitors.add(monitor);
        }
    }

    public void removeLineSentMonitor(Consumer<String> monitor) {
        if (monitor != null) {
            lineSentMonitors.remove(monitor);
        }
    }

    public void removeLineReceivedMonitor(Consumer<String> monitor) {
        if (monitor != null) {
            lineReceivedMonitors.remove(monitor);
        }
    }

    public boolean isConnected() {
        return isOpen;
    }

    private static void closeSafe(Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (Exception ignored) {
            }
        }
    }
}
