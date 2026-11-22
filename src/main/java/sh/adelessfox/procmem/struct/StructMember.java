// SPDX-License-Identifier: MIT
// SPDX-FileCopyrightText: 2026 ShadelessFox

package sh.adelessfox.procmem.struct;

import sh.adelessfox.procmem.Pointer;
import sh.adelessfox.procmem.Type;

/**
 * A declared field or explicit padding within a structure.
 */
public sealed interface StructMember {
    /**
     * A typed field handle belonging to definitions that contain this exact field.
     */
    final class Field<T> implements StructMember {
        private final int index;
        private final String struct;
        private final String name;
        private final Type.Sized<T> type;

        Field(int index, String struct, String name, Type.Sized<T> type) {
            this.index = index;
            this.struct = struct;
            this.name = name;
            this.type = type;
        }

        public String name() {
            return name;
        }

        public Type.Sized<T> type() {
            return type;
        }

        int index() {
            return index;
        }

        T read(Pointer pointer, StructLayout resolved) {
            long offset = resolved.offset(this);
            try {
                return type.read(pointer.add(offset));
            } catch (Exception e) {
                throw new IllegalStateException("Failed to read field " + struct + '.' + name + ": " + e.getMessage(), e);
            }
        }

        @Override
        public String toString() {
            return struct + '.' + name + ": " + type;
        }
    }

    record Padding(long bytes) implements StructMember {
        public Padding {
            if (bytes < 0) {
                throw new IllegalArgumentException("padding must be non-negative");
            }
        }
    }
}
