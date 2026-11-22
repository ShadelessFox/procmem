// SPDX-License-Identifier: MIT
// SPDX-FileCopyrightText: 2026 ShadelessFox

package sh.adelessfox.procmem.struct;

import sh.adelessfox.procmem.Type;

public final class StructBuilder5<F0, F1, F2, F3, F4> {
    private final String name;

    private final StructDefinition layout;

    private final StructMember.Field<F0> f0;

    private final StructMember.Field<F1> f1;

    private final StructMember.Field<F2> f2;

    private final StructMember.Field<F3> f3;

    private final StructMember.Field<F4> f4;

    StructBuilder5(String name, StructDefinition layout, StructMember.Field<F0> f0,
            StructMember.Field<F1> f1, StructMember.Field<F2> f2, StructMember.Field<F3> f3,
            StructMember.Field<F4> f4) {
        this.name = name;
        this.layout = layout;
        this.f0 = f0;
        this.f1 = f1;
        this.f2 = f2;
        this.f3 = f3;
        this.f4 = f4;
    }

    public <F5> StructBuilder6<F0, F1, F2, F3, F4, F5> field(String name, Type.Sized<F5> type) {
        var placement = layout.append(name, type);
        return new StructBuilder6<F0, F1, F2, F3, F4, F5>(this.name, placement.layout(), f0, f1, f2, f3, f4, placement.field());
    }

    public StructBuilder5<F0, F1, F2, F3, F4> padding(long bytes) {
        return new StructBuilder5<F0, F1, F2, F3, F4>(this.name, layout.padding(bytes), f0, f1, f2, f3, f4);
    }

    public <T> StructType<T> build(StructBuilder5.Constructor<F0, F1, F2, F3, F4, T> constructor) {
        return layout.build((pointer, resolved) -> constructor.create(f0.read(pointer, resolved), f1.read(pointer, resolved), f2.read(pointer, resolved), f3.read(pointer, resolved), f4.read(pointer, resolved)));
    }

    @FunctionalInterface
    public interface Constructor<F0, F1, F2, F3, F4, T> {
        T create(F0 f0, F1 f1, F2 f2, F3 f3, F4 f4);
    }
}
