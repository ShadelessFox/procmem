// SPDX-License-Identifier: MIT
// SPDX-FileCopyrightText: 2026 ShadelessFox

package sh.adelessfox.procmem.struct;

import sh.adelessfox.procmem.Type;

public final class StructBuilder1<F0> {
    private final String name;

    private final StructDefinition layout;

    private final StructMember.Field<F0> f0;

    StructBuilder1(String name, StructDefinition layout, StructMember.Field<F0> f0) {
        this.name = name;
        this.layout = layout;
        this.f0 = f0;
    }

    public <F1> StructBuilder2<F0, F1> field(String name, Type.Sized<F1> type) {
        var placement = layout.append(name, type);
        return new StructBuilder2<F0, F1>(this.name, placement.layout(), f0, placement.field());
    }

    public StructBuilder1<F0> padding(long bytes) {
        return new StructBuilder1<F0>(this.name, layout.padding(bytes), f0);
    }

    public <T> Type.Sized<T> build(StructBuilder1.Constructor<F0, T> constructor) {
        return layout.build(name, (pointer, resolved) -> constructor.create(f0.read(pointer, resolved)));
    }

    @FunctionalInterface
    public interface Constructor<F0, T> {
        T create(F0 f0);
    }
}
