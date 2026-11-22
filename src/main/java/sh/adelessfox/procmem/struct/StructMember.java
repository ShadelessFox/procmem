// SPDX-License-Identifier: MIT
// SPDX-FileCopyrightText: 2026 ShadelessFox

package sh.adelessfox.procmem.struct;

import sh.adelessfox.procmem.Pointer;
import sh.adelessfox.procmem.Type;

sealed interface StructMember {
    record Field<T>(int index, String struct, String name, Type.Sized<T> type) implements StructMember {
        T read(Pointer pointer, StructDefinition.StructLayout resolved) {
            try {
                return type.read(pointer.add(resolved.offsets()[index]));
            } catch (Exception e) {
                throw new IllegalStateException("Failed to read field " + struct + '.' + name + ": " + e.getMessage(), e);
            }
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
