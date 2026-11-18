// SPDX-License-Identifier: MIT
// SPDX-FileCopyrightText: 2026 ShadelessFox

package sh.adelessfox.procmem.struct;

import sh.adelessfox.procmem.Type;

public final class StructBuilder1<A> {
  private final String name;

  private final StructDefinition layout;

  private final StructMember.Field<A> a;

  StructBuilder1(String name, StructDefinition layout, StructMember.Field<A> a) {
    this.name = name;
    this.layout = layout;
    this.a = a;
  }

  public <B> StructBuilder2<A, B> field(String name, Type.Sized<B> type) {
    var placement = layout.append(name, type);
    return new StructBuilder2<A, B>(this.name, placement.layout(), a, placement.field());
  }

  public StructBuilder1<A> padding(long bytes) {
    return new StructBuilder1<A>(this.name, layout.padding(bytes), a);
  }

  public <T> Type.Sized<T> build(StructBuilder1.Constructor<A, T> constructor) {
    return layout.build(name, (pointer, resolved) -> constructor.create(a.read(pointer, resolved)));
  }

  @FunctionalInterface
  public interface Constructor<A, T> {
    T create(A a);
  }
}
