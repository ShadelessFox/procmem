// SPDX-License-Identifier: MIT
// SPDX-FileCopyrightText: 2026 ShadelessFox

package sh.adelessfox.procmem.struct;

import sh.adelessfox.procmem.Type;

public final class StructBuilder5<A, B, C, D, E> {
  private final String name;

  private final StructDefinition layout;

  private final StructMember.Field<A> a;

  private final StructMember.Field<B> b;

  private final StructMember.Field<C> c;

  private final StructMember.Field<D> d;

  private final StructMember.Field<E> e;

  StructBuilder5(String name, StructDefinition layout, StructMember.Field<A> a,
      StructMember.Field<B> b, StructMember.Field<C> c, StructMember.Field<D> d,
      StructMember.Field<E> e) {
    this.name = name;
    this.layout = layout;
    this.a = a;
    this.b = b;
    this.c = c;
    this.d = d;
    this.e = e;
  }

  public <F> StructBuilder6<A, B, C, D, E, F> field(String name, Type.Sized<F> type) {
    var placement = layout.append(name, type);
    return new StructBuilder6<A, B, C, D, E, F>(this.name, placement.layout(), a, b, c, d, e, placement.field());
  }

  public StructBuilder5<A, B, C, D, E> padding(long bytes) {
    return new StructBuilder5<A, B, C, D, E>(this.name, layout.padding(bytes), a, b, c, d, e);
  }

  public <T> Type.Sized<T> build(StructBuilder5.Constructor<A, B, C, D, E, T> constructor) {
    return layout.build(name, (pointer, resolved) -> constructor.create(a.read(pointer, resolved), b.read(pointer, resolved), c.read(pointer, resolved), d.read(pointer, resolved), e.read(pointer, resolved)));
  }

  @FunctionalInterface
  public interface Constructor<A, B, C, D, E, T> {
    T create(A a, B b, C c, D d, E e);
  }
}
