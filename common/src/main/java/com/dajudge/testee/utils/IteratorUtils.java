package com.dajudge.testee.utils;

import java.util.Iterator;
import java.util.List;
import java.util.function.Function;

/**
 * Utils for iterators.
 *
 * @author Alex Stockinger, IT-Stockinger
 */
public class IteratorUtils {
    /**
     * Creates a composite iterator.
     *
     * @param items      the list items from which iterators can be retrieved.
     * @param toIterator the function to retrieve an iterator from a list item.
     * @param <T>        the type of the list items.
     * @param <S>        the iterated type.
     * @return a composite iterator.
     */
    public static <T, S> Iterator<S> composite(
            final List<T> items,
            final Function<T, Iterator<S>> toIterator
    ) {
        final MutableContainer<Iterator<S>> container = new MutableContainer<>();
        return new Iterator<S>() {
            @Override
            public boolean hasNext() {
                if (container.getObject() == null || !container.getObject().hasNext()) {
                    if (items.isEmpty()) {
                        return false;
                    }
                    container.setObject(toIterator.apply(items.remove(0)));
                } else {
                    return true;
                }
                return hasNext();
            }

            @Override
            public S next() {
                if (!hasNext()) {
                    return null;
                }
                return container.getObject().next();
            }
        };
    }
}
