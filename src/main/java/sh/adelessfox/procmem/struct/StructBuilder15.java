package sh.adelessfox.procmem.struct;

import sh.adelessfox.procmem.Type;

public final class StructBuilder15<F0, F1, F2, F3, F4, F5, F6, F7, F8, F9, F10, F11, F12, F13, F14> {
    private final String name;

    private final StructDefinition layout;

    private final StructMember.Field<F0> f0;

    private final StructMember.Field<F1> f1;

    private final StructMember.Field<F2> f2;

    private final StructMember.Field<F3> f3;

    private final StructMember.Field<F4> f4;

    private final StructMember.Field<F5> f5;

    private final StructMember.Field<F6> f6;

    private final StructMember.Field<F7> f7;

    private final StructMember.Field<F8> f8;

    private final StructMember.Field<F9> f9;

    private final StructMember.Field<F10> f10;

    private final StructMember.Field<F11> f11;

    private final StructMember.Field<F12> f12;

    private final StructMember.Field<F13> f13;

    private final StructMember.Field<F14> f14;

    StructBuilder15(String name, StructDefinition layout, StructMember.Field<F0> f0,
            StructMember.Field<F1> f1, StructMember.Field<F2> f2, StructMember.Field<F3> f3,
            StructMember.Field<F4> f4, StructMember.Field<F5> f5, StructMember.Field<F6> f6,
            StructMember.Field<F7> f7, StructMember.Field<F8> f8, StructMember.Field<F9> f9,
            StructMember.Field<F10> f10, StructMember.Field<F11> f11, StructMember.Field<F12> f12,
            StructMember.Field<F13> f13, StructMember.Field<F14> f14) {
        this.name = name;
        this.layout = layout;
        this.f0 = f0;
        this.f1 = f1;
        this.f2 = f2;
        this.f3 = f3;
        this.f4 = f4;
        this.f5 = f5;
        this.f6 = f6;
        this.f7 = f7;
        this.f8 = f8;
        this.f9 = f9;
        this.f10 = f10;
        this.f11 = f11;
        this.f12 = f12;
        this.f13 = f13;
        this.f14 = f14;
    }

    public <F15> StructBuilder16<F0, F1, F2, F3, F4, F5, F6, F7, F8, F9, F10, F11, F12, F13, F14, F15> field(
            String name, Type.Sized<F15> type) {
        var placement = layout.append(name, type);
        return new StructBuilder16<>(this.name, placement.layout(), f0, f1, f2, f3, f4, f5, f6, f7, f8, f9, f10, f11, f12, f13, f14, placement.field());
    }

    public StructBuilder15<F0, F1, F2, F3, F4, F5, F6, F7, F8, F9, F10, F11, F12, F13, F14> padding(
            long bytes) {
        return new StructBuilder15<>(this.name, layout.padding(bytes), f0, f1, f2, f3, f4, f5, f6, f7, f8, f9, f10, f11, f12, f13, f14);
    }

    public <T> StructType<T> build(
            StructBuilder15.Constructor<F0, F1, F2, F3, F4, F5, F6, F7, F8, F9, F10, F11, F12, F13, F14, T> constructor) {
        return layout.build((pointer, resolved) -> constructor.create(f0.read(pointer, resolved), f1.read(pointer, resolved), f2.read(pointer, resolved), f3.read(pointer, resolved), f4.read(pointer, resolved), f5.read(pointer, resolved), f6.read(pointer, resolved), f7.read(pointer, resolved), f8.read(pointer, resolved), f9.read(pointer, resolved), f10.read(pointer, resolved), f11.read(pointer, resolved), f12.read(pointer, resolved), f13.read(pointer, resolved), f14.read(pointer, resolved)));
    }

    @FunctionalInterface
    public interface Constructor<F0, F1, F2, F3, F4, F5, F6, F7, F8, F9, F10, F11, F12, F13, F14, T> {
        T create(F0 f0, F1 f1, F2 f2, F3 f3, F4 f4, F5 f5, F6 f6, F7 f7, F8 f8, F9 f9, F10 f10,
                F11 f11, F12 f12, F13 f13, F14 f14);
    }
}
