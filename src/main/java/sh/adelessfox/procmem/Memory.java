// SPDX-License-Identifier: MIT
// SPDX-FileCopyrightText: 2026 ShadelessFox

package sh.adelessfox.procmem;

import java.nio.ByteBuffer;
import java.util.Objects;

/**
 * Represents a block of memory that can be read from and written to.
 * <p>
 * Operations that take multibyte values will use the byte order specified by the {@link #layout() memory layout}.
 */
public abstract class Memory implements AutoCloseable {
    final long base;

    public Memory(long base) {
        Objects.checkIndex(base, Long.MAX_VALUE);
        this.base = base;
    }

    /**
     * Returns the memory layout of this memory block.
     *
     * @return the memory layout
     */
    public abstract MemoryLayout layout();

    /**
     * Returns the base address of this memory block.
     *
     * @return the base address, in bytes
     */
    public final long base() {
        return base;
    }

    /**
     * Reads bytes from the memory at the specified address into the destination buffer.
     * <p>
     * The destination buffer's position will be advanced by the number of bytes read.
     *
     * @param offset the offset relative to the {@link base() base} to read from
     * @param dst    the destination buffer to write the bytes into
     */
    public abstract void read(long offset, ByteBuffer dst);

    /**
     * Reads a single signed byte.
     *
     * @param offset the offset relative to the {@link base() base} to read from
     * @return the value read from memory
     */
    public abstract byte readByte(long offset);

    /**
     * Reads a 16-bit signed integer.
     *
     * @param offset the offset relative to the {@link base() base} to read from
     * @return the value read from memory
     */
    public abstract short readShort(long offset);

    /**
     * Reads a 32-bit signed integer.
     *
     * @param offset the offset relative to the {@link base() base} to read from
     * @return the value read from memory
     */
    public abstract int readInt(long offset);

    /**
     * Reads a 32-bit signed integer.
     *
     * @param offset the offset relative to the {@link base() base} to read from
     * @return the value read from memory
     */
    public abstract long readLong(long offset);

    /**
     * Reads a 32-bit IEEE-754 single-precision float.
     *
     * @param offset the offset relative to the {@link base() base} to read from
     * @return the value read from memory
     */
    public final float readFloat(long offset) {
        return Float.intBitsToFloat(readInt(offset));
    }

    /**
     * Reads a 64-bit IEEE-754 double-precision float.
     *
     * @param offset the offset relative to the {@link base() base} to read from
     * @return the value read from memory
     */
    public final double readDouble(long offset) {
        return Double.longBitsToDouble(readLong(offset));
    }

    /**
     * Reads a pointer-sized integer.
     *
     * @param offset the offset relative to the {@link base() base} to read from
     * @return the value read from memory
     * @see MemoryLayout#pointerSize()
     */
    public final long readAddress(long offset) {
        return switch (layout().pointerSize()) {
            case 1 -> Byte.toUnsignedLong(readByte(offset));
            case 2 -> Short.toUnsignedLong(readShort(offset));
            case 4 -> Integer.toUnsignedLong(readInt(offset));
            case 8 -> readLong(offset);
            default -> throw new AssertionError("Unsupported pointer size: " + layout().pointerSize());
        };
    }

    /**
     * Reads a pointer from memory.
     *
     * @param offset the offset relative to the {@link base() base} to read from
     * @return a {@link Pointer} representing the value read from memory
     */
    public final Pointer readPointer(long offset) {
        return pointer(readAddress(offset));
    }

    /**
     * Writes bytes from the source buffer into the memory at the specified address.
     * <p>
     * The source buffer's position will be advanced by the number of bytes written.
     *
     * @param offset the offset relative to the {@link base() base} to write to
     * @param src    the source buffer containing the bytes to write
     */
    public abstract void write(long offset, ByteBuffer src);

    /**
     * Writes a single signed byte.
     *
     * @param offset the offset relative to the {@link base() base} to write to
     * @param value  the value to write to memory
     */
    public abstract void writeByte(long offset, byte value);

    /**
     * Writes a 16-bit signed integer.
     *
     * @param offset the offset relative to the {@link base() base} to write to
     * @param value  the value to write to memory
     */
    public abstract void writeShort(long offset, short value);

    /**
     * Writes a 32-bit signed integer.
     *
     * @param offset the offset relative to the {@link base() base} to write to
     * @param value  the value to write to memory
     */
    public abstract void writeInt(long offset, int value);

    /**
     * Writes a 64-bit signed integer.
     *
     * @param offset the offset relative to the {@link base() base} to write to
     * @param value  the value to write to memory
     */
    public abstract void writeLong(long offset, long value);

    /**
     * Writes a 32-bit IEEE-754 single-precision float.
     *
     * @param offset the offset relative to the {@link base() base} to write to
     * @param value  the value to write to memory
     */
    public final void writeFloat(long offset, float value) {
        writeInt(offset, Float.floatToIntBits(value));
    }

    /**
     * Writes a 64-bit IEEE-754 double-precision float.
     *
     * @param offset the offset relative to the {@link base() base} to write to
     * @param value  the value to write to memory
     */
    public final void writeDouble(long offset, double value) {
        writeLong(offset, Double.doubleToLongBits(value));
    }

    /**
     * Writes a pointer-sized integer.
     *
     * @param offset the offset relative to the {@link base() base} to write to
     * @param value  the value to write to memory
     * @see MemoryLayout#pointerSize()
     */
    public final void writeAddress(long offset, long value) {
        switch (layout().pointerSize()) {
            case 1 -> writeByte(offset, (byte) value);
            case 2 -> writeShort(offset, (short) value);
            case 4 -> writeInt(offset, (int) value);
            case 8 -> writeLong(offset, value);
            default -> throw new AssertionError("Unsupported pointer size: " + layout().pointerSize());
        }
    }

    /**
     * Writes a pointer to memory.
     *
     * @param offset  the offset relative to the {@link base() base} to write to
     * @param pointer the pointer to write to memory
     */
    public final void writePointer(long offset, Pointer pointer) {
        writeAddress(offset, pointer.address());
    }

    @Override
    public abstract void close();

    /**
     * Creates a {@link Pointer} representing the specified address in this memory block.
     *
     * @param offset the offset relative to the {@link base() base} to create a pointer for
     * @return a {@link Pointer} representing the specified address
     */
    public Pointer pointer(long offset) {
        return new Pointer(this, offset);
    }

    @Override
    public String toString() {
        return "Memory[base=%#x, layout=%s]".formatted(base(), layout());
    }
}
