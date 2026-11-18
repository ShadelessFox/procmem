// SPDX-License-Identifier: MIT
// SPDX-FileCopyrightText: 2026 ShadelessFox

package sh.adelessfox.procmem;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * Represents a pointer to a memory address in a block of memory.
 *
 * @param memory  the memory object that this pointer points to
 * @param address the address in memory, in bytes
 */
public record Pointer(Memory memory, long address) implements Comparable<Pointer> {
    /**
     * Creates a reference to {@code type} at this pointer's address.
     *
     * @param type the type to create a reference to
     * @return a reference to {@code type} at this pointer's address
     */
    public <T> Ref<T> ref(Type<T> type) {
        return type.at(this);
    }

    /**
     * Reads {@code size} bytes from this pointer's address.
     *
     * @param size the number of bytes to read
     * @return a byte array containing the bytes read from memory
     */
    public byte[] read(int size) {
        var array = new byte[size];
        memory.read(address, ByteBuffer.wrap(array));
        return array;
    }

    /**
     * Reads a single signed byte from this pointer's address.
     *
     * @return the value read from memory
     */
    public byte readByte() {
        return memory.readByte(address);
    }

    /**
     * Reads a single unsigned byte from this pointer's address.
     *
     * @return the value read from memory
     */
    public short readShort() {
        return memory.readShort(address);
    }

    /**
     * Reads a single signed short from this pointer's address.
     *
     * @return the value read from memory
     */
    public int readInt() {
        return memory.readInt(address);
    }

    /**
     * Reads a single signed long from this pointer's address.
     *
     * @return the value read from memory
     */
    public long readLong() {
        return memory.readLong(address);
    }

    /**
     * Reads a single float from this pointer's address.
     *
     * @return the value read from memory
     */
    public float readFloat() {
        return memory.readFloat(address);
    }

    /**
     * Reads a single double from this pointer's address.
     *
     * @return the value read from memory
     */
    public double readDouble() {
        return memory.readDouble(address);
    }

    public String readString(Charset charset, int length) {
        var buffer = ByteBuffer.allocate(length);
        memory.read(address, buffer);
        return charset.decode(buffer.flip()).toString();
    }

    public String readCString(int limit) {
        var buffer = ByteBuffer.allocate(limit);
        memory.read(address, buffer);
        for (int i = 0; i < buffer.position(); i++) {
            if (buffer.get(i) == 0) {
                return StandardCharsets.ISO_8859_1.decode(buffer.slice(0, i)).toString();
            }
        }
        throw new IllegalStateException("String out of bounds");
    }

    public String readCString() {
        return readCString(256);
    }

    public void fill(byte value, int size) {
        var array = new byte[size];
        Arrays.fill(array, value);
        var buffer = ByteBuffer.wrap(array);
        memory.write(address, buffer);
    }

    public void clear(int size) {
        fill((byte) 0, size);
    }

    /**
     * Dereferences this pointer, returning a new pointer to the address stored at this pointer's address.
     *
     * @return a new pointer to the address stored at this pointer's address
     */
    public Pointer deref() {
        return memory.readPointer(address);
    }

    /**
     * Returns a new pointer to the address at {@code byteOffset} bytes from this pointer's address.
     *
     * @param byteOffset the number of bytes to offset from this pointer's address
     * @return a new pointer to the address at {@code byteOffset} bytes from this pointer's address
     */
    public Pointer add(long byteOffset) {
        return new Pointer(memory, address + byteOffset);
    }

    public boolean isNull() {
        return address == 0;
    }

    public boolean isNotNull() {
        return !isNull();
    }

    @Override
    public String toString() {
        return "%#x".formatted(memory.address() + address);
    }

    @Override
    public int compareTo(Pointer o) {
        return Long.compareUnsigned(address, o.address);
    }
}
