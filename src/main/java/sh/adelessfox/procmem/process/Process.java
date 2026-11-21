// SPDX-License-Identifier: MIT
// SPDX-FileCopyrightText: 2026 ShadelessFox

package sh.adelessfox.procmem.process;

import sh.adelessfox.procmem.Memory;
import sh.adelessfox.procmem.MemoryLayout;
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
public abstract sealed class Process extends Memory
    implements AutoCloseable
    permits Win32Process, LinuxProcess {

    final long pid;
    final MemoryLayout layout;
    final ByteBuffer scratch;

    Process(long pid, MemoryLayout layout) {
        super(0);
        this.pid = pid;
        this.layout = layout;
        this.scratch = ByteBuffer.allocate(8).order(layout.byteOrder());
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

    @Override
    public byte readByte(long offset) {
        read(offset, scratch.position(0));
        return scratch.get(0);
    }

    @Override
    public short readShort(long offset) {
        read(offset, scratch.position(0));
        return scratch.getShort(0);
    }

    @Override
    public int readInt(long offset) {
        read(offset, scratch.position(0));
        return scratch.getInt(0);
    }

    @Override
    public long readLong(long offset) {
        read(offset, scratch.position(0));
        return scratch.getLong(0);
    }

    @Override
    public void writeByte(long offset, byte value) {
        write(offset, scratch.position(0).put(0, value));
    }

    @Override
    public void writeShort(long offset, short value) {
        write(offset, scratch.position(0).putShort(0, value));
    }

    @Override
    public void writeInt(long offset, int value) {
        write(offset, scratch.position(0).putInt(0, value));
    }

    @Override
    public void writeLong(long offset, long value) {
        write(offset, scratch.position(0).putLong(0, value));
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

    /**
     * Returns the module with the given name, if it exists.
     *
     * @param name the name of the module to find
     * @return an {@link Optional} containing the module if found, or an empty {@link Optional} otherwise
     */
    public Optional<Module> module(String name) {
        var ignoreCase = Platform.current() == Platform.WINDOWS;
        return modules().stream()
            .filter(module -> ignoreCase ? module.name().equalsIgnoreCase(name) : module.name().equals(name))
            .findFirst();
    }

    public abstract List<Section> sections(Module module);

    public Optional<Section> section(Module module, String name) {
        var ignoreCase = Platform.current() == Platform.WINDOWS;
        return sections(module).stream()
            .filter(section -> ignoreCase ? section.name().equalsIgnoreCase(name) : section.name().equals(name))
            .findFirst();
    }

    /**
     * Closes the process and releases any resources associated with it.
     */
    @Override
    public abstract void close();

    @Override
    public String toString() {
        return "Process[pid=%d, layout=%s]".formatted(pid, layout);
    }

    private static Process open(long pid) {
        var factory = ProcessFactory.get();
        return factory.open(pid);
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
