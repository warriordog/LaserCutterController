package net.acomputerdog.lccontroller.util;

public class Lock {
    private final Object lock;

    public Lock(Object lock) {
        this.lock = lock;

        if (lock == null) {
            throw new IllegalArgumentException("Lock cannot be null!");
        }
    }

    public void lock(long timeout) throws InterruptedException {
        synchronized (lock) {
            if (timeout > 0) {
                lock.wait(timeout);
            } else {
                lock.wait();
            }
        }
    }

    public void lock() throws InterruptedException {
        lock(-1);
    }

    public void unlock() {
        synchronized (lock) {
            lock.notify();
        }
    }

    public void unlockAll() {
        synchronized (lock) {
            lock.notifyAll();
        }
    }
}
