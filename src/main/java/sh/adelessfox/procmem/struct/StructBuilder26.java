package sh.adelessfox.procmem.struct;

import sh.adelessfox.procmem.Type;

public final class StructBuilder26<F0, F1, F2, F3, F4, F5, F6, F7, F8, F9, F10, F11, F12, F13, F14, F15, F16, F17, F18, F19, F20, F21, F22, F23, F24, F25> {
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

    private final StructMember.Field<F15> f15;

    private final StructMember.Field<F16> f16;

    private final StructMember.Field<F17> f17;

    private final StructMember.Field<F18> f18;

    private final StructMember.Field<F19> f19;

    private final StructMember.Field<F20> f20;

    private final StructMember.Field<F21> f21;

    private final StructMember.Field<F22> f22;

    private final StructMember.Field<F23> f23;

    private final StructMember.Field<F24> f24;

    private final StructMember.Field<F25> f25;

    StructBuilder26(String name, StructDefinition layout, StructMember.Field<F0> f0,
            StructMember.Field<F1> f1, StructMember.Field<F2> f2, StructMember.Field<F3> f3,
            StructMember.Field<F4> f4, StructMember.Field<F5> f5, StructMember.Field<F6> f6,
            StructMember.Field<F7> f7, StructMember.Field<F8> f8, StructMember.Field<F9> f9,
            StructMember.Field<F10> f10, StructMember.Field<F11> f11, StructMember.Field<F12> f12,
            StructMember.Field<F13> f13, StructMember.Field<F14> f14, StructMember.Field<F15> f15,
            StructMember.Field<F16> f16, StructMember.Field<F17> f17, StructMember.Field<F18> f18,
            StructMember.Field<F19> f19, StructMember.Field<F20> f20, StructMember.Field<F21> f21,
            StructMember.Field<F22> f22, StructMember.Field<F23> f23, StructMember.Field<F24> f24,
            StructMember.Field<F25> f25) {
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
        this.f15 = f15;
        this.f16 = f16;
        this.f17 = f17;
        this.f18 = f18;
        this.f19 = f19;
        this.f20 = f20;
        this.f21 = f21;
        this.f22 = f22;
        this.f23 = f23;
        this.f24 = f24;
        this.f25 = f25;
    }

    public <F26> StructBuilder27<F0, F1, F2, F3, F4, F5, F6, F7, F8, F9, F10, F11, F12, F13, F14, F15, F16, F17, F18, F19, F20, F21, F22, F23, F24, F25, F26> field(
            String name, Type.Sized<F26> type) {
        var placement = layout.append(name, type);
        return new StructBuilder27<>(this.name, placement.layout(), f0, f1, f2, f3, f4, f5, f6, f7, f8, f9, f10, f11, f12, f13, f14, f15, f16, f17, f18, f19, f20, f21, f22, f23, f24, f25, placement.field());
    }

    public StructBuilder26<F0, F1, F2, F3, F4, F5, F6, F7, F8, F9, F10, F11, F12, F13, F14, F15, F16, F17, F18, F19, F20, F21, F22, F23, F24, F25> padding(
            long bytes) {
        return new StructBuilder26<>(this.name, layout.padding(bytes), f0, f1, f2, f3, f4, f5, f6, f7, f8, f9, f10, f11, f12, f13, f14, f15, f16, f17, f18, f19, f20, f21, f22, f23, f24, f25);
    }

    public <T> StructType<T> build(
            StructBuilder26.Constructor<F0, F1, F2, F3, F4, F5, F6, F7, F8, F9, F10, F11, F12, F13, F14, F15, F16, F17, F18, F19, F20, F21, F22, F23, F24, F25, T> constructor) {
        return layout.build((pointer, resolved) -> constructor.create(f0.read(pointer, resolved), f1.read(pointer, resolved), f2.read(pointer, resolved), f3.read(pointer, resolved), f4.read(pointer, resolved), f5.read(pointer, resolved), f6.read(pointer, resolved), f7.read(pointer, resolved), f8.read(pointer, resolved), f9.read(pointer, resolved), f10.read(pointer, resolved), f11.read(pointer, resolved), f12.read(pointer, resolved), f13.read(pointer, resolved), f14.read(pointer, resolved), f15.read(pointer, resolved), f16.read(pointer, resolved), f17.read(pointer, resolved), f18.read(pointer, resolved), f19.read(pointer, resolved), f20.read(pointer, resolved), f21.read(pointer, resolved), f22.read(pointer, resolved), f23.read(pointer, resolved), f24.read(pointer, resolved), f25.read(pointer, resolved)));
    }

    @FunctionalInterface
    public interface Constructor<F0, F1, F2, F3, F4, F5, F6, F7, F8, F9, F10, F11, F12, F13, F14, F15, F16, F17, F18, F19, F20, F21, F22, F23, F24, F25, T> {
        T create(F0 f0, F1 f1, F2 f2, F3 f3, F4 f4, F5 f5, F6 f6, F7 f7, F8 f8, F9 f9, F10 f10,
                F11 f11, F12 f12, F13 f13, F14 f14, F15 f15, F16 f16, F17 f17, F18 f18, F19 f19,
                F20 f20, F21 f21, F22 f22, F23 f23, F24 f24, F25 f25);
    }
}
