// SPDX-License-Identifier: MIT
// SPDX-FileCopyrightText: 2026 ShadelessFox

package sh.adelessfox.procmem.process;

import sh.adelessfox.procmem.Memory;

import java.util.Objects;
import java.util.Set;

/**
 * Describes a named section of a loaded module.
 *
 * @see Process#sections(Module)
 */
public final class Section {
    private final Process process;
    private final String name;
    private final long base;
    private final long size;
    private final Set<Permission> permissions;

    Section(Process process, String name, long base, long size, Set<Permission> permissions) {
        Objects.checkFromIndexSize(base, size, Long.MAX_VALUE);
        this.process = process;
        this.name = name;
        this.base = base;
        this.size = size;
        this.permissions = Set.copyOf(permissions);
    }

    public Process process() {
        return process;
    }

    public String name() {
        return name;
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

    /**
     * Returns a non-owning memory view bounded by this section's runtime extent.
     */
    public Memory memory() {
        return process.memory(base, size);
    }

    @Override
    public String toString() {
        return "Section[name=%s, base=%#x, size=%#x, permissions=%s]".formatted(name, base, size, permissions);
    }
}
