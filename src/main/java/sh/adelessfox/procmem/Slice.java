// SPDX-License-Identifier: MIT
// SPDX-FileCopyrightText: 2026 ShadelessFox

package sh.adelessfox.procmem;

import org.jetbrains.annotations.Debug;

import java.util.Iterator;
import java.util.Objects;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * Represents a slice of memory containing a sequence of typed elements of type {@code T}.
 *
 * @param type    the type of the elements in the slice
 * @param pointer the pointer to the start of the slice in memory
 * @param length  the number of elements in the slice
 * @param <T>     the type of the elements in the slice
 */
@Debug.Renderer(childrenArray = "this.stream().toArray()", hasChildren = "this.length > 0")
public record Slice<T>(Type.Sized<T> type, Pointer pointer, int length) implements Iterable<T> {
    /**
     * Reads the element at the specified index in the slice.
     *
     * @param index the index of the element to read
     * @return the element at the specified index
     */
    public T read(int index) {
        Objects.checkIndex(index, length);
        var stride = stride();
        return type.read(pointer.add(Math.multiplyExact(index, stride)));
    }

    /**
     * Returns the total size of the slice in bytes.
     *
     * @return the total size of the slice in bytes
     */
    public long size() {
        return Math.multiplyExact(length, stride());
    }

    /**
     * Returns the stride of the slice in bytes, which is the size of each element in the slice.
     *
     * @return the stride of the slice in bytes
     */
    public long stride() {
        return type.layout(pointer.memory().layout()).size();
    }

    /**
     * Returns a stream of the elements in the slice.
     *
     * @return a stream of the elements in the slice
     */
    public Stream<T> stream() {
        return IntStream.range(0, length).mapToObj(this::read);
    }

    @Override
    public Iterator<T> iterator() {
        return stream().iterator();
    }

    @Override
    public String toString() {
        return type + "[" + length + "] @ " + pointer;
    }
}
