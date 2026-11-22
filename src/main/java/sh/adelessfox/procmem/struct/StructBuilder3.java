// SPDX-License-Identifier: MIT
// SPDX-FileCopyrightText: 2026 ShadelessFox

package sh.adelessfox.procmem.struct;

import sh.adelessfox.procmem.Type;

public final class StructBuilder3<F0, F1, F2> {
    private final String name;

    private final StructDefinition layout;

    private final StructMember.Field<F0> f0;

    private final StructMember.Field<F1> f1;

    private final StructMember.Field<F2> f2;

    StructBuilder3(String name, StructDefinition layout, StructMember.Field<F0> f0,
            StructMember.Field<F1> f1, StructMember.Field<F2> f2) {
        this.name = name;
        this.layout = layout;
        this.f0 = f0;
        this.f1 = f1;
        this.f2 = f2;
    }

    public <F3> StructBuilder4<F0, F1, F2, F3> field(String name, Type.Sized<F3> type) {
        var placement = layout.append(name, type);
        return new StructBuilder4<F0, F1, F2, F3>(this.name, placement.layout(), f0, f1, f2, placement.field());
    }

    public StructBuilder3<F0, F1, F2> padding(long bytes) {
        return new StructBuilder3<F0, F1, F2>(this.name, layout.padding(bytes), f0, f1, f2);
    }

    public <T> StructType<T> build(StructBuilder3.Constructor<F0, F1, F2, T> constructor) {
        return layout.build((pointer, resolved) -> constructor.create(f0.read(pointer, resolved), f1.read(pointer, resolved), f2.read(pointer, resolved)));
    }

    @FunctionalInterface
    public interface Constructor<F0, F1, F2, T> {
        T create(F0 f0, F1 f1, F2 f2);
    }
}
