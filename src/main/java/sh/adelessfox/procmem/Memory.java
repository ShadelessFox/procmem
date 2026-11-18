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
public interface Memory extends AutoCloseable {
    /**
     * Returns the memory layout of this memory block.
     *
     * @return the memory layout
     */
    MemoryLayout layout();

    /**
     * Reads bytes from the memory at the specified address into the destination buffer.
     * <p>
     * The destination buffer's position will be advanced by the number of bytes read.
     *
     * @param address the starting address to read from
     * @param dst     the destination buffer to write the bytes into
     */
    void read(long address, ByteBuffer dst);

    /**
     * Reads a single signed byte.
     *
     * @param address the address to read from
     * @return the value read from memory
     */
    default byte readByte(long address) {
        var buffer = ByteBuffer.allocate(Byte.BYTES);
        read(address, buffer);
        return buffer.flip().get();
    }

    /**
     * Reads a 16-bit signed integer.
     *
     * @param address the address to read from
     * @return the value read from memory
     */
    default short readShort(long address) {
        var buffer = ByteBuffer.allocate(Short.BYTES).order(layout().byteOrder());
        read(address, buffer);
        return buffer.flip().getShort();
    }

    /**
     * Reads a 32-bit signed integer.
     *
     * @param address the address to read from
     * @return the value read from memory
     */
    default int readInt(long address) {
        var buffer = ByteBuffer.allocate(Integer.BYTES).order(layout().byteOrder());
        read(address, buffer);
        return buffer.flip().getInt();
    }

    /**
     * Reads a 32-bit signed integer.
     *
     * @param address the address to read from
     * @return the value read from memory
     */
    default long readLong(long address) {
        var buffer = ByteBuffer.allocate(Long.BYTES).order(layout().byteOrder());
        read(address, buffer);
        return buffer.flip().getLong();
    }

    /**
     * Reads a 32-bit IEEE-754 single-precision float.
     *
     * @param address the address to read from
     * @return the value read from memory
     */
    default float readFloat(long address) {
        return Float.intBitsToFloat(readInt(address));
    }

    /**
     * Reads a 64-bit IEEE-754 double-precision float.
     *
     * @param address the address to read from
     * @return the value read from memory
     */
    default double readDouble(long address) {
        return Double.longBitsToDouble(readLong(address));
    }

    /**
     * Reads a pointer-sized integer.
     *
     * @param address the address to read from
     * @return the value read from memory
     * @see MemoryLayout#pointerSize()
     */
    default long readAddress(long address) {
        return switch (layout().pointerSize()) {
            case 4 -> Integer.toUnsignedLong(readInt(address));
            case 8 -> readLong(address);
            default -> throw new AssertionError("Unsupported pointer size: " + layout().pointerSize());
        };
    }

    /**
     * Reads a pointer from memory.
     *
     * @param address the address to read from
     * @return a {@link Pointer} representing the value read from memory
     */
    default Pointer readPointer(long address) {
        return pointer(readAddress(address));
    }

    /**
     * Writes bytes from the source buffer into the memory at the specified address.
     * <p>
     * The source buffer's position will be advanced by the number of bytes written.
     *
     * @param address the starting address to write to
     * @param src     the source buffer containing the bytes to write
     */
    void write(long address, ByteBuffer src);

    /**
     * Writes a single signed byte.
     *
     * @param address the address to write to
     * @param value   the value to write to memory
     */
    default void writeByte(long address, byte value) {
        var buffer = ByteBuffer.allocate(Byte.BYTES);
        buffer.put(value).flip();
        write(address, buffer);
    }

    /**
     * Writes a 16-bit signed integer.
     *
     * @param address the address to write to
     * @param value   the value to write to memory
     */
    default void writeShort(long address, short value) {
        var buffer = ByteBuffer.allocate(Short.BYTES).order(layout().byteOrder());
        buffer.putShort(value).flip();
        write(address, buffer);
    }

    /**
     * Writes a 32-bit signed integer.
     *
     * @param address the address to write to
     * @param value   the value to write to memory
     */
    default void writeInt(long address, int value) {
        var buffer = ByteBuffer.allocate(Integer.BYTES).order(layout().byteOrder());
        buffer.putInt(value).flip();
        write(address, buffer);
    }

    /**
     * Writes a 64-bit signed integer.
     *
     * @param address the address to write to
     * @param value   the value to write to memory
     */
    default void writeLong(long address, long value) {
        var buffer = ByteBuffer.allocate(Long.BYTES).order(layout().byteOrder());
        buffer.putLong(value).flip();
        write(address, buffer);
    }

    /**
     * Writes a 32-bit IEEE-754 single-precision float.
     *
     * @param address the address to write to
     * @param value   the value to write to memory
     */
    default void writeFloat(long address, float value) {
        writeInt(address, Float.floatToIntBits(value));
    }

    /**
     * Writes a 64-bit IEEE-754 double-precision float.
     *
     * @param address the address to write to
     * @param value   the value to write to memory
     */
    default void writeDouble(long address, double value) {
        writeLong(address, Double.doubleToLongBits(value));
    }

    /**
     * Writes a pointer-sized integer.
     *
     * @param address the address to write to
     * @param value   the value to write to memory
     * @see MemoryLayout#pointerSize()
     */
    default void writeAddress(long address, long value) {
        switch (layout().pointerSize()) {
            case 4 -> writeInt(address, (int) value);
            case 8 -> writeLong(address, value);
            default -> throw new AssertionError("Unsupported pointer size: " + layout().pointerSize());
        }
    }

    /**
     * Writes a pointer to memory.
     *
     * @param address the address to write to
     * @param pointer the pointer to write to memory
     */
    default void writePointer(long address, Pointer pointer) {
        writeAddress(address, pointer.address());
    }

    @Override
    void close();

    /**
     * Creates a {@link Pointer} representing the specified address in this memory block.
     *
     * @param address the address to create a pointer for
     * @return a {@link Pointer} representing the specified address
     */
    default Pointer pointer(long address) {
        return new Pointer(this, address);
    }

    /**
     * Creates a new {@link Memory} instance that represents a slice of this memory block.
     *
     * @param layout the memory layout of the slice
     * @return a new {@link Memory} instance representing the specified slice
     */
    default Memory slice(MemoryLayout layout) {
        return slice(0L, layout);
    }

    /**
     * Creates a new {@link Memory} instance that represents a slice of this memory block.
     *
     * @param offset the starting offset of the slice, in bytes
     * @return a new {@link Memory} instance representing the specified slice
     */
    default Memory slice(long offset) {
        return slice(offset, layout());
    }

    /**
     * Creates a new {@link Memory} instance that represents a slice of this memory block.
     *
     * @param offset the starting offset of the slice, in bytes
     * @param layout the memory layout of the slice
     * @return a new {@link Memory} instance representing the specified slice
     */
    default Memory slice(long offset, MemoryLayout layout) {
        return slice(offset, Long.MAX_VALUE, layout);
    }

    /**
     * Creates a new {@link Memory} instance that represents a slice of this memory block.
     *
     * @param offset the starting offset of the slice, in bytes
     * @param size   the size of the slice, in bytes
     * @return a new {@link Memory} instance representing the specified slice
     */
    default Memory slice(long offset, long size) {
        return slice(offset, size, layout());
    }

    /**
     * Creates a new {@link Memory} instance that represents a slice of this memory block.
     *
     * @param offset the starting offset of the slice, in bytes
     * @param size   the size of the slice, in bytes
     * @param layout the memory layout of the slice
     * @return a new {@link Memory} instance representing the specified slice
     */
    default Memory slice(long offset, long size, MemoryLayout layout) {
        return new Memory() {
            @Override
            public MemoryLayout layout() {
                return layout;
            }

            @Override
            public void read(long address, ByteBuffer dst) {
                Objects.checkIndex(address, size);
                Memory.this.read(offset + address, dst);
            }

            @Override
            public void write(long address, ByteBuffer src) {
                Objects.checkIndex(address, size);
                Memory.this.write(offset + address, src);
            }

            @Override
            public void close() {
                // do nothing
            }
        };
    }
}
