// SPDX-License-Identifier: MIT
// SPDX-FileCopyrightText: 2026 ShadelessFox

package sh.adelessfox.procmem.process;

import sh.adelessfox.procmem.Memory;
import sh.adelessfox.procmem.MemoryLayout;
import sh.adelessfox.procmem.Pointer;
import sh.adelessfox.procmem.util.Platform;

import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.OptionalLong;

/**
 * Represents a process in the operating system whose memory can be accessed for reading and writing.
 * <p>
 * Operations that take multibyte values will use the layout of the process. For example,
 * on a 64-bit Windows, reading a pointer from a WoW64 process will return a 32-bit value.
 *
 * @see Memory
 */
public abstract sealed class Process
    implements AutoCloseable
    permits Win32Process, LinuxProcess {

    final long pid;
    final MemoryLayout layout;

    private final Memory addressSpace;
    private boolean closed;

    Process(long pid, MemoryLayout layout) {
        this.pid = pid;
        this.layout = layout;
        this.addressSpace = memory(0, Long.MAX_VALUE);
    }

    /**
     * Opens a process for its PID.
     *
     * @param pid the PID of the process to open
     * @return the opened process
     */
    public static Process open(long pid) {
        var factory = ProcessFactory.get();
        return factory.open(pid);
    }

    /**
     * Attempts to open a new process for its module name.
     *
     * @param name the name of the process to open
     * @return an {@link Optional} containing the opened process if found, or an empty {@link Optional} if not found
     */
    public static Optional<Process> open(String name) {
        var ignoreCase = Platform.current() == Platform.WINDOWS;
        var pid = find(name, ignoreCase);
        return pid.stream()
            .mapToObj(Process::open)
            .findFirst();
    }

    /**
     * Returns a snapshot of the modules loaded in the process.
     * <p>
     * The list is not guaranteed to be up-to-date and may not reflect
     * changes made to the process after this method is called.
     *
     * @return a snapshot of the modules loaded in the process
     */
    public abstract List<Module> modules();

    abstract List<Section> sections(Module module);

    /**
     * Returns the module with the given name, if it exists.
     *
     * @param name the name of the module to find
     * @return an {@link Optional} containing the module if found, or an empty {@link Optional} otherwise
     */
    public final Optional<Module> module(String name) {
        var ignoreCase = Platform.current() == Platform.WINDOWS;
        return modules().stream()
            .filter(module -> ignoreCase ? module.name().equalsIgnoreCase(name) : module.name().equals(name))
            .findFirst();
    }

    public abstract List<Region> regions();

    /**
     * Returns the committed region containing an absolute address, if any.
     */
    public Optional<Region> region(long address) {
        ensureOpen();
        return regions().stream()
            .filter(region -> region.contains(address))
            .findFirst();
    }

    /**
     * Creates a pointer into this process's address space.
     */
    public final Pointer pointer(long address) {
        return addressSpace.pointer(address);
    }

    public final long pid() {
        return pid;
    }

    public final MemoryLayout layout() {
        return layout;
    }

    final Memory memory(long base, long size) {
        return new Memory(this, layout, base, size) {
            @Override
            protected void readImpl(long address, ByteBuffer dst) {
                ensureOpen();
                Process.this.read(address, dst);
            }

            @Override
            protected void writeImpl(long address, ByteBuffer src) {
                ensureOpen();
                Process.this.write(address, src);
            }
        };
    }

    abstract void read(long address, ByteBuffer dst);

    abstract void write(long address, ByteBuffer src);

    final void ensureOpen() {
        if (closed) {
            throw new IllegalStateException("Process is closed");
        }
    }

    /**
     * Closes the process and releases any resources associated with it.
     */
    @Override
    public final void close() {
        if (!closed) {
            closed = true;
            closeImpl();
        }
    }

    abstract void closeImpl();

    @Override
    public String toString() {
        return "Process[pid=%d, layout=%s]".formatted(pid, layout);
    }

    private static OptionalLong find(String name, boolean ignoreCase) {
        try (var processes = ProcessHandle.allProcesses()) {
            return processes
                .filter(process -> process.info().command()
                    .map(command -> Path.of(command).getFileName().toString())
                    .filter(module -> ignoreCase ? module.equalsIgnoreCase(name) : module.equals(name))
                    .isPresent())
                .mapToLong(ProcessHandle::pid)
                .findFirst();
        }
    }
}
