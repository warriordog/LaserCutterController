package net.acomputerdog.lccontroller.util;

import java.util.Enumeration;
import java.util.Iterator;

public class IterableEnumeration<T> implements Enumeration<T>, Iterable<T> {
    private final Enumeration<T> enumeration;

    public IterableEnumeration(Enumeration<T> enumeration) {
        this.enumeration = enumeration;
        if (enumeration == null) {
            throw new IllegalArgumentException("Enumeration cannot be null!");
        }
    }

    @Override
    public Iterator<T> iterator() {
        return new Iterator<T>() {
            @Override
            public boolean hasNext() {
                return hasMoreElements();
            }

            @Override
            public T next() {
                return nextElement();
            }
        };
    }

    @Override
    public boolean hasMoreElements() {
        return enumeration.hasMoreElements();
    }

    @Override
    public T nextElement() {
        return enumeration.nextElement();
    }
}
