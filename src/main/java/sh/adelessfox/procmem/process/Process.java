// SPDX-License-Identifier: MIT
// SPDX-FileCopyrightText: 2026 ShadelessFox

package sh.adelessfox.procmem.process;

import sh.adelessfox.procmem.Memory;
import sh.adelessfox.procmem.MemoryLayout;
import sh.adelessfox.procmem.util.Platform;

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

    Process(long pid, MemoryLayout layout) {
        super(0);
        this.pid = pid;
        this.layout = layout;
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
     * Closes the process and releases any resources associated with it.
     */
    @Override
    public abstract void close();

    @Override
    public String toString() {
        return "Process[pid=%d, layout=%s]".formatted(pid, layout);
    }
}
