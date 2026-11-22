// SPDX-License-Identifier: MIT
// SPDX-FileCopyrightText: 2026 ShadelessFox

package sh.adelessfox.procmem.struct;

import sh.adelessfox.procmem.MemoryLayout;
import sh.adelessfox.procmem.Pointer;
import sh.adelessfox.procmem.Type;
import sh.adelessfox.procmem.TypeLayout;

import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * A sized structure reader with its declaration and target-specific layout metadata.
 */
public final class StructType<T> extends Type.Sized<T> {
    private final StructDefinition definition;
    private final BiFunction<Pointer, StructLayout, T> reader;

    StructType(StructDefinition definition, BiFunction<Pointer, StructLayout, T> reader) {
        this.definition = definition;
        this.reader = reader;
    }

    public StructDefinition definition() {
        return definition;
    }

    @Override
    public TypeLayout layout(MemoryLayout target) {
        return definition.resolve(target).layout();
    }

    @Override
    public T read(Pointer pointer) {
        return reader.apply(pointer, definition.resolve(pointer.layout()));
    }

    @Override
    public <R> StructType<R> map(Function<? super T, ? extends R> mapper) {
        return new StructType<>(definition, (pointer, resolved) -> mapper.apply(reader.apply(pointer, resolved)));
    }

    @Override
    public String toString() {
        return definition.name();
    }
}
