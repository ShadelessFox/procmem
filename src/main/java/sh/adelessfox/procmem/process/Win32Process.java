// SPDX-License-Identifier: MIT
// SPDX-FileCopyrightText: 2026 ShadelessFox

package sh.adelessfox.procmem.process;

import sh.adelessfox.procmem.MemoryLayout;

import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.VarHandle;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import static java.lang.foreign.MemoryLayout.PathElement.groupElement;
import static java.lang.foreign.ValueLayout.*;

final class Win32Process extends Process {
    private final MemorySegment handle;

    private Win32Process(long pid, MemorySegment handle, MemoryLayout layout) {
        super(pid, layout);
        this.handle = handle;
    }

    static Win32Process open(long pid) {
        var handle = Kernel32.openProcess(
            Kernel32.PROCESS_VM_READ | Kernel32.PROCESS_QUERY_LIMITED_INFORMATION,
            false,
            Math.toIntExact(pid));
        if (handle.address() == 0L) {
            throw new IllegalStateException("Failed to open process");
        }
        return new Win32Process(pid, handle, processLayout(handle));
    }

    @Override
    public List<Module> modules() {
        var snapshot = Kernel32.createToolhelp32Snapshot(
            Kernel32.TH32CS_SNAPMODULE | Kernel32.TH32CS_SNAPMODULE32,
            Math.toIntExact(pid));
        var modules = new ArrayList<Module>();
        try (var arena = Arena.ofConfined()) {
            var entry = Kernel32.MODULEENTRY32W.allocate(arena);
            if (Kernel32.module32FirstW(snapshot, entry)) {
                do {
                    var name = entry.moduleName();
                    var path = Path.of(entry.modulePath());
                    var address = entry.baseAddr().address();
                    var size = entry.baseSize();
                    modules.add(new Module(name, path, address, size));
                } while (Kernel32.module32NextW(snapshot, entry));
            }
        } finally {
            Kernel32.closeHandle(snapshot);
        }
        return List.copyOf(modules);
    }

    @Override
    public MemoryLayout layout() {
        return layout;
    }

    @Override
    public void read(long address, ByteBuffer dst) {
        try {
            Kernel32.readProcessMemory(
                handle,
                MemorySegment.ofAddress(address),
                dst,
                dst.remaining(),
                MemorySegment.NULL);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to read %d bytes at %#x from process %d".formatted(dst.remaining(), address, pid), e);
        }
    }

    @Override
    public void write(long address, ByteBuffer src) {
        try {
            Kernel32.writeProcessMemory(
                handle,
                MemorySegment.ofAddress(address),
                src,
                src.remaining(),
                MemorySegment.NULL);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to write %d bytes at %#x to process %d".formatted(src.remaining(), address, pid), e);
        }
    }

    @Override
    public void close() {
        try {
            Kernel32.closeHandle(handle);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to close process %d".formatted(pid), e);
        }
    }

    private static MemoryLayout processLayout(MemorySegment handle) {
        var machine = Short.toUnsignedInt(processMachine(handle));
        int pointerSize = switch (machine) {
            case 0x014c, 0x01c4 -> 4; // IMAGE_FILE_MACHINE_I386, IMAGE_FILE_MACHINE_ARMNT
            case 0x8664, 0xaa64 -> 8; // IMAGE_FILE_MACHINE_AMD64, IMAGE_FILE_MACHINE_ARM64
            default -> throw new UnsupportedOperationException("Unsupported Windows machine: %#x".formatted(machine));
        };
        return new MemoryLayout(pointerSize, ByteOrder.LITTLE_ENDIAN);
    }

    private static short processMachine(MemorySegment handle) {
        try (var arena = Arena.ofConfined()) {
            var processMachine = arena.allocate(JAVA_SHORT);
            var nativeMachine = arena.allocate(JAVA_SHORT);
            Kernel32.isWow64Process2(handle, processMachine, nativeMachine);

            var machine = processMachine.get(JAVA_SHORT, 0);
            if (machine == Kernel32.IMAGE_FILE_MACHINE_UNKNOWN) {
                machine = nativeMachine.get(JAVA_SHORT, 0);
            }

            return machine;
        }
    }

    private static final class Kernel32 {
        // OpenProcess
        static final int PROCESS_VM_READ = 0x10;
        static final int PROCESS_QUERY_LIMITED_INFORMATION = 0x1000;

        // IsWow64Process2
        static final int IMAGE_FILE_MACHINE_UNKNOWN = 0x0;

        // CreateToolhelp32Snapshot
        static final long INVALID_HANDLE_VALUE = -1L;
        static final int TH32CS_SNAPMODULE = 0x00000008;
        static final int TH32CS_SNAPMODULE32 = 0x00000010;

        static final MethodHandle openProcess;
        static final MethodHandle closeHandle;
        static final MethodHandle readProcessMemory;
        static final MethodHandle writeProcessMemory;
        static final MethodHandle isWow64Process2;
        static final MethodHandle createToolhelp32Snapshot;
        static final MethodHandle module32FirstW;
        static final MethodHandle module32NextW;

        static final StructLayout captureStateLayout = Linker.Option.captureStateLayout();
        static final VarHandle captureStateLayout_getLastError = captureStateLayout.varHandle(groupElement("GetLastError"));

        record MODULEENTRY32W(MemorySegment segment) {
            static MODULEENTRY32W allocate(SegmentAllocator allocator) {
                var segment = allocator.allocate(1080);
                segment.set(JAVA_INT, 0, (int) segment.byteSize());
                return new MODULEENTRY32W(segment);
            }

            MemorySegment baseAddr() {
                return segment.get(ADDRESS, 24);
            }

            int baseSize() {
                return segment.get(JAVA_INT, 32);
            }

            String moduleName() {
                return segment.getString(48, StandardCharsets.UTF_16LE);
            }

            String modulePath() {
                return segment.getString(560, StandardCharsets.UTF_16LE);
            }
        }

        static {
            System.loadLibrary("Kernel32");

            var lookup = SymbolLookup.loaderLookup();
            var linker = Linker.nativeLinker();
            var ccs = Linker.Option.captureCallState("GetLastError");

            openProcess = linker.downcallHandle(
                lookup.findOrThrow("OpenProcess"),
                FunctionDescriptor.of(ADDRESS, JAVA_INT, JAVA_INT, JAVA_INT),
                ccs);
            closeHandle = linker.downcallHandle(
                lookup.findOrThrow("CloseHandle"),
                FunctionDescriptor.of(JAVA_INT, ADDRESS),
                ccs);
            readProcessMemory = linker.downcallHandle(
                lookup.findOrThrow("ReadProcessMemory"),
                FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS, ADDRESS, JAVA_LONG, ADDRESS),
                ccs);
            writeProcessMemory = linker.downcallHandle(
                lookup.findOrThrow("WriteProcessMemory"),
                FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS, ADDRESS, JAVA_LONG, ADDRESS),
                ccs);
            isWow64Process2 = linker.downcallHandle(
                lookup.findOrThrow("IsWow64Process2"),
                FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS, ADDRESS),
                ccs);
            createToolhelp32Snapshot = linker.downcallHandle(
                lookup.findOrThrow("CreateToolhelp32Snapshot"),
                FunctionDescriptor.of(ADDRESS, JAVA_INT, JAVA_INT),
                ccs);
            module32FirstW = linker.downcallHandle(
                lookup.findOrThrow("Module32FirstW"),
                FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS),
                ccs);
            module32NextW = linker.downcallHandle(
                lookup.findOrThrow("Module32NextW"),
                FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS),
                ccs);
        }

        static MemorySegment openProcess(int desiredAccess, boolean inheritHandle, int processId) {
            return checked(
                "OpenProcess",
                (_, cs) -> (MemorySegment) openProcess.invokeExact(cs, desiredAccess, inheritHandle ? 1 : 0, processId),
                handle -> handle.address() != 0L);
        }

        static void closeHandle(MemorySegment handle) {
            checked(
                "CloseHandle",
                (_, cs) -> (int) closeHandle.invokeExact(cs, handle),
                result -> result != 0);
        }

        static void readProcessMemory(MemorySegment process, MemorySegment address, ByteBuffer buffer, long size, MemorySegment numberOfBytesRead) {
            checked(
                "ReadProcessMemory",
                (arena, cs) -> {
                    var segment = arena.allocate(buffer.remaining());
                    var result = (int) readProcessMemory.invokeExact(cs, process, address, segment, size, numberOfBytesRead);
                    if (result != 0) {
                        buffer.put(segment.asByteBuffer());
                    }
                    return result;
                },
                result -> result != 0);
        }

        static void writeProcessMemory(MemorySegment process, MemorySegment address, ByteBuffer buffer, int size, MemorySegment numberOfBytesWritten) {
            checked(
                "WriteProcessMemory",
                (arena, cs) -> {
                    var segment = arena.allocate(buffer.remaining());
                    int result = (int) writeProcessMemory.invokeExact(cs, process, address, segment, size, numberOfBytesWritten);
                    if (result != 0) {
                        buffer.put(segment.asByteBuffer());
                    }
                    return result;
                },
                result -> result != 0);
        }

        static void isWow64Process2(MemorySegment process, MemorySegment processMachine, MemorySegment nativeMachine) {
            checked(
                "IsWow64Process2",
                (_, cs) -> (int) isWow64Process2.invokeExact(cs, process, processMachine, nativeMachine),
                result -> result != 0);
        }

        static MemorySegment createToolhelp32Snapshot(int flags, int processId) {
            return checked(
                "CreateToolhelp32Snapshot",
                (_, cs) -> (MemorySegment) createToolhelp32Snapshot.invokeExact(cs, flags, processId),
                handle -> handle.address() != INVALID_HANDLE_VALUE);
        }

        static boolean module32FirstW(MemorySegment snapshot, MODULEENTRY32W moduleEntry) {
            return checked(
                "Module32FirstW",
                (_, cs) -> (int) module32FirstW.invokeExact(cs, snapshot, moduleEntry.segment) != 0,
                _ -> true);
        }

        static boolean module32NextW(MemorySegment snapshot, MODULEENTRY32W moduleEntry) {
            return checked(
                "Module32NextW",
                (_, cs) -> (int) module32NextW.invokeExact(cs, snapshot, moduleEntry.segment) != 0,
                _ -> true);
        }

        private static <T> T checked(
            String function,
            Callback<T> callback,
            Predicate<T> succeeded
        ) {
            try (var arena = Arena.ofConfined()) {
                var cs = arena.allocate(captureStateLayout);

                T result;
                try {
                    result = callback.invoke(arena, cs);
                } catch (Throwable e) {
                    throw new AssertionError(e);
                }

                if (!succeeded.test(result)) {
                    var error = (int) captureStateLayout_getLastError.get(cs, 0L);
                    throw new IllegalStateException("%s failed: %#x".formatted(function, error));
                }

                return result;
            }
        }

        @FunctionalInterface
        interface Callback<T> {
            T invoke(Arena arena, MemorySegment cs) throws Throwable;
        }
    }
}
