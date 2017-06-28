package net.acomputerdog.lccontroller.util;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.Semaphore;

/*
  Pipes data from ONE thread to ONE other thread
 */
public class MessagePipe<T> implements Iterable<T> {
    private final Semaphore lock = new Semaphore(1);

    private Queue<T> messages;

    public MessagePipe() {
        messages = new LinkedList<>();
    }

    public void send(T obj) {
        lock.acquireUninterruptibly();
        messages.add(obj);
        lock.release();
    }

    public boolean hasMessage() {
        lock.acquireUninterruptibly();
        try {
            return !messages.isEmpty();
        } finally {
            lock.release();
        }
    }

    public T nextMessage() {
        lock.acquireUninterruptibly();
        try {
            // returns null if empty
            return messages.poll();
        } finally {
            lock.release();
        }
    }

    @Override
    public Iterator<T> iterator() {
        return new Iterator<T>() {
            @Override
            public boolean hasNext() {
                return hasMessage();
            }

            @Override
            public T next() {
                return nextMessage();
            }
        };
    }
}
