package net.acomputerdog.lpcontroller;

import com.fazecast.jSerialComm.SerialPort;
import net.acomputerdog.lpcontroller.ex.InternalIOException;
import net.acomputerdog.lpcontroller.util.LockedNotifier;

import java.io.*;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.Semaphore;

public class IOConnection {
    private static final int INPUT_BUFFER_SIZE = 2048;

    private final SerialPort serialPort;
    private final InputStream serialIn;
    private final Writer serialOut;

    private final Semaphore inputQueueLock = new Semaphore(1);
    private final Queue<String> inputQueue = new LinkedList<>();

    private final Thread serialReader;

    private boolean isOpen = true;

    private final LockedNotifier ackLock = new LockedNotifier(false);
    private final LockedNotifier lineLock = new LockedNotifier(true);

    public IOConnection(SerialPort serialPort) {
        this.serialPort = serialPort;

        this.serialIn = serialPort.getInputStream();
        this.serialOut = new OutputStreamWriter(serialPort.getOutputStream());

        this.serialReader = new Thread(new Runnable() {
            private char[] buffer = new char[INPUT_BUFFER_SIZE];
            private int bufferPos = 0;

            @Override
            public void run() {
                while (isOpen) {
                    try {
                        // read a char.  it will either be added, ignored as /n, or ignored because line is too long
                        char chr = (char) serialIn.read();

                        // if we get a newline, then store line
                        if (chr == '\n') {
                            String line = String.valueOf(buffer, 0, bufferPos);
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
                    } catch (IOException e) {
                        System.err.println("Exception in serial read thread.");
                        e.printStackTrace();
                        close();
                    }
                }
            }
        });
        serialReader.setDaemon(true);
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

    public boolean send(String line, long timeout) {
        try {
            serialOut.write(line);
            serialOut.write('\n');
            serialOut.flush();

            return ackLock.waitForNotify(timeout);
        } catch (IOException e) {
            throw new InternalIOException("Exception writing line.", e);
        }
    }

    public void close() {
        isOpen = false;
        closeSafe(serialIn);
        closeSafe(serialOut);
        serialReader.interrupt();
        serialPort.closePort();
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
