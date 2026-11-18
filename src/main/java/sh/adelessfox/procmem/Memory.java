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
    final long address;

    public Memory(long address) {
        Objects.checkIndex(address, Long.MAX_VALUE);
        this.address = address;
    }

    /**
     * Returns the memory layout of this memory block.
     *
     * @return the memory layout
     */
    public abstract MemoryLayout layout();

    public final long address() {
        return address;
    }

    /**
     * Reads bytes from the memory at the specified address into the destination buffer.
     * <p>
     * The destination buffer's position will be advanced by the number of bytes read.
     *
     * @param address the starting address to read from
     * @param dst     the destination buffer to write the bytes into
     */
    public abstract void read(long address, ByteBuffer dst);

    /**
     * Reads a single signed byte.
     *
     * @param offset the address to read from
     * @return the value read from memory
     */
    public byte readByte(long offset) {
        var buffer = ByteBuffer.allocate(Byte.BYTES);
        read(offset, buffer);
        return buffer.flip().get();
    }

    /**
     * Reads a 16-bit signed integer.
     *
     * @param offset the address to read from
     * @return the value read from memory
     */
    public short readShort(long offset) {
        var buffer = ByteBuffer.allocate(Short.BYTES).order(layout().byteOrder());
        read(offset, buffer);
        return buffer.flip().getShort();
    }

    /**
     * Reads a 32-bit signed integer.
     *
     * @param offset the address to read from
     * @return the value read from memory
     */
    public int readInt(long offset) {
        var buffer = ByteBuffer.allocate(Integer.BYTES).order(layout().byteOrder());
        read(offset, buffer);
        return buffer.flip().getInt();
    }

    /**
     * Reads a 32-bit signed integer.
     *
     * @param offset the address to read from
     * @return the value read from memory
     */
    public long readLong(long offset) {
        var buffer = ByteBuffer.allocate(Long.BYTES).order(layout().byteOrder());
        read(offset, buffer);
        return buffer.flip().getLong();
    }

    /**
     * Reads a 32-bit IEEE-754 single-precision float.
     *
     * @param offset the address to read from
     * @return the value read from memory
     */
    public final float readFloat(long offset) {
        return Float.intBitsToFloat(readInt(offset));
    }

    /**
     * Reads a 64-bit IEEE-754 double-precision float.
     *
     * @param offset the address to read from
     * @return the value read from memory
     */
    public final double readDouble(long offset) {
        return Double.longBitsToDouble(readLong(offset));
    }

    /**
     * Reads a pointer-sized integer.
     *
     * @param offset the address to read from
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
     * @param offset the address to read from
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
     * @param offset the starting address to write to
     * @param src     the source buffer containing the bytes to write
     */
    public abstract void write(long offset, ByteBuffer src);

    /**
     * Writes a single signed byte.
     *
     * @param offset the address to write to
     * @param value   the value to write to memory
     */
    public void writeByte(long offset, byte value) {
        var buffer = ByteBuffer.allocate(Byte.BYTES);
        buffer.put(value).flip();
        write(offset, buffer);
    }

    /**
     * Writes a 16-bit signed integer.
     *
     * @param offset the address to write to
     * @param value   the value to write to memory
     */
    public void writeShort(long offset, short value) {
        var buffer = ByteBuffer.allocate(Short.BYTES).order(layout().byteOrder());
        buffer.putShort(value).flip();
        write(offset, buffer);
    }

    /**
     * Writes a 32-bit signed integer.
     *
     * @param offset the address to write to
     * @param value   the value to write to memory
     */
    public void writeInt(long offset, int value) {
        var buffer = ByteBuffer.allocate(Integer.BYTES).order(layout().byteOrder());
        buffer.putInt(value).flip();
        write(offset, buffer);
    }

    /**
     * Writes a 64-bit signed integer.
     *
     * @param offset the address to write to
     * @param value   the value to write to memory
     */
    public void writeLong(long offset, long value) {
        var buffer = ByteBuffer.allocate(Long.BYTES).order(layout().byteOrder());
        buffer.putLong(value).flip();
        write(offset, buffer);
    }

    /**
     * Writes a 32-bit IEEE-754 single-precision float.
     *
     * @param offset the address to write to
     * @param value   the value to write to memory
     */
    public final void writeFloat(long offset, float value) {
        writeInt(offset, Float.floatToIntBits(value));
    }

    /**
     * Writes a 64-bit IEEE-754 double-precision float.
     *
     * @param offset the address to write to
     * @param value   the value to write to memory
     */
    public final void writeDouble(long offset, double value) {
        writeLong(offset, Double.doubleToLongBits(value));
    }

    /**
     * Writes a pointer-sized integer.
     *
     * @param offset the address to write to
     * @param value   the value to write to memory
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
     * @param offset the address to write to
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
     * @param offset the address to create a pointer for
     * @return a {@link Pointer} representing the specified address
     */
    public Pointer pointer(long offset) {
        return new Pointer(this, offset);
    }

    /**
     * Creates a new {@link Memory} instance that represents a slice of this memory block.
     *
     * @param layout the memory layout of the slice
     * @return a new {@link Memory} instance representing the specified slice
     */
    public Memory slice(MemoryLayout layout) {
        return slice(0L, layout);
    }

    /**
     * Creates a new {@link Memory} instance that represents a slice of this memory block.
     *
     * @param offset the starting address of the slice, in bytes
     * @return a new {@link Memory} instance representing the specified slice
     */
    public Memory slice(long offset) {
        return slice(offset, layout());
    }

    /**
     * Creates a new {@link Memory} instance that represents a slice of this memory block.
     *
     * @param offset the starting address of the slice, in bytes
     * @param layout the memory layout of the slice
     * @return a new {@link Memory} instance representing the specified slice
     */
    public Memory slice(long offset, MemoryLayout layout) {
        return slice(offset, Long.MAX_VALUE, layout);
    }

    /**
     * Creates a new {@link Memory} instance that represents a slice of this memory block.
     *
     * @param offset the starting address of the slice, in bytes
     * @param size   the size of the slice, in bytes
     * @return a new {@link Memory} instance representing the specified slice
     */
    public Memory slice(long offset, long size) {
        return slice(offset, size, layout());
    }

    /**
     * Creates a new {@link Memory} instance that represents a slice of this memory block.
     *
     * @param offset the starting address of the slice, in bytes
     * @param size   the size of the slice, in bytes
     * @param layout the memory layout of the slice
     * @return a new {@link Memory} instance representing the specified slice
     */
    public Memory slice(long offset, long size, MemoryLayout layout) {
        Objects.checkFromIndexSize(offset, size, Long.MAX_VALUE);
        return new Memory(address() + offset) {
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

    @Override
    public String toString() {
        return "Memory[address=%#x, layout=%s]".formatted(address(), layout());
    }
}
