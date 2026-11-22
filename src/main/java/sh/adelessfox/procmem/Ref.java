// SPDX-License-Identifier: MIT
// SPDX-FileCopyrightText: 2026 ShadelessFox

package sh.adelessfox.procmem;

import org.jetbrains.annotations.Debug;

/**
 * A reference to a value of type {@code T} in memory.
 *
 * @param type    the type of the value
 * @param pointer the pointer to the value in memory
 * @param <T>     the type of the value
 */
@Debug.Renderer(childrenArray = "this.read()", hasChildren = "this.pointer().address() != 0L")
public record Ref<T>(Type<T> type, Pointer pointer) {
    /**
     * Reads the value of type {@code T} from memory at the pointer's address.
     *
     * @return the value read from memory
     */
    public T read() {
        return type.read(pointer);
    }

    public Slice<T> slice(int length) {
        if (!(type instanceof Type.Sized<T> sized)) {
            throw new IllegalArgumentException("Type must be sized to create a slice");
        }
        return sized.slice(pointer, length);
    }

    public <R> Ref<R> map(Type<R> newType) {
        return new Ref<>(newType, pointer);
    }

    @Override
    public String toString() {
        return type + " @ " + pointer;
    }
}
