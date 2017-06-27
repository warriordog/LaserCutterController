package net.acomputerdog.lccontroller.util;

import java.util.concurrent.Semaphore;

public class LockedNotifier {
    private final Lock lock = new Lock(new Object());
    private final Semaphore notifiedLock = new Semaphore(1);
    private volatile int notifyCount = 0;

    private boolean debug;

    public LockedNotifier(boolean debug) {
        this.debug = debug;
    }

    public boolean waitForNotify() {
        return waitForNotify(-1);
    }

    public boolean waitForNotify(long timeout) {
        // make sure there is not already a notification
        notifiedLock.acquireUninterruptibly();
        if (notifyCount > 0) {
            // update atomically in case other thread is about to mark notified again
            notifyCount--;
            notifiedLock.release();

            //System.out.println("Already notified");

            return true;
        }
        notifiedLock.release();

        // wait for notification
        try {
            // acquire lock
            lock.lock(timeout);

            // lock variable when we are notified
            notifiedLock.acquireUninterruptibly();
            try {
                if (notifyCount > 0) {
                    notifyCount--;
                    return true;
                } else {
                    return false;
                }
            } finally {
                notifiedLock.release();
            }
        } catch (InterruptedException e) {
            return false;
        }
    }

    public void release() {
        if (debug) {
            //System.out.println("Releasing: count=" + (notifyCount+ 1));
        }

        notifiedLock.acquireUninterruptibly();
        notifyCount++;
        notifiedLock.release();

        lock.unlockAll();
    }
}
