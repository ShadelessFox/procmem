// SPDX-License-Identifier: MIT
// SPDX-FileCopyrightText: 2026 ShadelessFox

package sh.adelessfox.procmem.struct;

import sh.adelessfox.procmem.Type;

public final class StructBuilder6<F0, F1, F2, F3, F4, F5> {
    private final String name;

    private final StructDefinition layout;

    private final StructMember.Field<F0> f0;

    private final StructMember.Field<F1> f1;

    private final StructMember.Field<F2> f2;

    private final StructMember.Field<F3> f3;

    private final StructMember.Field<F4> f4;

    private final StructMember.Field<F5> f5;

    StructBuilder6(String name, StructDefinition layout, StructMember.Field<F0> f0,
            StructMember.Field<F1> f1, StructMember.Field<F2> f2, StructMember.Field<F3> f3,
            StructMember.Field<F4> f4, StructMember.Field<F5> f5) {
        this.name = name;
        this.layout = layout;
        this.f0 = f0;
        this.f1 = f1;
        this.f2 = f2;
        this.f3 = f3;
        this.f4 = f4;
        this.f5 = f5;
    }

    public <F6> StructBuilder7<F0, F1, F2, F3, F4, F5, F6> field(String name, Type.Sized<F6> type) {
        var placement = layout.append(name, type);
        return new StructBuilder7<F0, F1, F2, F3, F4, F5, F6>(this.name, placement.layout(), f0, f1, f2, f3, f4, f5, placement.field());
    }

    public StructBuilder6<F0, F1, F2, F3, F4, F5> padding(long bytes) {
        return new StructBuilder6<F0, F1, F2, F3, F4, F5>(this.name, layout.padding(bytes), f0, f1, f2, f3, f4, f5);
    }

    public <T> Type.Sized<T> build(
            StructBuilder6.Constructor<F0, F1, F2, F3, F4, F5, T> constructor) {
        return layout.build(name, (pointer, resolved) -> constructor.create(f0.read(pointer, resolved), f1.read(pointer, resolved), f2.read(pointer, resolved), f3.read(pointer, resolved), f4.read(pointer, resolved), f5.read(pointer, resolved)));
    }

    @FunctionalInterface
    public interface Constructor<F0, F1, F2, F3, F4, F5, T> {
        T create(F0 f0, F1 f1, F2 f2, F3 f3, F4 f4, F5 f5);
    }
}
