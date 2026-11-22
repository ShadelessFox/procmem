// SPDX-License-Identifier: MIT
// SPDX-FileCopyrightText: 2026 ShadelessFox

package sh.adelessfox.procmem.process;

import java.nio.file.Path;

/**
 * Describes a module loaded in a process.
 *
 * @see Process#modules()
 */
public final class Module {
    private final Process process;
    private final String name;
    private final Path path;
    private final long base;
    private final long size;

    Module(Process process, String name, Path path, long base, long size) {
        this.process = process;
        this.name = name;
        this.path = path;
        this.base = base;
        this.size = size;
    }

    public Process process() {
        return process;
    }

    public String name() {
        return name;
    }

    public Path path() {
        return path;
    }

    public long base() {
        return base;
    }

    public long size() {
        return size;
    }

    @Override
    public String toString() {
        return "Module[name=%s, path=%s, base=%#x, size=%#x]".formatted(name, path, base, size);
    }
}
