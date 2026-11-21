// SPDX-License-Identifier: MIT
// SPDX-FileCopyrightText: 2026 ShadelessFox

package sh.adelessfox.procmem.process;

import sh.adelessfox.procmem.MemoryLayout;
import sh.adelessfox.procmem.Type;

import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.VarHandle;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
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
                    var base = entry.baseAddr().address();
                    var size = entry.baseSize();
                    modules.add(new Module(name, path, base, size));
                } while (Kernel32.module32NextW(snapshot, entry));
            }
        } finally {
            Kernel32.closeHandle(snapshot);
        }
        return List.copyOf(modules);
    }

    @Override
    public List<Section> sections(Module module) {
        var image = pointer(module.base());
        if (Short.toUnsignedInt(image.readShort()) != 0x5a4d) {
            throw new IllegalArgumentException("Invalid DOS signature");
        }

        var ntOffset = Integer.toUnsignedLong(image.add(0x3c).readInt());
        var ntPointer = image.add(ntOffset);
        var nt = NtHeaders.TYPE.read(ntPointer);

        if (nt.signature() != 0x00004550) {
            throw new IllegalArgumentException("Invalid PE signature");
        }

        var fixedHeaderSize = NtHeaders.TYPE.layout(layout()).size();
        var table = ntPointer.add(fixedHeaderSize + nt.optionalHeaderSize());

        var sections = new ArrayList<Section>();
        for (var header : SectionHeader.TYPE.slice(table, nt.sectionCount())) {
            var base = Math.addExact(module.base(), header.virtualAddress());
            var size = header.virtualSize();
            var permissions = header.mapCharacteristicsToPermissions();

            sections.add(new Section(header.name(), base, size, permissions));
        }

        return List.copyOf(sections);
    }

    @Override
    public MemoryLayout layout() {
        return layout;
    }

    @Override
    public void read(long offset, ByteBuffer dst) {
        try {
            Kernel32.readProcessMemory(
                handle,
                MemorySegment.ofAddress(offset),
                dst,
                dst.remaining(),
                MemorySegment.NULL);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to read %d bytes at %#x from process %d".formatted(dst.remaining(), offset, pid), e);
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

    private static final Type.Sized<Integer> WORD =
        Type.SHORT.map(Short::toUnsignedInt);

    private static final Type.Sized<Long> DWORD =
        Type.INT.map(Integer::toUnsignedLong);

    private static final Type.Sized<String> SECTION_NAME = Type.of(
        pointer -> {
            var name = pointer.readString(StandardCharsets.US_ASCII, 8);
            int terminator = name.indexOf('\0');
            return terminator < 0 ? name : name.substring(0, terminator);
        }, 8, 1, "SectionName");

    private record NtHeaders(
        int signature,
        int sectionCount,
        int optionalHeaderSize
    ) {
        static final Type.Sized<NtHeaders> TYPE = Type.struct("NtHeaders")
            .field("signature", Type.INT)           // 0
            .padding(2)                            // Machine
            .field("sectionCount", WORD)           // 6
            .padding(12)                           // Timestamp and symbol-table fields
            .field("optionalHeaderSize", WORD)     // 20
            .padding(2)                            // Characteristics
            .build(NtHeaders::new);                // Size: 24
    }

    private record SectionHeader(
        String name,
        long virtualSize,
        long virtualAddress,
        int characteristics
    ) {
        static final Type.Sized<SectionHeader> TYPE = Type.struct("SectionHeader")
            .field("name", SECTION_NAME)           // 0
            .field("virtualSize", DWORD)           // 8
            .field("virtualAddress", DWORD)        // 12
            .padding(20)                          // File data, relocations, line numbers
            .field("characteristics", Type.INT)    // 36
            .build(SectionHeader::new);            // Size: 40

        private Set<Section.Permission> mapCharacteristicsToPermissions() {
            var permissions = EnumSet.noneOf(Section.Permission.class);
            if ((characteristics & 0x40000000) != 0) {
                permissions.add(Section.Permission.READ);
            }
            if ((characteristics & 0x80000000) != 0) {
                permissions.add(Section.Permission.WRITE);
            }
            if ((characteristics & 0x20000000) != 0) {
                permissions.add(Section.Permission.EXECUTE);
            }
            return Set.copyOf(permissions);
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

        // NT header stuff
        private static final int DOS_HEADER_SIZE = 64;
        private static final int DOS_NT_HEADERS_OFFSET = 0x3c;
        private static final int NT_FIXED_HEADER_SIZE = 24; // Signature + COFF header.
        private static final int NT_SECTION_COUNT_OFFSET = 6;
        private static final int NT_OPTIONAL_HEADER_SIZE_OFFSET = 20;
        private static final int SECTION_HEADER_SIZE = 40;
        private static final int SECTION_NAME_SIZE = 8;
        private static final int SECTION_VIRTUAL_SIZE_OFFSET = 8;
        private static final int SECTION_RVA_OFFSET = 12;

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

        static void readProcessMemory(
            MemorySegment process,
            MemorySegment address,
            ByteBuffer buffer,
            long size,
            MemorySegment numberOfBytesRead
        ) {
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

        static void writeProcessMemory(
            MemorySegment process,
            MemorySegment address,
            ByteBuffer buffer,
            int size,
            MemorySegment numberOfBytesWritten
        ) {
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
