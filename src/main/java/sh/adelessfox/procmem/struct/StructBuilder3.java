// SPDX-License-Identifier: MIT
// SPDX-FileCopyrightText: 2026 ShadelessFox

package sh.adelessfox.procmem.struct;

import sh.adelessfox.procmem.Type;

public final class StructBuilder3<A, B, C> {
  private final String name;

  private final StructDefinition layout;

  private final StructMember.Field<A> a;

  private final StructMember.Field<B> b;

  private final StructMember.Field<C> c;

  StructBuilder3(String name, StructDefinition layout, StructMember.Field<A> a,
      StructMember.Field<B> b, StructMember.Field<C> c) {
    this.name = name;
    this.layout = layout;
    this.a = a;
    this.b = b;
    this.c = c;
  }

  public <D> StructBuilder4<A, B, C, D> field(String name, Type.Sized<D> type) {
    var placement = layout.append(name, type);
    return new StructBuilder4<A, B, C, D>(this.name, placement.layout(), a, b, c, placement.field());
  }

  public StructBuilder3<A, B, C> padding(long bytes) {
    return new StructBuilder3<A, B, C>(this.name, layout.padding(bytes), a, b, c);
  }

  public <T> Type.Sized<T> build(StructBuilder3.Constructor<A, B, C, T> constructor) {
    return layout.build(name, (pointer, resolved) -> constructor.create(a.read(pointer, resolved), b.read(pointer, resolved), c.read(pointer, resolved)));
  }

  @FunctionalInterface
  public interface Constructor<A, B, C, T> {
    T create(A a, B b, C c);
  }
}
