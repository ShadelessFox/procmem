// SPDX-License-Identifier: MIT
// SPDX-FileCopyrightText: 2026 ShadelessFox

package sh.adelessfox.procmem.process;

import sh.adelessfox.procmem.Memory;

import java.util.Objects;
import java.util.Set;

/**
 * Describes a committed memory region of a process.
 *
 * @see Process#regions()
 */
public final class Region {
    private final Process process;
    private final long base;
    private final long size;
    private final Set<Permission> permissions;
    private final boolean guarded;

    Region(Process process, long base, long size, Set<Permission> permissions, boolean guarded) {
        Objects.checkFromIndexSize(base, size, Long.MAX_VALUE);
        this.process = process;
        this.base = base;
        this.size = size;
        this.permissions = Set.copyOf(permissions);
        this.guarded = guarded;
    }

    public Process process() {
        return process;
    }

    public long base() {
        return base;
    }

    public long size() {
        return size;
    }

    public long end() {
        return Math.addExact(base(), size());
    }

    public boolean contains(long address) {
        return address >= base() && address < end();
    }

    public Set<Permission> permissions() {
        return permissions;
    }

    /** Whether the region has guard protection, independently of its underlying permissions. */
    public boolean guarded() {
        return guarded;
    }

    public Memory memory() {
        return process.memory(base, size);
    }

    @Override
    public String toString() {
        return "Region[base=%#x, size=%#x, permissions=%s, guarded=%s]".formatted(base, size, permissions, guarded);
    }
}
