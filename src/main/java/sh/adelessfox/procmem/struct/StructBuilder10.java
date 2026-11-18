// SPDX-License-Identifier: MIT
// SPDX-FileCopyrightText: 2026 ShadelessFox

package sh.adelessfox.procmem.struct;

import sh.adelessfox.procmem.Type;

public final class StructBuilder10<A, B, C, D, E, F, G, H, I, J> {
  private final String name;

  private final StructDefinition layout;

  private final StructMember.Field<A> a;

  private final StructMember.Field<B> b;

  private final StructMember.Field<C> c;

  private final StructMember.Field<D> d;

  private final StructMember.Field<E> e;

  private final StructMember.Field<F> f;

  private final StructMember.Field<G> g;

  private final StructMember.Field<H> h;

  private final StructMember.Field<I> i;

  private final StructMember.Field<J> j;

  StructBuilder10(String name, StructDefinition layout, StructMember.Field<A> a,
      StructMember.Field<B> b, StructMember.Field<C> c, StructMember.Field<D> d,
      StructMember.Field<E> e, StructMember.Field<F> f, StructMember.Field<G> g,
      StructMember.Field<H> h, StructMember.Field<I> i, StructMember.Field<J> j) {
    this.name = name;
    this.layout = layout;
    this.a = a;
    this.b = b;
    this.c = c;
    this.d = d;
    this.e = e;
    this.f = f;
    this.g = g;
    this.h = h;
    this.i = i;
    this.j = j;
  }

  public <K> StructBuilder11<A, B, C, D, E, F, G, H, I, J, K> field(String name,
      Type.Sized<K> type) {
    var placement = layout.append(name, type);
    return new StructBuilder11<A, B, C, D, E, F, G, H, I, J, K>(this.name, placement.layout(), a, b, c, d, e, f, g, h, i, j, placement.field());
  }

  public StructBuilder10<A, B, C, D, E, F, G, H, I, J> padding(long bytes) {
    return new StructBuilder10<A, B, C, D, E, F, G, H, I, J>(this.name, layout.padding(bytes), a, b, c, d, e, f, g, h, i, j);
  }

  public <T> Type.Sized<T> build(
      StructBuilder10.Constructor<A, B, C, D, E, F, G, H, I, J, T> constructor) {
    return layout.build(name, (pointer, resolved) -> constructor.create(a.read(pointer, resolved), b.read(pointer, resolved), c.read(pointer, resolved), d.read(pointer, resolved), e.read(pointer, resolved), f.read(pointer, resolved), g.read(pointer, resolved), h.read(pointer, resolved), i.read(pointer, resolved), j.read(pointer, resolved)));
  }

  @FunctionalInterface
  public interface Constructor<A, B, C, D, E, F, G, H, I, J, T> {
    T create(A a, B b, C c, D d, E e, F f, G g, H h, I i, J j);
  }
}
