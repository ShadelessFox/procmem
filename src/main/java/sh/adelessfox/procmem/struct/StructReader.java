// SPDX-License-Identifier: MIT
// SPDX-FileCopyrightText: 2026 ShadelessFox

package sh.adelessfox.procmem.struct;

import sh.adelessfox.procmem.Pointer;

/**
 * Reads fields while a bound structure type is being decoded. Each call reads the field anew.
 */
public final class StructReader {
    private final Pointer pointer;
    private final StructLayout layout;

    StructReader(Pointer pointer, StructLayout layout) {
        this.pointer = pointer;
        this.layout = layout;
    }

    public <T> T read(StructMember.Field<T> field) {
        return field.read(pointer, layout);
    }
}
