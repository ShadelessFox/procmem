package sh.adelessfox.procmem.process;

import java.util.Set;

public record Section(String name, long base, long size, Set<Permission> permissions) {
    public enum Permission {
        EXECUTE,
        WRITE,
        READ
    }

    public Section {
        if (base < 0) {
            throw new IllegalArgumentException("base must be non-negative");
        }
        if (size < 0) {
            throw new IllegalArgumentException("size must be non-negative");
        }
        permissions = Set.copyOf(permissions);
    }

    public long end() {
        return Math.addExact(base, size);
    }

    public boolean contains(long address) {
        return address >= base && address < end();
    }

    @Override
    public String toString() {
        return "Section[name=%s, base=%#x, size=%#x, permissions=%s]".formatted(name, base, size, permissions);
    }
}
