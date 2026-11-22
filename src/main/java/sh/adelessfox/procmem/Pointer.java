// SPDX-License-Identifier: MIT
// SPDX-FileCopyrightText: 2026 ShadelessFox

package sh.adelessfox.procmem;

import sh.adelessfox.procmem.process.Process;

import java.nio.charset.Charset;

/**
 * Represents an absolute address in a process.
 */
public final class Pointer implements Comparable<Pointer> {
    private final Process process;
    private final Memory memory;
    private final StableValue<Memory> resolvedMemory;
    private final long address;

    public Pointer(Process process, long address) {
        this(process, address, StableValue.of());
    }

    private Pointer(Process process, long address, StableValue<Memory> resolvedMemory) {
        this.process = process;
        this.memory = null;
        this.resolvedMemory = resolvedMemory;
        this.address = address;
    }

    public Pointer(Memory memory, long address) {
        this.process = memory.process;
        this.memory = memory;
        this.resolvedMemory = null;
        this.address = address;
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

    /** Reads a string of {@code length} bytes, truncated at the first NUL if present. */
    public String readString(Charset charset, int length) {
        return memory().readString(address, charset, length);
    }

    /** Reads a NUL-terminated ISO-8859-1 string with a 256-byte limit, including the terminator. */
    public String readCString() {
        return memory().readCString(address);
    }

    /** Reads a NUL-terminated ISO-8859-1 string with a {@code maxLength}-byte limit, including the terminator. */
    public String readCString(int maxLength) {
        return memory().readCString(address, maxLength);
    }

    public Pointer deref() {
        return memory().readPointer(address);
    }

    public Pointer add(long byteOffset) {
        long next = Math.addExact(address, byteOffset);
        if (memory != null) {
            return new Pointer(memory, next);
        }
        var cached = resolvedMemory.orElse(null);
        if (cached != null && next >= cached.base() && next - cached.base() < cached.size()) {
            // Reuse the resolution, but remain unbound so later arithmetic can leave this region
            return new Pointer(process, next, resolvedMemory);
        }
        return new Pointer(process, next);
    }

    public boolean isNull() {
        return address == 0;
    }

    public boolean isNonNull() {
        return address != 0;
    }

    public long address() {
        return address;
    }

    public Memory memory() {
        if (memory != null) {
            return memory;
        }
        return resolvedMemory.orElseSet(() -> process.region(address)
            .orElseThrow(() -> new IllegalStateException("No committed region contains address %#x".formatted(address)))
            .memory());
    }

    public MemoryLayout layout() {
        return process.layout();
    }

    @Override
    public String toString() {
        return "%#x".formatted(address);
    }

    @Override
    public int compareTo(Pointer o) {
        return Long.compareUnsigned(address, o.address);
    }
}
