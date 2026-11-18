// SPDX-License-Identifier: MIT
// SPDX-FileCopyrightText: 2026 ShadelessFox

package sh.adelessfox.procmem;

import sh.adelessfox.procmem.struct.StructBuilder0;

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
public interface Type<T> {
    /** A sized type that represents a single byte. */
    Sized<Byte> BYTE = of(Pointer::readByte, Byte.BYTES, Byte.BYTES, "byte");
    /** A sized type that represents a single short. */
    Sized<Short> SHORT = of(Pointer::readShort, Short.BYTES, Short.BYTES, "short");
    /** A sized type that represents a single int. */
    Sized<Integer> INT = of(Pointer::readInt, Integer.BYTES, Integer.BYTES, "int");
    /** A sized type that represents a single long. */
    Sized<Long> LONG = of(Pointer::readLong, Long.BYTES, Long.BYTES, "long");
    /** A sized type that represents a single float. */
    Sized<Float> FLOAT = of(Pointer::readFloat, Float.BYTES, Float.BYTES, "float");
    /** A sized type that represents a single double. */
    Sized<Double> DOUBLE = of(Pointer::readDouble, Double.BYTES, Double.BYTES, "double");

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
    static <T> Sized<T> of(Function<Pointer, T> reader, long size, long alignment, String name) {
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
    static <T> Sized<T> of(Function<Pointer, T> reader, long size, long alignment) {
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
    static <T> Type<T> of(Function<Pointer, T> reader, String name) {
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
    static StructBuilder0 struct(String name) {
        return StructBuilder0.create(name);
    }

    /**
     * Reads a value from the given {@link Pointer}.
     *
     * @param pointer the pointer to read from
     * @return the value read from the pointer
     */
    T read(Pointer pointer);

    /**
     * Creates a new {@link Ref reference} to the value at the given {@link Pointer}.
     *
     * @param pointer the pointer to the value
     * @return a new reference to the value at the pointer
     */
    default Ref<T> at(Pointer pointer) {
        return new Ref<>(this, pointer);
    }

    /**
     * Creates a new {@link Sized sized type} that represents a reference to the value of this type.
     *
     * @return a new sized type that represents a reference to the value of this type
     */
    default Sized<Ref<T>> ref() {
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
                return Type.this + "*";
            }
        };
    }

    /**
     * A sized type that has a known size and alignment.
     *
     * @param <T> the type of value
     */
    interface Sized<T> extends Type<T> {
        /**
         * Returns the {@link TypeLayout layout} of this type for the given {@link MemoryLayout target layout}.
         *
         * @param target the memory layout to get the layout for
         * @return the layout of this type for the given memory layout
         */
        TypeLayout layout(MemoryLayout target);

        /**
         * Creates a new {@link Slice slice} of the given length at the given {@link Pointer}.
         *
         * @param pointer the pointer to the start of the slice
         * @param length  the length of the slice
         * @return a new slice of the given length at the given pointer
         */
        default Slice<T> slice(Pointer pointer, int length) {
            return new Slice<>(this, pointer, length);
        }
    }
}
