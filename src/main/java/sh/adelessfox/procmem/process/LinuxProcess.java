// SPDX-License-Identifier: MIT
// SPDX-FileCopyrightText: 2026 ShadelessFox

package sh.adelessfox.procmem.process;

import sh.adelessfox.procmem.MemoryLayout;

import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;

final class LinuxProcess extends Process {
    private final FileChannel memory;

    private LinuxProcess(long pid, FileChannel memory, MemoryLayout layout) {
        super(pid, layout);
        this.memory = memory;
    }

    static LinuxProcess of(long pid) {
        try {
            var layout = processLayout(pid);
            var memory = FileChannel.open(Path.of("/proc", Long.toString(pid), "mem"), StandardOpenOption.READ);
            return new LinuxProcess(pid, memory, layout);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to open process " + pid, e);
        }
    }

    @Override
    public List<Module> modules() {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<Section> sections(Module module) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<Region> regions() {
        throw new UnsupportedOperationException();
    }

    @Override
    void read(long address, ByteBuffer dst) {
        int start = dst.position();
        try {
            while (dst.hasRemaining()) {
                if (memory.read(dst, address + (dst.position() - start)) <= 0) {
                    throw new EOFException("Incomplete process memory read");
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read memory at %#x from process %d".formatted(address, pid), e);
        }
    }

    @Override
    void write(long address, ByteBuffer src) {
        int start = src.position();
        try {
            while (src.hasRemaining()) {
                if (memory.write(src, address + (src.position() - start)) <= 0) {
                    throw new EOFException("Incomplete process memory write");
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed to write memory at %#x to process %d".formatted(address, pid), e);
        }
    }

    @Override
    void closeImpl() {
        try {
            memory.close();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to close process " + pid, e);
        }
    }

    private static MemoryLayout processLayout(long pid) throws IOException {
        byte[] ident;
        try (var in = Files.newInputStream(Path.of("/proc", Long.toString(pid), "exe"))) {
            ident = in.readNBytes(16);
        }

        if (ident.length != 16 || ident[0] != 0x7f || ident[1] != 'E' || ident[2] != 'L' || ident[3] != 'F') {
            throw new IOException("Target executable is not a valid ELF image");
        }

        int pointerSize = switch (ident[4]) { // EI_CLASS
            case 1 -> 4; // ELFCLASS32
            case 2 -> 8; // ELFCLASS64
            default -> throw new IOException("Unsupported ELF class");
        };

        var order = switch (ident[5]) { // EI_DATA
            case 1 -> ByteOrder.LITTLE_ENDIAN;
            case 2 -> ByteOrder.BIG_ENDIAN;
            default -> throw new IOException("Unsupported ELF byte order");
        };

        return new MemoryLayout(pointerSize, order);
    }
}
