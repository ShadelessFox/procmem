// SPDX-License-Identifier: MIT
// SPDX-FileCopyrightText: 2026 ShadelessFox

package sh.adelessfox.procmem;

import sh.adelessfox.procmem.process.Process;

import java.nio.ByteBuffer;
import java.nio.ReadOnlyBufferException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

/**
 * Represents a bounded, non-owning view of process memory. All addresses are absolute.
 * <p>
 * Operations that take multibyte values will use the byte order specified by the {@link #layout() memory layout}.
 */
public abstract class Memory {
    final Process process;
    final MemoryLayout layout;
    final long base;
    final long size;
    private final ByteBuffer scratch;

    public Memory(Process process, MemoryLayout layout, long base, long size) {
        Objects.checkFromIndexSize(base, size, Long.MAX_VALUE);
        this.process = process;
        this.layout = layout;
        this.base = base;
        this.size = size;
        this.scratch = ByteBuffer.allocate(Long.BYTES).order(layout.byteOrder());
    }

    /**
     * Returns the memory layout of this memory block.
     *
     * @return the memory layout
     */
    public final MemoryLayout layout() {
        return layout;
    }

    /**
     * Returns the base address of this memory block.
     *
     * @return the base address, in bytes
     */
    public final long base() {
        return base;
    }

    /**
     * Returns the size of this memory block.
     *
     * @return the size, in bytes
     */
    public final long size() {
        return size;
    }

    // region Readers

    /**
     * Reads bytes from the memory at the specified address into the destination buffer.
     * <p>
     * The destination buffer's position will be advanced by the number of bytes read.
     * The entire remaining range must fit in this view before any I/O is attempted.
     *
     * @param address the address to read from
     * @param dst     the destination buffer to write the bytes into
     */
    public final void read(long address, ByteBuffer dst) {
        checkRange(address, dst.remaining());
        if (dst.isReadOnly()) {
            throw new ReadOnlyBufferException();
        }
        if (dst.hasRemaining()) {
            readImpl(address, dst);
        }
    }

    /**
     * Reads a nonempty, already bounds-checked range at an absolute address.
     */
    protected abstract void readImpl(long address, ByteBuffer dst);

    /**
     * Reads a single signed byte.
     *
     * @param address the address to read from
     * @return the value read from memory
     */
    public final byte readByte(long address) {
        return readBuffer(address, Byte.BYTES).get();
    }

    /**
     * Reads a 16-bit signed integer.
     *
     * @param address the address to read from
     * @return the value read from memory
     */
    public final short readShort(long address) {
        return readBuffer(address, Short.BYTES).getShort();
    }

    /**
     * Reads a 32-bit signed integer.
     *
     * @param address the address to read from
     * @return the value read from memory
     */
    public final int readInt(long address) {
        return readBuffer(address, Integer.BYTES).getInt();
    }

    /**
     * Reads a 64-bit signed integer.
     *
     * @param address the address to read from
     * @return the value read from memory
     */
    public final long readLong(long address) {
        return readBuffer(address, Long.BYTES).getLong();
    }

    private ByteBuffer readBuffer(long address, int size) {
        scratch.clear().limit(size);
        read(address, scratch);
        return scratch.flip();
    }

    /**
     * Reads a 32-bit IEEE-754 single-precision float.
     *
     * @param address the address to read from
     * @return the value read from memory
     */
    public final float readFloat(long address) {
        return Float.intBitsToFloat(readInt(address));
    }

    /**
     * Reads a 64-bit IEEE-754 double-precision float.
     *
     * @param address the address to read from
     * @return the value read from memory
     */
    public final double readDouble(long address) {
        return Double.longBitsToDouble(readLong(address));
    }

    /**
     * Reads a pointer-sized integer.
     *
     * @param address the address to read from
     * @return the value read from memory
     * @see MemoryLayout#pointerSize()
     */
    public final long readAddress(long address) {
        return switch (layout().pointerSize()) {
            case 1 -> Byte.toUnsignedLong(readByte(address));
            case 2 -> Short.toUnsignedLong(readShort(address));
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
    public final Pointer readPointer(long address) {
        return process.pointer(readAddress(address));
    }

    /**
     * Reads a fixed-size string, truncated at the first NUL if present.
     *
     * @param address the absolute address of the field
     * @param charset the charset used to decode the field
     * @param length  the field size in bytes, including any terminator and padding
     */
    public final String readString(long address, Charset charset, int length) {
        Objects.requireNonNull(charset);
        if (length < 0) {
            throw new IllegalArgumentException("length must be non-negative");
        }
        checkRange(address, length);
        var buffer = ByteBuffer.allocate(length);
        read(address, buffer);
        if (buffer.hasRemaining()) {
            throw new IllegalStateException("Incomplete string read at %#x".formatted(address));
        }
        buffer.flip();
        int terminator = findTerminator(buffer, terminatorWidth(charset));
        if (terminator >= 0) {
            buffer.limit(terminator);
        }
        return charset.decode(buffer).toString();
    }

    /**
     * Reads a NUL-terminated ISO-8859-1 string with a 256-byte limit, including the terminator.
     */
    public final String readCString(long address) {
        return readCString(address, 256);
    }

    /**
     * Reads a NUL-terminated ISO-8859-1 string.
     *
     * @param address   the absolute address of the string
     * @param maxLength the byte limit, including the NUL terminator
     * @throws IllegalArgumentException if {@code maxLength} is not positive
     * @throws IndexOutOfBoundsException if the starting address is outside this view
     * @throws IllegalStateException if no NUL is found before the limit or view boundary
     */
    public final String readCString(long address, int maxLength) {
        if (maxLength <= 0) {
            throw new IllegalArgumentException("maxLength must be positive");
        }
        checkRange(address, 1);
        int length = (int) Math.min(maxLength, size - (address - base));
        var value = readString(address, StandardCharsets.ISO_8859_1, length);
        if (value.length() == length) {
            // ISO-8859-1 maps each byte to one character, so a shorter result means a NUL was found
            throw new IllegalStateException("No NUL terminator within %d bytes at %#x".formatted(length, address));
        }
        return value;
    }

    private static int terminatorWidth(Charset charset) {
        return (switch (charset.name()) {
            case "UTF-16", "UTF-16BE", "UTF-16LE" -> 2;
            case "UTF-32", "UTF-32BE", "UTF-32LE" -> 4;
            default -> 1;
        });
    }

    private static int findTerminator(ByteBuffer buffer, int width) {
        for (int offset = 0; offset <= buffer.limit() - width; offset += width) {
            int value = 0;
            for (int i = 0; i < width; i++) {
                value |= buffer.get(offset + i);
            }
            if (value == 0) {
                return offset;
            }
        }
        return -1;
    }
    // endregion

    // region Writers

    /**
     * Writes bytes from the source buffer into the memory at the specified address.
     * <p>
     * The source buffer's position will be advanced by the number of bytes written.
     * The entire remaining range must fit in this view before any I/O is attempted.
     *
     * @param address the address to write to
     * @param src     the source buffer containing the bytes to write
     */
    public final void write(long address, ByteBuffer src) {
        checkRange(address, src.remaining());
        if (src.hasRemaining()) {
            writeImpl(address, src);
        }
    }

    /**
     * Writes a nonempty, already bounds-checked range at an absolute address.
     */
    protected abstract void writeImpl(long address, ByteBuffer src);

    /**
     * Writes a single signed byte.
     *
     * @param address the address to write to
     * @param value   the value to write to memory
     */
    public final void writeByte(long address, byte value) {
        write(address, scratch.clear().put(value).flip());
    }

    /**
     * Writes a 16-bit signed integer.
     *
     * @param address the address to write to
     * @param value   the value to write to memory
     */
    public final void writeShort(long address, short value) {
        write(address, scratch.clear().putShort(value).flip());
    }

    /**
     * Writes a 32-bit signed integer.
     *
     * @param address the address to write to
     * @param value   the value to write to memory
     */
    public final void writeInt(long address, int value) {
        write(address, scratch.clear().putInt(value).flip());
    }

    /**
     * Writes a 64-bit signed integer.
     *
     * @param address the address to write to
     * @param value   the value to write to memory
     */
    public final void writeLong(long address, long value) {
        write(address, scratch.clear().putLong(value).flip());
    }

    /**
     * Writes a 32-bit IEEE-754 single-precision float.
     *
     * @param address the address to write to
     * @param value   the value to write to memory
     */
    public final void writeFloat(long address, float value) {
        writeInt(address, Float.floatToIntBits(value));
    }

    /**
     * Writes a 64-bit IEEE-754 double-precision float.
     *
     * @param address the address to write to
     * @param value   the value to write to memory
     */
    public final void writeDouble(long address, double value) {
        writeLong(address, Double.doubleToLongBits(value));
    }

    /**
     * Writes a pointer-sized integer.
     *
     * @param address the address to write to
     * @param value   the value to write to memory
     * @see MemoryLayout#pointerSize()
     */
    public final void writeAddress(long address, long value) {
        switch (layout().pointerSize()) {
            case 1 -> writeByte(address, (byte) value);
            case 2 -> writeShort(address, (short) value);
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
    public final void writePointer(long address, Pointer pointer) {
        writeAddress(address, pointer.address());
    }
    // endregion

    private void checkRange(long address, long length) {
        if (address < base) {
            throw new IndexOutOfBoundsException("Address %#x is below memory base %#x".formatted(address, base));
        }
        Objects.checkFromIndexSize(address - base, length, size);
    }

    /**
     * Creates a {@link Pointer} representing the specified address in this memory block.
     *
     * @param address the address to create a pointer for
     * @return a {@link Pointer} representing the specified address
     */
    public Pointer pointer(long address) {
        return new Pointer(this, address);
    }

    @Override
    public String toString() {
        return "Memory[base=%#x, layout=%s]".formatted(base(), layout());
    }
}
