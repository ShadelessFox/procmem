// SPDX-License-Identifier: MIT
// SPDX-FileCopyrightText: 2026 ShadelessFox

package sh.adelessfox.procmem.struct;

import sh.adelessfox.procmem.MemoryLayout;
import sh.adelessfox.procmem.Pointer;
import sh.adelessfox.procmem.Type;
import sh.adelessfox.procmem.TypeLayout;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;

record StructDefinition(String name, List<StructMember> members) {
    StructDefinition {
        members = List.copyOf(members);
    }

    static StructDefinition empty(String name) {
        return new StructDefinition(name, List.of());
    }

    <T> Placement<T> append(String name, Type.Sized<T> type) {
        var field = new StructMember.Field<>(members.size(), this.name, name, type);
        var layout = append(field);
        return new Placement<>(layout, field);
    }

    StructDefinition padding(long bytes) {
        if (bytes < 0) {
            throw new IllegalArgumentException("padding must be non-negative");
        }
        if (bytes == 0) {
            return this;
        }
        return append(new StructMember.Padding(bytes));
    }

    private StructDefinition append(StructMember member) {
        var members = new ArrayList<>(members());
        members.add(member);
        return new StructDefinition(name, members);
    }

    <T> Type.Sized<T> build(String name, BiFunction<Pointer, StructLayout, T> reader) {
        return new Type.Sized<T>() {
            final ConcurrentHashMap<MemoryLayout, StructLayout> layouts = new ConcurrentHashMap<>();

            @Override
            public TypeLayout layout(MemoryLayout target) {
                return resolve(target).layout();
            }

            @Override
            public T read(Pointer pointer) {
                return reader.apply(pointer, resolve(pointer.layout()));
            }

            @Override
            public String toString() {
                return name;
            }

            StructLayout resolve(MemoryLayout target) {
                return layouts.computeIfAbsent(target, StructDefinition.this::resolve);
            }
        };
    }

    StructLayout resolve(MemoryLayout target) {
        var end = 0L;
        var alignment = 1L;
        var offsets = new long[members.size()];
        for (int i = 0; i < members.size(); i++) {
            switch (members.get(i)) {
                case StructMember.Field<?> field -> {
                    var layout = field.type().layout(target);
                    var offset = align(end, layout.alignment());
                    offsets[i] = offset;
                    end = Math.addExact(offset, layout.size());
                    alignment = Math.max(alignment, layout.alignment());
                }
                case StructMember.Padding padding -> {
                    offsets[i] = end;
                    end = Math.addExact(end, padding.bytes());
                }
            }
        }
        return new StructLayout(new TypeLayout(align(end, alignment), alignment), offsets);
    }

    private static long align(long value, long alignment) {
        long remainder = value % alignment;
        return remainder == 0 ? value : Math.addExact(value, alignment - remainder);
    }

    record Placement<T>(StructDefinition layout, StructMember.Field<T> field) {
    }

    record StructLayout(TypeLayout layout, long[] offsets) {
    }
}
