/*
 * Copyright (C) 2017 Alex Stockinger
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package fi.testee.utils;

import java.util.Iterator;
import java.util.List;
import java.util.function.Function;

/**
 * Utils for {@link Iterator iterators}.
 *
 * @author Alex Stockinger, IT-Stockinger
 */
public final class IteratorUtils {
    private IteratorUtils() {
    }

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
