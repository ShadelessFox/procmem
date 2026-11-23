// SPDX-License-Identifier: MIT
// SPDX-FileCopyrightText: 2026 ShadelessFox

package sh.adelessfox.procmem;

import java.nio.charset.Charset;

/**
 * Represents an absolute address in a memory view.
 */
public record Pointer(Memory memory, long address) {
    public Pointer {
        if (address < 0) {
            throw new IllegalArgumentException("address must be non-negative");
        }
    }

    public byte readByte() {
        return memory().readByte(address);
    }

    public short readShort() {
        return memory().readShort(address);
    }

    public int readInt() {
        return memory().readInt(address);
    }

    public long readLong() {
        return memory().readLong(address);
    }

    public float readFloat() {
        return memory().readFloat(address);
    }

    public double readDouble() {
        return memory().readDouble(address);
    }

    /**
     * Reads a string of {@code length} bytes, truncated at the first NUL if present.
     */
    public String readString(Charset charset, int length) {
        return memory().readString(address, charset, length);
    }

    /**
     * Reads a NUL-terminated ISO-8859-1 string with a 256-byte limit, including the terminator.
     */
    public String readCString() {
        return memory().readCString(address);
    }

    /**
     * Reads a NUL-terminated ISO-8859-1 string with a {@code maxLength}-byte limit, including the terminator.
     */
    public String readCString(int maxLength) {
        return memory().readCString(address, maxLength);
    }

    public Pointer deref() {
        return memory().readPointer(address);
    }

    public Pointer add(long byteOffset) {
        return new Pointer(memory, Math.addExact(address, byteOffset));
    }

    public boolean isNull() {
        return address == 0;
    }

    public boolean isNonNull() {
        return address != 0;
    }

    public MemoryLayout layout() {
        return memory.layout();
    }

    @Override
    public String toString() {
        return "%#x".formatted(address);
    }
}
