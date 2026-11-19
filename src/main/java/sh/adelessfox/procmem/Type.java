// SPDX-License-Identifier: MIT
// SPDX-FileCopyrightText: 2026 ShadelessFox

package sh.adelessfox.procmem;

import sh.adelessfox.procmem.struct.StructBuilder0;

import java.util.Optional;
import java.util.function.Function;

/**
 * A unit of data that describes how to read a value of type {@code T} from a {@link Pointer}.
 * <p>
 * To create a new instance, use the static factory methods {@code of}.
 * <p>
 * For composite types, use the {@link #struct(String)} method to create a struct builder.
 *
 * @param <T> the type of value
 * @see #of(Function, long, long, String)
 * @see #struct(String)
 */
@SuppressWarnings("StaticInitializerReferencesSubClass")
public abstract class Type<T> {
    /** A sized type that represents a single byte. */
    public static final Sized<Byte> BYTE = of(Pointer::readByte, Byte.BYTES, Byte.BYTES, "byte");
    /** A sized type that represents a single short. */
    public static final Sized<Short> SHORT = of(Pointer::readShort, Short.BYTES, Short.BYTES, "short");
    /** A sized type that represents a single int. */
    public static final Sized<Integer> INT = of(Pointer::readInt, Integer.BYTES, Integer.BYTES, "int");
    /** A sized type that represents a single long. */
    public static final Sized<Long> LONG = of(Pointer::readLong, Long.BYTES, Long.BYTES, "long");
    /** A sized type that represents a single float. */
    public static final Sized<Float> FLOAT = of(Pointer::readFloat, Float.BYTES, Float.BYTES, "float");
    /** A sized type that represents a single double. */
    public static final Sized<Double> DOUBLE = of(Pointer::readDouble, Double.BYTES, Double.BYTES, "double");

    /** A sized type that represents a raw pointer. */
    public static final Sized<Pointer> POINTER = new Sized<>() {
        @Override
        public Pointer read(Pointer pointer) {
            return pointer.deref();
        }

        @Override
        public TypeLayout layout(MemoryLayout memoryLayout) {
            return new TypeLayout(
                memoryLayout.pointerSize(),
                memoryLayout.pointerSize()
            );
        }

        @Override
        public String toString() {
            return "pointer";
        }
    };

    /** A sized type that represents {@code const char *}. */
    public static final Type.Sized<Optional<String>> C_STRING =
        Type.of(Pointer::readCString, "CString")
            .nullable()
            .indirect();

    /**
     * Creates a new {@link Sized sized type} with the given reader, size, alignment, and name.
     *
     * @param reader    the function to read a value of type {@code T} from a {@link Pointer}
     * @param size      the size of the type in bytes
     * @param alignment the alignment of the type in bytes
     * @param name      the name of the type
     * @param <T>       the type of value
     * @return a new {@link Sized} type
     */
    public static <T> Sized<T> of(Function<Pointer, T> reader, long size, long alignment, String name) {
        if (size <= 0) {
            throw new IllegalArgumentException("size must be greater than 0");
        }
        if (alignment <= 0) {
            throw new IllegalArgumentException("alignment must be greater than 0");
        }
        if ((alignment & alignment - 1) != 0) {
            throw new IllegalArgumentException("alignment must be a power of 2");
        }
        var layout = new TypeLayout(size, alignment);
        return new Sized<>() {
            @Override
            public T read(Pointer pointer) {
                return reader.apply(pointer);
            }

            @Override
            public TypeLayout layout(MemoryLayout target) {
                return layout;
            }

            @Override
            public String toString() {
                return name;
            }
        };
    }

    /**
     * Creates a new {@link Sized sized type} with the given reader, size, and alignment.
     * <p>
     * The name of the type is derived from the size.
     *
     * @param reader    the function to read a value of type {@code T} from a {@link Pointer}
     * @param size      the size of the type in bytes
     * @param alignment the alignment of the type in bytes
     * @param <T>       the type of value
     * @return a new {@link Sized} type
     */
    public static <T> Sized<T> of(Function<Pointer, T> reader, long size, long alignment) {
        return of(reader, size, alignment, "[" + size + " bytes]");
    }

    /**
     * Creates a new {@link Type} with the given reader and name.
     *
     * @param reader the function to read a value of type {@code T} from a {@link Pointer}
     * @param name   the name of the type
     * @param <T>    the type of value
     * @return a new {@link Type} type
     */
    public static <T> Type<T> of(Function<Pointer, T> reader, String name) {
        return new Type<>() {
            @Override
            public T read(Pointer pointer) {
                return reader.apply(pointer);
            }

            @Override
            public String toString() {
                return name;
            }
        };
    }

    /**
     * Creates a new struct builder with given name for the type.
     * <p>
     * Struct builders provide a convenient way to define struct types with multiple fields and padding:
     * {@snippet lang = java:
     * record MyStruct(int a, long b) {}
     *
     * Type.Sized<MyStruct> myStructType = Type.struct("MyStruct")
     *     .field("a", Type.INT)
     *     .padding(4) // optional padding
     *     .field("b", Type.LONG)
     *     .build(MyStruct::new);
     *
     * Pointer pointer = null;
     * MyStruct struct = myStructType.read(pointer);
     *}
     * <p>
     * The traditional approach would require manually calculating offsets and alignments
     * for each field, which can be error-prone and tedious.
     *
     * @param name the name of the struct type
     * @return a new {@link StructBuilder0} to build the struct type
     */
    public static StructBuilder0 struct(String name) {
        return StructBuilder0.create(name);
    }

    /**
     * Reads a value from the given {@link Pointer}.
     *
     * @param pointer the pointer to read from
     * @return the value read from the pointer
     */
    public abstract T read(Pointer pointer);

    /**
     * Creates a new {@link Ref reference} to the value at the given {@link Pointer}.
     *
     * @param pointer the pointer to the value
     * @return a new reference to the value at the pointer
     */
    public Ref<T> at(Pointer pointer) {
        return new Ref<>(this, pointer);
    }

    /**
     * Creates a new {@link Sized sized type} that represents a reference to the value of this type.
     *
     * @return a new sized type that represents a reference to the value of this type
     */
    public Sized<Ref<T>> ref() {
        return new Sized<>() {
            @Override
            public Ref<T> read(Pointer pointer) {
                return Type.this.at(pointer.deref());
            }

            @Override
            public TypeLayout layout(MemoryLayout target) {
                return new TypeLayout(target.pointerSize(), target.pointerSize());
            }

            @Override
            public String toString() {
                return "Ref<" + Type.this + ">";
            }
        };
    }

    public <R> Type<R> map(Function<? super T, ? extends R> mapper) {
        return Type.of(p -> mapper.apply(read(p)), toString());
    }

    public Type<Optional<T>> nullable() {
        return Type.of(p -> p.isNotNull() ? Optional.of(read(p)) : Optional.empty(), "Nullable<" + this + ">");
    }

    public Type<Ref<T>> deferred() {
        return Type.of(this::at, toString());
    }

    public Sized<T> indirect() {
        return ref().map(Ref::read);
    }

    /**
     * A sized type that has a known size and alignment.
     *
     * @param <T> the type of value
     */
    public abstract static class Sized<T> extends Type<T> {
        /**
         * Returns the {@link TypeLayout layout} of this type for the given {@link MemoryLayout target layout}.
         *
         * @param target the memory layout to get the layout for
         * @return the layout of this type for the given memory layout
         */
        public abstract TypeLayout layout(MemoryLayout target);

        /**
         * Creates a new {@link Slice slice} of the given length at the given {@link Pointer}.
         *
         * @param pointer the pointer to the start of the slice
         * @param length  the length of the slice
         * @return a new slice of the given length at the given pointer
         */
        public Slice<T> slice(Pointer pointer, int length) {
            return new Slice<>(this, pointer, length);
        }

        public Sized<Slice<T>> slice(int length) {
            return new Sized<>() {
                @Override
                public TypeLayout layout(MemoryLayout target) {
                    var elementLayout = Sized.this.layout(target);
                    return new TypeLayout(Math.multiplyExact(elementLayout.size(), length), elementLayout.alignment());
                }

                @Override
                public Slice<T> read(Pointer pointer) {
                    return Sized.this.slice(pointer, length);
                }
            };
        }

        @Override
        public <R> Sized<R> map(Function<? super T, ? extends R> mapper) {
            return sized(super.map(mapper), this::layout);
        }

        @Override
        public Sized<Optional<T>> nullable() {
            return sized(super.nullable(), this::layout);
        }

        @Override
        public Sized<Ref<T>> deferred() {
            return sized(super.deferred(), this::layout);
        }

        private static <T> Sized<T> sized(Type<T> delegate, Function<MemoryLayout, TypeLayout> layout) {
            return new Sized<>() {
                @Override
                public T read(Pointer pointer) {
                    return delegate.read(pointer);
                }

                @Override
                public TypeLayout layout(MemoryLayout target) {
                    return layout.apply(target);
                }

                @Override
                public String toString() {
                    return delegate.toString();
                }
            };
        }
    }
}
