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
import java.util.function.Function;

/**
 * An immutable structure declaration, including fields and explicit padding in declaration order.
 */
public final class StructDefinition {
    private final String name;
    private final List<StructMember> members;
    private final ConcurrentHashMap<MemoryLayout, StructLayout> layouts = new ConcurrentHashMap<>();

    private StructDefinition(String name, List<StructMember> members) {
        this.name = name;
        this.members = List.copyOf(members);
    }

    /**
     * Creates a new structure builder.
     */
    public static Builder builder(String name) {
        return new Builder(name);
    }

    /**
     * Returns the name of this structure.
     */
    public String name() {
        return name;
    }

    /**
     * Returns the members in declaration order.
     */
    public List<StructMember> members() {
        return members;
    }

    /**
     * Binds this definition to a Java value reader.
     */
    public <T> StructType<T> bind(Function<StructReader, T> reader) {
        return build((pointer, resolved) -> reader.apply(new StructReader(pointer, resolved)));
    }

    /**
     * Resolves member layouts and offsets for the target.
     */
    public StructLayout resolve(MemoryLayout target) {
        return layouts.computeIfAbsent(target, this::resolveLayout);
    }

    static StructDefinition empty(String name) {
        return new StructDefinition(name, List.of());
    }

    <T> Placement<T> append(String name, Type.Sized<T> type) {
        for (var member : members) {
            if (member instanceof StructMember.Field<?> field && field.name().equals(name)) {
                throw new IllegalArgumentException("Duplicate field: " + name);
            }
        }
        var field = new StructMember.Field<>(members.size(), this.name, name, type);
        return new Placement<>(append(field), field);
    }

    StructDefinition padding(long bytes) {
        if (bytes < 0) {
            throw new IllegalArgumentException("padding must be non-negative");
        }
        return bytes == 0 ? this : append(new StructMember.Padding(bytes));
    }

    private StructDefinition append(StructMember member) {
        var copy = new ArrayList<>(members);
        copy.add(member);
        return new StructDefinition(name, copy);
    }

    <T> StructType<T> build(BiFunction<Pointer, StructLayout, T> reader) {
        return new StructType<>(this, reader);
    }

    private StructLayout resolveLayout(MemoryLayout target) {
        long end = 0;
        long alignment = 1;
        var resolved = new ArrayList<StructLayout.Member>();
        for (var member : members) {
            switch (member) {
                case StructMember.Field<?> field -> {
                    var layout = field.type().layout(target);
                    long offset = align(end, layout.alignment());
                    resolved.add(new StructLayout.Member(field, offset, layout));
                    end = Math.addExact(offset, layout.size());
                    alignment = Math.max(alignment, layout.alignment());
                }
                case StructMember.Padding padding -> {
                    resolved.add(new StructLayout.Member(padding, end, new TypeLayout(padding.bytes(), 1)));
                    end = Math.addExact(end, padding.bytes());
                }
            }
        }
        return new StructLayout(this, new TypeLayout(align(end, alignment), alignment), resolved);
    }

    private static long align(long value, long alignment) {
        long remainder = value % alignment;
        return remainder == 0 ? value : Math.addExact(value, alignment - remainder);
    }

    record Placement<T>(StructDefinition layout, StructMember.Field<T> field) {
    }

    /**
     * A mutable declaration builder; built definitions are immutable snapshots.
     */
    public static final class Builder {
        private StructDefinition definition;

        private Builder(String name) {
            definition = empty(name);
        }

        /**
         * Appends a field and returns its typed handle.
         */
        public <T> StructMember.Field<T> field(String name, Type.Sized<T> type) {
            var placement = definition.append(name, type);
            definition = placement.layout();
            return placement.field();
        }

        /**
         * Appends explicit padding.
         */
        public Builder padding(long bytes) {
            definition = definition.padding(bytes);
            return this;
        }

        /**
         * Returns the current definition snapshot.
         */
        public StructDefinition build() {
            return definition;
        }
    }
}
