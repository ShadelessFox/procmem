// SPDX-License-Identifier: MIT
// SPDX-FileCopyrightText: 2026 ShadelessFox

package sh.adelessfox.procmem.struct;

import sh.adelessfox.procmem.Type;

public final class StructBuilder2<F0, F1> {
    private final String name;

    private final StructDefinition layout;

    private final StructMember.Field<F0> f0;

    private final StructMember.Field<F1> f1;

    StructBuilder2(String name, StructDefinition layout, StructMember.Field<F0> f0,
            StructMember.Field<F1> f1) {
        this.name = name;
        this.layout = layout;
        this.f0 = f0;
        this.f1 = f1;
    }

    public <F2> StructBuilder3<F0, F1, F2> field(String name, Type.Sized<F2> type) {
        var placement = layout.append(name, type);
        return new StructBuilder3<F0, F1, F2>(this.name, placement.layout(), f0, f1, placement.field());
    }

    public StructBuilder2<F0, F1> padding(long bytes) {
        return new StructBuilder2<F0, F1>(this.name, layout.padding(bytes), f0, f1);
    }

    public <T> StructType<T> build(StructBuilder2.Constructor<F0, F1, T> constructor) {
        return layout.build((pointer, resolved) -> constructor.create(f0.read(pointer, resolved), f1.read(pointer, resolved)));
    }

    @FunctionalInterface
    public interface Constructor<F0, F1, T> {
        T create(F0 f0, F1 f1);
    }
}
