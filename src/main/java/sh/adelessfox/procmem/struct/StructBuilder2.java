// SPDX-License-Identifier: MIT
// SPDX-FileCopyrightText: 2026 ShadelessFox

package sh.adelessfox.procmem.struct;

import sh.adelessfox.procmem.Type;

public final class StructBuilder2<A, B> {
  private final String name;

  private final StructDefinition layout;

  private final StructMember.Field<A> a;

  private final StructMember.Field<B> b;

  StructBuilder2(String name, StructDefinition layout, StructMember.Field<A> a,
      StructMember.Field<B> b) {
    this.name = name;
    this.layout = layout;
    this.a = a;
    this.b = b;
  }

  public <C> StructBuilder3<A, B, C> field(String name, Type.Sized<C> type) {
    var placement = layout.append(name, type);
    return new StructBuilder3<A, B, C>(this.name, placement.layout(), a, b, placement.field());
  }

  public StructBuilder2<A, B> padding(long bytes) {
    return new StructBuilder2<A, B>(this.name, layout.padding(bytes), a, b);
  }

  public <T> Type.Sized<T> build(StructBuilder2.Constructor<A, B, T> constructor) {
    return layout.build(name, (pointer, resolved) -> constructor.create(a.read(pointer, resolved), b.read(pointer, resolved)));
  }

  @FunctionalInterface
  public interface Constructor<A, B, T> {
    T create(A a, B b);
  }
}
