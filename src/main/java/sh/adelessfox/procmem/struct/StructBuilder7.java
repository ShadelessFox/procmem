package sh.adelessfox.procmem.struct;

import sh.adelessfox.procmem.Type;

public final class StructBuilder7<F0, F1, F2, F3, F4, F5, F6> {
    private final String name;

    private final StructDefinition layout;

    private final StructMember.Field<F0> f0;

    private final StructMember.Field<F1> f1;

    private final StructMember.Field<F2> f2;

    private final StructMember.Field<F3> f3;

    private final StructMember.Field<F4> f4;

    private final StructMember.Field<F5> f5;

    private final StructMember.Field<F6> f6;

    StructBuilder7(String name, StructDefinition layout, StructMember.Field<F0> f0,
            StructMember.Field<F1> f1, StructMember.Field<F2> f2, StructMember.Field<F3> f3,
            StructMember.Field<F4> f4, StructMember.Field<F5> f5, StructMember.Field<F6> f6) {
        this.name = name;
        this.layout = layout;
        this.f0 = f0;
        this.f1 = f1;
        this.f2 = f2;
        this.f3 = f3;
        this.f4 = f4;
        this.f5 = f5;
        this.f6 = f6;
    }

    public <F7> StructBuilder8<F0, F1, F2, F3, F4, F5, F6, F7> field(String name,
            Type.Sized<F7> type) {
        var placement = layout.append(name, type);
        return new StructBuilder8<>(this.name, placement.layout(), f0, f1, f2, f3, f4, f5, f6, placement.field());
    }

    public StructBuilder7<F0, F1, F2, F3, F4, F5, F6> padding(long bytes) {
        return new StructBuilder7<>(this.name, layout.padding(bytes), f0, f1, f2, f3, f4, f5, f6);
    }

    public <T> StructType<T> build(
            StructBuilder7.Constructor<F0, F1, F2, F3, F4, F5, F6, T> constructor) {
        return layout.build((pointer, resolved) -> constructor.create(f0.read(pointer, resolved), f1.read(pointer, resolved), f2.read(pointer, resolved), f3.read(pointer, resolved), f4.read(pointer, resolved), f5.read(pointer, resolved), f6.read(pointer, resolved)));
    }

    @FunctionalInterface
    public interface Constructor<F0, F1, F2, F3, F4, F5, F6, T> {
        T create(F0 f0, F1 f1, F2 f2, F3 f3, F4 f4, F5 f5, F6 f6);
    }
}
